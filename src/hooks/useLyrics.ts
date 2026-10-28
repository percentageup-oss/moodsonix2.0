import { useState, useEffect, useRef } from 'react';
import { Song, LyricsData } from '../types/music';

export function useLyrics(currentSong: Song | null, currentTime: number) {
  const [lyrics, setLyrics] = useState<LyricsData | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const activeSongId = useRef<string | null>(null);

  useEffect(() => {
    if (!currentSong) {
      setLyrics(null);
      setIsLoading(false);
      return;
    }

    activeSongId.current = currentSong.id;
    setIsLoading(true);
    setError(null);

    const controller = new AbortController();

    const fetchLyrics = async () => {
      try {
        const params = new URLSearchParams({
          id: currentSong.id,
          title: currentSong.title,
          artist: currentSong.artist,
          duration: (currentSong.duration || 0).toString()
        });

        const res = await fetch(`/api/lyrics?${params.toString()}`, {
          signal: controller.signal
        });

        if (!res.ok) throw new Error('Lyrics request failed');
        const data: LyricsData = await res.json();

        // Check if track is still the current active track
        if (activeSongId.current === currentSong.id) {
          setLyrics(data);
          setIsLoading(false);
        }
      } catch (err: any) {
        if (err.name !== 'AbortError' && activeSongId.current === currentSong.id) {
          setError(err.message || 'Could not fetch lyrics');
          setIsLoading(false);
        }
      }
    };

    fetchLyrics();

    return () => {
      controller.abort();
    };
  }, [currentSong?.id]);

  // Calculate current active line index for synced lyrics
  let activeLineIndex = -1;
  if (lyrics?.syncedLyrics && lyrics.syncedLyrics.length > 0) {
    for (let i = 0; i < lyrics.syncedLyrics.length; i++) {
      if (currentTime >= lyrics.syncedLyrics[i].time - 0.2) {
        activeLineIndex = i;
      } else {
        break;
      }
    }
  }

  return {
    lyrics,
    isLoading,
    error,
    activeLineIndex
  };
}
