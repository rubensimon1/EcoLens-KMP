package com.rubensimon.ecolens.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.*
import platform.CoreGraphics.*
import platform.CoreImage.*
import platform.Foundation.*
import platform.UIKit.*

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PlatformQRCodeView(
    content: String,
    modifier: Modifier
) {
    UIKitView(
        modifier = modifier,
        factory = {
            UIImageView().apply {
                contentMode = UIViewContentMode.UIViewContentModeScaleAspectFit
                backgroundColor = UIColor.whiteColor
                clipsToBounds = true
            }
        },
        update = { imageView ->

            if (content.isBlank()) {
                imageView.image = null
                return@UIKitView
            }

            try {

                // String -> NSData
                val bytes = content.encodeToByteArray()

                val data = bytes.usePinned {
                    NSData.dataWithBytes(
                        bytes = it.addressOf(0),
                        length = bytes.size.toULong()
                    )
                }

                // QR Filter
                val qrFilter = CIFilter.filterWithName("CIQRCodeGenerator")
                    ?: return@UIKitView

                qrFilter.setValue(data, forKey = "inputMessage")
                qrFilter.setValue("M", forKey = "inputCorrectionLevel")

                val outputImage = qrFilter.outputImage
                    ?: return@UIKitView

                // Escalar QR
                val transformed = outputImage.imageByApplyingTransform(
                    CGAffineTransformMakeScale(12.0, 12.0)
                )

                // Contexto normal (SIN software renderer)
                val context = CIContext()

                val cgImage = context.createCGImage(
                    transformed,
                    fromRect = transformed.extent
                ) ?: return@UIKitView

                // UIImage final
                imageView.image = UIImage.imageWithCGImage(cgImage)

            } catch (e: Exception) {
                println("QR ERROR: ${e.message}")
            }
        }
    )
}
