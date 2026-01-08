package com.nextcs.aurora.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*

class VoiceNavigationService(private val context: Context) {
    private var tts: TextToSpeech? = null
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady
    
    private val _isEnabled = MutableStateFlow(true)
    val isEnabled: StateFlow<Boolean> = _isEnabled

    init {
        initializeTTS()
    }

    private fun initializeTTS() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.let { textToSpeech ->
                    val result = textToSpeech.setLanguage(Locale.US)
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        _isReady.value = false
                    } else {
                        textToSpeech.setSpeechRate(1.0f)
                        textToSpeech.setPitch(1.0f)
                        _isReady.value = true
                    }
                }
            } else {
                _isReady.value = false
            }
        }

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {}
            override fun onError(utteranceId: String?) {}
        })
    }

    fun setEnabled(enabled: Boolean) {
        _isEnabled.value = enabled
    }

    fun announce(text: String, priority: Int = TextToSpeech.QUEUE_ADD) {
        if (!_isEnabled.value || !_isReady.value) return
        
        tts?.speak(text, priority, null, UUID.randomUUID().toString())
    }

    fun announceImmediate(text: String) {
        if (!_isEnabled.value || !_isReady.value) return
        
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
    }

    // Navigation-specific announcements
    fun announceTurn(direction: String, distance: Int, roadName: String? = null) {
        val distanceText = when {
            distance < 100 -> "in ${distance} meters"
            distance < 1000 -> "in ${distance / 100 * 100} meters"
            else -> "in ${distance / 1000} kilometers"
        }
        
        val announcement = if (roadName != null) {
            "$direction $distanceText onto $roadName"
        } else {
            "$direction $distanceText"
        }
        
        announce(announcement)
    }

    fun announceSpeedWarning(currentSpeed: Int, speedLimit: Int) {
        announceImmediate("You are exceeding the speed limit. Current speed: $currentSpeed kilometers per hour")
    }

    fun announceHazard(hazardType: String, distance: Int) {
        val distanceText = if (distance < 1000) "${distance} meters" else "${distance / 1000} kilometers"
        announceImmediate("Warning: $hazardType ahead in $distanceText")
    }

    fun announceRouteRecalculation() {
        announceImmediate("Route recalculated")
    }

    fun announceDestination(eta: String) {
        announce("You will arrive at your destination in $eta")
    }

    fun announceArrival() {
        announce("You have arrived at your destination")
    }
    
    fun announceWeatherAlert(message: String) {
        announceImmediate("Weather alert: $message")
    }
    
    fun announceTrafficUpdate(message: String) {
        announce("Traffic update: $message")
    }
    
    fun announceReroute(reason: String) {
        announceImmediate("Rerouting: $reason")
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        _isReady.value = false
    }
}
