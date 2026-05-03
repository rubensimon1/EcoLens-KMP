package com.rubensimon.ecolens.utils

import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSBundle
import platform.Foundation.NSURL
import platform.AudioToolbox.AudioServicesPlaySystemSound
import platform.AudioToolbox.SystemSoundID
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
actual object PlatformAudio {
    private var musicPlayer: platform.AVFAudio.AVAudioPlayer? = null
    
    actual fun playSound(soundName: String) {
        val soundId: SystemSoundID = if (soundName == "success") 1054u else 1052u
        AudioServicesPlaySystemSound(soundId)
    }

    actual fun playSuccess() {
        playSound("success")
    }

    actual fun playMusic(resourceName: String) {
        val bundle = platform.Foundation.NSBundle.mainBundle
        val path = bundle.pathForResource(resourceName, "mp3") ?: bundle.pathForResource(resourceName, "wav")
        if (path != null) {
            val url = platform.Foundation.NSURL.fileURLWithPath(path)
            try {
                val player = platform.AVFAudio.AVAudioPlayer(contentsOfURL = url, error = null)
                player.numberOfLoops = -1L
                player.prepareToPlay()
                player.play()
                musicPlayer = player
            } catch (e: Exception) {
                println("[PlatformAudio] Error iOS: ${e.message}")
            }
        }
    }

    actual fun stopMusic() {
        musicPlayer?.stop()
        musicPlayer = null
    }
}
