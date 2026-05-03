package com.rubensimon.ecolens.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreImage.*
import platform.Foundation.*
import platform.UIKit.*

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PlatformQRCodeView(
    content: String,
    modifier: Modifier
) {
    Box(modifier = modifier) {
        UIKitView(
            factory = {
                val imageView = UIImageView()
                imageView.contentMode = UIViewContentMode.UIViewContentModeScaleAspectFit
                
                val data = (content as NSString).dataUsingEncoding(NSUTF8StringEncoding)
                if (data != null) {
                    val filter = CIFilter.filterWithName("CIQRCodeGenerator")
                    filter?.setValue(data, forKey = "inputMessage")
                    filter?.setValue("Q", forKey = "inputCorrectionLevel")
                    
                    val outputImage = filter?.outputImage
                    if (outputImage != null) {
                        val transform = platform.CoreGraphics.CGAffineTransformMakeScale(10.0, 10.0)
                        val scaledImage = outputImage.imageByApplyingTransform(transform)
                        val uiImage = UIImage.imageWithCIImage(scaledImage)
                        imageView.image = uiImage
                    }
                }
                imageView
            },
            modifier = modifier
        )
    }
}
