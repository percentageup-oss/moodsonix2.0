package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.AppTheme
import com.example.data.model.PlaybackQuality
import com.example.data.model.Song
import org.json.JSONArray
import org.json.JSONObject

class UserLibraryRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("moodsonix_prefs", Context.MODE_PRIVATE)

    fun getLikedSongs(): List<Song> {
        val jsonString = prefs.getString("liked_songs_list", null) ?: return emptyList()
        return deserializeSongList(jsonString)
    }

    fun toggleLiked(song: Song): Boolean {
        val current = getLikedSongs().toMutableList()
        val existingIndex = current.indexOfFirst { it.id == song.id }
        val isNowLiked: Boolean
        if (existingIndex >= 0) {
            current.removeAt(existingIndex)
            isNowLiked = false
        } else {
            current.add(0, song)
            isNowLiked = true
        }
        prefs.edit().putString("liked_songs_list", serializeSongList(current)).apply()
        return isNowLiked
    }

    fun isSongLiked(songId: String): Boolean {
        return getLikedSongs().any { it.id == songId }
    }

    fun getPlayHistory(): List<Song> {
        val jsonString = prefs.getString("play_history_list", null) ?: return emptyList()
        return deserializeSongList(jsonString)
    }

    fun recordPlay(song: Song) {
        val current = getPlayHistory().toMutableList()
        current.removeAll { it.id == song.id }
        current.add(0, song)
        // Keep up to 50 recent items
        val trimmed = current.take(50)
        prefs.edit().putString("play_history_list", serializeSongList(trimmed)).apply()
    }

    fun getTheme(): AppTheme {
        val name = prefs.getString("app_theme", AppTheme.MIDNIGHT_DARK.name)
        return try {
            AppTheme.valueOf(name ?: AppTheme.MIDNIGHT_DARK.name)
        } catch (_: Exception) {
            AppTheme.MIDNIGHT_DARK
        }
    }

    fun setTheme(theme: AppTheme) {
        prefs.edit().putString("app_theme", theme.name).apply()
    }

    fun getQuality(): PlaybackQuality {
        val name = prefs.getString("playback_quality", PlaybackQuality.AUTO.name)
        return try {
            PlaybackQuality.valueOf(name ?: PlaybackQuality.AUTO.name)
        } catch (_: Exception) {
            PlaybackQuality.AUTO
        }
    }

    fun setQuality(quality: PlaybackQuality) {
        prefs.edit().putString("playback_quality", quality.name).apply()
    }

    private fun serializeSongList(list: List<Song>): String {
        val array = JSONArray()
        for (s in list) {
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("title", s.title)
            obj.put("artist", s.artist)
            obj.put("channel", s.channel)
            obj.put("thumbnail", s.thumbnail)
            obj.put("duration", s.duration)
            obj.put("url", s.url)
            array.put(obj)
        }
        return array.toString()
    }

    private fun deserializeSongList(json: String): List<Song> {
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<Song>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    Song(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        artist = obj.getString("artist"),
                        channel = obj.optString("channel", ""),
                        thumbnail = obj.getString("thumbnail"),
                        duration = obj.optInt("duration", 0),
                        url = obj.optString("url", "")
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }
}
