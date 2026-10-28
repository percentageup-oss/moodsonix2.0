package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainViewModel
import com.example.ui.components.SongItemRow
import com.example.ui.components.TechSectionHeader

@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val likedSongs by viewModel.likedSongs.collectAsState()
    val historySongs by viewModel.playHistory.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TechSectionHeader(label = "// YOUR LIBRARY")
            Spacer(modifier = Modifier.height(6.dp))

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "FAVORITES (${likedSongs.size})",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "HISTORY (${historySongs.size})",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                )
            }
        }

        if (selectedTab == 0) {
            // Liked songs actions
            if (likedSongs.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.clearQueue()
                                likedSongs.forEach { viewModel.addToQueue(it) }
                                viewModel.playSong(likedSongs.first())
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("liked_play_all_btn")
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Play All")
                        }

                        OutlinedButton(
                            onClick = {
                                val shuffled = likedSongs.shuffled()
                                viewModel.clearQueue()
                                shuffled.forEach { viewModel.addToQueue(it) }
                                viewModel.playSong(shuffled.first())
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("liked_shuffle_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Shuffle, contentDescription = "Shuffle")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Shuffle")
                        }
                    }
                }

                items(likedSongs) { song ->
                    val curSong = viewModel.playerState.value.currentSong
                    val isCurrent = curSong?.id == song.id
                    val isPlaying = isCurrent && viewModel.playerState.value.isPlaying

                    SongItemRow(
                        song = song,
                        isCurrentPlaying = isCurrent,
                        isPlaying = isPlaying,
                        isLiked = true,
                        onPlayClick = { viewModel.playSong(song) },
                        onQueueClick = { viewModel.addToQueue(song) },
                        onLikeClick = { viewModel.toggleLike(song) }
                    )
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 50.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No liked songs yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap the heart icon on any track to save it here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            // History tab
            if (historySongs.isNotEmpty()) {
                items(historySongs) { song ->
                    val curSong = viewModel.playerState.value.currentSong
                    val isCurrent = curSong?.id == song.id
                    val isPlaying = isCurrent && viewModel.playerState.value.isPlaying
                    val isLiked = viewModel.isSongLiked(song.id)

                    SongItemRow(
                        song = song,
                        isCurrentPlaying = isCurrent,
                        isPlaying = isPlaying,
                        isLiked = isLiked,
                        onPlayClick = { viewModel.playSong(song) },
                        onQueueClick = { viewModel.addToQueue(song) },
                        onLikeClick = { viewModel.toggleLike(song) }
                    )
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 50.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No listening history yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
