package com.rubensimon.ecolens.utils

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

actual class PlatformImagePicker(
    private val onLaunch: () -> Unit
) {
    actual fun launchPicker() {
        onLaunch()
    }
}

@Composable
actual fun rememberPlatformImagePicker(onImagePicked: (ByteArray) -> Unit): PlatformImagePicker {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val bytes = stream.readBytes()
                    onImagePicked(bytes)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    return remember {
        PlatformImagePicker {
            launcher.launch("image/*")
        }
    }
}
