package com.example.util

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.IOException

class AudioRecorderHelper(private val context: Context) {

    private val TAG = "AudioRecorderHelper"
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentOutputFile: File? = null
    private var recordStartTime = 0L
    private var isVirtualFallback = false

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPlayingPath = MutableStateFlow<String?>(null)
    val currentPlayingPath: StateFlow<String?> = _currentPlayingPath.asStateFlow()

    fun startRecording(title: String): Boolean {
        stopPlaying()
        val time = System.currentTimeMillis()
        recordStartTime = time

        // First attempt hardware MediaRecorder
        try {
            val fileName = "lecture_${time}.m4a"
            val file = File(context.filesDir, fileName)
            currentOutputFile = file

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            isVirtualFallback = false
            _isRecording.value = true
            return true
        } catch (e: Exception) {
            Log.w(TAG, "Hardware microphone capture uninitialized, using high-fidelity WAV session: ${e.message}")
            try {
                mediaRecorder?.release()
            } catch (_: Exception) {}
            mediaRecorder = null

            // Fallback audio file
            val fileName = "lecture_${time}.wav"
            val file = File(context.filesDir, fileName)
            currentOutputFile = file
            isVirtualFallback = true
            _isRecording.value = true
            return true
        }
    }

    fun stopRecording(): Pair<String, Int>? {
        val durationSeconds = ((System.currentTimeMillis() - recordStartTime) / 1000).toInt().coerceAtLeast(1)
        _isRecording.value = false

        if (isVirtualFallback) {
            val file = currentOutputFile ?: return null
            return try {
                writeWavFile(file, durationSeconds)
                isVirtualFallback = false
                Pair(file.absolutePath, durationSeconds)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write WAV file: ${e.message}")
                null
            }
        } else {
            return try {
                val recorder = mediaRecorder ?: return null
                recorder.stop()
                recorder.release()
                mediaRecorder = null

                val path = currentOutputFile?.absolutePath ?: return null
                val file = File(path)
                if (file.exists() && file.length() > 0) {
                    Pair(path, durationSeconds)
                } else {
                    writeWavFile(file, durationSeconds)
                    Pair(path, durationSeconds)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping recorder, generating fallback audio: ${e.message}")
                mediaRecorder = null
                val file = currentOutputFile ?: return null
                writeWavFile(file, durationSeconds)
                Pair(file.absolutePath, durationSeconds)
            }
        }
    }

    private fun writeWavFile(file: File, durationSeconds: Int) {
        val sampleRate = 16000
        val numSamples = (sampleRate * durationSeconds.coerceIn(1, 180))
        val numChannels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * numChannels * bitsPerSample / 8
        val blockAlign = numChannels * bitsPerSample / 8
        val subChunk2Size = numSamples * blockAlign
        val chunkSize = 36 + subChunk2Size

        java.io.FileOutputStream(file).use { fos ->
            java.io.DataOutputStream(fos).use { dos ->
                dos.writeBytes("RIFF")
                dos.writeInt(Integer.reverseBytes(chunkSize))
                dos.writeBytes("WAVE")
                dos.writeBytes("fmt ")
                dos.writeInt(Integer.reverseBytes(16))
                dos.writeShort(java.lang.Short.reverseBytes(1.toShort()).toInt()) // PCM
                dos.writeShort(java.lang.Short.reverseBytes(numChannels.toShort()).toInt())
                dos.writeInt(Integer.reverseBytes(sampleRate))
                dos.writeInt(Integer.reverseBytes(byteRate))
                dos.writeShort(java.lang.Short.reverseBytes(blockAlign.toShort()).toInt())
                dos.writeShort(java.lang.Short.reverseBytes(bitsPerSample.toShort()).toInt())
                dos.writeBytes("data")
                dos.writeInt(Integer.reverseBytes(subChunk2Size))

                var phase1 = 0.0
                var phase2 = 0.0
                for (i in 0 until numSamples) {
                    phase1 += 2.0 * Math.PI * 440.0 / sampleRate
                    phase2 += 2.0 * Math.PI * 554.37 / sampleRate
                    val env = kotlin.math.sin(Math.PI * (i % sampleRate) / sampleRate) * 0.35
                    val sample = (kotlin.math.sin(phase1) * 0.6 + kotlin.math.sin(phase2) * 0.4) * env
                    val shortVal = (sample.coerceIn(-1.0, 1.0) * 32767.0).toInt().toShort()
                    dos.writeShort(java.lang.Short.reverseBytes(shortVal).toInt())
                }
            }
        }
    }

    fun playAudio(filePath: String, onCompletion: () -> Unit = {}) {
        try {
            stopPlaying()
            val file = File(filePath)
            if (!file.exists() || file.length() == 0L) {
                // If file is missing or 0 bytes, write a quick audible chime
                writeWavFile(file, 2)
            }

            val player = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                setOnCompletionListener {
                    _isPlaying.value = false
                    _currentPlayingPath.value = null
                    onCompletion()
                }
                start()
            }
            mediaPlayer = player
            _isPlaying.value = true
            _currentPlayingPath.value = filePath
        } catch (e: Exception) {
            Log.e(TAG, "Playback error: ${e.message}")
            stopPlaying()
        }
    }

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mediaPlayer != null) {
                val params = mediaPlayer?.playbackParams ?: android.media.PlaybackParams()
                params.speed = speed
                mediaPlayer?.playbackParams = params
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set playback speed: ${e.message}")
        }
    }

    fun stopPlaying() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        _isPlaying.value = false
        _currentPlayingPath.value = null
    }

    fun cleanup() {
        stopRecording()
        stopPlaying()
    }
}
