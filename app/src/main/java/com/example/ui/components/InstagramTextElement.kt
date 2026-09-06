package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlin.math.abs

@Composable
fun CanvasInstagramTextElement(
    element: StoryTextElementItem,
    canvasWidth: Dp,
    isDragging: Boolean,
    isFocused: Boolean,
    onDragStart: () -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: (isOverTrash: Boolean) -> Unit,
    onTap: () -> Unit,
    onTransform: (zoom: Float, rotate: Float, pan: Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var linesBounds by remember { mutableStateOf(listOf<Rect>()) }

    val textColor = when (element.colorSchemeType) {
        StoryColorSchemeType.TEXT_ONLY -> element.colorScheme.primary
        StoryColorSchemeType.BACKGROUND -> element.colorScheme.onPrimary
        StoryColorSchemeType.INVERTED -> element.colorScheme.primary
        StoryColorSchemeType.GLASS -> element.colorScheme.primary
    }

    val badgeColor = when (element.colorSchemeType) {
        StoryColorSchemeType.TEXT_ONLY -> Color.Transparent
        StoryColorSchemeType.BACKGROUND -> element.colorScheme.primary
        StoryColorSchemeType.INVERTED -> element.colorScheme.onPrimary
        StoryColorSchemeType.GLASS -> Color.Black.copy(alpha = 0.72f)
    }

    val borderAlpha by animateFloatAsState(
        targetValue = if (isDragging) 0.85f else 0f,
        label = "drag_border"
    )

    val horizPaddingPx = with(density) { 14.dp.toPx() }
    val vertPaddingPx = with(density) { 6.dp.toPx() }
    val cornerRadiusPx = with(density) { 10.dp.toPx() }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = element.transformation.displayScale.value
                scaleY = element.transformation.displayScale.value
                rotationZ = element.transformation.displayRotation.value
                translationX = element.transformation.displayOffset.value.x
                translationY = element.transformation.displayOffset.value.y
            }
            .border(1.dp, Color.White.copy(alpha = borderAlpha), RoundedCornerShape(12.dp))
            .widthIn(max = canvasWidth * 0.90f)
            .then(
                if (!isFocused) {
                    Modifier.pointerInput(element.id) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            var cumulativePan = Offset.Zero
                            var cumulativeZoom = 1f
                            var cumulativeRotate = 0f
                            var hasTransformed = false
                            val touchSlop = viewConfiguration.touchSlop

                            do {
                                val event = awaitPointerEvent()
                                val zoom = event.calculateZoom()
                                val rotate = event.calculateRotation()
                                val pan = event.calculatePan()

                                cumulativePan += pan
                                cumulativeZoom *= zoom
                                cumulativeRotate += rotate

                                val moved = cumulativePan.getDistance() > touchSlop ||
                                        abs(1f - cumulativeZoom) > 0.05f ||
                                        abs(cumulativeRotate) > 2f

                                if (moved && !hasTransformed) {
                                    hasTransformed = true
                                    onDragStart()
                                }

                                if (hasTransformed) {
                                    onTransform(zoom, rotate, pan)
                                    val currentRootPos = down.position + cumulativePan
                                    onDragMove(currentRootPos)
                                    event.changes.forEach { it.consume() }
                                }
                            } while (event.changes.any { it.pressed })

                            if (!hasTransformed) {
                                onTap()
                            } else {
                                onDragEnd(false)
                            }
                        }
                    }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .drawWithContent {
                    if (linesBounds.isNotEmpty() && element.colorSchemeType != StoryColorSchemeType.TEXT_ONLY) {
                        val cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)

                        linesBounds.forEach { lineRect ->
                            if (lineRect.width > 0f) {
                                val badgeTopLeft = Offset(
                                    x = lineRect.left - horizPaddingPx,
                                    y = lineRect.top - vertPaddingPx
                                )
                                val badgeSize = Size(
                                    width = lineRect.width + (horizPaddingPx * 2f),
                                    height = lineRect.height + (vertPaddingPx * 2f)
                                )

                                drawRoundRect(
                                    color = badgeColor,
                                    topLeft = badgeTopLeft,
                                    size = badgeSize,
                                    cornerRadius = cornerRadius
                                )

                                if (element.colorSchemeType == StoryColorSchemeType.GLASS) {
                                    drawRoundRect(
                                        color = Color.White.copy(alpha = 0.35f),
                                        topLeft = badgeTopLeft,
                                        size = badgeSize,
                                        cornerRadius = cornerRadius,
                                        style = Stroke(width = 2f)
                                    )
                                }
                            }
                        }
                    }
                    drawContent()
                }
        ) {
            Text(
                text = element.text,
                color = textColor,
                textAlign = element.alignType.toTextAlign(),
                style = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = element.fontStyle.fontWeight,
                    fontFamily = element.fontStyle.fontFamily,
                    fontStyle = element.fontStyle.fontStyle,
                    textAlign = element.alignType.toTextAlign(),
                    shadow = if (element.colorSchemeType == StoryColorSchemeType.TEXT_ONLY) {
                        Shadow(Color.Black.copy(alpha = 0.85f), Offset(2f, 2f), 8f)
                    } else null
                ),
                onTextLayout = { layoutResult ->
                    if (layoutResult.layoutInput.text.isEmpty()) {
                        linesBounds = emptyList()
                        return@Text
                    }
                    linesBounds = List(layoutResult.lineCount) { line ->
                        val lineContent = layoutResult.layoutInput.text.text.substring(
                            layoutResult.getLineStart(line),
                            layoutResult.getLineEnd(line)
                        )
                        val top = layoutResult.getLineTop(line)
                        val bottom = layoutResult.getLineBottom(line)
                        if (lineContent.isBlank()) {
                            Rect(0f, top, 0f, bottom)
                        } else {
                            Rect(
                                left = layoutResult.getLineLeft(line),
                                top = top,
                                right = layoutResult.getLineRight(line),
                                bottom = bottom
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun InstagramTextEditOverlay(
    element: StoryTextElementItem,
    canvasWidth: Dp,
    onDone: (shouldKeep: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    var linesBounds by remember { mutableStateOf(listOf<Rect>()) }

    val textColor = when (element.colorSchemeType) {
        StoryColorSchemeType.TEXT_ONLY -> element.colorScheme.primary
        StoryColorSchemeType.BACKGROUND -> element.colorScheme.onPrimary
        StoryColorSchemeType.INVERTED -> element.colorScheme.primary
        StoryColorSchemeType.GLASS -> element.colorScheme.primary
    }

    val badgeColor = when (element.colorSchemeType) {
        StoryColorSchemeType.TEXT_ONLY -> Color.Transparent
        StoryColorSchemeType.BACKGROUND -> element.colorScheme.primary
        StoryColorSchemeType.INVERTED -> element.colorScheme.onPrimary
        StoryColorSchemeType.GLASS -> Color.Black.copy(alpha = 0.72f)
    }

    val horizPaddingPx = with(density) { 14.dp.toPx() }
    val vertPaddingPx = with(density) { 6.dp.toPx() }
    val cornerRadiusPx = with(density) { 10.dp.toPx() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .imePadding()
            .clickable(onClick = { onDone(element.text.isNotBlank()) })
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { element.alignType = element.alignType.next() },
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                Icon(
                    imageVector = when (element.alignType) {
                        StoryAlignType.START -> Icons.AutoMirrored.Filled.FormatAlignLeft
                        StoryAlignType.CENTER -> Icons.Default.FormatAlignCenter
                        StoryAlignType.END -> Icons.AutoMirrored.Filled.FormatAlignRight
                    },
                    contentDescription = null,
                    tint = Color.White
                )
            }

            IconButton(
                onClick = { element.colorSchemeType = element.colorSchemeType.next() },
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when (element.colorSchemeType) {
                            StoryColorSchemeType.BACKGROUND -> element.colorScheme.primary
                            StoryColorSchemeType.INVERTED -> element.colorScheme.onPrimary
                            StoryColorSchemeType.GLASS -> Color.Black.copy(alpha = 0.7f)
                            StoryColorSchemeType.TEXT_ONLY -> Color.White.copy(alpha = 0.2f)
                        }
                    )
                    .then(
                        if (element.colorSchemeType == StoryColorSchemeType.GLASS)
                            Modifier.border(1.5.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        else Modifier
                    )
            ) {
                Text(
                    text = "A",
                    color = when (element.colorSchemeType) {
                        StoryColorSchemeType.BACKGROUND -> element.colorScheme.onPrimary
                        StoryColorSchemeType.INVERTED -> element.colorScheme.primary
                        StoryColorSchemeType.GLASS -> element.colorScheme.primary
                        StoryColorSchemeType.TEXT_ONLY -> Color.White
                    },
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
            }

            Button(
                onClick = { onDone(element.text.isNotBlank()) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    stringResource(R.string.btn_done),
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = canvasWidth * 0.90f)
                .padding(horizontal = 16.dp)
                .clickable(enabled = false) {},
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .drawWithContent {
                        if (linesBounds.isNotEmpty() && element.colorSchemeType != StoryColorSchemeType.TEXT_ONLY) {
                            val cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)

                            linesBounds.forEach { lineRect ->
                                if (lineRect.width > 0f) {
                                    val badgeTopLeft = Offset(
                                        x = lineRect.left - horizPaddingPx,
                                        y = lineRect.top - vertPaddingPx
                                    )
                                    val badgeSize = Size(
                                        width = lineRect.width + (horizPaddingPx * 2f),
                                        height = lineRect.height + (vertPaddingPx * 2f)
                                    )

                                    drawRoundRect(
                                        color = badgeColor,
                                        topLeft = badgeTopLeft,
                                        size = badgeSize,
                                        cornerRadius = cornerRadius
                                    )

                                    if (element.colorSchemeType == StoryColorSchemeType.GLASS) {
                                        drawRoundRect(
                                            color = Color.White.copy(alpha = 0.35f),
                                            topLeft = badgeTopLeft,
                                            size = badgeSize,
                                            cornerRadius = cornerRadius,
                                            style = Stroke(width = 2f)
                                        )
                                    }
                                }
                            }
                        }
                        drawContent()
                    }
            ) {
                BasicTextField(
                    value = element.text,
                    onValueChange = { element.text = it },
                    textStyle = TextStyle(
                        color = textColor,
                        fontSize = 26.sp,
                        fontWeight = element.fontStyle.fontWeight,
                        fontFamily = element.fontStyle.fontFamily,
                        fontStyle = element.fontStyle.fontStyle,
                        textAlign = element.alignType.toTextAlign(),
                        shadow = if (element.colorSchemeType == StoryColorSchemeType.TEXT_ONLY) {
                            Shadow(Color.Black.copy(alpha = 0.9f), Offset(2f, 2f), 8f)
                        } else null
                    ),
                    cursorBrush = SolidColor(textColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.Center) {
                            if (element.text.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.placeholder_type_text),
                                    color = Color.White.copy(alpha = 0.45f),
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = element.alignType.toTextAlign(),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            innerTextField()
                        }
                    },
                    onTextLayout = { layoutResult ->
                        if (layoutResult.layoutInput.text.isEmpty()) {
                            linesBounds = emptyList()
                            return@BasicTextField
                        }
                        linesBounds = List(layoutResult.lineCount) { line ->
                            val lineContent = layoutResult.layoutInput.text.text.substring(
                                layoutResult.getLineStart(line),
                                layoutResult.getLineEnd(line)
                            )
                            val top = layoutResult.getLineTop(line)
                            val bottom = layoutResult.getLineBottom(line)
                            if (lineContent.isBlank()) {
                                Rect(0f, top, 0f, bottom)
                            } else {
                                Rect(
                                    left = layoutResult.getLineLeft(line),
                                    top = top,
                                    right = layoutResult.getLineRight(line),
                                    bottom = bottom
                                )
                            }
                        }
                    }
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clickable(enabled = false) {}
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StoryFontStyle.Presets.forEach { fontStyle ->
                    val isSelected = element.fontStyle.id == fontStyle.id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isSelected) Color.White else Color.White.copy(alpha = 0.2f))
                            .border(
                                width = if (isSelected) 0.dp else 1.dp,
                                color = Color.White.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .clickable { element.fontStyle = fontStyle }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = fontStyle.name,
                            fontSize = 12.sp,
                            fontWeight = fontStyle.fontWeight,
                            fontFamily = fontStyle.fontFamily,
                            fontStyle = fontStyle.fontStyle,
                            color = if (isSelected) Color.Black else Color.White
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StoryColorScheme.Presets.forEach { colorScheme ->
                    val isSelected = element.colorScheme.id == colorScheme.id
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.4f),
                                shape = CircleShape
                            )
                            .padding(3.dp)
                            .clip(CircleShape)
                            .background(colorScheme.primary)
                            .clickable { element.colorScheme = colorScheme }
                    )
                }
            }
        }
    }
}

@Composable
fun EditorDeleteButton(
    isDragging: Boolean,
    pointerPosition: Offset,
    isOverTrash: Boolean,
    onOverTrashChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current
    var buttonBounds by remember { mutableStateOf(Rect.Zero) }

    val currentOverTrash by remember(pointerPosition, buttonBounds) {
        derivedStateOf {
            if (pointerPosition.isSpecified && buttonBounds != Rect.Zero) {
                buttonBounds.contains(pointerPosition)
            } else {
                false
            }
        }
    }

    LaunchedEffect(currentOverTrash) {
        onOverTrashChanged(currentOverTrash)
        if (currentOverTrash) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (isOverTrash) 1.45f else 1f,
        animationSpec = spring(),
        label = "delete_scale"
    )

    AnimatedVisibility(
        visible = isDragging,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .onGloballyPositioned { coordinates ->
                    buttonBounds = coordinates.boundsInRoot()
                }
                .scale(scale)
                .size(56.dp)
                .clip(CircleShape)
                .background(if (isOverTrash) Color(0xFFFF3B30) else Color.Black.copy(alpha = 0.65f))
                .border(2.dp, if (isOverTrash) Color.White else Color.White.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}
