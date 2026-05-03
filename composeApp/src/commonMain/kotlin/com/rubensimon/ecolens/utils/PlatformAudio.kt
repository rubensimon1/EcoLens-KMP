package com.rubensimon.ecolens.utils

/**
 * SoundManager Multiplatform.
 * Reproduce sonidos y música utilizando las APIs nativas de cada plataforma.
 */
expect object PlatformAudio {
    fun playSound(soundName: String)
    fun playSuccess()
    fun playMusic(resourceName: String)
    fun stopMusic()
}
