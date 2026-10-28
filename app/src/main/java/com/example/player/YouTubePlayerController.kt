package com.example.player

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlayerPlaybackState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionSeconds: Double = 0.0,
    val durationSeconds: Double = 0.0,
    val isReady: Boolean = false,
    val errorCode: Int? = null
)

class YouTubePlayerController(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())

    private val _playbackState = MutableStateFlow(PlayerPlaybackState())
    val playbackState: StateFlow<PlayerPlaybackState> = _playbackState.asStateFlow()

    var onTrackEndedListener: (() -> Unit)? = null

    private var _webView: WebView? = null

    @SuppressLint("SetJavaScriptEnabled")
    fun getOrCreateWebView(): WebView {
        _webView?.let { return it }
        val wv = WebView(context).apply {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false
                cacheMode = WebSettings.LOAD_NO_CACHE
                databaseEnabled = false
            }
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                }

                override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                    return true
                }
            }
            addJavascriptInterface(AndroidJsBridge(), "AndroidInterface")
        }
        _webView = wv
        return wv
    }

    val webView: WebView
        get() = getOrCreateWebView()

    private var currentVideoId: String = ""
    private var isPlayerHtmlLoaded: Boolean = false

    private fun loadPlayerHtml(initialId: String) {
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
              <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
              <style>
                * { margin:0; padding:0; box-sizing: border-box; background: #000; }
                html, body { width: 100%; height: 100%; overflow: hidden; }
                #player { width: 100%; height: 100%; }
              </style>
            </head>
            <body>
              <div id="player"></div>
              <script>
                var tag = document.createElement('script');
                tag.src = "https://www.youtube.com/iframe_api";
                var firstScriptTag = document.getElementsByTagName('script')[0];
                firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

                var player;
                var progressTimer = null;

                function onYouTubeIframeAPIReady() {
                  player = new YT.Player('player', {
                    width: '100%',
                    height: '100%',
                    videoId: '$initialId',
                    playerVars: {
                      'autoplay': 1,
                      'controls': 0,
                      'playsinline': 1,
                      'rel': 0,
                      'fs': 0,
                      'modestbranding': 1,
                      'origin': 'https://localhost'
                    },
                    events: {
                      'onReady': onPlayerReady,
                      'onStateChange': onPlayerStateChange,
                      'onError': onPlayerError
                    }
                  });
                }

                function onPlayerReady(event) {
                  window.AndroidInterface.onReady();
                  if (progressTimer) clearInterval(progressTimer);
                  progressTimer = setInterval(function() {
                    if (player && typeof player.getCurrentTime === 'function') {
                      var cur = player.getCurrentTime() || 0;
                      var dur = player.getDuration() || 0;
                      window.AndroidInterface.onProgress(cur, dur);
                    }
                  }, 500);
                }

                function onPlayerStateChange(event) {
                  window.AndroidInterface.onStateChange(event.data);
                }

                function onPlayerError(event) {
                  window.AndroidInterface.onError(event.data);
                }

                function play() { if (player && player.playVideo) player.playVideo(); }
                function pause() { if (player && player.pauseVideo) player.pauseVideo(); }
                function loadVideo(id) {
                  if (player && player.loadVideoById) {
                    player.loadVideoById(id);
                  }
                }
                function seekTo(sec) { if (player && player.seekTo) player.seekTo(sec, true); }
                function setVolume(v) { if (player && player.setVolume) player.setVolume(v); }
                function setQuality(q) {
                  if (player && player.setPlaybackQuality) {
                    player.setPlaybackQuality(q);
                  }
                }
              </script>
            </body>
            </html>
        """.trimIndent()

        val wv = getOrCreateWebView()
        wv.loadDataWithBaseURL("https://localhost", html, "text/html", "utf-8", null)
    }

    fun playSong(song: Song) {
        _playbackState.value = _playbackState.value.copy(
            currentSong = song,
            currentPositionSeconds = 0.0,
            durationSeconds = song.duration.toDouble(),
            isBuffering = true,
            errorCode = null
        )
        currentVideoId = song.id
        mainHandler.post {
            val wv = getOrCreateWebView()
            if (!isPlayerHtmlLoaded) {
                isPlayerHtmlLoaded = true
                loadPlayerHtml(song.id)
            } else {
                wv.evaluateJavascript("loadVideo('${song.id}');", null)
            }
        }
    }

    fun play() {
        mainHandler.post {
            _webView?.evaluateJavascript("play();", null)
        }
    }

    fun pause() {
        mainHandler.post {
            _webView?.evaluateJavascript("pause();", null)
        }
    }

    fun seekTo(seconds: Double) {
        _playbackState.value = _playbackState.value.copy(currentPositionSeconds = seconds)
        mainHandler.post {
            _webView?.evaluateJavascript("seekTo($seconds);", null)
        }
    }

    fun setQuality(quality: String) {
        val ytbQuality = when (quality.uppercase()) {
            "TINY" -> "small"
            "SMALL" -> "small"
            "MEDIUM" -> "medium"
            "LARGE" -> "large"
            "HD720" -> "hd720"
            "HD1080" -> "hd1080"
            else -> "default"
        }
        mainHandler.post {
            _webView?.evaluateJavascript("setQuality('$ytbQuality');", null)
        }
    }

    fun release() {
        mainHandler.post {
            _webView?.stopLoading()
            _webView?.destroy()
            _webView = null
        }
    }

    private inner class AndroidJsBridge {

        @JavascriptInterface
        fun onReady() {
            mainHandler.post {
                _playbackState.value = _playbackState.value.copy(isReady = true)
                if (currentVideoId.isNotEmpty()) {
                    _webView?.evaluateJavascript("loadVideo('$currentVideoId');", null)
                }
            }
        }

        @JavascriptInterface
        fun onStateChange(state: Int) {
            mainHandler.post {
                // YT.PlayerState:
                // -1 (unstarted), 0 (ended), 1 (playing), 2 (paused), 3 (buffering), 5 (video cued)
                when (state) {
                    0 -> { // Ended
                        _playbackState.value = _playbackState.value.copy(
                            isPlaying = false,
                            isBuffering = false
                        )
                        onTrackEndedListener?.invoke()
                    }
                    1 -> { // Playing
                        _playbackState.value = _playbackState.value.copy(
                            isPlaying = true,
                            isBuffering = false
                        )
                    }
                    2 -> { // Paused
                        _playbackState.value = _playbackState.value.copy(
                            isPlaying = false,
                            isBuffering = false
                        )
                    }
                    3 -> { // Buffering
                        _playbackState.value = _playbackState.value.copy(isBuffering = true)
                    }
                }
            }
        }

        @JavascriptInterface
        fun onProgress(currentSec: Float, durationSec: Float) {
            mainHandler.post {
                val dur = if (durationSec > 0) durationSec.toDouble() else _playbackState.value.durationSeconds
                _playbackState.value = _playbackState.value.copy(
                    currentPositionSeconds = currentSec.toDouble(),
                    durationSeconds = dur
                )
            }
        }

        @JavascriptInterface
        fun onError(errorCode: Int) {
            mainHandler.post {
                _playbackState.value = _playbackState.value.copy(
                    errorCode = errorCode,
                    isBuffering = false
                )
            }
        }
    }
}
