package com.example.data.repository

import com.example.data.model.LyricLine
import com.example.data.model.LyricsData
import com.example.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class LyricsRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val lyricsCache = mutableMapOf<String, LyricsData>()

    suspend fun getLyrics(song: Song): LyricsData = withContext(Dispatchers.IO) {
        lyricsCache[song.id]?.let { return@withContext it }

        val cleanTitle = cleanSongTitle(song.title)
        val cleanArtist = cleanArtistName(song.artist)

        // 1. Try LRCLIB exact match
        var lyrics = tryExactLookup(cleanTitle, cleanArtist, song.duration, song.id)

        // 2. Fallback to LRCLIB search
        if (lyrics == null) {
            lyrics = trySearchLookup("$cleanTitle $cleanArtist", song.id)
        }

        // 3. Fallback to title only search
        if (lyrics == null && cleanTitle.isNotEmpty()) {
            lyrics = trySearchLookup(cleanTitle, song.id)
        }

        val result = lyrics ?: LyricsData(
            songId = song.id,
            title = song.title,
            artist = song.artist,
            plainLyrics = null,
            syncedLyrics = emptyList(),
            notFound = true
        )

        lyricsCache[song.id] = result
        result
    }

    private fun tryExactLookup(trackName: String, artistName: String, duration: Int, songId: String): LyricsData? {
        try {
            val encTrack = URLEncoder.encode(trackName, StandardCharsets.UTF_8.toString())
            val encArtist = URLEncoder.encode(artistName, StandardCharsets.UTF_8.toString())
            val url = "https://lrclib.net/api/get?track_name=$encTrack&artist_name=$encArtist" +
                    if (duration > 0) "&duration=$duration" else ""

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Moodsonix-Android-Player/1.0 (https://github.com/moodsonix)")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return null
                return parseLrclibJson(JSONObject(body), songId)
            }
        } catch (_: Exception) {
            // Ignore & continue to search
        }
        return null
    }

    private fun trySearchLookup(query: String, songId: String): LyricsData? {
        try {
            val encQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
            val url = "https://lrclib.net/api/search?q=$encQuery"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Moodsonix-Android-Player/1.0 (https://github.com/moodsonix)")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return null
                val array = JSONArray(body)
                if (array.length() > 0) {
                    val firstMatch = array.getJSONObject(0)
                    return parseLrclibJson(firstMatch, songId)
                }
            }
        } catch (_: Exception) {
            // Ignore
        }
        return null
    }

    private fun parseLrclibJson(json: JSONObject, songId: String): LyricsData {
        val trackName = json.optString("trackName", "")
        val artistName = json.optString("artistName", "")
        val synced = json.optString("syncedLyrics", "")
        val plain = json.optString("plainLyrics", "")

        val parsedLines = if (synced.isNotEmpty()) {
            parseLrc(synced)
        } else {
            emptyList()
        }

        return LyricsData(
            songId = songId,
            title = trackName,
            artist = artistName,
            plainLyrics = if (plain.isNotEmpty()) plain else null,
            syncedLyrics = parsedLines,
            provider = "LRCLIB",
            notFound = parsedLines.isEmpty() && plain.isEmpty()
        )
    }

    private fun parseLrc(lrcText: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        // LRC timestamp regex: [mm:ss.xx] or [mm:ss.xxx] or [mm:ss]
        val pattern = Pattern.compile("""\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?\](.*)""")

        lrcText.lines().forEach { rawLine ->
            val matcher = pattern.matcher(rawLine.trim())
            if (matcher.find()) {
                val min = matcher.group(1)?.toIntOrNull() ?: 0
                val sec = matcher.group(2)?.toIntOrNull() ?: 0
                val msStr = matcher.group(3)
                val ms = if (msStr != null) {
                    if (msStr.length == 2) msStr.toDouble() / 100.0 else msStr.toDouble() / 1000.0
                } else 0.0

                val totalSeconds = (min * 60) + sec + ms
                val text = matcher.group(4)?.trim() ?: ""
                if (text.isNotEmpty()) {
                    lines.add(LyricLine(totalSeconds, text))
                }
            }
        }

        return lines.sortedBy { it.timeSeconds }
    }

    private fun cleanSongTitle(title: String): String {
        var cleaned = title
        // Remove text in parentheses/brackets matching official tags
        val removePatterns = listOf(
            """(?i)\((?:official\s+)?(?:music\s+)?video\)""",
            """(?i)\[(?:official\s+)?(?:music\s+)?video\]""",
            """(?i)\((?:official\s+)?audio\)""",
            """(?i)\[(?:official\s+)?audio\]""",
            """(?i)\(lyrics?\s*(?:video)?\)""",
            """(?i)\[lyrics?\s*(?:video)?\]""",
            """(?i)\(visualizer\)""",
            """(?i)\[visualizer\]""",
            """(?i)\(hd\)""",
            """(?i)\[hd\]""",
            """(?i)\(4k\)""",
            """(?i)\[4k\]""",
            """(?i)-?\s*topic\b""",
            """(?i)\|.*$"""
        )

        for (pat in removePatterns) {
            cleaned = cleaned.replace(Regex(pat), "")
        }

        // Remove extraneous quotes or double spaces
        return cleaned.replace("\"", "").replace("'", "").trim()
    }

    private fun cleanArtistName(artist: String): String {
        return artist
            .replace(Regex("""(?i)\s*-\s*topic"""), "")
            .replace(Regex("""(?i)\s*vevo"""), "")
            .trim()
    }
}
