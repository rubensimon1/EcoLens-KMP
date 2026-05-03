package com.rubensimon.ecolens.utils

import android.media.AudioManager
import android.media.ToneGenerator

/**
 * Implementación de audio para Android. 
 * Usando ToneGenerator como fallback de efectos rápido y nativo.
 */
actual object PlatformAudio {
    
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        } catch (e: Exception) {
            println("[PlatformAudio] Error init: ${e.message}")
        }
    }

    private var mediaPlayer: android.media.MediaPlayer? = null
    private var appContext: android.content.Context? = null

    fun setContext(context: android.content.Context) {
        appContext = context
    }

    actual fun playSound(soundName: String) {
        val tone = when (soundName) {
            "success" -> ToneGenerator.TONE_PROP_ACK
            "error" -> ToneGenerator.TONE_PROP_NACK
            else -> ToneGenerator.TONE_PROP_BEEP
        }
        try {
            toneGenerator?.startTone(tone, 150)
        } catch (e: Exception) { }
    }

    actual fun playSuccess() {
        playSound("success")
    }

    actual fun playMusic(resourceName: String) {
        val ctx = appContext ?: return
        try {
            stopMusic()
            val resId = ctx.resources.getIdentifier(resourceName, "raw", ctx.packageName)
            if (resId != 0) {
                mediaPlayer = android.media.MediaPlayer.create(ctx, resId).apply {
                    isLooping = true
                    start()
                }
            }
        } catch (e: Exception) {
            println("[PlatformAudio] Error Music: ${e.message}")
        }
    }

    actual fun stopMusic() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) { }
    }
}
