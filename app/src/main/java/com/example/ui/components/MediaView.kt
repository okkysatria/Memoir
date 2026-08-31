package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.decode.VideoFrameDecoder
import coil.request.videoFrameMillis
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.ui.theme.*

@Composable
fun rememberColorFilterForEffect(effect: String): ColorFilter? {
    return remember(effect) {
        when (effect) {
            "Cinematic Noir" -> ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
            "Vintage Sepia" -> ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f,      0f,      0f,      1f, 0f
            )))
            "Warm Sun" -> ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                1.15f, 0f,    0f,    0f, 0f,
                0f,    1.02f, 0f,    0f, 0f,
                0f,    0f,    0.85f, 0f, 0f,
                0f,    0f,    0f,    1f, 0f
            )))
            "Cool Breeze" -> ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                0.85f, 0f,    0f,    0f, 0f,
                0f,    1.02f, 0f,    0f, 0f,
                0f,    0f,    1.15f, 0f, 0f,
                0f,    0f,    0f,    1f, 0f
            )))
            "High Contrast" -> ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                1.4f, 0f,   0f,   0f, -30f,
                0f,   1.4f, 0f,   0f, -30f,
                0f,   0f,   1.4f, 0f, -30f,
                0f,   0f,   0f,   1.0f, 0f
            )))
            "Cyberpunk" -> ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                0.8f, 0.3f, 0.3f, 0f, 20f,
                0.1f, 0.7f, 0.2f, 0f, 10f,
                0.4f, 0.1f, 1.2f, 0f, 30f,
                0f,   0f,   0f,   1.0f, 0f
            )))
            else -> null
        }
    }
}

@Composable
fun ZoomableBox(
    modifier: Modifier = Modifier,
    onDoubleClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "scaleAnim"
    )
    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "offsetXAnim"
    )
    val animatedOffsetY by animateFloatAsState(
        targetValue = offsetY,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "offsetYAnim"
    )

    val dimAlpha = ((animatedScale - 1f) / 2f).coerceIn(0f, 0.75f)

    Box(
        modifier = modifier
            .zIndex(if (animatedScale > 1.01f) 100f else 0f)
    ) {
        if (dimAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = dimAlpha))
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = animatedScale
                    scaleY = animatedScale
                    translationX = animatedOffsetX
                    translationY = animatedOffsetY
                }
                .pointerInput(Unit) {
                    if (onDoubleClick != null) {
                        detectTapGestures(
                            onDoubleTap = { onDoubleClick() }
                        )
                    }
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        do {
                            val event = awaitPointerEvent()
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()

                            if (event.changes.size > 1 || scale > 1.01f) {
                                val newScale = (scale * zoom).coerceIn(1f, 4.5f)
                                scale = newScale
                                if (newScale > 1f) {
                                    offsetX += pan.x
                                    offsetY += pan.y
                                    event.changes.forEach { it.consume() }
                                } else {
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            }
                        } while (event.changes.any { it.pressed })

                        
                        scale = 1f
                        offsetX = 0f
                        offsetY = 0f
                    }
                }
        ) {
            content()
        }
    }
}

@Composable
fun ShimmerPlaceholder(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF1A1A1A),
            Color(0xFF2A2A2A),
            Color(0xFF333333),
            Color(0xFF2A2A2A),
            Color(0xFF1A1A1A),
        ),
        start = Offset(translateAnim - 200f, 0f),
        end = Offset(translateAnim + 200f, 0f)
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(shimmerBrush)
    )
}

