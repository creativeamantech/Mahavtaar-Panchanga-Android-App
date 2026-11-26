package androidx.compose.material.icons.rounded

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

private fun roundedIcon(name: String, vararg paths: String): ImageVector =
    ImageVector.Builder(
        name = "Rounded.$name",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        paths.forEach { d ->
            addPath(
                pathData = PathParser().parsePathString(d).toNodes(),
                fill = SolidColor(Color.Black)
            )
        }
    }.build()

val Icons.Rounded.AccessAlarm: ImageVector
    get() = _accessAlarm ?: roundedIcon(
        "AccessAlarm",
        "M22,5.72l-4.6,-3.86l-1.29,1.53l4.6,3.86L22,5.72zM7.88,3.39L6.6,1.86L2,5.71l1.29,1.53L7.88,3.39zM12.5,8H11v6l4.75,2.85l0.75,-1.23l-4,-2.37V8zM12,4c-4.97,0 -9,4.03 -9,9s4.02,9 9,9c4.97,0 9,-4.03 9,-9S16.97,4 12,4zM12,20c-3.87,0 -7,-3.13 -7,-7s3.13,-7 7,-7s7,3.13 7,7S15.87,20 12,20z"
    ).also { _accessAlarm = it }
private var _accessAlarm: ImageVector? = null

val Icons.Rounded.Air: ImageVector
    get() = _air ?: roundedIcon(
        "Air",
        "M14.5,17c0,1.65 -1.35,3 -3,3s-3,-1.35 -3,-3h2c0,0.55 0.45,1 1,1s1,-0.45 1,-1s-0.45,-1 -1,-1H2v-2h9.5C13.15,14 14.5,15.35 14.5,17zM19,6.5C19,4.57 17.43,3 15.5,3S12,4.57 12,6.5h2C14,5.67 14.67,5 15.5,5S17,5.67 17,6.5S16.33,8 15.5,8H2v2h13.5C17.43,10 19,8.43 19,6.5zM18.5,11H2v2h16.5c0.83,0 1.5,0.67 1.5,1.5S19.33,16 18.5,16v2c1.93,0 3.5,-1.57 3.5,-3.5S20.43,11 18.5,11z"
    ).also { _air = it }
private var _air: ImageVector? = null

val Icons.Rounded.AudioFile: ImageVector
    get() = _audioFile ?: roundedIcon(
        "AudioFile",
        "M14,2H6C4.9,2 4,2.9 4,4v16c0,1.1 0.9,2 2,2h12c1.1,0 2,-0.9 2,-2V8L14,2zM16,13h-3v3.75c0,1.24 -1.01,2.25 -2.25,2.25S8.5,17.99 8.5,16.75s1.01,-2.25 2.25,-2.25c0.46,0 0.89,0.14 1.25,0.38V11h4V13zM13,9V3.5L18.5,9H13z"
    ).also { _audioFile = it }
private var _audioFile: ImageVector? = null

val Icons.Rounded.AutoAwesome: ImageVector
    get() = _autoAwesome ?: roundedIcon(
        "AutoAwesome",
        "M19,9l1.25,-2.75L23,5l-2.75,-1.25L19,1l-1.25,2.75L15,5l2.75,1.25L19,9zM11.5,9.5L9,4L6.5,9.5L1,12l5.5,2.5L9,20l2.5,-5.5L17,12L11.5,9.5zM19,15l-1.25,2.75L15,19l2.75,1.25L19,23l1.25,-2.75L23,19l-2.75,-1.25L19,15z"
    ).also { _autoAwesome = it }
private var _autoAwesome: ImageVector? = null

val Icons.Rounded.Bedtime: ImageVector
    get() = _bedtime ?: roundedIcon(
        "Bedtime",
        "M12.3,4.9c0.4,-0.2 0.6,-0.7 0.5,-1.1s-0.6,-0.8 -1.1,-0.8c-4.9,0.1 -8.7,4.1 -8.7,9c0,5 4,9 9,9c3.8,0 7.1,-2.4 8.4,-5.9c0.2,-0.4 0,-0.9 -0.4,-1.2c-0.4,-0.3 -0.9,-0.2 -1.2,0.1c-1,0.9 -2.3,1.4 -3.7,1.4c-3.1,0 -5.7,-2.5 -5.7,-5.7C9.4,7.8 10.5,5.9 12.3,4.9z"
    ).also { _bedtime = it }
private var _bedtime: ImageVector? = null

val Icons.Rounded.BookmarkAdd: ImageVector
    get() = _bookmarkAdd ?: roundedIcon(
        "BookmarkAdd",
        "M21,7h-2v2h-2V7h-2V5h2V3h2v2h2V7zM19,21l-7,-3l-7,3V5c0,-1.1 0.9,-2 2,-2h7c-0.63,0.84 -1,1.87 -1,3c0,2.76 2.24,5 5,5c0.34,0 0.68,-0.03 1,-0.1V21z"
    ).also { _bookmarkAdd = it }
private var _bookmarkAdd: ImageVector? = null

val Icons.Rounded.Brightness3: ImageVector
    get() = _brightness3 ?: roundedIcon(
        "Brightness3",
        "M9,2c-1.05,0 -2.05,0.16 -3,0.46c4.06,1.27 7,5.06 7,9.54s-2.94,8.27 -7,9.54c0.95,0.3 1.95,0.46 3,0.46c5.52,0 10,-4.48 10,-10S14.52,2 9,2z"
    ).also { _brightness3 = it }
private var _brightness3: ImageVector? = null

val Icons.Rounded.CalendarMonth: ImageVector
    get() = _calendarMonth ?: roundedIcon(
        "CalendarMonth",
        "M19,4h-1V2h-2v2H8V2H6v2H5C3.89,4 3.01,4.9 3.01,6L3,20c0,1.1 0.89,2 2,2h14c1.1,0 2,-0.9 2,-2V6C21,4.9 20.1,4 19,4zM19,20H5V10h14V20zM9,14H7v-2h2V14zM13,14h-2v-2h2V14zM17,14h-2v-2h2V14zM9,18H7v-2h2V18zM13,18h-2v-2h2V18zM17,18h-2v-2h2V18z"
    ).also { _calendarMonth = it }
private var _calendarMonth: ImageVector? = null

