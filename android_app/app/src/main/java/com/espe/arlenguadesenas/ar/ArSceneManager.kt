package com.espe.arlenguadesenas.ar

import android.content.Context

class ArSceneManager(private val context: Context) {
    val planeHelper = PlaneAnchorHelper(context)
    val avatarController = Avatar3DController()
    var isSessionActive = false

    fun startSession() {
        isSessionActive = true
    }

    fun stopSession() {
        isSessionActive = false
        avatarController.resetToIdle()
    }
}
