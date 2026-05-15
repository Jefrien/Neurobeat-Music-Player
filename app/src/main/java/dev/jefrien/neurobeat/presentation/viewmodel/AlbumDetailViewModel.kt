package dev.jefrien.neurobeat.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jefrien.neurobeat.domain.model.Album
import dev.jefrien.neurobeat.domain.model.Song
import dev.jefrien.neurobeat.domain.repository.SubsonicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    private val repository: SubsonicRepository
) : ViewModel() {

    sealed class AlbumState {
        data object Loading : AlbumState()
        data class Error(val message: String) : AlbumState()
        data class Success(
            val album: Album,
            val songs: List<Song>
        ) : AlbumState()
    }

    private val _state = MutableStateFlow<AlbumState>(AlbumState.Loading)
    val state: StateFlow<AlbumState> = _state

    fun loadAlbum(albumId: String) {
        viewModelScope.launch {
            _state.value = AlbumState.Loading
            repository.getAlbum(albumId).fold(
                onSuccess = { album ->
                    _state.value = AlbumState.Success(album = album, songs = album.songs)
                },
                onFailure = { error ->
                    _state.value = AlbumState.Error(error.message ?: "Failed to load album")
                }
            )
        }
    }
}