val Icons.Rounded.Celebration: ImageVector
    get() = _celebration ?: roundedIcon(
        "Celebration",
        "M2,22l14,-5L7,8L2,22zM12.35,16.18L5.3,18.7l2.52,-7.05L12.35,16.18z",
        "M14.53,12.53l5.59,-5.59c0.49,-0.49 1.28,-0.49 1.77,0l0.59,0.59l1.06,-1.06l-0.59,-0.59c-1.07,-1.07 -2.82,-1.07 -3.89,0l-5.59,5.59L14.53,12.53z",
        "M10.06,6.88L9.47,7.47l1.06,1.06l0.59,-0.59c1.07,-1.07 1.07,-2.82 0,-3.89l-0.59,-0.59L9.47,4.53l0.59,0.59C10.54,5.6 10.54,6.4 10.06,6.88z",
        "M17.06,11.88l-1.59,1.59l1.06,1.06l1.59,-1.59c0.49,-0.49 1.28,-0.49 1.77,0l1.61,1.61l1.06,-1.06l-1.61,-1.61C19.87,10.81 18.13,10.81 17.06,11.88z",
        "M15.06,5.88l-3.59,3.59l1.06,1.06l3.59,-3.59c1.07,-1.07 1.07,-2.82 0,-3.89l-1.59,-1.59l-1.06,1.06l1.59,1.59C15.54,4.6 15.54,5.4 15.06,5.88z"
    ).also { _celebration = it }
private var _celebration: ImageVector? = null

val Icons.Rounded.ChevronLeft: ImageVector
    get() = _chevronLeft ?: roundedIcon(
        "ChevronLeft",
        "M14.71,6.71c-0.39,-0.39 -1.02,-0.39 -1.41,0L8.71,11.3c-0.39,0.39 -0.39,1.02 0,1.41l4.59,4.59c0.39,0.39 1.02,0.39 1.41,0c0.39,-0.39 0.39,-1.02 0,-1.41L10.83,12l3.88,-3.88C15.1,7.73 15.09,7.09 14.71,6.71z"
    ).also { _chevronLeft = it }
private var _chevronLeft: ImageVector? = null

val Icons.Rounded.ChevronRight: ImageVector
    get() = _chevronRight ?: roundedIcon(
        "ChevronRight",
        "M9.29,6.71c-0.39,0.39 -0.39,1.02 0,1.41L13.17,12l-3.88,3.88c-0.39,0.39 -0.39,1.02 0,1.41c0.39,0.39 1.02,0.39 1.41,0l4.59,-4.59c0.39,-0.39 0.39,-1.02 0,-1.41L10.7,6.7C10.32,6.32 9.68,6.32 9.29,6.71z"
    ).also { _chevronRight = it }
private var _chevronRight: ImageVector? = null

val Icons.Rounded.Code: ImageVector
    get() = _code ?: roundedIcon(
        "Code",
        "M8.7,15.9L4.8,12l3.9,-3.9c0.39,-0.39 0.39,-1.01 0,-1.4c-0.39,-0.39 -1.01,-0.39 -1.4,0L2.71,11.29c-0.39,0.39 -0.39,1.02 0,1.41l4.59,4.6c0.39,0.39 1.01,0.39 1.4,0C9.09,16.91 9.09,16.29 8.7,15.9zM15.3,15.9l3.9,-3.9l-3.9,-3.9c-0.39,-0.39 -0.39,-1.01 0,-1.4c0.39,-0.39 1.01,-0.39 1.4,0l4.59,4.59c0.39,0.39 0.39,1.02 0,1.41l-4.59,4.6c-0.39,0.39 -1.01,0.39 -1.4,0C14.91,16.91 14.91,16.29 15.3,15.9z"
    ).also { _code = it }
private var _code: ImageVector? = null

val Icons.Rounded.DeleteOutline: ImageVector
    get() = _deleteOutline ?: roundedIcon(
        "DeleteOutline",
        "M6,19c0,1.1 0.9,2 2,2h8c1.1,0 2,-0.9 2,-2V7H6V19zM8,9h8v10H8V9zM15.5,4l-1,-1h-5l-1,1H5v2h14V4H15.5z"
    ).also { _deleteOutline = it }
private var _deleteOutline: ImageVector? = null

val Icons.Rounded.ExpandLess: ImageVector
    get() = _expandLess ?: roundedIcon(
        "ExpandLess",
        "M11.29,8.71L6.7,13.3c-0.39,0.39 -0.39,1.02 0,1.41c0.39,0.39 1.02,0.39 1.41,0L12,10.83l3.88,3.88c0.39,0.39 1.02,0.39 1.41,0c0.39,-0.39 0.39,-1.02 0,-1.41L12.7,8.71C12.32,8.32 11.68,8.32 11.29,8.71z"
    ).also { _expandLess = it }
private var _expandLess: ImageVector? = null

val Icons.Rounded.ExpandMore: ImageVector
    get() = _expandMore ?: roundedIcon(
        "ExpandMore",
        "M15.88,9.29L12,13.17L8.12,9.29c-0.39,-0.39 -1.02,-0.39 -1.41,0c-0.39,0.39 -0.39,1.02 0,1.41l4.59,4.59c0.39,0.39 1.02,0.39 1.41,0l4.59,-4.59c0.39,-0.39 0.39,-1.02 0,-1.41C16.91,8.91 16.27,8.9 15.88,9.29z"
    ).also { _expandMore = it }
private var _expandMore: ImageVector? = null

val Icons.Rounded.Explore: ImageVector
    get() = _explore ?: roundedIcon(
        "Explore",
        "M12,10.9c-0.61,0 -1.1,0.49 -1.1,1.1s0.49,1.1 1.1,1.1c0.61,0 1.1,-0.49 1.1,-1.1S12.61,10.9 12,10.9zM12,2C6.48,2 2,6.48 2,12s4.48,10 10,10s10,-4.48 10,-10S17.52,2 12,2zM14.19,14.19L6,18l3.81,-8.19L18,6L14.19,14.19z"
    ).also { _explore = it }
private var _explore: ImageVector? = null

val Icons.Rounded.GraphicEq: ImageVector
    get() = _graphicEq ?: roundedIcon(
        "GraphicEq",
        "M8,18c0.55,0 1,-0.45 1,-1V7c0,-0.55 -0.45,-1 -1,-1S7,6.45 7,7v10C7,17.55 7.45,18 8,18zM12,22c0.55,0 1,-0.45 1,-1V3c0,-0.55 -0.45,-1 -1,-1s-1,0.45 -1,1v18C11,21.55 11.45,22 12,22zM4,14c0.55,0 1,-0.45 1,-1v-2c0,-0.55 -0.45,-1 -1,-1s-1,0.45 -1,1v2C3,13.55 3.45,14 4,14zM16,18c0.55,0 1,-0.45 1,-1V7c0,-0.55 -0.45,-1 -1,-1s-1,0.45 -1,1v10C15,17.55 15.45,18 16,18zM19,11v2c0,0.55 0.45,1 1,1s1,-0.45 1,-1v-2c0,-0.55 -0.45,-1 -1,-1S19,10.45 19,11z"
    ).also { _graphicEq = it }
private var _graphicEq: ImageVector? = null

