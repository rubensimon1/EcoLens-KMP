package com.rubensimon.ecolens.utils

import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.popoverPresentationController
import kotlinx.cinterop.ExperimentalForeignApi
import androidx.compose.ui.interop.LocalUIViewController

actual object PlatformShare {
    @OptIn(ExperimentalForeignApi::class)
    actual fun shareText(title: String, text: String) {
        val window = UIApplication.sharedApplication.keyWindow
        val rootViewController = window?.rootViewController
        
        if (rootViewController != null) {
            val activityController = UIActivityViewController(
                activityItems = listOf(text),
                applicationActivities = null
            )
            
            // Necesario para iPad (para evitar crash si no hay popover configurado)
            activityController.popoverPresentationController()?.let {
                it.sourceView = rootViewController.view
                it.sourceRect = rootViewController.view.bounds
            }
            
            rootViewController.presentViewController(
                viewControllerToPresent = activityController,
                animated = true,
                completion = null
            )
        }
    }
}
