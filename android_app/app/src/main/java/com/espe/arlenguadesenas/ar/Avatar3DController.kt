package com.espe.arlenguadesenas.ar

class Avatar3DController {

    enum class PlaybackState {
        IDLE, PLAYING, PAUSED, FINISHED
    }

    var currentState = PlaybackState.IDLE
        private set
    var playbackSpeed = 1.0f
        private set

    fun playSignAnimation(clipName: String) {
        currentState = PlaybackState.PLAYING
    }

    fun pause() {
        if (currentState == PlaybackState.PLAYING) {
            currentState = PlaybackState.PAUSED
        }
    }

    fun resume() {
        if (currentState == PlaybackState.PAUSED) {
            currentState = PlaybackState.PLAYING
        }
    }

    fun setSpeed(speed: Float) {
        playbackSpeed = speed.coerceIn(0.25f, 2.0f)
    }

    fun resetToIdle() {
        currentState = PlaybackState.IDLE
    }
}
