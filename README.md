# Moodsonix 2.0 (Android)

Moodsonix is a music streaming and audio atmosphere companion app built with Kotlin and Jetpack Compose for Android. It combines YouTube playback, YouTube Music discovery, synchronized lyrics from LRCLIB, Pomodoro productivity timers, and real-time synthesized ambient soundscapes.

## Features

- **Music Search Engine**: Real-time YouTube Music and video search with resilient fallback instances, query normalization, and fast caching.
- **Official YouTube Playback**: Bidirectional playback control powered by the official YouTube IFrame Player API with play, pause, seek, volume, repeat modes, and shuffle.
- **Synchronized Lyrics (LRCLIB First, No AI)**: Real-time synchronized LRC lyrics from LRCLIB with line-by-line active highlighting, tap-to-seek playback synchronization, auto-scrolling, and plain lyrics fallback with provider attribution.
- **Mood Discovery**: 10+ curated sonic atmospheres (Rainy Night, Deep Focus, Late Night Drive, Calm Morning, Gym Energy, Melancholy, Feel-Good, Cinematic, Nostalgic, Chill Vibes) plus custom natural language mood search.
- **YouTube Playlist Importer**: Paste public YouTube playlist links to import and queue tracks instantly.
- **Playback Queue & Library**: Full queue management (reorder, play next, remove, clear) plus persistent liked songs and listening history.
- **Synthesized Ambient Sound Engine**: Real-time synthesized ambient audio mixer (Rain, Campfire, Nature) powered by low-latency PCM audio generation, playing simultaneously alongside music.
- **Pomodoro Focus Timer**: 25-minute deep focus and 5-minute break timer with circular visual progress and notification tones.
- **Theme Customizer**: Three polished appearance presets:
  - *Midnight Dark* (Default): Deep #0A0A0F canvas with #C8F542 electric lime accents.
  - *Neon Cyberpunk*: High-contrast neon green and vivid purple highlights.
  - *Minimalist Slate*: Clean slate aesthetic with balanced contrast.
- **Quality & Data Saver**: Select preferred video streaming resolution (Auto, Tiny, Small, Medium, Large, 720p, 1080p).