package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import java.util.UUID
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

enum class StoryAlignType {
    START,
    CENTER,
    END;

    fun toTextAlign(): TextAlign = when (this) {
        START -> TextAlign.Start
        CENTER -> TextAlign.Center
        END -> TextAlign.End
    }

    fun next(): StoryAlignType = when (this) {
        START -> CENTER
        CENTER -> END
        END -> START
    }
}

enum class StoryColorSchemeType {
    TEXT_ONLY,
    BACKGROUND,
    INVERTED,
    GLASS;

    fun next(): StoryColorSchemeType = when (this) {
        TEXT_ONLY -> BACKGROUND
        BACKGROUND -> INVERTED
        INVERTED -> GLASS
        GLASS -> TEXT_ONLY
    }
}

data class StoryColorScheme(
    val id: String,
    val primary: Color,
    val onPrimary: Color,
    val hex: String
) {
    companion object {
        val White = StoryColorScheme("white", Color.White, Color.Black, "#FFFFFFFF")
        val Black = StoryColorScheme("black", Color.Black, Color.White, "#FF000000")
        val Yellow = StoryColorScheme("yellow", Color(0xFFFFEB3B), Color.Black, "#FFFFEB3B")
        val Green = StoryColorScheme("green", Color(0xFF4CAF50), Color.White, "#FF4CAF50")
        val Pink = StoryColorScheme("pink", Color(0xFFFF4081), Color.White, "#FFFF4081")
        val Cyan = StoryColorScheme("cyan", Color(0xFF00E5FF), Color.Black, "#FF00E5FF")
        val Purple = StoryColorScheme("purple", Color(0xFF9C27B0), Color.White, "#FF9C27B0")
        val Orange = StoryColorScheme("orange", Color(0xFFFF9800), Color.White, "#FFFF9800")
        val Red = StoryColorScheme("red", Color(0xFFF44336), Color.White, "#FFF44336")
        val Blue = StoryColorScheme("blue", Color(0xFF2196F3), Color.White, "#FF2196F3")

        val Presets = listOf(White, Black, Yellow, Green, Pink, Cyan, Purple, Orange, Red, Blue)

        fun fromHex(hex: String?): StoryColorScheme {
            if (hex.isNullOrBlank()) return White
            return Presets.find { it.hex.equals(hex, ignoreCase = true) } ?: try {
                val parsed = Color(android.graphics.Color.parseColor(hex))
                StoryColorScheme("custom", parsed, Color.White, hex)
            } catch (e: Exception) {
                White
            }
        }
    }
}

data class StoryFontStyle(
    val id: String,
    val name: String,
    val fontFamily: FontFamily = FontFamily.Default,
    val fontWeight: FontWeight = FontWeight.Normal,
    val fontStyle: FontStyle = FontStyle.Normal
) {
    companion object {
        val Modern = StoryFontStyle("MODERN", "Modern", FontFamily.Default, FontWeight.Bold)
        val Classic = StoryFontStyle("CLASSIC", "Classic", FontFamily.Serif, FontWeight.Normal)
        val Neon = StoryFontStyle("NEON", "Neon", FontFamily.Default, FontWeight.ExtraBold)
        val Typewriter = StoryFontStyle("TYPEWRITER", "Typewriter", FontFamily.Monospace, FontWeight.Normal)
        val Serif = StoryFontStyle("SERIF", "Serif", FontFamily.Serif, FontWeight.Bold)
        val Strong = StoryFontStyle("STRONG", "Strong", FontFamily.SansSerif, FontWeight.Black)
        val Elegant = StoryFontStyle("ELEGANT", "Elegant", FontFamily.Default, FontWeight.Normal, FontStyle.Italic)

        val Presets = listOf(Modern, Classic, Neon, Typewriter, Serif, Strong, Elegant)

        fun fromId(id: String?): StoryFontStyle {
            if (id.isNullOrBlank()) return Modern
            return Presets.find { it.id.equals(id, ignoreCase = true) } ?: Modern
        }
    }
}

