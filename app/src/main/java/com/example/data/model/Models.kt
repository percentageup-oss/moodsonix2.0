package com.example.data.model

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val channel: String = "",
    val thumbnail: String,
    val duration: Int = 0,
    val url: String = "https://www.youtube.com/watch?v=$id",
    val isPlayable: Boolean = true
)

data class LyricLine(
    val timeSeconds: Double,
    val text: String
)

data class LyricsData(
    val songId: String,
    val title: String,
    val artist: String,
    val plainLyrics: String? = null,
    val syncedLyrics: List<LyricLine> = emptyList(),
    val provider: String = "LRCLIB",
    val notFound: Boolean = false
)

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

enum class PlaybackQuality(val label: String, val hint: String) {
    AUTO("Auto", "Adapts to network speed"),
    TINY("Tiny (144p)", "Lowest data usage"),
    SMALL("Small (240p)", "Very low data"),
    MEDIUM("Medium (360p)", "Standard mobile quality"),
    LARGE("Large (480p)", "Good quality"),
    HD720("720p HD", "High quality, higher data"),
    HD1080("1080p HD", "Maximum quality, heavy data")
}

enum class AppTheme(val displayName: String, val subtitle: String) {
    MIDNIGHT_DARK("Midnight Dark", "Deep dark with #C8F542 lime accent (Default)"),
    NEON_CYBERPUNK("Neon Cyberpunk", "High-contrast neon green & vivid violet"),
    MINIMALIST_SLATE("Minimalist Slate", "Clean slate aesthetic with subtle contrast")
}

data class MoodCategory(
    val id: String,
    val name: String,
    val query: String,
    val emoji: String,
    val description: String
)
