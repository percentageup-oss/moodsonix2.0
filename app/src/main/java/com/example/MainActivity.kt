package com.example

import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.NowPlayingSheet
import com.example.ui.components.PlayerBar
import com.example.ui.components.ToolsBottomSheet
import com.example.ui.screens.AccountScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.QueueScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.theme.MoodsonixTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            val cacheBase = java.io.File(cacheDir, "WebView/Default/HTTP Cache/Code Cache")
            java.io.File(cacheBase, "js").mkdirs()
            java.io.File(cacheBase, "wasm").mkdirs()

            // Remove zero-byte or corrupt variations seed files to prevent seed missing signature warnings
            val dataDir = applicationInfo.dataDir
            val appWebviewDir = java.io.File(dataDir, "app_webview")
            if (appWebviewDir.exists()) {
                val seed = java.io.File(appWebviewDir, "variations_seed")
                val seedNew = java.io.File(appWebviewDir, "variations_seed_new")
                if (seed.exists() && seed.length() < 100L) seed.delete()
                if (seedNew.exists() && seedNew.length() < 100L) seedNew.delete()
            }
        } catch (_: Exception) {}

        enableEdgeToEdge()

        setContent {
            val viewModel: MainViewModel = viewModel()
            val currentTheme by viewModel.currentTheme.collectAsState()

            MoodsonixTheme(appTheme = currentTheme) {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: MainViewModel) {
    val navTab by viewModel.currentNavTab.collectAsState()
    val isNowPlayingExpanded by viewModel.isNowPlayingExpanded.collectAsState()
    val isToolsVisible by viewModel.isToolsSheetVisible.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val queue by viewModel.queue.collectAsState()
    val queueIndex by viewModel.currentQueueIndex.collectAsState()
    val lyricsData by viewModel.lyrics.collectAsState()
    val isLyricsLoading by viewModel.isLyricsLoading.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val isShuffle by viewModel.isShuffle.collectAsState()
    val quality by viewModel.playbackQuality.collectAsState()
    val currentTheme by viewModel.currentTheme.collectAsState()
    val nowPlayingTab by viewModel.nowPlayingTabIndex.collectAsState()

    // Handle back presses gracefully
    BackHandler(enabled = isNowPlayingExpanded || isToolsVisible || navTab != 0) {
        when {
            isNowPlayingExpanded -> viewModel.isNowPlayingExpanded.value = false
            isToolsVisible -> viewModel.isToolsSheetVisible.value = false
            navTab != 0 -> viewModel.currentNavTab.value = 0
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Player WebView is activated once playback is initiated
        if (playerState.currentSong != null) {
            Box(
                modifier = Modifier
                    .size(1.dp)
                    .align(Alignment.TopStart)
            ) {
                AndroidView(
                    factory = {
                        val wv = viewModel.playerController.getOrCreateWebView()
                        wv.setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
                        if (wv.parent != null) {
                            (wv.parent as? ViewGroup)?.removeView(wv)
                        }
                        wv
                    },
                    modifier = Modifier.size(1.dp)
                )
            }
        }

        Scaffold(
            bottomBar = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Mini Player Bar (shown whenever a song is loaded or ready)
                    if (playerState.currentSong != null && !isNowPlayingExpanded) {
                        val isLiked = viewModel.isSongLiked(playerState.currentSong?.id ?: "")
                        PlayerBar(
                            playbackState = playerState,
                            isLiked = isLiked,
                            onBarClick = { viewModel.isNowPlayingExpanded.value = true },
                            onPlayPauseClick = { viewModel.togglePlayPause() },
                            onNextClick = { viewModel.playNext() },
                            onLikeClick = {
                                playerState.currentSong?.let { viewModel.toggleLike(it) }
                            }
                        )
                    }

                    // Bottom Navigation Bar
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("main_bottom_nav")
                    ) {
                        NavigationBarItem(
                            selected = navTab == 0,
                            onClick = { viewModel.currentNavTab.value = 0 },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                            label = { Text("Home", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                        )
                        NavigationBarItem(
                            selected = navTab == 1,
                            onClick = { viewModel.currentNavTab.value = 1 },
                            icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                            label = { Text("Search", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                        )
                        NavigationBarItem(
                            selected = navTab == 2,
                            onClick = { viewModel.currentNavTab.value = 2 },
                            icon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Queue") },
                            label = { Text("Queue", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                        )
                        NavigationBarItem(
                            selected = navTab == 3,
                            onClick = { viewModel.currentNavTab.value = 3 },
                            icon = { Icon(Icons.Default.Favorite, contentDescription = "Liked") },
                            label = { Text("Liked", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                        )
                        NavigationBarItem(
                            selected = navTab == 4,
                            onClick = { viewModel.currentNavTab.value = 4 },
                            icon = { Icon(Icons.Default.Person, contentDescription = "Account") },
                            label = { Text("Account", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (navTab) {
                    0 -> HomeScreen(
                        viewModel = viewModel,
                        onNavigateToSearch = { viewModel.currentNavTab.value = 1 }
                    )
                    1 -> SearchScreen(viewModel = viewModel)
                    2 -> QueueScreen(viewModel = viewModel)
                    3 -> LibraryScreen(viewModel = viewModel)
                    4 -> AccountScreen(viewModel = viewModel)
                }

                // Floating Tools FAB in Home or Library screens
                if (navTab == 0 || navTab == 4) {
                    FloatingActionButton(
                        onClick = { viewModel.isToolsSheetVisible.value = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = CircleShape,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 16.dp)
                            .testTag("floating_tools_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Open Tools"
                        )
                    }
                }
            }
        }

        // Full Screen Now Playing Sheet
        if (isNowPlayingExpanded && playerState.currentSong != null) {
            val isLiked = viewModel.isSongLiked(playerState.currentSong?.id ?: "")
            NowPlayingSheet(
                playbackState = playerState,
                queue = queue,
                currentQueueIndex = queueIndex,
                lyricsData = lyricsData,
                isLyricsLoading = isLyricsLoading,
                isLiked = isLiked,
                repeatMode = repeatMode,
                isShuffle = isShuffle,
                quality = quality,
                selectedTab = nowPlayingTab,
                onTabSelected = { viewModel.nowPlayingTabIndex.value = it },
                onCollapseClick = { viewModel.isNowPlayingExpanded.value = false },
                onPlayPauseClick = { viewModel.togglePlayPause() },
                onNextClick = { viewModel.playNext() },
                onPreviousClick = { viewModel.playPrevious() },
                onSeek = { viewModel.seekTo(it) },
                onToggleLike = {
                    playerState.currentSong?.let { viewModel.toggleLike(it) }
                },
                onToggleRepeat = { viewModel.cycleRepeatMode() },
                onToggleShuffle = { viewModel.toggleShuffle() },
                onQualitySelected = { viewModel.setQuality(it) },
                onPlayQueuedSong = { viewModel.playSong(it) },
                onRemoveFromQueue = { viewModel.removeFromQueue(it) },
                onMoveQueueItem = { from, to -> viewModel.moveQueueItem(from, to) },
                onClearQueue = { viewModel.clearQueue() },
                onOpenTools = { viewModel.isToolsSheetVisible.value = true }
            )
        }

        // Floating Tools Bottom Sheet (Focus Pomodoro + Ambient Mixer + Themes)
        if (isToolsVisible) {
            ToolsBottomSheet(
                pomodoroEngine = viewModel.pomodoroEngine,
                ambientEngine = viewModel.ambientEngine,
                currentTheme = currentTheme,
                onThemeSelected = { viewModel.setTheme(it) },
                onDismissRequest = { viewModel.isToolsSheetVisible.value = false }
            )
        }
    }
}