val Icons.Rounded.GridView: ImageVector
    get() = _gridView ?: roundedIcon(
        "GridView",
        "M5,11h4c1.1,0 2,-0.9 2,-2V5c0,-1.1 -0.9,-2 -2,-2H5C3.9,3 3,3.9 3,5v4C3,10.1 3.9,11 5,11zM5,21h4c1.1,0 2,-0.9 2,-2v-4c0,-1.1 -0.9,-2 -2,-2H5c-1.1,0 -2,0.9 -2,2v4C3,20.1 3.9,21 5,21zM13,5v4c0,1.1 0.9,2 2,2h4c1.1,0 2,-0.9 2,-2V5c0,-1.1 -0.9,-2 -2,-2h-4C13.9,3 13,3.9 13,5zM15,21h4c1.1,0 2,-0.9 2,-2v-4c0,-1.1 -0.9,-2 -2,-2h-4c-1.1,0 -2,0.9 -2,2v4C13,20.1 13.9,21 15,21z"
    ).also { _gridView = it }
private var _gridView: ImageVector? = null

val Icons.Rounded.LocalFireDepartment: ImageVector
    get() = _localFireDepartment ?: roundedIcon(
        "LocalFireDepartment",
        "M12,12.9l-2.13,2.09C9.31,15.55 9,16.28 9,17.06C9,18.68 10.35,20 12,20s3,-1.32 3,-2.94c0,-0.78 -0.31,-1.52 -0.87,-2.07L12,12.9z",
        "M16,6l-0.44,0.55C14.38,8.02 12,7.19 12,5.3V2c0,0 -8,4 -8,11c0,2.92 1.56,5.47 3.89,6.86C7.33,19.07 7,18.1 7,17.06c0,-1.32 0.52,-2.56 1.47,-3.5L12,10.1l3.53,3.47c0.95,0.93 1.47,2.17 1.47,3.5c0,1.02 -0.31,1.96 -0.85,2.75c1.89,-1.15 3.29,-3.06 3.71,-5.3C20.52,10.97 18.79,7.62 16,6z"
    ).also { _localFireDepartment = it }
private var _localFireDepartment: ImageVector? = null

val Icons.Rounded.MenuBook: ImageVector
    get() = _menuBook ?: roundedIcon(
        "MenuBook",
        "M21,5c-1.11,-0.35 -2.33,-0.5 -3.5,-0.5c-1.95,0 -4.05,0.4 -5.5,1.5c-1.45,-1.1 -3.55,-1.5 -5.5,-1.5S2.45,4.9 1,6v14.65c0,0.25 0.25,0.5 0.5,0.5c0.1,0 0.15,-0.05 0.25,-0.05C3.1,20.45 5.05,20 6.5,20c1.95,0 4.05,0.4 5.5,1.5c1.35,-0.85 3.8,-1.5 5.5,-1.5c1.65,0 3.35,0.3 4.75,1.05c0.1,0.05 0.15,0.05 0.25,0.05c0.25,0 0.5,-0.25 0.5,-0.5V6C22.4,5.55 21.75,5.25 21,5zM21,18.5c-1.1,-0.35 -2.3,-0.5 -3.5,-0.5c-1.7,0 -4.15,0.65 -5.5,1.5V8c1.35,-0.85 3.8,-1.5 5.5,-1.5c1.2,0 2.4,0.15 3.5,0.5V18.5z"
    ).also { _menuBook = it }
private var _menuBook: ImageVector? = null

val Icons.Rounded.Mic: ImageVector
    get() = _mic ?: roundedIcon(
        "Mic",
        "M12,14c1.66,0 3,-1.34 3,-3V5c0,-1.66 -1.34,-3 -3,-3S9,3.34 9,5v6C9,12.66 10.34,14 12,14zM17.3,11c0,3 -2.54,5.1 -5.3,5.1S6.7,14 6.7,11H5c0,3.41 2.72,6.23 6,6.72V21h2v-3.28c3.28,-0.49 6,-3.31 6,-6.72H17.3z"
    ).also { _mic = it }
private var _mic: ImageVector? = null

val Icons.Rounded.MusicNote: ImageVector
    get() = _musicNote ?: roundedIcon(
        "MusicNote",
        "M12,5v8.55c-0.94,-0.54 -2.1,-0.75 -3.33,-0.32c-1.34,0.48 -2.37,1.67 -2.61,3.07c-0.46,2.74 1.86,5.08 4.59,4.65c1.96,-0.31 3.35,-2.11 3.35,-4.1V7h2c1.1,0 2,-0.9 2,-2s-0.9,-2 -2,-2h-2C12.9,3 12,3.9 12,5z"
    ).also { _musicNote = it }
private var _musicNote: ImageVector? = null

val Icons.Rounded.MyLocation: ImageVector
    get() = _myLocation ?: roundedIcon(
        "MyLocation",
        "M12,8c-2.21,0 -4,1.79 -4,4s1.79,4 4,4s4,-1.79 4,-4S14.21,8 12,8zM20.94,11c-0.46,-4.17 -3.77,-7.48 -7.94,-7.94V2c0,-0.55 -0.45,-1 -1,-1s-1,0.45 -1,1v1.06C6.83,3.52 3.52,6.83 3.06,11H2c-0.55,0 -1,0.45 -1,1s0.45,1 1,1h1.06c0.46,4.17 3.77,7.48 7.94,7.94V22c0,0.55 0.45,1 1,1s1,-0.45 1,-1v-1.06c4.17,-0.46 7.48,-3.77 7.94,-7.94H22c0.55,0 1,-0.45 1,-1s-0.45,-1 -1,-1H20.94zM12,19c-3.87,0 -7,-3.13 -7,-7s3.13,-7 7,-7s7,3.13 7,7S15.87,19 12,19z"
    ).also { _myLocation = it }
private var _myLocation: ImageVector? = null

val Icons.Rounded.NotificationsActive: ImageVector
    get() = _notificationsActive ?: roundedIcon(
        "NotificationsActive",
        "M7.58,4.08L6.15,2.65C3.75,4.48 2.17,7.3 2.03,10.5h2C4.18,7.85 5.54,5.53 7.58,4.08zM19.97,10.5h2c-0.15,-3.2 -1.73,-6.02 -4.12,-7.85l-1.42,1.43C18.45,5.53 19.82,7.85 19.97,10.5zM18,11c0,-3.07 -1.64,-5.64 -4.5,-6.32V4c0,-0.83 -0.67,-1.5 -1.5,-1.5s-1.5,0.67 -1.5,1.5v0.68C7.63,5.36 6,7.92 6,11v5l-2,2v1h16v-1l-2,-2V11zM12,22c1.1,0 2,-0.9 2,-2h-4C10,21.1 10.89,22 12,22z"
    ).also { _notificationsActive = it }
private var _notificationsActive: ImageVector? = null

