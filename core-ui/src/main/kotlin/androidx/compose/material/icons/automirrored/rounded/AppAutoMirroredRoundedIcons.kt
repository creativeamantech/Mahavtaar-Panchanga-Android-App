package androidx.compose.material.icons.automirrored.rounded

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

private fun autoMirroredIcon(name: String, pathData: String): ImageVector =
    ImageVector.Builder(
        name = "AutoMirrored.Rounded.$name",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
        autoMirror = true
    ).apply {
        addPath(
            pathData = PathParser().parsePathString(pathData).toNodes(),
            fill = SolidColor(Color.Black)
        )
    }.build()

val Icons.AutoMirrored.Rounded.VolumeUp: ImageVector
    get() = _volumeUp ?: autoMirroredIcon(
        name = "VolumeUp",
        pathData = "M3,9v6h4l5,5V4L7,9H3zM16.5,12c0,-1.77 -1.02,-3.29 -2.5,-4.03v8.05c1.48,-0.73 2.5,-2.25 2.5,-4.02zM14,3.23v2.06c2.89,0.86 5,3.54 5,6.71s-2.11,5.85 -5,6.71v2.06c4.01,-0.91 7,-4.49 7,-8.77s-2.99,-7.86 -7,-8.77z"
    ).also { _volumeUp = it }

private var _volumeUp: ImageVector? = null

val Icons.AutoMirrored.Rounded.VolumeOff: ImageVector
    get() = _volumeOff ?: autoMirroredIcon(
        name = "VolumeOff",
        pathData = "M16.5,12c0,-1.77 -1.02,-3.29 -2.5,-4.03v2.21l2.45,2.45c0.03,-0.2 0.05,-0.41 0.05,-0.63zM19,12c0,0.94 -0.2,1.82 -0.54,2.64l1.51,1.51C20.63,14.91 21,13.5 21,12c0,-4.28 -2.99,-7.86 -7,-8.77v2.06c2.89,0.86 5,3.54 5,6.71zM4.27,3L3,4.27 7.73,9H3v6h4l5,5v-6.73l4.25,4.25c-0.67,0.52 -1.42,0.93 -2.25,1.18v2.06c1.38,-0.31 2.63,-0.95 3.69,-1.81L19.73,21 21,19.73l-9,-9L4.27,3zM12,4L9.91,6.09 12,8.18V4z"
    ).also { _volumeOff = it }

private var _volumeOff: ImageVector? = null

val Icons.AutoMirrored.Rounded.Logout: ImageVector
    get() = _logout ?: autoMirroredIcon(
        name = "Logout",
        pathData = "M17,7l-1.41,1.41L18.17,11H8v2h10.17l-2.58,2.58L17,17l5,-5L17,7zM4,5h8V3H4C2.9,3 2,3.9 2,5v14c0,1.1 0.9,2 2,2h8v-2H4V5z"
    ).also { _logout = it }

private var _logout: ImageVector? = null

