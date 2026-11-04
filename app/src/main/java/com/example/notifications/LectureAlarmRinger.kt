package com.example.notifications

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.sin

enum class AlarmToneOption(
    val id: String,
    val displayName: String,
    val subtitle: String
) {
    AURORA_CHIME("AURORA_CHIME", "Aurora Crystal Chime", "Crystalline ascending C5–E5–G5–C6 bell"),
    SUNRISE_HARP("SUNRISE_HARP", "Gentle Sunrise Harp", "Warm golden Fmaj7 morning arpeggio"),
    ACADEMIC_BELL("ACADEMIC_BELL", "University Tower Bell", "Classic resonant campus chime cadence"),
    QUANTUM_PULSE("QUANTUM_PULSE", "Quantum Digital Pulse", "Crisp high-energy double-beep alert"),
    ZEN_BOWL("ZEN_BOWL", "Zen Singing Bowl", "Calm 432Hz harmonic meditation chime"),
    SYSTEM_ALARM("SYSTEM_ALARM", "Phone System Alarm", "Device default Android alarm ringtone"),
    CUSTOM_AUDIO("CUSTOM_AUDIO", "Custom Audio File", "Your uploaded MP3 / audio alarm");

    companion object {
        fun fromId(id: String?): AlarmToneOption {
            if (id.isNullOrBlank()) return AURORA_CHIME
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: AURORA_CHIME
        }
    }
}

data class ActiveAlarmPayload(
    val notificationId: Int,
    val title: String,
    val message: String,
    val category: String,
    val courseCode: String,
    val timeText: String,
    val eventId: Long,
    val alarmToneId: String = AlarmToneOption.AURORA_CHIME.id,
    val customAlarmPath: String = ""
)

/**
 * Single-instance, thread-safe lecture & exam alarm ringer.
 * Guarantees strictly ONE audio stream plays at a time (eliminating overlapping double alarms)
 * and supports customizable built-in harmonic tones, system alarm, and user-uploaded audio files.
 */
object LectureAlarmRinger {

    private val ringerLock = Any()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var ringJob: Job? = null
    private var activeMediaPlayer: MediaPlayer? = null
    private var synthTrack: AudioTrack? = null
    private var activeVibrator: Vibrator? = null

    private var lastTriggerNotificationId: Int = -1
    private var lastTriggerTimestampMs: Long = 0L

    private val _activeAlarmFlow = MutableStateFlow<ActiveAlarmPayload?>(null)
    val activeAlarmFlow: StateFlow<ActiveAlarmPayload?> = _activeAlarmFlow.asStateFlow()

    private val _previewingToneId = MutableStateFlow<String?>(null)
    val previewingToneId: StateFlow<String?> = _previewingToneId.asStateFlow()

    val isRinging: Boolean
        get() = synchronized(ringerLock) { ringJob?.isActive == true && _activeAlarmFlow.value != null }

    fun startRinging(
        context: Context,
        payload: ActiveAlarmPayload,
        playSound: Boolean = true,
        vibrate: Boolean = true,
        gentleFadeIn: Boolean = true
    ) {
        synchronized(ringerLock) {
            val now = System.currentTimeMillis()
            // Deduplicate rapid duplicate broadcasts for the same alarm so it never restarts a second stream
            if (
                _activeAlarmFlow.value?.notificationId == payload.notificationId &&
                ringJob?.isActive == true &&
                (now - lastTriggerTimestampMs) < 15_000L
            ) {
                return
            }

            stopInternal(context)
            lastTriggerNotificationId = payload.notificationId
            lastTriggerTimestampMs = now
            _activeAlarmFlow.value = payload

            if (vibrate) {
                startVibration(context)
            }

            if (playSound) {
                val tone = AlarmToneOption.fromId(payload.alarmToneId)
                ringJob = scope.launch {
                    playExclusiveSingleAlarmStream(
                        context = context.applicationContext,
                        tone = tone,
                        customFilePath = payload.customAlarmPath,
                        maxDurationSeconds = 60,
                        gentleFadeIn = gentleFadeIn,
                        onFinished = { stopRinging(context.applicationContext) }
                    )
                }
            }
        }
    }

