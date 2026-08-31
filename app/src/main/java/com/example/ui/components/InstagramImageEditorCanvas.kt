package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.utils.FileUtils

@Composable
fun InstagramImageEditorCanvas(
    mediaPath: String,
    selectedRatio: String = "1:1",
    onRatioChange: ((String) -> Unit)? = null,
    initialScale: Float = 1f,
    initialRotation: Float = 0f,
    initialOffset: Offset = Offset.Zero,
    onTransformChange: ((scale: Float, rotation: Float, offsetX: Float, offsetY: Float, vw: Float, vh: Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var scale by remember(mediaPath) { mutableFloatStateOf(initialScale) }
    var rotation by remember(mediaPath) { mutableFloatStateOf(initialRotation) }
    var offset by remember(mediaPath) { mutableStateOf(initialOffset) }
    var isGesturing by remember { mutableStateOf(false) }

    var viewportSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }

    val originalRatio = remember(mediaPath) {
        FileUtils.getImageAspectRatio(context, mediaPath)
    }

    val originalLabel = stringResource(R.string.label_aspect_original)
    val aspectRatio = when {
        selectedRatio == "ORIGINAL" || selectedRatio == "Asli" || selectedRatio == "Original" || selectedRatio.startsWith("Asli:") || selectedRatio.startsWith("Original:") -> originalRatio
        selectedRatio == "4:5" -> 0.8f
        selectedRatio == "16:9" -> 16f / 9f
        selectedRatio == "9:16" -> 9f / 16f
        else -> 1f 
    }

    LaunchedEffect(scale, rotation, offset, viewportSize) {
        onTransformChange?.invoke(scale, rotation, offset.x, offset.y, viewportSize.width.toFloat(), viewportSize.height.toFloat())
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val containerRatio = this.maxWidth / this.maxHeight
            val targetRatio = aspectRatio.coerceIn(0.4f, 2.5f)
            val canvasModifier = if (targetRatio > containerRatio) {
                Modifier.fillMaxWidth(0.96f).aspectRatio(targetRatio)
            } else {
                Modifier.fillMaxHeight(0.88f).aspectRatio(targetRatio)
            }

            Box(
                modifier = canvasModifier
                    .clipToBounds()
                    .background(Color.Black)
                    .onGloballyPositioned { coords ->
                        viewportSize = coords.size
                    }
                    .pointerInput(mediaPath) {
                        detectTransformGestures { _, pan, zoom, gestureRotation ->
                            scale = (scale * zoom).coerceIn(1f, 5.0f)
                            offset += pan
                            rotation = (rotation + gestureRotation) % 360f
                        }
                    }
                    .pointerInput(mediaPath) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            isGesturing = true
                            do {
                                val event = awaitPointerEvent()
                            } while (event.changes.any { it.pressed })
                            isGesturing = false
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                MediaView(
                    path = mediaPath,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            rotationZ = rotation
                            translationX = offset.x
                            translationY = offset.y
                        }
                )

                AnimatedVisibility(
                    visible = isGesturing,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    RuleOfThirdsGridOverlay()
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), CircleShape)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            IconButton(
                onClick = {
                    rotation = (rotation + 90f) % 360f
                },
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.RotateRight,
                    contentDescription = stringResource(R.string.label_rotate_90),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (onRatioChange != null) {
                val ratios = listOf("ORIGINAL", "1:1", "4:5", "16:9", "9:16")
                ratios.forEach { ratio ->
                    val isSelected = (ratio == "ORIGINAL" && (selectedRatio == "ORIGINAL" || selectedRatio == "Asli" || selectedRatio == "Original")) || selectedRatio == ratio
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            onRatioChange(ratio)
                            offset = Offset.Zero
                        },
                        label = { 
                            Text(
                                text = if (ratio == "ORIGINAL") stringResource(R.string.label_aspect_original) else ratio, 
                                fontSize = 10.sp, 
                                fontWeight = FontWeight.Bold
                            ) 
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = null,
                        modifier = Modifier.height(26.dp)
                    )
                }
            }

            IconButton(
                onClick = {
                    scale = 1f
                    rotation = 0f
                    offset = Offset.Zero
                },
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.label_reset),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun RuleOfThirdsGridOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        val strokeWidth = 1.dp.toPx()
        val gridColor = Color.White.copy(alpha = 0.5f)

        drawLine(
            color = gridColor,
            start = Offset(width / 3f, 0f),
            end = Offset(width / 3f, height),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = gridColor,
            start = Offset(2 * width / 3f, 0f),
            end = Offset(2 * width / 3f, height),
            strokeWidth = strokeWidth
        )

        drawLine(
            color = gridColor,
            start = Offset(0f, height / 3f),
            end = Offset(width, height / 3f),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = gridColor,
            start = Offset(0f, 2 * height / 3f),
            end = Offset(width, 2 * height / 3f),
            strokeWidth = strokeWidth
        )
    }
}