val Icons.Rounded.Public: ImageVector
    get() = _public ?: roundedIcon(
        "Public",
        "M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10s10,-4.48 10,-10S17.52,2 12,2zM11,19.93c-3.95,-0.49 -7,-3.85 -7,-7.93c0,-0.62 0.08,-1.21 0.21,-1.79L9,15v1c0,1.1 0.9,2 2,2V19.93zM17.9,17.39c-0.26,-0.81 -1,-1.39 -1.9,-1.39h-1v-3c0,-0.55 -0.45,-1 -1,-1H8v-2h2c0.55,0 1,-0.45 1,-1V7h2c1.1,0 2,-0.9 2,-2v-0.41c2.93,1.19 5,4.06 5,7.41C20,14.08 19.2,15.97 17.9,17.39z"
    ).also { _public = it }
private var _public: ImageVector? = null

val Icons.Rounded.RecordVoiceOver: ImageVector
    get() = _recordVoiceOver ?: roundedIcon(
        "RecordVoiceOver",
        "M9,13c2.21,0 4,-1.79 4,-4s-1.79,-4 -4,-4S5,6.79 5,9S6.79,13 9,13zM9,15c-2.67,0 -8,1.34 -8,4v2h16v-2C17,16.34 11.67,15 9,15zM16.76,5.36l-1.68,1.69c0.84,1.18 0.84,2.71 0,3.89l1.68,1.69C18.78,10.63 18.78,7.38 16.76,5.36zM20.07,2l-1.63,1.63c2.77,3.02 2.77,7.56 0,10.74L20.07,16C23.97,12.11 23.98,6.05 20.07,2z"
    ).also { _recordVoiceOver = it }
private var _recordVoiceOver: ImageVector? = null

val Icons.Rounded.Remove: ImageVector
    get() = _remove ?: roundedIcon(
        "Remove",
        "M18,13H6c-0.55,0 -1,-0.45 -1,-1s0.45,-1 1,-1h12c0.55,0 1,0.45 1,1S18.55,13 18,13z"
    ).also { _remove = it }
private var _remove: ImageVector? = null

val Icons.Rounded.Schedule: ImageVector
    get() = _schedule ?: roundedIcon(
        "Schedule",
        "M11.99,2C6.47,2 2,6.48 2,12s4.47,10 9.99,10C17.52,22 22,17.52 22,12S17.52,2 11.99,2zM12,20c-4.42,0 -8,-3.58 -8,-8s3.58,-8 8,-8s8,3.58 8,8S16.42,20 12,20zM12.5,7H11v6l5.25,3.15l0.75,-1.23l-4.5,-2.67V7z"
    ).also { _schedule = it }
private var _schedule: ImageVector? = null

val Icons.Rounded.SelfImprovement: ImageVector
    get() = _selfImprovement ?: roundedIcon(
        "SelfImprovement",
        "M12,6m-2,0a2,2 0,1 1,4 0a2,2 0,1 1,-4 0",
        "M21,16v-2c-2.24,0 -4.16,-0.96 -5.6,-2.68l-1.34,-1.6C13.68,9.26 13.12,9 12.53,9h-1.05c-0.59,0 -1.15,0.26 -1.53,0.72l-1.34,1.6C7.16,13.04 5.24,14 3,14v2c2.77,0 5.19,-1.17 7,-3.25V15l-3.88,1.55C5.45,16.82 5,17.48 5,18.21C5,19.2 5.8,20 6.79,20H9v-0.5c0,-1.38 1.12,-2.5 2.5,-2.5h3c0.28,0 0.5,0.22 0.5,0.5S14.78,18 14.5,18h-3c-0.83,0 -1.5,0.67 -1.5,1.5V20h7.21C18.2,20 19,19.2 19,18.21c0,-0.73 -0.45,-1.39 -1.12,-1.66L14,15v-2.25C15.81,14.83 18.23,16 21,16z"
    ).also { _selfImprovement = it }
private var _selfImprovement: ImageVector? = null

val Icons.Rounded.Spa: ImageVector
    get() = _spa ?: roundedIcon(
        "Spa",
        "M15.49,9.63c-0.18,-2.79 -1.31,-5.51 -3.43,-7.63c-2.14,2.14 -3.32,4.86 -3.55,7.63c1.28,0.68 2.46,1.56 3.49,2.63C13.03,11.19 14.21,10.31 15.49,9.63zM22,12c-5.52,0 -10,4.48 -10,10C17.52,22 22,17.52 22,12zM12,22c0,-5.52 -4.48,-10 -10,-10C2,17.52 6.48,22 12,22z"
    ).also { _spa = it }
private var _spa: ImageVector? = null

val Icons.Rounded.Stars: ImageVector
    get() = _stars ?: roundedIcon(
        "Stars",
        "M11.99,2C6.47,2 2,6.48 2,12s4.47,10 9.99,10C17.52,22 22,17.52 22,12S17.52,2 11.99,2zM16.23,18L12,15.45L7.77,18l1.12,-4.81l-3.73,-3.23l4.92,-0.42L12,5l1.92,4.53l4.92,0.42l-3.73,3.23L16.23,18z"
    ).also { _stars = it }
private var _stars: ImageVector? = null

val Icons.Rounded.Stop: ImageVector
    get() = _stop ?: roundedIcon(
        "Stop",
        "M8,6h8c1.1,0 2,0.9 2,2v8c0,1.1 -0.9,2 -2,2H8c-1.1,0 -2,-0.9 -2,-2V8C6,6.9 6.9,6 8,6z"
    ).also { _stop = it }
private var _stop: ImageVector? = null

val Icons.Rounded.Timeline: ImageVector
    get() = _timeline ?: roundedIcon(
        "Timeline",
        "M23,8c0,1.1 -0.9,2 -2,2c-0.18,0 -0.35,-0.02 -0.51,-0.07l-3.56,3.55C16.98,13.64 17,13.82 17,14c0,1.1 -0.9,2 -2,2s-2,-0.9 -2,-2c0,-0.18 0.02,-0.36 0.07,-0.52l-2.55,-2.55C10.36,10.98 10.18,11 10,11s-0.36,-0.02 -0.52,-0.07l-4.55,4.56C4.98,15.65 5,15.82 5,16c0,1.1 -0.9,2 -2,2s-2,-0.9 -2,-2s0.9,-2 2,-2c0.18,0 0.35,0.02 0.51,0.07l4.56,-4.55C8.02,9.36 8,9.18 8,9c0,-1.1 0.9,-2 2,-2s2,0.9 2,2c0,0.18 -0.02,0.36 -0.07,0.52l2.55,2.55C14.64,12.02 14.82,12 15,12s0.36,0.02 0.52,0.07l3.55,-3.56C19.02,8.35 19,8.18 19,8c0,-1.1 0.9,-2 2,-2S23,6.9 23,8z"
    ).also { _timeline = it }
private var _timeline: ImageVector? = null

