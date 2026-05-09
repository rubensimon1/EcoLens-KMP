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
    UIKitView(
        factory = {
            UIImageView().apply {
                contentMode = UIViewContentMode.UIViewContentModeScaleAspectFit
                backgroundColor = UIColor.whiteColor
                clipsToBounds = true
            }
        },
        modifier = modifier,
        update = { imageView ->
            val data = (content as? NSString)?.dataUsingEncoding(NSUTF8StringEncoding)
            if (data != null) {
                val qrFilter = CIFilter.filterWithName("CIQRCodeGenerator")
                qrFilter?.setDefaults()
                qrFilter?.setValue(data, forKey = "inputMessage")
                qrFilter?.setValue("M", forKey = "inputCorrectionLevel")
                
                val qrImage = qrFilter?.outputImage
                if (qrImage != null) {
                    val colorFilter = CIFilter.filterWithName("CIFalseColor")
                    colorFilter?.setDefaults()
                    colorFilter?.setValue(qrImage, forKey = "inputImage")
                    colorFilter?.setValue(CIColor.blackColor(), forKey = "inputColor0")
                    colorFilter?.setValue(CIColor.whiteColor(), forKey = "inputColor1")
                    
                    val outputImage = colorFilter?.outputImage
                    if (outputImage != null) {
                        val scale = 20.0
                        val transformedImage = outputImage.imageByApplyingTransform(
                            platform.CoreGraphics.CGAffineTransformMakeScale(scale, scale)
                        )
                        
                        val context = CIContext.contextWithOptions(null)
                        val cgImage = context.createCGImage(transformedImage, transformedImage.extent)
                        if (cgImage != null) {
                            imageView.image = UIImage.imageWithCGImage(cgImage)
                        }
                    }
                }
            }
        }
    )
}
