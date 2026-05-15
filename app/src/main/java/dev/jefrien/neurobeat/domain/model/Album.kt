package dev.jefrien.neurobeat.domain.model

data class Album(
    val id: String,
    val name: String,
    val artistId: String,
    val artistName: String,
    val coverArtId: String? = null,
    val songCount: Int = 0,
    val duration: Int = 0,
    val year: Int? = null,
    val genre: String? = null,
    val isStarred: Boolean = false,
    val rating: Int? = null,
    val created: Long? = null,
    val isAvailableOffline: Boolean = false,
    val songs: List<Song> = emptyList()
)
