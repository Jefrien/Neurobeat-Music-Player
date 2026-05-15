package dev.jefrien.neurobeat.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jefrien.neurobeat.domain.model.Album
import dev.jefrien.neurobeat.domain.model.Artist
import dev.jefrien.neurobeat.domain.model.Song
import dev.jefrien.neurobeat.domain.repository.SubsonicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArtistDetailViewModel @Inject constructor(
    private val repository: SubsonicRepository
) : ViewModel() {

    sealed class ArtistState {
        data object Loading : ArtistState()
        data class Error(val message: String) : ArtistState()
        data class Success(
            val artist: Artist,
            val albums: List<Album>,
            val topSongs: List<Song>
        ) : ArtistState()
    }

    private val _state = MutableStateFlow<ArtistState>(ArtistState.Loading)
    val state: StateFlow<ArtistState> = _state

    fun loadArtist(artistId: String) {
        viewModelScope.launch {
            _state.value = ArtistState.Loading
            repository.getArtist(artistId).fold(
                onSuccess = { artist ->
                    _state.value = ArtistState.Success(
                        artist = artist,
                        albums = artist.albums,
                        topSongs = emptyList()
                    )
                },
                onFailure = { error ->
                    _state.value = ArtistState.Error(error.message ?: "Failed to load artist")
                }
            )
        }
    }
}