@Composable
fun MediaView(
    path: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    filterName: String = "Original",
    showLabel: Boolean = false,
    contentScale: ContentScale = ContentScale.Crop,
    autoPlay: Boolean = true
) {
    val isGradient = path.startsWith("gradient_")
    val colorFilter = rememberColorFilterForEffect(filterName)

    if (isGradient) {
        val brush = when (path) {
            "gradient_blue" -> Brush.linearGradient(listOf(OceanStart, OceanEnd))
            "gradient_sunset" -> Brush.linearGradient(listOf(SunsetStart, SunsetEnd))
            "gradient_temple" -> Brush.linearGradient(listOf(TempleStart, TempleEnd))
            "gradient_nature" -> Brush.linearGradient(listOf(NatureStart, NatureEnd))
            "gradient_orange" -> Brush.linearGradient(listOf(SunsetEnd, SunsetStart))
            "gradient_slate" -> Brush.linearGradient(listOf(SlateStart, SlateEnd))
            "gradient_purple" -> Brush.linearGradient(listOf(PurpleStart, PurpleEnd))
            else -> Brush.linearGradient(listOf(SlateStart, SlateEnd))
        }

        val label = when (path) {
            "gradient_blue" -> stringResource(R.string.preset_blue)
            "gradient_sunset" -> stringResource(R.string.preset_sunset)
            "gradient_temple" -> stringResource(R.string.preset_temple)
            "gradient_nature" -> stringResource(R.string.preset_nature)
            "gradient_orange" -> stringResource(R.string.preset_orange)
            "gradient_slate" -> stringResource(R.string.preset_slate)
            "gradient_purple" -> stringResource(R.string.preset_purple)
            else -> stringResource(R.string.preset_default)
        }

        Box(
            modifier = modifier
                .background(brush)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.width.coerceAtMost(size.height) * 0.22f
                
                drawCircle(
                    color = Color.White.copy(alpha = 0.15f),
                    radius = radius,
                    style = Stroke(width = 4.dp.toPx())
                )
                for (i in 0 until 6) {
                    val angle = (i * 60) * Math.PI / 180.0
                    val xStart = center.x + radius * Math.cos(angle).toFloat()
                    val yStart = center.y + radius * Math.sin(angle).toFloat()
                    val xEnd = center.x + (radius + 40f) * Math.cos(angle + 0.5).toFloat()
                    val yEnd = center.y + (radius + 40f) * Math.sin(angle + 0.5).toFloat()
                    
                    drawLine(
                        color = Color.White.copy(alpha = 0.1f),
                        start = Offset(xStart, yStart),
                        end = Offset(xEnd, yEnd),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }

            if (showLabel) {
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.35f), shape = MaterialTheme.shapes.small)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    } else if (com.example.utils.FileUtils.isVideoPath(path)) {
        if (autoPlay) {
            VideoPlayerView(
                path = path,
                modifier = modifier,
                filterName = filterName,
                contentScale = contentScale,
                autoPlay = true
            )
        } else {
            
            
            val context = androidx.compose.ui.platform.LocalContext.current
            val videoUri = remember(path) {
                when {
                    path.startsWith("content://") || path.startsWith("file://") -> android.net.Uri.parse(path)
                    path.startsWith("/") -> android.net.Uri.fromFile(java.io.File(path))
                    else -> android.net.Uri.fromFile(com.example.utils.FileUtils.getLocalFile(context, path))
                }
            }

            SubcomposeAsyncImage(
                model = coil.request.ImageRequest.Builder(context)
                    .data(videoUri)
                    .crossfade(true)
                    .decoderFactory(VideoFrameDecoder.Factory())
                    .videoFrameMillis(1000) 
                    .build(),
                contentDescription = contentDescription,
                modifier = modifier.fillMaxSize(),
                contentScale = contentScale,
                colorFilter = colorFilter,
            ) {
                val state = painter.state
                if (state is AsyncImagePainter.State.Loading) {
                    ShimmerPlaceholder(modifier = Modifier.fillMaxSize())
                } else if (state is AsyncImagePainter.State.Error) {
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1E1E1E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                } else {
                    SubcomposeAsyncImageContent()
                }
            }
        }
    } else {
        val context = androidx.compose.ui.platform.LocalContext.current
        val imageModel: Any = remember(path) {
            when {
                path.startsWith("content://") || path.startsWith("file://") || path.startsWith("http://") || path.startsWith("https://") -> {
                    android.net.Uri.parse(path)
                }
                path.startsWith("/") -> {
                    java.io.File(path)
                }
                else -> {
                    com.example.utils.FileUtils.getLocalFile(context, path)
                }
            }
        }

        SubcomposeAsyncImage(
            model = imageModel,
            contentDescription = contentDescription ?: stringResource(R.string.nav_home),
            modifier = modifier.fillMaxSize(),
            contentScale = contentScale,
            colorFilter = colorFilter,
        ) {
            when (painter.state) {
                is AsyncImagePainter.State.Loading -> {
                    ShimmerPlaceholder(modifier = Modifier.fillMaxSize())
                }
                is AsyncImagePainter.State.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1E1E1E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BrokenImage,
                            contentDescription = stringResource(R.string.desc_error_media),
                            tint = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                else -> {
                    SubcomposeAsyncImageContent()
                }
            }
        }
    }
}



