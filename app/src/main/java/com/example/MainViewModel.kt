package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AmbientSoundEngine
import com.example.audio.PomodoroEngine
import com.example.audio.PomodoroMode
import com.example.data.model.AppTheme
import com.example.data.model.LyricLine
import com.example.data.model.LyricsData
import com.example.data.model.MoodCategory
import com.example.data.model.PlaybackQuality
import com.example.data.model.RepeatMode
import com.example.data.model.Song
import com.example.data.repository.LyricsRepository
import com.example.data.repository.MusicRepository
import com.example.data.repository.UserLibraryRepository
import com.example.player.PlayerPlaybackState
import com.example.player.YouTubePlayerController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val musicRepo = MusicRepository()
    private val lyricsRepo = LyricsRepository()
    private val libraryRepo = UserLibraryRepository(application)

    val playerController = YouTubePlayerController(application)
    val ambientEngine = AmbientSoundEngine()
    val pomodoroEngine = PomodoroEngine(viewModelScope)

    val playerState: StateFlow<PlayerPlaybackState> = playerController.playbackState

    // Navigation & UI Sheets
    val currentNavTab = MutableStateFlow(0) // 0: Home, 1: Search, 2: Queue, 3: Library, 4: Account
    val isNowPlayingExpanded = MutableStateFlow(false)
    val isToolsSheetVisible = MutableStateFlow(false)
    val nowPlayingTabIndex = MutableStateFlow(0) // 0: Player, 1: Lyrics, 2: Queue

    // Queue & Playback logic
    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _currentQueueIndex = MutableStateFlow(-1)
    val currentQueueIndex: StateFlow<Int> = _currentQueueIndex.asStateFlow()

    val repeatMode = MutableStateFlow(RepeatMode.OFF)
    val isShuffle = MutableStateFlow(false)

    // Lyrics
    private val _lyrics = MutableStateFlow<LyricsData?>(null)
    val lyrics: StateFlow<LyricsData?> = _lyrics.asStateFlow()
    val isLyricsLoading = MutableStateFlow(false)

    // Library & Theme
    val likedSongs = MutableStateFlow<List<Song>>(emptyList())
    val playHistory = MutableStateFlow<List<Song>>(emptyList())
    val currentTheme = MutableStateFlow(AppTheme.MIDNIGHT_DARK)
    val playbackQuality = MutableStateFlow(PlaybackQuality.AUTO)

    // Search
    val searchQuery = MutableStateFlow("")
    val searchResults = MutableStateFlow<List<Song>>(emptyList())
    val isSearching = MutableStateFlow(false)
    val searchError = MutableStateFlow<String?>(null)
    val playlistImportStatus = MutableStateFlow<String?>(null)

    // Curated moods for Home screen
    val moodCategories = listOf(
        MoodCategory("rain", "Rainy Night", "ambient rainy night chill lo-fi study", "🌧️", "Gentle rain & cozy beats"),
        MoodCategory("focus", "Deep Focus", "deep focus binaural study electronic ambient", "🧠", "Flow state instrumental frequencies"),
        MoodCategory("drive", "Late Night Drive", "synthwave night drive retro chillwave", "🚗", "Neon roads & synth nostalgia"),
        MoodCategory("morning", "Calm Morning", "calm morning acoustic coffee acoustic chill", "☕", "Soft sunbeams & acoustic warmth"),
        MoodCategory("workout", "Gym Energy", "high energy workout bass phonk electronic beats", "⚡", "Intense rhythm & cardio fuel"),
        MoodCategory("melancholy", "Melancholy", "sad acoustic slow piano ambient cinematic", "🍂", "Quiet emotional resonance"),
        MoodCategory("feelgood", "Feel-Good", "feel good upbeat indie pop sunshine grooves", "✨", "Uplifting vibrant melodies"),
        MoodCategory("cinematic", "Cinematic", "epic cinematic ambient orchestral soundscapes", "🌌", "Expansive sonic journeys"),
        MoodCategory("nostalgic", "Nostalgic", "2000s 90s vintage lofi nostalgia mix", "📼", "Memories of another era"),
        MoodCategory("chill", "Chill Vibes", "chillout downtempo ambient lounge relaxing", "🌿", "Easygoing calming rhythms")
    )

    private var searchDebounceJob: Job? = null

    init {
        // Load initial state
        currentTheme.value = libraryRepo.getTheme()
        playbackQuality.value = libraryRepo.getQuality()
        likedSongs.value = libraryRepo.getLikedSongs()
        playHistory.value = libraryRepo.getPlayHistory()

        val initialDiscovery = musicRepo.getInitialDiscoverySongs()
        _queue.value = initialDiscovery
        searchResults.value = initialDiscovery

        // Start ambient engine background thread
        ambientEngine.start()

        // Handle track end event from player
        playerController.onTrackEndedListener = {
            handleTrackEnded()
        }
    }

    fun playSong(song: Song) {
        val q = _queue.value.toMutableList()
        val existingIdx = q.indexOfFirst { it.id == song.id }
        if (existingIdx >= 0) {
            _currentQueueIndex.value = existingIdx
        } else {
            q.add(song)
            _queue.value = q
            _currentQueueIndex.value = q.lastIndex
        }

        playerController.playSong(song)
        libraryRepo.recordPlay(song)
        playHistory.value = libraryRepo.getPlayHistory()

        // Fetch lyrics
        fetchLyricsForSong(song)
    }

    fun togglePlayPause() {
        val st = playerState.value
        if (st.isPlaying) {
            playerController.pause()
        } else {
            if (st.currentSong != null) {
                playerController.play()
            } else if (_queue.value.isNotEmpty()) {
                val nextIdx = maxOf(0, _currentQueueIndex.value)
                playSong(_queue.value[nextIdx])
            }
        }
    }

    fun playNext() {
        val q = _queue.value
        if (q.isEmpty()) return

        val curIdx = _currentQueueIndex.value
        val nextIdx = if (isShuffle.value) {
            q.indices.filter { it != curIdx }.randomOrNull() ?: 0
        } else {
            if (curIdx + 1 < q.size) curIdx + 1 else 0
        }
        _currentQueueIndex.value = nextIdx
        playSong(q[nextIdx])
    }

    fun playPrevious() {
        val q = _queue.value
        if (q.isEmpty()) return

        // If played more than 3 seconds, replay current song
        if (playerState.value.currentPositionSeconds > 3.0) {
            playerController.seekTo(0.0)
            return
        }

        val curIdx = _currentQueueIndex.value
        val prevIdx = if (curIdx - 1 >= 0) curIdx - 1 else q.lastIndex
        _currentQueueIndex.value = prevIdx
        playSong(q[prevIdx])
    }

    private fun handleTrackEnded() {
        when (repeatMode.value) {
            RepeatMode.ONE -> {
                playerState.value.currentSong?.let { playSong(it) }
            }
            RepeatMode.ALL -> {
                playNext()
            }
            RepeatMode.OFF -> {
                val curIdx = _currentQueueIndex.value
                if (curIdx + 1 < _queue.value.size) {
                    playNext()
                } else {
                    playerController.pause()
                }
            }
        }
    }

    fun seekTo(seconds: Double) {
        playerController.seekTo(seconds)
    }

    fun addToQueue(song: Song) {
        val q = _queue.value.toMutableList()
        if (!q.any { it.id == song.id }) {
            q.add(song)
            _queue.value = q
        }
    }

    fun playNextInQueue(song: Song) {
        val q = _queue.value.toMutableList()
        val curIdx = _currentQueueIndex.value
        val targetIdx = if (curIdx >= 0 && curIdx < q.size) curIdx + 1 else 0
        q.removeIf { it.id == song.id }
        q.add(targetIdx, song)
        _queue.value = q
    }

    fun removeFromQueue(index: Int) {
        val q = _queue.value.toMutableList()
        if (index in q.indices) {
            val removingCurrent = index == _currentQueueIndex.value
            q.removeAt(index)
            _queue.value = q
            if (removingCurrent) {
                if (index < q.size) {
                    playSong(q[index])
                } else if (q.isNotEmpty()) {
                    playSong(q.first())
                }
            } else if (index < _currentQueueIndex.value) {
                _currentQueueIndex.value = _currentQueueIndex.value - 1
            }
        }
    }

    fun moveQueueItem(from: Int, to: Int) {
        val q = _queue.value.toMutableList()
        if (from in q.indices && to in q.indices) {
            val item = q.removeAt(from)
            q.add(to, item)
            _queue.value = q
        }
    }

    fun clearQueue() {
        val curSong = playerState.value.currentSong
        _queue.value = if (curSong != null) listOf(curSong) else emptyList()
        _currentQueueIndex.value = if (curSong != null) 0 else -1
    }

    fun toggleLike(song: Song) {
        libraryRepo.toggleLiked(song)
        likedSongs.value = libraryRepo.getLikedSongs()
    }

    fun isSongLiked(songId: String): Boolean {
        return likedSongs.value.any { it.id == songId }
    }

    fun cycleRepeatMode() {
        repeatMode.value = when (repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    fun toggleShuffle() {
        isShuffle.value = !isShuffle.value
    }

    fun setQuality(quality: PlaybackQuality) {
        playbackQuality.value = quality
        libraryRepo.setQuality(quality)
        playerController.setQuality(quality.name)
    }

    fun setTheme(theme: AppTheme) {
        currentTheme.value = theme
        libraryRepo.setTheme(theme)
    }

    fun onSearchQueryChanged(newQuery: String) {
        searchQuery.value = newQuery
        searchDebounceJob?.cancel()
        if (newQuery.isBlank()) {
            searchResults.value = musicRepo.getInitialDiscoverySongs()
            searchError.value = null
            return
        }

        searchDebounceJob = viewModelScope.launch {
            delay(500)
            executeSearch(newQuery)
        }
    }

    fun executeSearch(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            isSearching.value = true
            searchError.value = null
            val result = musicRepo.searchSongs(query)
            result.onSuccess {
                searchResults.value = it
                isSearching.value = false
            }.onFailure {
                searchError.value = it.localizedMessage ?: "Search failed. Showing cached songs."
                isSearching.value = false
            }
        }
    }

    fun onMoodSelected(mood: MoodCategory) {
        searchQuery.value = mood.name
        currentNavTab.value = 1 // Switch to Search tab to view mood discovery
        executeSearch(mood.query)
    }

    fun importPlaylistUrl(url: String) {
        if (url.isBlank()) return
        viewModelScope.launch {
            playlistImportStatus.value = "Importing playlist..."
            val res = musicRepo.importPlaylist(url)
            res.onSuccess { (title, songs) ->
                val currentQ = _queue.value.toMutableList()
                currentQ.addAll(songs)
                _queue.value = currentQ
                playlistImportStatus.value = "Successfully imported \"$title\" (${songs.size} tracks)"
                if (playerState.value.currentSong == null && songs.isNotEmpty()) {
                    playSong(songs.first())
                }
            }.onFailure { err ->
                playlistImportStatus.value = "Import error: ${err.message}"
            }
        }
    }

    private fun fetchLyricsForSong(song: Song) {
        viewModelScope.launch {
            isLyricsLoading.value = true
            _lyrics.value = null
            val data = lyricsRepo.getLyrics(song)
            _lyrics.value = data
            isLyricsLoading.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerController.release()
        ambientEngine.stop()
        pomodoroEngine.release()
    }
}
