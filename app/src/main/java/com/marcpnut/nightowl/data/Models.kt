package com.marcpnut.nightowl.data

import org.json.JSONArray
import org.json.JSONObject

data class NightOwlSession(val serverUrl: String, val accessToken: String, val deviceId: String)

data class MediaItem(
    val kind: String,
    val id: String,
    val title: String,
    val description: String = "",
    val posterUrl: String = "",
    val backdropUrl: String = "",
    val releaseYear: Int? = null,
    val genre: String = "",
    val positionSeconds: Long = 0,
    val durationSeconds: Long? = null,
    val streamId: String = "",
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val seriesId: String = "",
    val seriesTitle: String = "",
    val streamExtension: String = ""
) {
    val playbackId: String get() = streamId.ifBlank { id }
}

data class Shelf(val id: String, val title: String, val items: List<MediaItem>)
data class HomeData(val featured: MediaItem?, val rows: List<Shelf>)
data class ProfileInfo(val id: String, val name: String, val settings: ProfileSettings = ProfileSettings())
data class ProfileSettings(
    val subtitleSearchLanguages: List<String> = listOf("en", "nl", "pl"),
    val preferHearingImpairedSubtitles: Boolean = false,
    val hiddenLiveCategories: Set<String> = emptySet(),
    val hiddenMovieCategories: Set<String> = emptySet(),
    val hiddenSeriesCategories: Set<String> = emptySet()
)
data class BrowseCategory(val id: String, val name: String)
data class SeasonInfo(val number: Int, val count: Int)
data class EpisodePage(val items: List<MediaItem>, val hasMore: Boolean)

internal fun JSONObject.toMediaItem(fallbackKind: String = "movie"): MediaItem {
    return MediaItem(
        kind = optString("kind", fallbackKind),
        id = optString("id"),
        title = optString("title", "Untitled"),
        description = optString("description"),
        posterUrl = optString("posterUrl"),
        backdropUrl = optString("backdropUrl"),
        releaseYear = optIntOrNull("releaseYear"),
        genre = optString("genre"),
        positionSeconds = optLong("positionSeconds", 0),
        durationSeconds = optLongOrNull("durationSeconds"),
        streamId = optString("streamId"),
        seasonNumber = optIntOrNull("seasonNumber"),
        episodeNumber = optIntOrNull("episodeNumber"),
        seriesId = optString("seriesId"),
        seriesTitle = optString("seriesTitle"),
        streamExtension = optString("streamExtension").trim().removePrefix(".").lowercase()
    )
}

internal fun JSONObject.toProfileInfo(): ProfileInfo {
    val profileSettings = optJSONObject("preferences")?.optJSONObject("settings")
    val hidden = profileSettings?.optJSONObject("hiddenCategories")
    fun strings(source: org.json.JSONArray?): Set<String> = if (source == null) emptySet()
        else (0 until source.length()).mapNotNull { source.optString(it).takeIf(String::isNotBlank) }.toSet()
    val languages = profileSettings?.optJSONArray("subtitleSearchLanguages")
        ?.let { source -> (0 until source.length()).map { source.optString(it) }.filter { it.matches(Regex("^[a-z]{2}$")) } }
        ?.ifEmpty { listOf("en", "nl", "pl") } ?: listOf("en", "nl", "pl")
    return ProfileInfo(optString("id"), optString("name"), ProfileSettings(
        subtitleSearchLanguages = languages,
        preferHearingImpairedSubtitles = profileSettings?.optBoolean("preferHearingImpairedSubtitles") ?: false,
        hiddenLiveCategories = strings(hidden?.optJSONArray("live")),
        hiddenMovieCategories = strings(hidden?.optJSONArray("movies")),
        hiddenSeriesCategories = strings(hidden?.optJSONArray("series"))
    ))
}

internal fun JSONObject.toShelf(): Shelf = Shelf(
    id = optString("id"),
    title = optString("title"),
    items = optJSONArray("items").toMediaItems()
)

internal fun JSONArray?.toMediaItems(fallbackKind: String = "movie"): List<MediaItem> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { index -> optJSONObject(index)?.toMediaItem(fallbackKind) }
}

internal fun JSONObject.optIntOrNull(key: String): Int? {
    if (!has(key) || isNull(key)) return null
    return optInt(key).takeIf { it != 0 || optString(key) == "0" }
}

internal fun JSONObject.optLongOrNull(key: String): Long? {
    if (!has(key) || isNull(key)) return null
    return optLong(key).takeIf { it != 0L || optString(key) == "0" }
}
