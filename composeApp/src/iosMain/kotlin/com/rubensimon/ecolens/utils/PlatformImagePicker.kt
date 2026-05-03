package com.rubensimon.ecolens.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.interop.LocalUIViewController
import platform.UIKit.*
import platform.Foundation.*
import platform.PhotosUI.*
import platform.Photos.*
import platform.darwin.*
import platform.posix.memcpy
import kotlinx.cinterop.*

@OptIn(ExperimentalForeignApi::class)
actual class PlatformImagePicker(
    private val onLaunch: (PlatformImagePicker) -> Unit
) {
    var activeDelegate: Any? = null

    actual fun launchPicker() {
        onLaunch(this)
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberPlatformImagePicker(onImagePicked: (ByteArray) -> Unit): PlatformImagePicker {
    val viewController = LocalUIViewController.current
    
    return remember(viewController) {
        PlatformImagePicker { pickerInstance ->
            val config = PHPickerConfiguration()
            config.selectionLimit = 1L
            config.filter = PHPickerFilter.imagesFilter()
            
            val pickerController = PHPickerViewController(configuration = config)
            val delegate = object : NSObject(), PHPickerViewControllerDelegateProtocol {
                override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
                    picker.dismissViewControllerAnimated(true, null)
                    
                    val result = didFinishPicking.firstOrNull() as? PHPickerResult ?: return
                    val provider = result.itemProvider
                    
                    val typeIdentifier = "public.image"
                    if (provider.hasItemConformingToTypeIdentifier(typeIdentifier)) {
                        provider.loadDataRepresentationForTypeIdentifier(typeIdentifier) { data, error ->
                            if (error == null && data != null) {
                                // Importante: Volver al hilo principal para manipular UI/State de Compose
                                dispatch_async(dispatch_get_main_queue()) {
                                    val bytes = ByteArray(data.length.toInt())
                                    memcpy(bytes.refTo(0), data.bytes, data.length)
                                    onImagePicked(bytes)
                                    pickerInstance.activeDelegate = null
                                }
                            } else {
                                dispatch_async(dispatch_get_main_queue()) {
                                    pickerInstance.activeDelegate = null
                                }
                            }
                        }
                    } else {
                        pickerInstance.activeDelegate = null
                    }
                }
            }
            
            pickerController.delegate = delegate
            // Guardamos el delegado en la instancia de PlatformImagePicker para que no sea recolectado
            pickerInstance.activeDelegate = delegate
            
            viewController.presentViewController(pickerController, animated = true, completion = null)
        }
    }
}
