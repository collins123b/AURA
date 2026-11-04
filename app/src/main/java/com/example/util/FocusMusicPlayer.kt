package com.example.util

import android.media.AudioAttributes
import android.media.MediaPlayer
import com.example.data.local.entity.FocusTrackEntity
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

enum class MusicLoopMode(val label: String) {
    LOOP_ALL("Loop All"),
    LOOP_ONE("Loop Track"),
    OFF("Play Once")
}

class FocusMusicPlayer {

    private val playerLock = Any()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private var playlistSnapshot: List<FocusTrackEntity> = emptyList()

    private val _currentTrack = MutableStateFlow<FocusTrackEntity?>(null)
    val currentTrack: StateFlow<FocusTrackEntity?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionSec = MutableStateFlow(0)
    val currentPositionSec: StateFlow<Int> = _currentPositionSec.asStateFlow()

    private val _durationSec = MutableStateFlow(0)
    val durationSec: StateFlow<Int> = _durationSec.asStateFlow()

    private val _volume = MutableStateFlow(0.8f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _loopMode = MutableStateFlow(MusicLoopMode.LOOP_ALL)
    val loopMode: StateFlow<MusicLoopMode> = _loopMode.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    fun updatePlaylist(tracks: List<FocusTrackEntity>) {
        synchronized(playerLock) {
            playlistSnapshot = tracks
            val current = _currentTrack.value
            if (current != null && tracks.none { it.id == current.id }) {
                stopInternal()
            } else if (current != null) {
                _currentTrack.value = tracks.find { it.id == current.id } ?: current
            }
        }
    }

    fun playTrack(track: FocusTrackEntity, playlist: List<FocusTrackEntity> = playlistSnapshot) {
        synchronized(playerLock) {
            playlistSnapshot = playlist
            val file = File(track.localFilePath)
            if (!file.exists()) {
                stopInternal()
                return
            }

            releasePlayerOnly()
            try {
                val mp = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    setDataSource(file.absolutePath)
                    isLooping = (_loopMode.value == MusicLoopMode.LOOP_ONE)
                    prepare()
                    val vol = _volume.value.coerceIn(0f, 1f)
                    setVolume(vol, vol)
                    setOnCompletionListener {
                        handleTrackCompleted()
                    }
                    start()
                }
                mediaPlayer = mp
                _currentTrack.value = track
                _durationSec.value = (mp.duration / 1000).coerceAtLeast(track.durationSeconds)
                _currentPositionSec.value = 0
                _isPlaying.value = true
                startProgressTicker()
            } catch (_: Exception) {
                stopInternal()
            }
        }
    }

    fun togglePlayPause(playlist: List<FocusTrackEntity> = playlistSnapshot) {
        synchronized(playerLock) {
            playlistSnapshot = playlist
            val mp = mediaPlayer
            if (mp != null) {
                try {
                    if (mp.isPlaying) {
                        mp.pause()
                        _isPlaying.value = false
                        progressJob?.cancel()
                    } else {
                        mp.start()
                        _isPlaying.value = true
                        startProgressTicker()
                    }
                    return
                } catch (_: Exception) {
                    stopInternal()
                }
            }
            val target = _currentTrack.value ?: playlist.firstOrNull() ?: return
            playTrack(target, playlist)
        }
    }

    fun skipNext(playlist: List<FocusTrackEntity> = playlistSnapshot) {
        synchronized(playerLock) {
            playlistSnapshot = playlist
            if (playlist.isEmpty()) return
            val current = _currentTrack.value
            val next = if (_isShuffleEnabled.value && playlist.size > 1) {
                playlist.filter { it.id != current?.id }.randomOrNull() ?: playlist.first()
            } else {
                val idx = playlist.indexOfFirst { it.id == current?.id }
                if (idx < 0 || idx + 1 >= playlist.size) playlist.first() else playlist[idx + 1]
            }
            playTrack(next, playlist)
        }
    }

    fun skipPrevious(playlist: List<FocusTrackEntity> = playlistSnapshot) {
        synchronized(playerLock) {
            playlistSnapshot = playlist
            if (playlist.isEmpty()) return
            val mp = mediaPlayer
            if (mp != null && _currentPositionSec.value > 4) {
                seekToSeconds(0)
                return
            }
            val current = _currentTrack.value
            val idx = playlist.indexOfFirst { it.id == current?.id }
            val prev = if (idx <= 0) playlist.last() else playlist[idx - 1]
            playTrack(prev, playlist)
        }
    }

    fun seekToSeconds(seconds: Int) {
        synchronized(playerLock) {
            val mp = mediaPlayer ?: return
            try {
                val clamped = seconds.coerceIn(0, _durationSec.value.coerceAtLeast(0))
                mp.seekTo(clamped * 1000)
                _currentPositionSec.value = clamped
            } catch (_: Exception) {
            }
        }
    }

    fun setVolume(newVolume: Float) {
        synchronized(playerLock) {
            val clamped = newVolume.coerceIn(0f, 1f)
            _volume.value = clamped
            try {
                mediaPlayer?.setVolume(clamped, clamped)
            } catch (_: Exception) {
            }
        }
    }

    fun cycleLoopMode() {
        synchronized(playerLock) {
            val nextMode = when (_loopMode.value) {
                MusicLoopMode.LOOP_ALL -> MusicLoopMode.LOOP_ONE
                MusicLoopMode.LOOP_ONE -> MusicLoopMode.OFF
                MusicLoopMode.OFF -> MusicLoopMode.LOOP_ALL
            }
            _loopMode.value = nextMode
            try {
                mediaPlayer?.isLooping = (nextMode == MusicLoopMode.LOOP_ONE)
            } catch (_: Exception) {
            }
        }
    }

    fun toggleShuffle() {
        _isShuffleEnabled.value = !_isShuffleEnabled.value
    }

    fun stop() {
        synchronized(playerLock) {
            stopInternal()
        }
    }

    private fun handleTrackCompleted() {
        synchronized(playerLock) {
            val list = playlistSnapshot
            if (list.isEmpty()) {
                stopInternal()
                return
            }
            when (_loopMode.value) {
                MusicLoopMode.LOOP_ONE -> {
                    val cur = _currentTrack.value ?: return
                    playTrack(cur, list)
                }
                MusicLoopMode.LOOP_ALL -> {
                    skipNext(list)
                }
                MusicLoopMode.OFF -> {
                    val cur = _currentTrack.value
                    val idx = list.indexOfFirst { it.id == cur?.id }
                    if (idx in 0 until list.lastIndex) {
                        playTrack(list[idx + 1], list)
                    } else {
                        _isPlaying.value = false
                        _currentPositionSec.value = 0
                        progressJob?.cancel()
                    }
                }
            }
        }
    }

    private fun startProgressTicker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && _isPlaying.value) {
                synchronized(playerLock) {
                    val mp = mediaPlayer
                    if (mp != null) {
                        try {
                            if (mp.isPlaying) {
                                _currentPositionSec.value = (mp.currentPosition / 1000).coerceAtLeast(0)
                            }
                        } catch (_: Exception) {
                        }
                    }
                }
                delay(500L)
            }
        }
    }

    private fun releasePlayerOnly() {
        progressJob?.cancel()
        progressJob = null
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                reset()
                release()
            }
        } catch (_: Exception) {
        }
        mediaPlayer = null
    }

    private fun stopInternal() {
        releasePlayerOnly()
        _isPlaying.value = false
        _currentPositionSec.value = 0
    }
}