    /**
     * Plays a clean 5-second single-stream preview of the selected alarm tone in Settings / Lecture Dialog.
     * Tapping preview again on the same tone immediately stops it.
     */
    fun previewTone(
        context: Context,
        toneId: String,
        customFilePath: String = ""
    ) {
        synchronized(ringerLock) {
            val wasSamePreview = _previewingToneId.value == toneId && ringJob?.isActive == true
            stopInternal(context)
            if (wasSamePreview) {
                return
            }

            val tone = AlarmToneOption.fromId(toneId)
            _previewingToneId.value = tone.id
            ringJob = scope.launch {
                playExclusiveSingleAlarmStream(
                    context = context.applicationContext,
                    tone = tone,
                    customFilePath = customFilePath,
                    maxDurationSeconds = 5,
                    onFinished = {
                        synchronized(ringerLock) {
                            if (_previewingToneId.value == tone.id) {
                                stopInternal(context.applicationContext)
                            }
                        }
                    }
                )
            }
        }
    }

    fun stopPreview(context: Context? = null) {
        synchronized(ringerLock) {
            if (_previewingToneId.value != null) {
                stopInternal(context)
            }
        }
    }

    fun stopRinging(context: Context? = null) {
        synchronized(ringerLock) {
            stopInternal(context)
        }
    }

    private suspend fun playExclusiveSingleAlarmStream(
        context: Context,
        tone: AlarmToneOption,
        customFilePath: String,
        maxDurationSeconds: Int,
        gentleFadeIn: Boolean = false,
        onFinished: () -> Unit
    ) {
        // Path A: User-selected Custom Audio File OR System Alarm via single MediaPlayer
        if (tone == AlarmToneOption.CUSTOM_AUDIO || tone == AlarmToneOption.SYSTEM_ALARM) {
            val mediaStarted = tryStartSingleMediaPlayer(context, tone, customFilePath, gentleFadeIn)
            if (mediaStarted) {
                var elapsedMs = 0L
                val limitMs = maxDurationSeconds * 1000L
                while (scope.isActive && elapsedMs < limitMs) {
                    val mpRef = synchronized(ringerLock) { activeMediaPlayer } ?: return
                    if (gentleFadeIn && elapsedMs <= 8000L) {
                        val vol = (0.18f + 0.82f * (elapsedMs.toFloat() / 8000f)).coerceIn(0.18f, 1.0f)
                        runCatching { mpRef.setVolume(vol, vol) }
                    }
                    delay(250L)
                    elapsedMs += 250L
                }
                onFinished()
                return
            }
        }

        // Path B: Synthesized Melodic Alarm Tone (or deterministic fallback if MediaPlayer failed)
        val effectiveSynthTone = if (tone == AlarmToneOption.CUSTOM_AUDIO || tone == AlarmToneOption.SYSTEM_ALARM) {
            AlarmToneOption.AURORA_CHIME
        } else {
            tone
        }
        playSynthesizedToneLoop(effectiveSynthTone, maxDurationSeconds, gentleFadeIn)
        if (scope.isActive) {
            onFinished()
        }
    }

    private fun tryStartSingleMediaPlayer(
        context: Context,
        tone: AlarmToneOption,
        customFilePath: String,
        gentleFadeIn: Boolean = false
    ): Boolean {
        synchronized(ringerLock) {
            releaseMediaPlayerLocked()
            val mp = MediaPlayer()
            return try {
                mp.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                when (tone) {
                    AlarmToneOption.CUSTOM_AUDIO -> {
                        val file = File(customFilePath)
                        if (!file.exists() || file.length() == 0L) {
                            mp.release()
                            return false
                        }
                        mp.setDataSource(file.absolutePath)
                    }
                    AlarmToneOption.SYSTEM_ALARM -> {
                        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        if (uri == null) {
                            mp.release()
                            return false
                        }
                        mp.setDataSource(context, uri)
                    }
                    else -> {
                        mp.release()
                        return false
                    }
                }
                mp.isLooping = true
                mp.prepare()
                if (gentleFadeIn) {
                    runCatching { mp.setVolume(0.18f, 0.18f) }
                }
                mp.start()
                activeMediaPlayer = mp
                true
            } catch (_: Exception) {
                try {
                    mp.reset()
                    mp.release()
                } catch (_: Exception) {
                }
                activeMediaPlayer = null
                false
            }
        }
    }

    private suspend fun playSynthesizedToneLoop(
        tone: AlarmToneOption,
        maxDurationSeconds: Int,
        gentleFadeIn: Boolean = false
    ) {
        val sampleRate = 22050
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)

        val track = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBuf * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (_: Exception) {
            return
        }

        synchronized(ringerLock) {
            if (ringJob?.isActive != true) {
                try { track.release() } catch (_: Exception) {}
                return
            }
            synthTrack = track
        }

