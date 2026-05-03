package com.rubensimon.ecolens.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Iconos personalizados extraídos de los SVGs proporcionados por el usuario.
 * Se definen como ImageVectors para ser usados en Compose Multiplatform.
 */
object CustomIcons {

    val Gear: ImageVector
        get() = ImageVector.Builder(
            name = "Gear",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.EvenOdd
        ) {
            moveTo(11.078f, 2.25f)
            curveToRelative(-0.917f, 0f, -1.699f, 0.663f, -1.85f, 1.567f)
            lineToRelative(-0.178f, 1.072f)
            curveToRelative(-0.02f, 0.12f, -0.115f, 0.26f, -0.297f, 0.348f)
            arcToRelative(7.493f, 7.493f, 0f, false, false, -0.986f, 0.57f)
            curveToRelative(-0.166f, 0.115f, -0.334f, 0.126f, -0.45f, 0.083f)
            lineToRelative(-1.019f, -0.382f)
            arcToRelative(1.875f, 1.875f, 0f, false, false, -2.282f, 0.819f)
            lineToRelative(-0.922f, 1.597f)
            arcToRelative(1.875f, 1.875f, 0f, false, false, 0.432f, 2.385f)
            lineToRelative(0.84f, 0.692f)
            curveToRelative(0.095f, 0.078f, 0.17f, 0.229f, 0.154f, 0.43f)
            arcToRelative(7.598f, 7.598f, 0f, false, false, 0f, 1.139f)
            curveToRelative(0.015f, 0.2f, -0.059f, 0.352f, -0.153f, 0.43f)
            lineToRelative(-0.841f, 0.692f)
            arcToRelative(1.875f, 1.875f, 0f, false, false, -0.432f, 2.385f)
            lineToRelative(0.922f, 1.597f)
            arcToRelative(1.875f, 1.875f, 0f, false, false, 2.282f, 0.818f)
            lineToRelative(1.019f, -0.382f)
            curveToRelative(0.115f, -0.043f, 0.283f, -0.031f, 0.45f, 0.082f)
            curveToRelative(0.312f, 0.214f, 0.641f, 0.405f, 0.985f, 0.57f)
            curveToRelative(0.182f, 0.088f, 0.277f, 0.228f, 0.297f, 0.35f)
            lineToRelative(0.178f, 1.071f)
            curveToRelative(0.151f, 0.904f, 0.933f, 1.567f, 1.85f, 1.567f)
            horizontalLineToRelative(1.844f)
            curveToRelative(0.916f, 0f, 1.699f, -0.663f, 1.85f, -1.567f)
            lineToRelative(0.178f, -1.072f)
            curveToRelative(0.02f, -0.12f, 0.114f, -0.26f, 0.297f, -0.349f)
            arcToRelative(7.44f, 7.44f, 0f, false, false, 0.985f, -0.57f)
            curveToRelative(0.167f, -0.114f, 0.335f, -0.125f, 0.45f, -0.082f)
            lineToRelative(1.02f, 0.382f)
            arcToRelative(1.875f, 1.875f, 0f, false, false, 2.28f, -0.819f)
            lineToRelative(0.923f, -1.597f)
            arcToRelative(1.875f, 1.875f, 0f, false, false, -0.432f, -2.385f)
            lineToRelative(-0.84f, -0.692f)
            curveToRelative(-0.095f, -0.078f, -0.17f, -0.229f, -0.154f, -0.43f)
            arcToRelative(7.614f, 7.614f, 0f, false, false, 0f, -1.139f)
            curveToRelative(-0.016f, -0.2f, 0.059f, -0.352f, 0.153f, -0.43f)
            lineToRelative(0.84f, -0.692f)
            curveToRelative(0.708f, -0.582f, 0.891f, -1.59f, 0.433f, -2.385f)
            lineToRelative(-0.922f, -1.597f)
            arcToRelative(1.875f, 1.875f, 0f, false, false, -2.282f, -0.818f)
            lineToRelative(-1.02f, 0.382f)
            curveToRelative(-0.114f, 0.043f, -0.282f, 0.031f, -0.449f, -0.083f)
            arcToRelative(7.49f, 7.49f, 0f, false, false, -0.985f, -0.57f)
            curveToRelative(-0.183f, -0.087f, -0.277f, -0.227f, -0.297f, -0.348f)
            lineToRelative(-0.179f, -1.072f)
            arcToRelative(1.875f, 1.875f, 0f, false, false, -1.85f, -1.567f)
            horizontalLineToRelative(-1.843f)
            close()
            moveTo(12f, 15.75f)
            arcToRelative(3.75f, 3.75f, 0f, true, false, 0f, -7.5f)
            arcToRelative(3.75f, 3.75f, 0f, false, false, 0f, 7.5f)
            close()
        }.build()

