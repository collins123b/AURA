package com.example.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Random
import kotlin.math.sin

enum class SoundscapeType(val displayName: String, val description: String) {
    OFF("Silent", "No background ambient soundscape"),
    LOFI_CHORDS("Midnight Lo-Fi Pad", "Warm nostalgic minor-7th synth chords for relaxed late-night study"),
    BROWN_NOISE("Deep Brown Noise", "Deep rumble for intense concentration & blocking distractions"),
    RAIN_STUDY("Gentle Rain", "Rhythmic raindrops patter for calm focus"),
    ALPHA_WAVES("Alpha 40Hz Waves", "Binaural pulses for memory retention & study flow"),
    BINAURAL_FLOW("Theta-Alpha Pad", "240Hz warm harmonic drone for deep reading"),
    PINK_NOISE("Soft Pink Noise", "Balanced spectrum to reduce mental fatigue"),
    WHITE_NOISE("Pure White Noise", "Equal frequency masking for noisy libraries")
}

class SoundscapeSynthesizer {

    private val synthLock = Any()
    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    @Volatile
    var currentSoundscape: SoundscapeType = SoundscapeType.OFF
        private set

    @Volatile
    var volumeFactor: Float = 0.60f
        set(value) {
            field = value.coerceIn(0f, 1f)
            try {
                audioTrack?.setVolume(field)
            } catch (_: Exception) {
            }
        }

    fun play(soundscape: SoundscapeType) {
        synchronized(synthLock) {
            if (soundscape == currentSoundscape && playbackJob?.isActive == true) return
            stop()
            if (soundscape == SoundscapeType.OFF) return

            currentSoundscape = soundscape
            playbackJob = scope.launch {
                generateAudioLoop(soundscape)
            }
        }
    }

    fun stop() {
        synchronized(synthLock) {
            currentSoundscape = SoundscapeType.OFF
            playbackJob?.cancel()
            playbackJob = null
            try {
                audioTrack?.apply {
                    if (playState == AudioTrack.PLAYSTATE_PLAYING) {
                        pause()
                        flush()
                    }
                    release()
                }
            } catch (e: Exception) {
                Log.e("SoundscapeSynthesizer", "Error stopping track", e)
            }
            audioTrack = null
        }
    }

    private fun generateAudioLoop(type: SoundscapeType) {
        val sampleRate = 22050
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096) * 2

        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            synchronized(synthLock) {
                if (currentSoundscape != type) {
                    track.release()
                    return
                }
                audioTrack = track
            }

            track.setVolume(volumeFactor)
            track.play()

            val random = Random()
            val buffer = ShortArray(bufferSize / 2)
            var lastBrown = 0.0
            var phase = 0.0
            var sampleCounter = 0L
            var b0 = 0.0
            var b1 = 0.0
            var b2 = 0.0
            var b3 = 0.0
            var b4 = 0.0
            var b5 = 0.0
            var b6 = 0.0

            // Warm Lo-Fi chord progression (Dm7 -> G7 -> Cmaj7 -> Am7)
            val chords = arrayOf(
                doubleArrayOf(293.66, 349.23, 440.00, 523.25),
                doubleArrayOf(196.00, 246.94, 349.23, 392.00),
                doubleArrayOf(261.63, 329.63, 392.00, 493.88),
                doubleArrayOf(220.00, 261.63, 329.63, 392.00)
            )
            val samplesPerChord = sampleRate * 4L // 4 seconds per warm chord

            while (scope.isActive && currentSoundscape == type) {
                for (i in buffer.indices) {
                    sampleCounter++
                    val sample: Double = when (type) {
                        SoundscapeType.LOFI_CHORDS -> {
                            val chordIdx = ((sampleCounter / samplesPerChord) % chords.size).toInt()
                            val chord = chords[chordIdx]
                            val posInChord = (sampleCounter % samplesPerChord).toDouble() / samplesPerChord
                            val swell = sin(Math.PI * posInChord).coerceIn(0.15, 1.0)
                            val t = sampleCounter.toDouble() / sampleRate
                            val wow = 1.0 + 0.0025 * sin(2.0 * Math.PI * 0.7 * t)
                            val notesSum = (
                                sin(2.0 * Math.PI * chord[0] * wow * t) +
                                sin(2.0 * Math.PI * chord[1] * wow * t) * 0.8 +
                                sin(2.0 * Math.PI * chord[2] * wow * t) * 0.7 +
                                sin(2.0 * Math.PI * chord[3] * wow * t) * 0.6
                            ) * 0.14 * swell
                            val vinylDust = if (random.nextInt(1200) < 2) (random.nextDouble() - 0.5) * 0.18 else 0.0
                            val warmHiss = (random.nextDouble() * 2.0 - 1.0) * 0.015
                            notesSum + vinylDust + warmHiss
                        }
                        SoundscapeType.BINAURAL_FLOW -> {
                            val t = sampleCounter.toDouble() / sampleRate
                            val f1 = sin(2.0 * Math.PI * 174.0 * t) * 0.25
                            val f2 = sin(2.0 * Math.PI * 182.0 * t) * 0.22
                            val fifth = sin(2.0 * Math.PI * 261.0 * t) * 0.12
                            f1 + f2 + fifth
                        }
                        SoundscapeType.BROWN_NOISE -> {
                            val white = (random.nextDouble() * 2.0 - 1.0)
                            lastBrown = (lastBrown + (0.025 * white)) / 1.02
                            lastBrown * 3.5
                        }
                        SoundscapeType.PINK_NOISE -> {
                            val white = (random.nextDouble() * 2.0 - 1.0)
                            b0 = 0.99886 * b0 + white * 0.0555179
                            b1 = 0.99332 * b1 + white * 0.0750759
                            b2 = 0.96900 * b2 + white * 0.1538520
                            b3 = 0.86650 * b3 + white * 0.3104856
                            b4 = 0.55000 * b4 + white * 0.5329522
                            b5 = -0.7616 * b5 - white * 0.0168980
                            val pink = b0 + b1 + b2 + b3 + b4 + b5 + b6 + white * 0.5362
                            b6 = white * 0.115926
                            (pink * 0.11)
                        }
                        SoundscapeType.ALPHA_WAVES -> {
                            phase += (2.0 * Math.PI * 220.0) / sampleRate
                            val carrier = sin(phase)
                            val modPhase = (phase * (40.0 / 220.0))
                            val am = (sin(modPhase) + 1.0) * 0.5
                            val noise = (random.nextDouble() * 2.0 - 1.0) * 0.12
                            (carrier * am * 0.38) + noise
                        }
                        SoundscapeType.RAIN_STUDY -> {
                            val isDrop = random.nextInt(100) < 4
                            val dropPulse = if (isDrop) (random.nextDouble() * 0.85) else 0.0
                            val white = (random.nextDouble() * 2.0 - 1.0) * 0.24
                            (white + dropPulse) * 0.6
                        }
                        SoundscapeType.WHITE_NOISE -> {
                            (random.nextDouble() * 2.0 - 1.0) * 0.32
                        }
                        SoundscapeType.OFF -> 0.0
                    }

                    val clamped = sample.coerceIn(-1.0, 1.0)
                    buffer[i] = (clamped * 32767.0).toInt().toShort()
                }

                track.write(buffer, 0, buffer.size)
            }
        } catch (e: Exception) {
            Log.e("SoundscapeSynthesizer", "Playback loop error", e)
        }
    }
}