@Stable
class StoryElementTransformation(
    initialScale: Float = 1f,
    initialRotation: Float = 0f,
    initialOffset: Offset = Offset.Zero
) {
    var scale by mutableStateOf(initialScale)
    var rotation by mutableStateOf(initialRotation)
    var offset by mutableStateOf(initialOffset)

    val displayScale: Animatable<Float, AnimationVector1D> = Animatable(initialScale)
    val displayRotation: Animatable<Float, AnimationVector1D> = Animatable(initialRotation)
    val displayOffset: Animatable<Offset, AnimationVector2D> =
        Animatable(initialOffset, Offset.VectorConverter)

    var isEditing by mutableStateOf(false)
        private set

    suspend fun startEdit(targetOffset: Offset = Offset.Zero) {
        isEditing = true
        coroutineScope {
            launch { displayScale.animateTo(1f, spring()) }
            launch { displayRotation.animateTo(0f, spring()) }
            launch { displayOffset.animateTo(targetOffset, spring()) }
        }
    }

    suspend fun stopEdit(maxExtentX: Float, maxExtentY: Float) {
        isEditing = false
        val safeOffset = Offset(
            x = offset.x.coerceIn(-maxExtentX, maxExtentX),
            y = offset.y.coerceIn(-maxExtentY, maxExtentY)
        )
        offset = safeOffset

        coroutineScope {
            launch { displayScale.animateTo(scale, spring()) }
            launch { displayRotation.animateTo(rotation, spring()) }
            launch { displayOffset.animateTo(offset, spring()) }
        }
    }

    fun updateTransform(zoom: Float, rotate: Float, pan: Offset, maxExtentX: Float, maxExtentY: Float) {
        scale = (scale * zoom).coerceIn(0.4f, 6.0f)
        rotation += rotate
        offset = Offset(
            x = (offset.x + pan.x).coerceIn(-maxExtentX, maxExtentX),
            y = (offset.y + pan.y).coerceIn(-maxExtentY, maxExtentY)
        )
    }
}

@Stable
class StoryTextElementItem(
    val id: String = UUID.randomUUID().toString(),
    initialText: String = "",
    initialColorScheme: StoryColorScheme = StoryColorScheme.White,
    initialColorSchemeType: StoryColorSchemeType = StoryColorSchemeType.TEXT_ONLY,
    initialAlignType: StoryAlignType = StoryAlignType.CENTER,
    initialFontStyle: StoryFontStyle = StoryFontStyle.Modern,
    initialScale: Float = 1f,
    initialRotation: Float = 0f,
    initialOffset: Offset = Offset.Zero
) {
    var text by mutableStateOf(initialText)
    var colorScheme by mutableStateOf(initialColorScheme)
    var colorSchemeType by mutableStateOf(initialColorSchemeType)
    var alignType by mutableStateOf(initialAlignType)
    var fontStyle by mutableStateOf(initialFontStyle)

    val transformation = StoryElementTransformation(
        initialScale = initialScale,
        initialRotation = initialRotation,
        initialOffset = initialOffset
    )

    fun copyItem(): StoryTextElementItem {
        return StoryTextElementItem(
            id = id,
            initialText = text,
            initialColorScheme = colorScheme,
            initialColorSchemeType = colorSchemeType,
            initialAlignType = alignType,
            initialFontStyle = fontStyle,
            initialScale = transformation.scale,
            initialRotation = transformation.rotation,
            initialOffset = transformation.offset
        )
    }
}

@Stable
class StoryEditorState(
    val elementsBoundsFraction: Rect = Rect(0.04f, 0.08f, 0.96f, 0.92f)
) {
    var editorSize by mutableStateOf(IntSize.Zero)
    var focusedElement by mutableStateOf<StoryTextElementItem?>(null)
    var draggedElement by mutableStateOf<StoryTextElementItem?>(null)
    var pointerPosition by mutableStateOf(Offset.Unspecified)
    var isOverDelete by mutableStateOf(false)

    val isEditing: Boolean
        get() = focusedElement != null

    val isDragging: Boolean
        get() = draggedElement != null

    fun isElementFocusable(element: StoryTextElementItem): Boolean {
        return (focusedElement == null || focusedElement === element) && draggedElement == null
    }
}

@Composable
fun rememberStoryEditorState(): StoryEditorState {
    return remember { StoryEditorState() }
}
