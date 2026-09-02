package com.example.ui.components

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import java.io.File
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.platform.LocalConfiguration

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerView(
    path: String,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = true,
    looping: Boolean = true,
    showControls: Boolean = false,
    contentScale: ContentScale = ContentScale.Crop,
    filterName: String = "Original"
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val config = LocalConfiguration.current
    val screenHeight = config.screenHeightDp.dp.value * config.densityDpi / 160f
    var isMuted by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(autoPlay) }
    var isVisibleOnScreen by remember { mutableStateOf(false) }
    val mediaUri = remember(path) {
        when {
            path.startsWith("content://") || path.startsWith("file://") -> Uri.parse(path)
            path.startsWith("/") -> Uri.fromFile(File(path))
            else -> {
                val localFile = com.example.utils.FileUtils.getLocalFile(context, path)
                if (localFile.exists()) Uri.fromFile(localFile) else Uri.parse(path)
            }
        }
    }
    val exoPlayer = remember(path) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(mediaUri))
            repeatMode = if (looping) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
            volume = if (isMuted) 0f else 1f
            playWhenReady = isPlaying
            prepare()
        }
    }
    LaunchedEffect(isPlaying, isVisibleOnScreen, autoPlay) {
        if (autoPlay) {
            exoPlayer.playWhenReady = isPlaying && isVisibleOnScreen
        } else {
            exoPlayer.playWhenReady = isPlaying
        }
    }
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> {
                    if (isPlaying && (isVisibleOnScreen || !autoPlay)) exoPlayer.play()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                val y = coords.positionInWindow().y
                val height = coords.size.height
                isVisibleOnScreen = (y + height * 0.8f > 0) && (y + height * 0.2f < screenHeight)
            },
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = showControls
                    try {
                        val method = this.javaClass.getMethod("setSurfaceType", Int::class.javaPrimitiveType)
                        method.invoke(this, 2)
                    } catch (_: Exception) {}
                    resizeMode = when (contentScale) {
                        ContentScale.Fit -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        ContentScale.FillBounds -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                        else -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    }
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                playerView.player = exoPlayer
                playerView.resizeMode = when (contentScale) {
                    ContentScale.Fit -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    ContentScale.FillBounds -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    else -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) {
                    isPlaying = !isPlaying
                }
        )
        AnimatedVisibility(
            visible = !isPlaying,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable {
                    isMuted = !isMuted
                    exoPlayer.volume = if (isMuted) 0f else 1f
                }
                .padding(6.dp)
        ) {
            Icon(
                imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                contentDescription = if (isMuted) "Unmute" else "Mute",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