val Icons.Rounded.Translate: ImageVector
    get() = _translate ?: roundedIcon(
        "Translate",
        "M12.87,15.07l-2.54,-2.51l0.03,-0.03c1.74,-1.94 2.98,-4.17 3.71,-6.53H17V4h-7V2H8v2H1v1.99h11.17C11.5,7.92 10.44,9.75 9,11.35C8.07,10.32 7.3,9.19 6.69,8h-2c0.73,1.63 1.73,3.17 2.98,4.56l-5.09,5.02L4,19l5,-5l3.11,3.11L12.87,15.07zM18.5,10h-2L12,22h2l1.12,-3h4.75L21,22h2L18.5,10zM15.88,17l1.62,-4.33L19.12,17H15.88z"
    ).also { _translate = it }
private var _translate: ImageVector? = null

val Icons.Rounded.Tune: ImageVector
    get() = _tune ?: roundedIcon(
        "Tune",
        "M3,17v2h6v-2H3zM3,5v2h10V5H3zM13,21v-2h8v-2h-8v-2h-2v6H13zM7,9v2H3v2h4v2h2V9H7zM21,13v-2H11v2H21zM15,9h2V7h4V5h-4V3h-2V9z"
    ).also { _tune = it }
private var _tune: ImageVector? = null

val Icons.Rounded.Verified: ImageVector
    get() = _verified ?: roundedIcon(
        "Verified",
        "M23,12l-2.44,-2.79l0.34,-3.69l-3.61,-0.82L15.4,1.5L12,2.96L8.6,1.5L6.71,4.69L3.1,5.5L3.44,9.2L1,12l2.44,2.79l-0.34,3.7l3.61,0.82L8.6,22.5l3.4,-1.47l3.4,1.46l1.89,-3.19l3.61,-0.82l-0.34,-3.69L23,12zM10.09,16.72l-3.8,-3.81l1.48,-1.48l2.32,2.33l5.85,-5.87l1.48,1.48L10.09,16.72z"
    ).also { _verified = it }
private var _verified: ImageVector? = null

val Icons.Rounded.ViewAgenda: ImageVector
    get() = _viewAgenda ?: roundedIcon(
        "ViewAgenda",
        "M19,13H5c-1.1,0 -2,0.9 -2,2v4c0,1.1 0.9,2 2,2h14c1.1,0 2,-0.9 2,-2v-4C21,13.9 20.1,13 19,13zM19,3H5C3.9,3 3,3.9 3,5v4c0,1.1 0.9,2 2,2h14c1.1,0 2,-0.9 2,-2V5C21,3.9 20.1,3 19,3z"
    ).also { _viewAgenda = it }
private var _viewAgenda: ImageVector? = null

val Icons.Rounded.WarningAmber: ImageVector
    get() = _warningAmber ?: roundedIcon(
        "WarningAmber",
        "M12,5.99L19.53,19H4.47L12,5.99M12,2L1,21h22L12,2L12,2zM13,16h-2v2h2V16zM13,10h-2v4h2V10z"
    ).also { _warningAmber = it }
private var _warningAmber: ImageVector? = null

val Icons.Rounded.WbSunny: ImageVector
    get() = _wbSunny ?: roundedIcon(
        "WbSunny",
        "M6.76,4.84l-1.8,-1.79l-1.41,1.41l1.79,1.79L6.76,4.84zM4,10.5H1v2h3V10.5zM13,0.55h-2V3.5h2V0.55zM20.45,4.46l-1.41,-1.41l-1.79,1.79l1.41,1.41L20.45,4.46zM17.24,18.16l1.79,1.8l1.41,-1.41l-1.8,-1.79L17.24,18.16zM20,10.5v2h3v-2H20zM12,5.5c-3.31,0 -6,2.69 -6,6s2.69,6 6,6s6,-2.69 6,-6S15.31,5.5 12,5.5zM11,22.45h2V19.5h-2V22.45zM3.55,18.54l1.41,1.41l1.79,-1.8l-1.41,-1.41L3.55,18.54z"
    ).also { _wbSunny = it }
private var _wbSunny: ImageVector? = null

val Icons.Rounded.WbTwilight: ImageVector
    get() = _wbTwilight ?: roundedIcon(
        "WbTwilight",
        "M16.95,8.64l1.41,-1.41l1.41,1.41l-1.41,1.41L16.95,8.64zM2,18h20v2H2V18zM11,4h2v3h-2V4zM4.22,8.64l1.41,-1.41l1.41,1.41l-1.41,1.41L4.22,8.64zM5,16h14c0,-3.87 -3.13,-7 -7,-7S5,12.13 5,16z"
    ).also { _wbTwilight = it }
private var _wbTwilight: ImageVector? = null

val Icons.Rounded.Download: ImageVector
    get() = _download ?: roundedIcon(
        "Download",
        "M16.59,9H15V4c0,-0.55 -0.45,-1 -1,-1h-4C9.45,3 9,3.45 9,4v5H7.41c-0.89,0 -1.34,1.08 -0.71,1.71l4.59,4.59c0.39,0.39 1.02,0.39 1.41,0l4.59,-4.59C17.92,10.08 17.48,9 16.59,9zM5,19c0,0.55 0.45,1 1,1h12c0.55,0 1,-0.45 1,-1s-0.45,-1 -1,-1H6C5.45,18 5,18.45 5,19z"
    ).also { _download = it }
private var _download: ImageVector? = null

val Icons.Rounded.Print: ImageVector
    get() = _print ?: roundedIcon(
        "Print",
        "M19,8H5c-1.66,0 -3,1.34 -3,3v4c0,1.1 0.9,2 2,2h2v2c0,1.1 0.9,2 2,2h8c1.1,0 2,-0.9 2,-2v-2h2c1.1,0 2,-0.9 2,-2v-4C22,9.34 20.66,8 19,8zM16,19H8v-5h8V19zM19,12c-0.55,0 -1,-0.45 -1,-1s0.45,-1 1,-1s1,0.45 1,1S19.55,12 19,12zM18,4c0,-0.55 -0.45,-1 -1,-1H7C6.45,3 6,3.45 6,4v3h12V4z"
    ).also { _print = it }
private var _print: ImageVector? = null

val Icons.Rounded.PersonAdd: ImageVector
    get() = _personAdd ?: roundedIcon(
        "PersonAdd",
        "M15,12c2.21,0 4,-1.79 4,-4s-1.79,-4 -4,-4s-4,1.79 -4,4S12.79,12 15,12zM6,10V8c0,-0.55 -0.45,-1 -1,-1S4,7.45 4,8v2H2c-0.55,0 -1,0.45 -1,1s0.45,1 1,1h2v2c0,0.55 0.45,1 1,1s1,-0.45 1,-1v-2h2c0.55,0 1,-0.45 1,-1s-0.45,-1 -1,-1H6zM15,14c-2.67,0 -8,1.34 -8,4v1c0,0.55 0.45,1 1,1h14c0.55,0 1,-0.45 1,-1v-1C23,15.34 17.67,14 15,14z"
    ).also { _personAdd = it }
private var _personAdd: ImageVector? = null