        try {
            track.play()

            data class ToneStep(val freqHz: Double, val harmonicRatio: Double, val durationMs: Int)

            val pattern: List<ToneStep> = when (tone) {
                AlarmToneOption.SUNRISE_HARP -> listOf(
                    ToneStep(349.23, 0.35, 220), // F4
                    ToneStep(440.00, 0.35, 220), // A4
                    ToneStep(523.25, 0.30, 220), // C5
                    ToneStep(659.25, 0.30, 260), // E5
                    ToneStep(783.99, 0.25, 340)  // G5
                )
                AlarmToneOption.ACADEMIC_BELL -> listOf(
                    ToneStep(659.25, 0.45, 320), // E5
                    ToneStep(523.25, 0.45, 320), // C5
                    ToneStep(587.33, 0.45, 320), // D5
                    ToneStep(392.00, 0.50, 520)  // G4
                )
                AlarmToneOption.QUANTUM_PULSE -> listOf(
                    ToneStep(880.00, 0.20, 120),
                    ToneStep(0.0, 0.0, 70),
                    ToneStep(880.00, 0.20, 120),
                    ToneStep(0.0, 0.0, 70),
                    ToneStep(1174.66, 0.20, 180)
                )
                AlarmToneOption.ZEN_BOWL -> listOf(
                    ToneStep(432.00, 0.55, 650),
                    ToneStep(648.00, 0.35, 550)
                )
                else -> listOf( // AURORA_CHIME
                    ToneStep(523.25, 0.30, 190), // C5
                    ToneStep(659.25, 0.30, 190), // E5
                    ToneStep(783.99, 0.25, 190), // G5
                    ToneStep(1046.50, 0.25, 280) // C6
                )
            }

            val pauseBetweenPhrasesMs = when (tone) {
                AlarmToneOption.ZEN_BOWL -> 550L
                AlarmToneOption.QUANTUM_PULSE -> 380L
                else -> 440L
            }

            val startMs = System.currentTimeMillis()
            val maxMs = maxDurationSeconds * 1000L

            while (scope.isActive && (System.currentTimeMillis() - startMs) < maxMs) {
                val jobStillRunning = synchronized(ringerLock) { ringJob?.isActive == true && synthTrack === track }
                if (!jobStillRunning) break

                for (step in pattern) {
                    val activeCheck = synchronized(ringerLock) { ringJob?.isActive == true && synthTrack === track }
                    if (!activeCheck) break

                    val sampleCount = ((sampleRate * step.durationMs) / 1000).coerceAtLeast(256)
                    val buffer = ShortArray(sampleCount)
                    val elapsedRatio = if (gentleFadeIn) {
                        val elapsed = (System.currentTimeMillis() - startMs).coerceAtLeast(0L)
                        (0.22 + 0.78 * (elapsed.toDouble() / 8000.0)).coerceIn(0.22, 1.0)
                    } else 1.0
                    if (step.freqHz > 0.0) {
                        val attackSamples = (sampleCount * 0.08).toInt().coerceAtLeast(1)
                        for (i in 0 until sampleCount) {
                            val attack = (i.toDouble() / attackSamples).coerceIn(0.0, 1.0)
                            val decay = (1.0 - (i.toDouble() / sampleCount)).coerceIn(0.0, 1.0)
                            val env = attack * decay * elapsedRatio
                            val fundamental = sin(2.0 * Math.PI * step.freqHz * i / sampleRate)
                            val overtone = sin(2.0 * Math.PI * (step.freqHz * 2.0) * i / sampleRate) * step.harmonicRatio
                            val sample = ((fundamental + overtone) / (1.0 + step.harmonicRatio)) * 0.58 * env
                            buffer[i] = (sample.coerceIn(-1.0, 1.0) * 32767).toInt().toShort()
                        }
                    }
                    track.write(buffer, 0, buffer.size)
                }
                delay(pauseBetweenPhrasesMs)
            }
        } catch (_: Exception) {
        }
    }

    private fun startVibration(context: Context) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            activeVibrator = vibrator
            val pattern = longArrayOf(0, 500, 300, 500, 400)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (_: Exception) {
        }
    }

    private fun releaseMediaPlayerLocked() {
        try {
            activeMediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                reset()
                release()
            }
        } catch (_: Exception) {
        }
        activeMediaPlayer = null
    }

    private fun stopInternal(context: Context? = null) {
        _activeAlarmFlow.value = null
        _previewingToneId.value = null
        ringJob?.cancel()
        ringJob = null

        releaseMediaPlayerLocked()

        try {
            synthTrack?.apply {
                if (playState == AudioTrack.PLAYSTATE_PLAYING) {
                    pause()
                    flush()
                }
                release()
            }
        } catch (_: Exception) {
        }
        synthTrack = null

        try {
            activeVibrator?.cancel()
            if (context != null) {
                val vm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
                vm?.cancel()
            }
        } catch (_: Exception) {
        }
        activeVibrator = null
    }
}
