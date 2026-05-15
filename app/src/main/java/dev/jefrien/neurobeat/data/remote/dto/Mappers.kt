package dev.jefrien.neurobeat.data.remote.dto

import dev.jefrien.neurobeat.domain.model.Album
import dev.jefrien.neurobeat.domain.model.Artist
import dev.jefrien.neurobeat.domain.model.Genre
import dev.jefrien.neurobeat.domain.model.Playlist
import dev.jefrien.neurobeat.domain.model.Song

fun SongDto.toDomain(): Song = Song(
    id = id,
    title = title,
    artistId = artistId ?: "",
    artistName = artist ?: "Unknown Artist",
    albumId = albumId ?: "",
    albumName = album ?: "Unknown Album",
    duration = duration ?: 0,
    track = track,
    year = year,
    genre = genre,
    coverArtId = coverArt,
    contentType = contentType,
    bitRate = bitRate,
    path = path,
    isStarred = starred != null,
    rating = rating,
    playCount = playCount ?: 0
)

fun AlbumDto.toDomain(): Album = Album(
    id = id,
    name = name,
    artistId = artistId ?: "",
    artistName = artist ?: "Unknown Artist",
    coverArtId = coverArt,
    songCount = songCount ?: 0,
    duration = duration ?: 0,
    year = year,
    genre = genre,
    isStarred = starred != null,
    rating = rating,
    created = created?.let { parseDate(it) },
    songs = song?.map { it.toDomain() } ?: emptyList()
)

fun ArtistDto.toDomain(): Artist = Artist(
    id = id,
    name = name,
    coverArtId = coverArt,
    albumCount = albumCount ?: 0,
    albums = album?.map { it.toDomain() } ?: emptyList()
)

fun GenreDto.toDomain(): Genre = Genre(
    name = value,
    songCount = songCount ?: 0,
    albumCount = albumCount ?: 0
)

fun PlaylistDto.toDomain(): Playlist = Playlist(
    id = id,
    name = name,
    songCount = songCount ?: 0,
    duration = duration ?: 0,
    created = created?.let { parseDate(it) },
    changed = changed?.let { parseDate(it) },
    isPublic = public ?: false,
    owner = owner,
    songs = entry?.map { it.toDomain() } ?: emptyList()
)

private fun parseDate(date: String): Long? {
    return try {
        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
            .parse(date)?.time
    } catch (_: Exception) {
        null
    }
}
