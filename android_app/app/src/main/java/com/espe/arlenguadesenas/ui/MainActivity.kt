package com.espe.arlenguadesenas.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.espe.arlenguadesenas.speech.TextToSpeechHelper
import com.espe.arlenguadesenas.ui.screens.*
import com.espe.arlenguadesenas.ui.theme.ARLenguaDeSenasTheme

class MainActivity : ComponentActivity() {

    private lateinit var ttsHelper: TextToSpeechHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ttsHelper = TextToSpeechHelper(this)

        setContent {
            ARLenguaDeSenasTheme {
                var currentScreen by remember { mutableStateOf("home") }

                when (currentScreen) {
                    "home" -> HomeScreen(
                        onNavigateToSignToText = { currentScreen = "sign_to_text" },
                        onNavigateToTextToSign = { currentScreen = "text_to_sign" },
                        onNavigateToDictionary = { currentScreen = "dictionary" }
                    )
                    "sign_to_text" -> SignToTextScreen(
                        onSpeakResult = { text -> ttsHelper.speak(text) }
                    )
                    "text_to_sign" -> TextToSignArScreen()
                    "dictionary" -> DictionaryScreen()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsHelper.shutdown()
    }
}
