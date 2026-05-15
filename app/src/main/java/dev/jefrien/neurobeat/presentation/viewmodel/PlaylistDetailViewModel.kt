package dev.jefrien.neurobeat.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jefrien.neurobeat.domain.model.Playlist
import dev.jefrien.neurobeat.domain.model.Song
import dev.jefrien.neurobeat.domain.repository.SubsonicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    private val repository: SubsonicRepository
) : ViewModel() {

    sealed class PlaylistState {
        data object Loading : PlaylistState()
        data class Error(val message: String) : PlaylistState()
        data class Success(
            val playlist: Playlist,
            val songs: List<Song>
        ) : PlaylistState()
    }

    private val _state = MutableStateFlow<PlaylistState>(PlaylistState.Loading)
    val state: StateFlow<PlaylistState> = _state

    fun loadPlaylist(playlistId: String) {
        viewModelScope.launch {
            _state.value = PlaylistState.Loading
            repository.getPlaylist(playlistId).fold(
                onSuccess = { playlist ->
                    _state.value = PlaylistState.Success(playlist = playlist, songs = playlist.songs)
                },
                onFailure = { error ->
                    _state.value = PlaylistState.Error(error.message ?: "Failed to load playlist")
                }
            )
        }
    }
}