val Icons.Rounded.FolderOpen: ImageVector
    get() = _folderOpen ?: roundedIcon(
        "FolderOpen",
        "M20,6h-8l-1.41,-1.41C10.21,4.21 9.7,4 9.17,4H4C2.9,4 2.01,4.9 2.01,6L2,18c0,1.1 0.9,2 2,2h16.77c0.68,0 1.27,-0.43 1.51,-1.06l1.7,-5.09C24.21,13.19 23.72,12.5 23,12.5H6.5L4.5,18V6h5.17l1.41,1.41c0.38,0.38 0.89,0.59 1.41,0.59H20V6z"
    ).also { _folderOpen = it }
private var _folderOpen: ImageVector? = null

val Icons.Rounded.Description: ImageVector
    get() = _description ?: roundedIcon(
        "Description",
        "M14.59,2.59C14.21,2.21 13.7,2 13.17,2H6C4.9,2 4,2.9 4,4v16c0,1.1 0.89,2 1.99,2H18c1.1,0 2,-0.9 2,-2V8.83c0,-0.53 -0.21,-1.04 -0.59,-1.41L14.59,2.59zM15,18H9c-0.55,0 -1,-0.45 -1,-1s0.45,-1 1,-1h6c0.55,0 1,0.45 1,1S15.55,18 15,18zM15,14H9c-0.55,0 -1,-0.45 -1,-1s0.45,-1 1,-1h6c0.55,0 1,0.45 1,1S15.55,14 15,14zM13,9V3.5L18.5,9H13z"
    ).also { _description = it }
private var _description: ImageVector? = null

val Icons.Rounded.RateReview: ImageVector
    get() = _rateReview ?: roundedIcon(
        "RateReview",
        "M20,2H4C2.9,2 2.01,2.9 2.01,4L2,22l4,-4h14c1.1,0 2,-0.9 2,-2V4C22,2.9 21.1,2 20,2zM6,14v-2.47l6.88,-6.88c0.2,-0.2 0.51,-0.2 0.71,0l1.77,1.77c0.2,0.2 0.2,0.51 0,0.71L8.47,14H6zM17,14h-6.5l2,-2H17c0.55,0 1,0.45 1,1S17.55,14 17,14z"
    ).also { _rateReview = it }
private var _rateReview: ImageVector? = null

val Icons.Rounded.PushPin: ImageVector
    get() = _pushPin ?: roundedIcon(
        "PushPin",
        "M16,9V4h1c0.55,0 1,-0.45 1,-1s-0.45,-1 -1,-1H7C6.45,2 6,2.45 6,3s0.45,1 1,1h1v5c0,1.66 -1.34,3 -3,3v2h5.97v7l1,1l1,-1v-7H19v-2C17.34,12 16,10.66 16,9z"
    ).also { _pushPin = it }
private var _pushPin: ImageVector? = null

val Icons.Rounded.Visibility: ImageVector
    get() = _visibility ?: roundedIcon(
        "Visibility",
        "M12,4.5C7,4.5 2.73,7.61 1,12c1.73,4.39 6,7.5 11,7.5s9.27,-3.11 11,-7.5C21.27,7.61 17,4.5 12,4.5zM12,17c-2.76,0 -5,-2.24 -5,-5s2.24,-5 5,-5s5,2.24 5,5S14.76,17 12,17zM12,9c-1.66,0 -3,1.34 -3,3s1.34,3 3,3s3,-1.34 3,-3S13.66,9 12,9z"
    ).also { _visibility = it }
private var _visibility: ImageVector? = null

val Icons.Rounded.CloudDone: ImageVector
    get() = _cloudDone ?: roundedIcon(
        "CloudDone",
        "M19.35,10.04C18.67,6.59 15.64,4 12,4C9.11,4 6.6,5.64 5.35,8.04C2.34,8.36 0,10.91 0,14c0,3.31 2.69,6 6,6h13c2.76,0 5,-2.24 5,-5C24,12.36 21.95,10.22 19.35,10.04zM10.71,16.29c-0.39,0.39 -1.02,0.39 -1.41,0l-2.8,-2.8c-0.39,-0.39 -0.39,-1.02 0,-1.41s1.02,-0.39 1.41,0L10,14.17l4.49,-4.49c0.39,-0.39 1.02,-0.39 1.41,0s0.39,1.02 0,1.41L10.71,16.29z"
    ).also { _cloudDone = it }
private var _cloudDone: ImageVector? = null

val Icons.Rounded.Sync: ImageVector
    get() = _sync ?: roundedIcon(
        "Sync",
        "M12,4V2.21c0,-0.45 -0.54,-0.67 -0.85,-0.35l-2.79,2.79c-0.2,0.2 -0.2,0.51 0,0.71l2.79,2.79C11.46,8.46 12,8.24 12,7.79V6c3.31,0 6,2.69 6,6c0,1.01 -0.25,1.97 -0.7,2.8c0.35,0.35 0.87,0.44 1.29,0.16C19.47,14.08 20,13.09 20,12C20,7.58 16.42,4 12,4zM12,18c-3.31,0 -6,-2.69 -6,-6c0,-1.01 0.25,-1.97 0.7,-2.8c-0.35,-0.35 -0.87,-0.44 -1.29,-0.16C4.53,9.92 4,10.91 4,12c0,4.42 3.58,8 8,8v1.79c0,0.45 0.54,0.67 0.85,0.35l2.79,-2.79c0.2,-0.2 0.2,-0.51 0,-0.71l-2.79,-2.79C12.54,15.54 12,15.76 12,16.21V18z"
    ).also { _sync = it }
private var _sync: ImageVector? = null

val Icons.Rounded.Devices: ImageVector
    get() = _devices ?: roundedIcon(
        "Devices",
        "M4,6h16c0.55,0 1,-0.45 1,-1s-0.45,-1 -1,-1H4C2.9,4 2,4.9 2,6v11H1c-0.55,0 -1,0.45 -1,1s0.45,1 1,1h13v-2H4V6zM22,8h-6c-0.55,0 -1,0.45 -1,1v10c0,0.55 0.45,1 1,1h6c0.55,0 1,-0.45 1,-1V9C23,8.45 22.55,8 22,8zM21,17h-4v-7h4V17z"
    ).also { _devices = it }
private var _devices: ImageVector? = null

val Icons.Rounded.VerifiedUser: ImageVector
    get() = _verifiedUser ?: roundedIcon(
        "VerifiedUser",
        "M11.19,1.36l-7,3.11C3.47,4.79 3,5.51 3,6.3V11c0,5.55 3.84,10.74 9,12c5.16,-1.26 9,-6.45 9,-12V6.3c0,-0.79 -0.47,-1.51 -1.19,-1.83l-7,-3.11C12.3,1.13 11.7,1.13 11.19,1.36zM10.29,15.29L7.7,12.7c-0.39,-0.39 -0.39,-1.02 0,-1.41s1.02,-0.39 1.41,0L11,13.17l4.88,-4.88c0.39,-0.39 1.02,-0.39 1.41,0s0.39,1.02 0,1.41l-5.59,5.59C11.32,15.68 10.68,15.68 10.29,15.29z"
    ).also { _verifiedUser = it }
