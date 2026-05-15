package dev.jefrien.neurobeat.domain.model

data class Artist(
    val id: String,
    val name: String,
    val coverArtId: String? = null,
    val albumCount: Int = 0,
    val songCount: Int = 0,
    val isStarred: Boolean = false,
    val isAvailableOffline: Boolean = false,
    val albums: List<Album> = emptyList()
)
