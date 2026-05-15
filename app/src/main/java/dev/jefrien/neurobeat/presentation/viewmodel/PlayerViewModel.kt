package dev.jefrien.neurobeat.presentation.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.jefrien.neurobeat.data.remote.api.CoverArtUrlBuilder
import dev.jefrien.neurobeat.domain.model.Song
import dev.jefrien.neurobeat.service.playback.MusicPlaybackService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    val exoPlayer: ExoPlayer,
    private val urlBuilder: CoverArtUrlBuilder,
    @ApplicationContext private val context: Context
) : ViewModel() {

    data class PlayerState(
        val currentSong: Song? = null,
        val isPlaying: Boolean = false,
        val currentPosition: Long = 0,
        val duration: Long = 0,
        val queue: List<Song> = emptyList(),
        val currentIndex: Int = 0,
        val isShuffleOn: Boolean = false,
        val repeatMode: Int = Player.REPEAT_MODE_OFF
    )

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.value = _state.value.copy(isPlaying = isPlaying)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val index = exoPlayer.currentMediaItemIndex
            val song = _state.value.queue.getOrNull(index)
            _state.value = _state.value.copy(
                currentSong = song,
                currentIndex = index,
                duration = exoPlayer.duration.coerceAtLeast(0)
            )
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                _state.value = _state.value.copy(duration = exoPlayer.duration.coerceAtLeast(0))
            }
        }
    }

    init {
        exoPlayer.addListener(playerListener)
        exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
        exoPlayer.shuffleModeEnabled = false

        viewModelScope.launch {
            while (isActive) {
                if (_state.value.isPlaying) {
                    _state.value = _state.value.copy(
                        currentPosition = exoPlayer.currentPosition.coerceAtLeast(0)
                    )
                }
                delay(500)
            }
        }
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        _state.value = _state.value.copy(
            queue = songs,
            currentIndex = startIndex,
            currentSong = songs.getOrNull(startIndex)
        )
        exoPlayer.clearMediaItems()
        songs.forEach { song ->
            val streamUrl = urlBuilder.buildStreamUrl(song.id) ?: return@forEach
            val mediaItem = MediaItem.Builder()
                .setMediaId(song.id)
                .setUri(streamUrl)
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artistName)
                        .setAlbumTitle(song.albumName)
                        .setArtworkUri(
                            song.coverArtId?.let {
                                urlBuilder.buildCoverArtUrl(it)?.let { url ->
                                    android.net.Uri.parse(url)
                                }
                            }
                        )
                        .build()
                )
                .build()
            exoPlayer.addMediaItem(mediaItem)
        }
        exoPlayer.prepare()
        exoPlayer.seekTo(startIndex, 0)
        exoPlayer.play()

        // Start the foreground service for playback notification
        startPlaybackService()
    }

    private fun startPlaybackService() {
        val intent = Intent(context, MusicPlaybackService::class.java)
        try {
            context.startForegroundService(intent)
        } catch (_: Exception) {
            // Fallback for older Android versions
            context.startService(intent)
        }
    }

    fun playPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
            startPlaybackService()
        }
    }

    fun next() {
        if (exoPlayer.hasNextMediaItem()) {
            exoPlayer.seekToNextMediaItem()
        }
    }

    fun previous() {
        if (exoPlayer.hasPreviousMediaItem()) {
            exoPlayer.seekToPreviousMediaItem()
        }
    }

    fun seekTo(position: Long) {
        exoPlayer.seekTo(position)
        _state.value = _state.value.copy(currentPosition = position)
    }

    fun toggleShuffle() {
        val newShuffle = !_state.value.isShuffleOn
        exoPlayer.shuffleModeEnabled = newShuffle
        _state.value = _state.value.copy(isShuffleOn = newShuffle)
    }

    fun toggleRepeat() {
        val newMode = when (_state.value.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        exoPlayer.repeatMode = newMode
        _state.value = _state.value.copy(repeatMode = newMode)
    }

    override fun onCleared() {
        exoPlayer.removeListener(playerListener)
        super.onCleared()
    }
}