private var _verifiedUser: ImageVector? = null

val Icons.Rounded.AdminPanelSettings: ImageVector
    get() = _adminPanelSettings ?: roundedIcon(
        "AdminPanelSettings",
        "M17,11c0.34,0 0.67,0.04 1,0.09V7.58c0,-0.8 -0.47,-1.52 -1.2,-1.83l-5.5,-2.4c-0.51,-0.22 -1.09,-0.22 -1.6,0l-5.5,2.4C3.47,6.07 3,6.79 3,7.58v3.6c0,4.54 3.2,8.79 7.5,9.82c0.55,-0.13 1.08,-0.32 1.6,-0.55C11.41,19.47 11,18.28 11,17C11,13.69 13.69,11 17,11z",
        "M17,13c-2.21,0 -4,1.79 -4,4c0,2.21 1.79,4 4,4s4,-1.79 4,-4C21,14.79 19.21,13 17,13zM17,14.38c0.62,0 1.12,0.51 1.12,1.12s-0.51,1.12 -1.12,1.12s-1.12,-0.51 -1.12,-1.12S16.38,14.38 17,14.38zM17,19.75c-0.93,0 -1.74,-0.46 -2.24,-1.17c0.05,-0.72 1.51,-1.08 2.24,-1.08s2.19,0.36 2.24,1.08C18.74,19.29 17.93,19.75 17,19.75z"
    ).also { _adminPanelSettings = it }
private var _adminPanelSettings: ImageVector? = null

val Icons.Rounded.Security: ImageVector
    get() = _security ?: roundedIcon(
        "Security",
        "M11.19,1.36l-7,3.11C3.47,4.79 3,5.51 3,6.3V11c0,5.55 3.84,10.74 9,12c5.16,-1.26 9,-6.45 9,-12V6.3c0,-0.79 -0.47,-1.51 -1.19,-1.83l-7,-3.11C12.3,1.13 11.7,1.13 11.19,1.36zM12,11.99h7c-0.53,4.12 -3.28,7.79 -7,8.94V12H5V6.3l7,-3.11v8.8z"
    ).also { _security = it }
private var _security: ImageVector? = null

val Icons.Rounded.HourglassEmpty: ImageVector
    get() = _hourglassEmpty ?: roundedIcon(
        "HourglassEmpty",
        "M8,2C6.9,2 6,2.9 6,4v3.17c0,0.53 0.21,1.04 0.59,1.42L10,12l-3.42,3.42C6.21,15.8 6,16.31 6,16.84V20c0,1.1 0.9,2 2,2h8c1.1,0 2,-0.9 2,-2v-3.16c0,-0.53 -0.21,-1.04 -0.58,-1.41L14,12l3.41,-3.4C17.79,8.22 18,7.71 18,7.18V4c0,-1.1 -0.9,-2 -2,-2H8zM16,16.5V19c0,0.55 -0.45,1 -1,1H9c-0.55,0 -1,-0.45 -1,-1v-2.5l4,-4l4,4zM12,11.5l-4,-4V5c0,-0.55 0.45,-1 1,-1h6c0.55,0 1,0.45 1,1v2.5l-4,4z"
    ).also { _hourglassEmpty = it }
private var _hourglassEmpty: ImageVector? = null

val Icons.Rounded.PhonelinkLock: ImageVector
    get() = _phonelinkLock ?: roundedIcon(
        "PhonelinkLock",
        "M19,1H9C7.9,1 7,1.9 7,3v2c0,0.55 0.45,1 1,1s1,-0.45 1,-1V4h10v16H9v-1c0,-0.55 -0.45,-1 -1,-1s-1,0.45 -1,1v2c0,1.1 0.9,2 2,2h10c1.1,0 2,-0.9 2,-2V3C21,1.9 20.1,1 19,1zM10.8,11V9.5C10.8,8.1 9.4,7 8,7S5.2,8.1 5.2,9.5V11C4.6,11 4,11.6 4,12.2v3.5C4,16.4 4.6,17 5.2,17h5.5c0.7,0 1.3,-0.6 1.3,-1.2v-3.5C12,11.6 11.4,11 10.8,11zM9.5,11h-3V9.5c0,-0.8 0.7,-1.3 1.5,-1.3s1.5,0.5 1.5,1.3V11z"
    ).also { _phonelinkLock = it }
private var _phonelinkLock: ImageVector? = null

val Icons.Rounded.Block: ImageVector
    get() = _block ?: roundedIcon(
        "Block",
        "M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10s10,-4.48 10,-10S17.52,2 12,2zM4,12c0,-4.42 3.58,-8 8,-8c1.85,0 3.55,0.63 4.9,1.69L5.69,16.9C4.63,15.55 4,13.85 4,12zM12,20c-1.85,0 -3.55,-0.63 -4.9,-1.69L18.31,7.1C19.37,8.45 20,10.15 20,12c0,4.42 -3.58,8 -8,8z"
    ).also { _block = it }
private var _block: ImageVector? = null

val Icons.Rounded.Lock: ImageVector
    get() = _lock ?: roundedIcon(
        "Lock",
        "M18,8h-1V6c0,-2.76 -2.24,-5 -5,-5S7,3.24 7,6v2H6c-1.1,0 -2,0.9 -2,2v10c0,1.1 0.9,2 2,2h12c1.1,0 2,-0.9 2,-2V10C20,8.9 19.1,8 18,8zM12,17c-1.1,0 -2,-0.9 -2,-2s0.9,-2 2,-2s2,0.9 2,2S13.1,17 12,17zM15.1,8H8.9V6c0,-1.71 1.39,-3.1 3.1,-3.1s3.1,1.39 3.1,3.1V8z"
    ).also { _lock = it }
private var _lock: ImageVector? = null

val Icons.Rounded.EventAvailable: ImageVector
    get() = _eventAvailable ?: roundedIcon(
        "EventAvailable",
        "M16.03,10.53l-4.29,4.29c-0.39,0.39 -1.02,0.39 -1.41,0l-2.06,-2.06c-0.39,-0.39 -0.39,-1.02 0,-1.41c0.39,-0.39 1.02,-0.39 1.41,0l1.36,1.36l3.59,-3.59c0.39,-0.39 1.02,-0.39 1.41,0C16.42,9.51 16.42,10.14 16.03,10.53zM19,3h-1V2c0,-0.55 -0.45,-1 -1,-1s-1,0.45 -1,1v1H8V2c0,-0.55 -0.45,-1 -1,-1S6,1.45 6,2v1H5C3.89,3 3.01,3.9 3.01,5L3,19c0,1.1 0.89,2 2,2h14c1.1,0 2,-0.9 2,-2V5C21,3.9 20.1,3 19,3zM18,19H6c-0.55,0 -1,-0.45 -1,-1V8h14v10C19,18.55 18.55,19 18,19z"
    ).also { _eventAvailable = it }