    val Backpack: ImageVector
        get() = ImageVector.Builder(
            name = "Backpack",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 256f,
            viewportHeight = 256f
        ).path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(168f, 40.58f)
            verticalLineTo(32f)
            arcToRelative(24f, 24f, 0f, false, false, -24f, -24f)
            horizontalLineTo(112f)
            arcToRelative(24f, 24f, 0f, false, false, -24f, 24f)
            verticalLineToRelative(8.58f)
            arcTo(56.09f, 56.09f, 0f, false, false, 40f, 96f)
            verticalLineTo(216f)
            arcToRelative(16f, 16f, 0f, false, false, 16f, 16f)
            horizontalLineTo(200f)
            arcToRelative(16f, 16f, 0f, false, false, 16f, -16f)
            verticalLineTo(96f)
            arcTo(56.09f, 56.09f, 0f, false, false, 168f, 40.58f)
            close()
            moveTo(112f, 24f)
            horizontalLineToRelative(32f)
            arcToRelative(8f, 8f, 0f, false, true, 8f, 8f)
            verticalLineToRelative(8f)
            horizontalLineTo(104f)
            verticalLineToRelative(-8f)
            arcToRelative(8f, 8f, 0f, false, true, 8f, -8f)
            close()
            moveTo(168f, 160f)
            horizontalLineTo(88f)
            verticalLineToRelative(-8f)
            arcToRelative(8f, 8f, 0f, false, true, 8f, -8f)
            horizontalLineToRelative(64f)
            arcToRelative(8f, 8f, 0f, false, true, 8f, 8f)
            close()
            moveTo(88f, 176f)
            horizontalLineToRelative(48f)
            verticalLineToRelative(8f)
            arcToRelative(8f, 8f, 0f, false, false, 16f, 0f)
            verticalLineToRelative(-8f)
            horizontalLineToRelative(16f)
            verticalLineToRelative(40f)
            horizontalLineTo(88f)
            close()
            moveTo(200f, 216f)
            horizontalLineTo(184f)
            verticalLineTo(152f)
            arcToRelative(24f, 24f, 0f, false, false, -24f, -24f)
            horizontalLineTo(96f)
            arcToRelative(24f, 24f, 0f, false, false, -24f, 24f)
            verticalLineToRelative(64f)
            horizontalLineTo(56f)
            verticalLineTo(96f)
            arcToRelative(40f, 40f, 0f, false, true, 40f, -40f)
            horizontalLineToRelative(64f)
            arcToRelative(40f, 40f, 0f, false, true, 40f, 40f)
            verticalLineTo(216f)
            close()
            moveTo(152f, 88f)
            arcToRelative(8f, 8f, 0f, false, true, -8f, 8f)
            horizontalLineTo(112f)
            arcToRelative(8f, 8f, 0f, false, true, 0f, -16f)
            horizontalLineToRelative(32f)
            arcToRelative(8f, 8f, 0f, false, true, 8f, 8f)
            close()
        }.build()

    val Trophy: ImageVector
        get() = ImageVector.Builder(
            name = "Trophy",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = SolidColor(Color.Black),
            strokeAlpha = 1f,
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(16.5f, 18.75f)
            horizontalLineToRelative(-9f)
            moveToRelative(9f, 0f)
            arcToRelative(3f, 3f, 0f, false, true, 3f, 3f)
            horizontalLineToRelative(-15f)
            arcToRelative(3f, 3f, 0f, false, true, 3f, -3f)
            moveToRelative(9f, 0f)
            verticalLineToRelative(-3.375f)
            curveToRelative(0f, -0.621f, -0.503f, -1.125f, -1.125f, -1.125f)
            horizontalLineToRelative(-0.871f)
            moveTo(7.5f, 18.75f)
            verticalLineToRelative(-3.375f)
            curveToRelative(0f, -0.621f, 0.504f, -1.125f, 1.125f, -1.125f)
            horizontalLineToRelative(0.872f)
            moveToRelative(5.007f, 0f)
            horizontalLineTo(9.497f)
            moveToRelative(5.007f, 0f)
            arcToRelative(7.454f, 7.454f, 0f, false, true, -0.982f, -3.172f)
            moveTo(9.497f, 14.25f)
            arcToRelative(7.454f, 7.454f, 0f, false, false, 0.981f, -3.172f)
            moveTo(5.25f, 4.236f)
            curveToRelative(-0.982f, 0.143f, -1.954f, 0.317f, -2.916f, 0.52f)
            arcToRelative(6.003f, 6.003f, 0f, false, false, 5.396f, 4.972f)
            moveTo(5.25f, 4.236f)
            verticalLineTo(4.5f)
            curveToRelative(0f, 2.108f, 0.966f, 3.99f, 2.48f, 5.228f)
            moveTo(5.25f, 4.236f)
            verticalLineTo(2.721f)
            curveTo(7.456f, 2.41f, 9.71f, 2.25f, 12f, 2.25f)
            curveToRelative(2.291f, 0f, 4.545f, 0.16f, 6.75f, 0.47f)
            verticalLineToRelative(1.516f)
            moveTo(7.73f, 9.728f)
            arcToRelative(6.726f, 6.726f, 0f, false, false, 2.748f, 1.35f)
            moveToRelative(8.272f, -6.842f)
            verticalLineTo(4.5f)
            curveToRelative(0f, 2.108f, -0.966f, 3.99f, -2.48f, 5.228f)
            moveToRelative(2.48f, -5.492f)
            arcToRelative(46.32f, 46.32f, 0f, false, true, 2.916f, 0.52f)
            arcToRelative(6.003f, 6.003f, 0f, false, true, -5.395f, 4.972f)
            moveToRelative(0f, 0f)
            arcToRelative(6.726f, 6.726f, 0f, false, true, -2.749f, 1.35f)
            moveToRelative(0f, 0f)
            arcToRelative(6.772f, 6.772f, 0f, false, true, -3.044f, 0f)
        }.build()

    val SunMoon: ImageVector
        get() = ImageVector.Builder(
            name = "SunMoon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            fill = null,
            fillAlpha = 1f,
            stroke = SolidColor(Color.Black),
            strokeAlpha = 1f,
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(12f, 2f)
            verticalLineToRelative(2f)
            moveTo(14.837f, 16.385f)
            arcToRelative(6f, 6f, 0f, true, true, -7.223f, -7.222f)
            curveToRelative(0.624f, -0.147f, 0.97f, 0.66f, 0.715f, 1.248f)
            arcToRelative(4f, 4f, 0f, false, false, 5.26f, 5.259f)
            curveToRelative(0.589f, -0.255f, 1.396f, 0.09f, 1.248f, 0.715f)
            moveTo(16f, 12f)
            arcToRelative(4f, 4f, 0f, false, false, -4f, -4f)
            moveToRelative(3f, -7f)
            lineToRelative(-1.256f, 1.256f)
            moveTo(20f, 12f)
            horizontalLineToRelative(2f)
        }.build()

    val Bell: ImageVector
        get() = ImageVector.Builder(
            name = "Bell",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(5.85f, 3.5f)
            arcToRelative(0.75f, 0.75f, 0f, false, false, -1.117f, -1f)
            arcToRelative(9.719f, 9.719f, 0f, false, false, -2.348f, 4.876f)
            arcToRelative(0.75f, 0.75f, 0f, false, false, 1.479f, 0.248f)
            arcToRelative(8.219f, 8.219f, 0f, false, true, 1.986f, -4.124f)
            close()
            moveTo(19.267f, 2.5f)
            arcToRelative(0.75f, 0.75f, 0f, true, false, -1.118f, 1f)
            arcToRelative(8.22f, 8.220f, 0f, false, true, 1.987f, 4.124f)
            arcToRelative(0.75f, 0.75f, 0f, false, false, 1.48f, -0.248f)
            arcToRelative(9.72f, 9.72f, 0f, false, false, -2.349f, -4.876f)
            close()
        }.path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.EvenOdd
        ) {
            moveTo(12f, 2.25f)
            arcToRelative(6.75f, 6.75f, 0f, false, false, -6.75f, 6.75f)
            verticalLineToRelative(0.75f)
            arcToRelative(8.217f, 8.217f, 0f, false, true, -2.119f, 5.52f)
            arcToRelative(0.75f, 0.75f, 0f, false, false, 0.298f, 1.206f)
            curveToRelative(1.544f, 0.57f, 3.16f, 0.99f, 4.831f, 1.243f)
            arcToRelative(3.75f, 3.75f, 0f, true, false, 7.48f, 0f)
            arcToRelative(24.583f, 24.583f, 0f, false, false, 4.83f, -1.244f)
            arcToRelative(0.75f, 0.75f, 0f, false, false, 0.298f, -1.205f)
            arcToRelative(8.217f, 8.217f, 0f, false, true, -2.118f, -5.52f)
            verticalLineTo(9f)
            arcToRelative(6.75f, 6.75f, 0f, false, false, -6.75f, -6.75f)
            close()
            moveTo(9.75f, 18f)
            curveToRelative(0f, -0.034f, 0f, -0.067f, 0.002f, -0.1f)
            arcToRelative(25.05f, 25.05f, 0f, false, false, 4.496f, 0f)
            lineToRelative(0.002f, 0.1f)
            arcToRelative(2.25f, 2.25f, 0f, true, true, -4.5f, 0f)
            close()
        }.build()

    val Volume: ImageVector
        get() = ImageVector.Builder(
            name = "Volume",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(13.5f, 4.06f)
            curveToRelative(0f, -1.336f, -1.616f, -2.005f, -2.56f, -1.06f)
            lineToRelative(-4.5f, 4.5f)
            horizontalLineTo(4.508f)
            curveToRelative(-1.141f, 0f, -2.318f, 0.664f, -2.66f, 1.905f)
            arcToRelative(9.76f, 9.76f, 0f, false, false, 0f, 5.18f)
            curveToRelative(0.341f, 1.24f, 1.518f, 1.905f, 2.659f, 1.905f)
            horizontalLineToRelative(1.93f)
            lineToRelative(4.5f, 4.5f)
            curveToRelative(0.945f, 0.945f, 2.561f, 0.276f, 2.561f, -1.06f)
            verticalLineTo(4.06f)
            close()
            moveTo(18.584f, 5.106f)
            arcToRelative(0.75f, 0.75f, 0f, false, true, 1.06f, 0f)
            curveToRelative(3.808f, 3.807f, 3.808f, 9.98f, 0f, 13.788f)
            arcToRelative(0.75f, 0.75f, 0f, false, true, -1.06f, -1.06f)
            arcToRelative(8.25f, 8.25f, 0f, false, false, 0f, -11.668f)
            arcToRelative(0.75f, 0.75f, 0f, false, true, 0f, -1.06f)
            close()
            moveTo(15.932f, 7.757f)
            arcToRelative(0.75f, 0.75f, 0f, false, true, 1.061f, 0f)
            arcToRelative(6f, 6f, 0f, false, true, 0f, 8.486f)
            arcToRelative(0.75f, 0.75f, 0f, false, true, -1.06f, -1.061f)
            arcToRelative(4.5f, 4.5f, 0f, false, false, 0f, -6.364f)
            arcToRelative(0.75f, 0.75f, 0f, false, true, 0f, -1.06f)
            close()
        }.build()

    val Trash: ImageVector
        get() = ImageVector.Builder(
            name = "Trash",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.EvenOdd
        ) {
            moveTo(16.5f, 4.478f)
            verticalLineToRelative(0.227f)
            arcToRelative(48.816f, 48.816f, 0f, false, true, 3.878f, 0.512f)
            arcToRelative(0.75f, 0.75f, 0f, true, true, -0.256f, 1.478f)
            lineToRelative(-0.209f, -0.035f)
            lineToRelative(-1.005f, 13.07f)
            arcToRelative(3f, 3f, 0f, false, true, -2.991f, 2.77f)
            horizontalLineTo(8.084f)
            arcToRelative(3f, 3f, 0f, false, true, -2.991f, -2.77f)
            lineTo(4.087f, 6.66f)
            lineToRelative(-0.209f, 0.035f)
            arcToRelative(0.75f, 0.75f, 0f, true, true, -0.256f, -1.478f)
            arcToRelative(48.567f, 48.567f, 0f, false, true, 3.878f, -0.227f)
            verticalLineToRelative(-0.227f)
            curveToRelative(0f, -1.564f, 1.213f, -2.9f, 2.816f, -2.951f)
            arcToRelative(52.662f, 52.662f, 0f, false, true, 3.369f, 0f)
            curveToRelative(1.603f, 0.051f, 2.815f, 1.387f, 2.815f, 2.951f)
            close()
            moveTo(10.364f, 3.026f)
            arcToRelative(51.196f, 51.196f, 0f, false, true, 3.273f, 0f)
            curveToRelative(0.753f, 0.024f, 1.363f, 0.658f, 1.363f, 1.452f)
            verticalLineToRelative(0.113f)
            arcToRelative(49.488f, 49.488f, 0f, false, false, -6f, 0f)
            verticalLineToRelative(-0.113f)
            curveToRelative(0f, -0.794f, 0.609f, -1.428f, 1.364f, -1.452f)
            close()
            moveTo(10.009f, 8.971f)
            arcToRelative(0.75f, 0.75f, 0f, true, false, -1.5f, 0.058f)
            lineToRelative(0.347f, 9f)
            arcToRelative(0.75f, 0.75f, 0f, true, false, 1.499f, -0.058f)
            lineToRelative(-0.346f, -9f)
            close()
            moveTo(15.489f, 9.029f)
            arcToRelative(0.75f, 0.75f, 0f, true, false, -1.498f, -0.058f)
            lineToRelative(-0.347f, 9f)
            arcToRelative(0.75f, 0.75f, 0f, false, false, 1.5f, 0.058f)
            lineToRelative(0.345f, -9f)
            close()
        }.build()

    val EyeOff: ImageVector
        get() = ImageVector.Builder(
            name = "EyeOff",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(3.53f, 2.47f)
            arcToRelative(0.75f, 0.75f, 0f, false, false, -1.06f, 1.06f)
            lineToRelative(18f, 18f)
            arcToRelative(0.75f, 0.75f, 0f, true, false, 1.06f, -1.06f)
            lineToRelative(-18f, -18f)
            close()
            moveTo(22.676f, 12.553f)
            arcToRelative(11.249f, 11.249f, 0f, false, true, -2.631f, 4.31f)
            lineToRelative(-3.099f, -3.099f)
            arcToRelative(5.25f, 5.25f, 0f, false, false, -6.71f, -6.71f)
            lineToRelative(-2.477f, -2.477f)
            arcToRelative(11.217f, 11.217f, 0f, false, true, 4.242f, -0.827f)
            curveToRelative(4.97f, 0f, 9.185f, 3.223f, 10.675f, 7.69f)
            arcToRelative(1.113f, 1.113f, 0f, false, true, 0f, 1.113f)
            close()
            moveTo(15.75f, 12f)
            curveToRelative(0f, 0.18f, -0.013f, 0.357f, -0.037f, 0.53f)
            lineToRelative(-4.244f, -4.243f)
            arcToRelative(3.75f, 3.75f, 0f, false, true, 4.281f, 3.713f)
            close()
            moveTo(12.53f, 15.713f)
            lineToRelative(-4.243f, -4.244f)
            arcToRelative(3.75f, 3.75f, 0f, false, false, 4.244f, 4.243f)
            close()
            moveTo(6.75f, 12f)
            curveToRelative(0f, -0.619f, 0.107f, -1.213f, 0.304f, -1.764f)
            lineToRelative(-3.1f, -3.1f)
            arcToRelative(11.25f, 11.25f, 0f, false, false, -2.63f, 4.31f)
            arcToRelative(1.114f, 1.114f, 0f, false, false, 0f, 1.114f)
            curveToRelative(1.489f, 4.467f, 5.704f, 7.69f, 10.675f, 7.69f)
            curveToRelative(1.5f, 0f, 2.933f, -0.294f, 4.242f, -0.827f)
            lineToRelative(-2.477f, -2.477f)
            arcToRelative(5.25f, 5.25f, 0f, false, true, -6.953f, -4.953f)
            close()
        }.build()
}
