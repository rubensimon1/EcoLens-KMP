package com.rubensimon.ecolens.utils

import androidx.compose.runtime.Composable

/**
 * Selector de imágenes multiplataforma.
 */
expect class PlatformImagePicker {
    fun launchPicker()
}

@Composable
expect fun rememberPlatformImagePicker(onImagePicked: (ByteArray) -> Unit): PlatformImagePicker
