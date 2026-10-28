package com.example.audio

import android.media.AudioManager
import android.media.ToneGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class PomodoroMode(val durationMinutes: Int, val label: String) {
    FOCUS(25, "Deep Focus"),
    SHORT_BREAK(5, "Short Break")
}

data class PomodoroState(
    val mode: PomodoroMode = PomodoroMode.FOCUS,
    val isRunning: Boolean = false,
    val secondsRemaining: Int = 25 * 60,
    val totalSeconds: Int = 25 * 60,
    val completedSessions: Int = 0
)

class PomodoroEngine(private val scope: CoroutineScope) {

    private val _state = MutableStateFlow(PomodoroState())
    val state: StateFlow<PomodoroState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
        } catch (_: Exception) {}
    }

    fun start() {
        if (_state.value.isRunning) return
        _state.value = _state.value.copy(isRunning = true)

        timerJob?.cancel()
        timerJob = scope.launch(Dispatchers.Default) {
            while (isActive && _state.value.isRunning && _state.value.secondsRemaining > 0) {
                delay(1000)
                val newRemaining = _state.value.secondsRemaining - 1
                if (newRemaining <= 0) {
                    onTimerFinished()
                    break
                } else {
                    _state.value = _state.value.copy(secondsRemaining = newRemaining)
                }
            }
        }
    }

    fun pause() {
        _state.value = _state.value.copy(isRunning = false)
        timerJob?.cancel()
    }

    fun reset() {
        pause()
        val total = _state.value.mode.durationMinutes * 60
        _state.value = _state.value.copy(
            secondsRemaining = total,
            totalSeconds = total
        )
    }

    fun setMode(mode: PomodoroMode) {
        pause()
        val total = mode.durationMinutes * 60
        _state.value = _state.value.copy(
            mode = mode,
            secondsRemaining = total,
            totalSeconds = total
        )
    }

    private fun onTimerFinished() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 400)
        } catch (_: Exception) {}

        val nextMode = if (_state.value.mode == PomodoroMode.FOCUS) {
            PomodoroMode.SHORT_BREAK
        } else {
            PomodoroMode.FOCUS
        }
        val completed = if (_state.value.mode == PomodoroMode.FOCUS) {
            _state.value.completedSessions + 1
        } else {
            _state.value.completedSessions
        }
        val total = nextMode.durationMinutes * 60

        _state.value = _state.value.copy(
            mode = nextMode,
            isRunning = false,
            secondsRemaining = total,
            totalSeconds = total,
            completedSessions = completed
        )
    }

    fun release() {
        timerJob?.cancel()
        toneGenerator?.release()
        toneGenerator = null
    }
}
