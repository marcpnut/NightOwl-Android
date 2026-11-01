package com.marcpnut.nightowl.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class NightOwlApi(private val session: NightOwlSession) {
    suspend fun health() = request("GET", "/health")

    suspend fun requestLogout() { request("POST", "/auth/logout", JSONObject()) }

    suspend fun profiles(): ProfilesData {
        val data = request("GET", "/profiles")
        val items = data.optJSONArray("items") ?: org.json.JSONArray()
        return ProfilesData(
            items = (0 until items.length()).mapNotNull { i -> items.optJSONObject(i)?.toProfileInfo() },
            selectedProfileId = data.optString("selectedProfileId").takeUnless { it.isBlank() || it == "null" },
            mainProfileId = data.optString("mainProfileId").takeUnless { it.isBlank() || it == "null" }
        )
    }

    suspend fun selectProfile(profileId: String) {
        request("PUT", "/profiles/selection", JSONObject().put("profileId", profileId))
    }

    suspend fun setMainProfile(profileId: String) {
        request("PUT", "/profiles/${encode(profileId)}/main", JSONObject())
    }

    suspend fun createProfile(name: String): ProfileInfo {
        val data = request("POST", "/profiles", JSONObject().put("name", name))
        return data.toProfileInfo().let { it.copy(name = it.name.ifBlank { name }) }
    }

    suspend fun saveSettings(profileId: String, settings: ProfileSettings) {
        val hidden = JSONObject().put("live", org.json.JSONArray(settings.hiddenLiveCategories.toList()))
            .put("movies", org.json.JSONArray(settings.hiddenMovieCategories.toList()))
            .put("series", org.json.JSONArray(settings.hiddenSeriesCategories.toList()))
        val body = JSONObject().put("settings", JSONObject()
            .put("subtitleSearchLanguages", org.json.JSONArray(settings.subtitleSearchLanguages))
            .put("preferHearingImpairedSubtitles", settings.preferHearingImpairedSubtitles)
            .put("hiddenCategories", hidden))
        request("PUT", "/profiles/${encode(profileId)}/preferences", body)
    }

    suspend fun home(profileId: String): HomeData {
        val data = request("GET", "/home?profileId=${encode(profileId)}")
        val rows = data.optJSONArray("rows")
        val shelves = rows?.let { list ->
            (0 until list.length()).mapNotNull { i -> list.optJSONObject(i)?.toShelf() }
        } ?: emptyList()
        val enrichedShelves = coroutineScope {
            shelves.map { shelf ->
                if (shelf.id == "continue") shelf.copy(items = shelf.items.map { item ->
                    async { if (item.kind == "episode") enrichEpisode(item) else item }
                }.awaitAll()) else shelf
            }
        }
        return HomeData(data.optJSONObject("featured")?.toMediaItem(), enrichedShelves)
    }

    suspend fun whatsNew(): HomeData {
        val data = request("GET", "/whats-new?language=en")
        val rows = data.optJSONArray("rows")
        return HomeData(null, rows?.let { list ->
            (0 until list.length()).mapNotNull { i -> list.optJSONObject(i)?.toShelf() }
        } ?: emptyList())
    }

    suspend fun browse(kind: String, page: Int = 1, categoryId: String = ""): Pair<List<MediaItem>, Boolean> {
        // The server intentionally caps catalog pages at 20 records.
        val path = "/$kind?page=$page&limit=20&language=en" + if (categoryId.isNotBlank()) "&category=${encode(categoryId)}" else ""
        val response = request("GET", path)
        val fallbackKind = when (kind) {
            "movies" -> "movie"
            "series" -> "series"
            "live" -> "live"
            else -> kind
        }
        return response.optJSONArray("items").toMediaItems(fallbackKind) to response.optBoolean("hasMore")
    }

    suspend fun categories(kind: String): List<BrowseCategory> {
        val items = request("GET", "/$kind/categories").optJSONArray("items") ?: return emptyList()
        return (0 until items.length()).mapNotNull { i -> items.optJSONObject(i)?.let { BrowseCategory(it.optString("id"), it.optString("name")) } }
    }

    suspend fun myList(profileId: String): List<MediaItem> =
        request("GET", "/profiles/${encode(profileId)}/my-list").optJSONArray("items").toMediaItems()

    suspend fun search(query: String, page: Int = 1): Pair<List<MediaItem>, Boolean> {
        val response = request("GET", "/search?q=${encode(query)}&page=$page&limit=20&language=en")
        return (response.optJSONArray("movies").toMediaItems("movie") + response.optJSONArray("series").toMediaItems("series")) to response.optBoolean("hasMore")
    }

    suspend fun detail(item: MediaItem): MediaItem {
        val route = if (item.kind == "movie") "movies" else "series"
        return request("GET", "/$route/${encode(item.id)}?language=en").toMediaItem(item.kind)
    }

    suspend fun enrichEpisode(item: MediaItem): MediaItem {
        if (item.kind != "episode" || (item.seriesId.isNotBlank() && item.seriesTitle.isNotBlank())) return item
        val parent = request("GET", "/episodes/${encode(item.id)}/parent")
        return item.copy(seriesId = item.seriesId.ifBlank { parent.optString("seriesId") },
            seriesTitle = item.seriesTitle.ifBlank { parent.optString("seriesTitle") })
    }

    suspend fun episodeParent(item: MediaItem): MediaItem? {
        if (item.kind != "episode") return item
        if (item.seriesId.isNotBlank()) return MediaItem(kind = "series", id = item.seriesId,
            title = item.seriesTitle.ifBlank { item.title }, posterUrl = item.posterUrl, backdropUrl = item.backdropUrl)
        val parent = request("GET", "/episodes/${encode(item.id)}/parent")
        val id = parent.optString("seriesId")
        if (id.isBlank()) return null
        return MediaItem(kind = "series", id = id, title = parent.optString("seriesTitle").ifBlank { item.title },
            posterUrl = item.posterUrl, backdropUrl = item.backdropUrl)
    }

    suspend fun seasons(seriesId: String): List<SeasonInfo> {
        val items = request("GET", "/series/${encode(seriesId)}/seasons").optJSONArray("seasons") ?: return emptyList()
        return (0 until items.length()).mapNotNull { i -> items.optJSONObject(i)?.let { SeasonInfo(it.optInt("number"), it.optInt("count")) } }
    }

    suspend fun episodes(seriesId: String, season: Int, page: Int = 1): EpisodePage {
        val data = request("GET", "/series/${encode(seriesId)}/seasons/$season/episodes?page=$page&limit=20&language=en")
        return EpisodePage(data.optJSONArray("items").toMediaItems("episode").map { it.copy(kind = "episode", seriesId = seriesId) },
            data.optBoolean("hasMore"))
    }

    suspend fun followingEpisode(item: MediaItem): MediaItem? {
        val enriched = enrichEpisode(item)
        val seriesId = enriched.seriesId
        if (seriesId.isBlank()) return null
        val seasons = seasons(seriesId).sortedBy { it.number }
        val currentSeason = enriched.seasonNumber ?: seasons.firstOrNull()?.number ?: return null
        val currentEpisode = enriched.episodeNumber
        for (season in seasons) {
            if (season.number < currentSeason) continue
            for (page in 1..100) {
                val result = episodes(seriesId, season.number, page)
                val ordered = result.items.sortedWith(compareBy<MediaItem> { it.episodeNumber ?: 0 }.thenBy { it.id })
                if (season.number > currentSeason) return ordered.firstOrNull()
                val index = ordered.indexOfFirst { it.id == enriched.id || it.playbackId == enriched.playbackId ||
                    (currentEpisode != null && it.episodeNumber == currentEpisode) }
                if (index >= 0) {
                    ordered.getOrNull(index + 1)?.let { return it }
                    if (!result.hasMore) break
                    episodes(seriesId, season.number, page + 1).items.firstOrNull()?.let { return it }
                    break
                }
                if (!result.hasMore) break
            }
        }
        return null
    }

    suspend fun resolve(item: MediaItem): String = request("POST", "/playback/resolve",
        JSONObject().put("kind", item.kind).put("id", item.playbackId)).getString("url")

    suspend fun remuxUrl(item: MediaItem): String {
        val path = request("POST", "/playback/remux-ticket",
            JSONObject().put("kind", item.kind).put("id", item.playbackId).put("transcode", false)).getString("url")
        return session.serverUrl.trimEnd('/') + path
    }

    suspend fun setFavorite(profileId: String, item: MediaItem, saved: Boolean) {
        val target = if (item.kind == "episode") episodeParent(item) ?: throw IllegalStateException("Could not find the series for this episode.") else item
        request("POST", "/profiles/${encode(profileId)}/my-list",
            JSONObject().put("kind", target.kind).put("id", target.id).put("saved", saved))
    }

    suspend fun progress(profileId: String, item: MediaItem, positionSeconds: Long, durationSeconds: Long?, completed: Boolean) {
        // Progress and My List use the selected catalog id; playbackId may be a preferred alternate stream.
        val body = JSONObject().put("kind", item.kind).put("id", item.id)
            .put("positionSeconds", positionSeconds).put("completed", completed)
        if (durationSeconds != null) body.put("durationSeconds", durationSeconds)
        request("POST", "/profiles/${encode(profileId)}/progress", body)
    }

    private suspend fun request(method: String, path: String, body: JSONObject? = null): JSONObject = withContext(Dispatchers.IO) {
        val connection = (URL(session.serverUrl.trimEnd('/') + "/api/v1" + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer ${session.accessToken}")
            setRequestProperty("X-Device-Id", session.deviceId)
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
        }
        try {
            if (body != null) connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val message = try { JSONObject(text).optJSONObject("error")?.optString("message") } catch (_: Exception) { null }
                throw NightOwlApiException(status, message?.takeIf { it.isNotBlank() } ?: "Server returned HTTP $status.")
            }
            if (text.isBlank()) JSONObject() else JSONObject(text)
        } finally { connection.disconnect() }
    }

    companion object {
        suspend fun login(serverUrl: String, username: String, password: String, deviceId: String): NightOwlSession = withContext(Dispatchers.IO) {
            val normalized = serverUrl.trim().trimEnd('/')
            val url = URL(normalized)
            require(url.protocol == "https") { "Use the secure https:// server address." }
            val connection = (URL("$normalized/api/v1/auth/login").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 30_000
                doOutput = true
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("X-Device-Id", deviceId)
            }
            try {
                val body = JSONObject().put("username", username.trim()).put("password", password)
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val status = connection.responseCode
                val text = (if (status in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                val payload = try { JSONObject(text) } catch (_: Exception) { JSONObject() }
                if (status !in 200..299) {
                    val message = when (payload.optJSONObject("error")?.optString("code")) {
                        "INVALID_CREDENTIALS" -> "Username or password is incorrect."
                        "LOGIN_RATE_LIMITED" -> "Too many sign-in attempts. Try again later."
                        "ACCOUNT_SETUP_REQUIRED" -> "Ask the NightOwl administrator to create an account."
                        else -> payload.optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() } ?: "Sign-in failed (HTTP $status)."
                    }
                    throw NightOwlApiException(status, message)
                }
                val token = payload.optString("accessToken")
                if (token.isBlank()) throw IllegalStateException("The server did not return a sign-in session.")
                NightOwlSession(normalized, token, deviceId)
            } finally { connection.disconnect() }
        }

        private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())
    }
}

class NightOwlApiException(val statusCode: Int, message: String) : Exception(message)