private var _eventAvailable: ImageVector? = null

val Icons.Rounded.Search: ImageVector
    get() = _search ?: roundedIcon(
        "Search",
        "M15.5,14h-0.79l-0.28,-0.27c1.2,-1.4 1.82,-3.31 1.48,-5.34c-0.47,-2.78 -2.79,-5 -5.59,-5.34c-4.23,-0.52 -7.79,3.04 -7.27,7.27c0.34,2.8 2.56,5.12 5.34,5.59c2.03,0.34 3.94,-0.28 5.34,-1.48L14,14.71v0.79l4.25,4.25c0.41,0.41 1.08,0.41 1.49,0c0.41,-0.41 0.41,-1.08 0,-1.49L15.5,14zM9.5,14C7.01,14 5,11.99 5,9.5S7.01,5 9.5,5S14,7.01 14,9.5S11.99,14 9.5,14z"
    ).also { _search = it }
private var _search: ImageVector? = null

val Icons.Rounded.CheckCircle: ImageVector
    get() = _checkCircle ?: roundedIcon(
        "CheckCircle",
        "M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10s10,-4.48 10,-10S17.52,2 12,2zM9.29,16.29L5.7,12.7c-0.39,-0.39 -0.39,-1.02 0,-1.41s1.02,-0.39 1.41,0L10,14.17l6.88,-6.88c0.39,-0.39 1.02,-0.39 1.41,0s0.39,1.02 0,1.41l-7.59,7.59C10.32,16.68 9.68,16.68 9.29,16.29z"
    ).also { _checkCircle = it }
private var _checkCircle: ImageVector? = null

val Icons.Rounded.ManageAccounts: ImageVector
    get() = _manageAccounts ?: roundedIcon(
        "ManageAccounts",
        "M10,12c2.21,0 4,-1.79 4,-4s-1.79,-4 -4,-4S6,5.79 6,8S7.79,12 10,12zM10,14c-2.67,0 -8,1.34 -8,4v1c0,0.55 0.45,1 1,1h7v-1.76c0,-1.54 0.78,-2.91 1.98,-3.74C11.34,14.18 10.67,14 10,14zM20.75,16c0,-0.22 -0.03,-0.42 -0.06,-0.63l1.14,-1.01l-1,-1.73l-1.45,0.49c-0.32,-0.27 -0.68,-0.48 -1.08,-0.63L18,11h-2l-0.3,1.49c-0.4,0.15 -0.76,0.36 -1.08,0.63l-1.45,-0.49l-1,1.73l1.14,1.01c-0.03,0.21 -0.06,0.41 -0.06,0.63s0.03,0.42 0.06,0.63l-1.14,1.01l1,1.73l1.45,-0.49c0.32,0.27 0.68,0.48 1.08,0.63L16,21h2l0.3,-1.49c0.4,-0.15 0.76,-0.36 1.08,-0.63l1.45,0.49l1,-1.73l-1.14,-1.01C20.72,16.42 20.75,16.22 20.75,16zM17,18c-1.1,0 -2,-0.9 -2,-2s0.9,-2 2,-2s2,0.9 2,2S18.1,18 17,18z"
    ).also { _manageAccounts = it }
private var _manageAccounts: ImageVector? = null

val Icons.Rounded.CloudUpload: ImageVector
    get() = _cloudUpload ?: roundedIcon(
        "CloudUpload",
        "M19.35,10.04C18.67,6.59 15.64,4 12,4C9.11,4 6.6,5.64 5.35,8.04C2.34,8.36 0,10.91 0,14c0,3.31 2.69,6 6,6h13c2.76,0 5,-2.24 5,-5C24,12.36 21.95,10.22 19.35,10.04zM14,13v4h-4v-4H7l4.65,-4.65c0.2,-0.2 0.51,-0.2 0.71,0L17,13H14z"
    ).also { _cloudUpload = it }
private var _cloudUpload: ImageVector? = null

val Icons.Rounded.History: ImageVector
    get() = _history ?: roundedIcon(
        "History",
        "M13.26,3C8.17,2.86 4,6.95 4,12H2.21c-0.45,0 -0.67,0.54 -0.35,0.85l2.79,2.8c0.2,0.2 0.51,0.2 0.71,0l2.79,-2.8C8.46,12.54 8.24,12 7.79,12H6c0,-3.9 3.18,-7.05 7.1,-7c3.72,0.05 6.85,3.18 6.9,6.9C20.05,15.81 16.9,19 13,19c-1.68,0 -3.22,-0.6 -4.43,-1.59c-0.38,-0.32 -0.95,-0.29 -1.3,0.06c-0.42,0.42 -0.39,1.11 0.06,1.49C8.88,20.23 10.85,21 13,21c5.05,0 9.14,-4.17 9,-9.26C21.86,7.05 17.95,3.14 13.26,3zM12.5,8L12.5,8C12.09,8 11.75,8.34 11.75,8.75v3.69c0,0.35 0.18,0.68 0.49,0.86l3.12,1.85c0.36,0.21 0.82,0.09 1.03,-0.26c0.21,-0.36 0.1,-0.82 -0.26,-1.03l-2.87,-1.7V8.75C13.25,8.34 12.91,8 12.5,8z"
    ).also { _history = it }
private var _history: ImageVector? = null

val Icons.Rounded.FilterList: ImageVector
    get() = _filterList ?: roundedIcon(
        "FilterList",
        "M11,18h2c0.55,0 1,-0.45 1,-1s-0.45,-1 -1,-1h-2c-0.55,0 -1,0.45 -1,1S10.45,18 11,18zM3,7c0,0.55 0.45,1 1,1h16c0.55,0 1,-0.45 1,-1s-0.45,-1 -1,-1H4C3.45,6 3,6.45 3,7zM7,13h10c0.55,0 1,-0.45 1,-1s-0.45,-1 -1,-1H7c-0.55,0 -1,0.45 -1,1S6.45,13 7,13z"
    ).also { _filterList = it }
private var _filterList: ImageVector? = null

val Icons.Rounded.SwitchAccount: ImageVector
    get() = _switchAccount ?: roundedIcon(
        "SwitchAccount",
        "M4,6H2v14c0,1.1 0.9,2 2,2h14v-2H4V6zM20,2H8C6.9,2 6,2.9 6,4v12c0,1.1 0.9,2 2,2h12c1.1,0 2,-0.9 2,-2V4C22,2.9 21.1,2 20,2zM14,5c1.66,0 3,1.34 3,3c0,1.66 -1.34,3 -3,3s-3,-1.34 -3,-3C11,6.34 12.34,5 14,5zM7.76,16c1.47,-1.83 3.71,-3 6.24,-3s4.77,1.17 6.24,3H7.76z"
    ).also { _switchAccount = it }
private var _switchAccount: ImageVector? = null



