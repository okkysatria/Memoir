package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.database.StoryEntity
import com.example.data.database.StoryHighlightEntity
import kotlinx.coroutines.delay

@Composable
fun StoryViewer(
    stories: List<StoryEntity>,
    currentIndex: Int,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onClose: () -> Unit,
    onDeleteStory: ((StoryEntity) -> Unit)? = null,
    highlights: List<StoryHighlightEntity> = emptyList(),
    onAddToHighlight: ((StoryEntity, Long) -> Unit)? = null,
    onAddHighlight: ((String, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (stories.isEmpty() || currentIndex >= stories.size || currentIndex < 0) return

    val currentStory = stories[currentIndex]
    val parentHighlight = remember(currentStory.highlightId, highlights) {
        highlights.find { it.id == currentStory.highlightId }
    }
    var progress by remember { mutableStateOf(0f) }
    var isPaused by remember { mutableStateOf(false) }

    
    var showHighlightsPopup by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showNewHighlightDialog by remember { mutableStateOf(false) }
    var newHighlightName by remember { mutableStateOf("") }
    val context = LocalContext.current

    androidx.activity.compose.BackHandler {
        if (showNewHighlightDialog) {
            showNewHighlightDialog = false
        } else if (showHighlightsPopup) {
            showHighlightsPopup = false
        } else if (showDeleteConfirmation) {
            showDeleteConfirmation = false
        } else {
            onClose()
        }
    }

    
    LaunchedEffect(currentIndex) {
        progress = 0f
    }

    val anyOverlayOpen = showHighlightsPopup || showDeleteConfirmation || showNewHighlightDialog
    val activePaused = isPaused || anyOverlayOpen

    
    LaunchedEffect(currentIndex, activePaused) {
        if (activePaused) return@LaunchedEffect
        val step = 0.01f
        val delayMillis = 50L 
        while (progress < 1.0f) {
            delay(delayMillis)
            progress += step
        }
        onNext()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        try {
                            isPaused = true
                            awaitRelease()
                        } finally {
                            isPaused = false
                        }
                    },
                    onTap = { offset ->
                        val screenWidth = size.width
                        if (offset.x < screenWidth * 0.3f) {
                            onPrev()
                        } else {
                            onNext()
                        }
                    }
                )
            }
    ) {
        
        MediaView(
            path = currentStory.mediaPath,
            modifier = Modifier.fillMaxSize()
        )

        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                    )
                )
        )

        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                stories.forEachIndexed { index, _ ->
                    val segmentProgress = when {
                        index < currentIndex -> 1f
                        index > currentIndex -> 0f
                        else -> progress
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(segmentProgress)
                                .background(Color.White)
                        )
                    }
                }
            }

            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                androidx.compose.ui.graphics.Brush.linearGradient(
                                    listOf(Color(0xFFE1306C), Color(0xFFC13584))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.label_me),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column {
                        Text(
                            text = parentHighlight?.name ?: stringResource(R.string.my_memoir),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        val timeAgo = remember(currentStory.createdAt, context) {
                            val diff = System.currentTimeMillis() - currentStory.createdAt
                            when {
                                diff < 60 * 1000L -> context.getString(R.string.msg_just_now)
                                diff < 60 * 60 * 1000L -> context.getString(R.string.msg_minutes_ago, diff / (60 * 1000L))
                                diff < 24 * 60 * 60 * 1000L -> context.getString(R.string.msg_hours_ago, diff / (3600 * 1000L))
                                else -> context.getString(R.string.msg_yesterday)
                            }
                        }
                        val statusLabel = if (parentHighlight != null) stringResource(R.string.label_highlight) else stringResource(R.string.label_private)
                        Text(
                            text = "$statusLabel • $timeAgo",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                }

                IconButton(
                    onClick = onClose,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.btn_done)
                    )
                }
            }
        }

        
        if (!currentStory.caption.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.85f),
                contentAlignment = Alignment.Center
            ) {
                val parsedColor = remember(currentStory.textColor) {
                    try {
                        currentStory.textColor?.let { Color(android.graphics.Color.parseColor(it)) } ?: Color.White
                    } catch (e: Exception) {
                        Color.White
                    }
                }

                val textStyleModifier = when (currentStory.textStyle) {
                    "Neon" -> Modifier
                        .background(Color(0xFFE1306C).copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                    "Typewriter" -> Modifier
                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                    "Classic" -> Modifier
                        .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                    else -> Modifier.padding(16.dp)
                }

                val fontFamily = when (currentStory.textStyle) {
                    "Serif" -> androidx.compose.ui.text.font.FontFamily.Serif
                    "Typewriter" -> androidx.compose.ui.text.font.FontFamily.Monospace
                    else -> androidx.compose.ui.text.font.FontFamily.Default
                }

                val textShadow = if (currentStory.textStyle != "Typewriter" && currentStory.textStyle != "Neon") {
                    androidx.compose.ui.graphics.Shadow(
                        color = Color.Black.copy(alpha = 0.8f),
                        offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                        blurRadius = 4f
                    )
                } else null

                Text(
                    text = currentStory.caption,
                    color = parsedColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontFamily,
                    style = LocalTextStyle.current.copy(shadow = textShadow),
                    modifier = textStyleModifier,
                    textAlign = TextAlign.Center
                )
            }
        }

        
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                    )
                )
        )

        
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {  },
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showHighlightsPopup = !showHighlightsPopup }
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = if (currentStory.highlightId != null) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = stringResource(R.string.label_highlight),
                    tint = if (currentStory.highlightId != null) Color(0xFFE1306C) else Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (currentStory.highlightId != null) stringResource(R.string.label_highlighted) else stringResource(R.string.label_highlight),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            
            if (onDeleteStory != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showDeleteConfirmation = true }
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.btn_delete),
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.btn_delete),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        
        if (showHighlightsPopup) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable { showHighlightsPopup = false }
            ) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clickable(enabled = false) {  },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.title_save_highlight),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { showHighlightsPopup = false }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.btn_done))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            
                            item {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable {
                                        showNewHighlightDialog = true
                                    }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = stringResource(R.string.new_highlight),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(R.string.new_highlight),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            
                            items(highlights) { highlight ->
                                val isSelected = currentStory.highlightId == highlight.id
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable {
                                        if (onAddToHighlight != null) {
                                            onAddToHighlight(currentStory, highlight.id)
                                        }
                                        showHighlightsPopup = false
                                    }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clip(CircleShape)
                                    ) {
                                        MediaView(path = highlight.coverPath, modifier = Modifier.fillMaxSize())
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = highlight.name,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        
        if (showNewHighlightDialog) {
            AlertDialog(
                onDismissRequest = { showNewHighlightDialog = false },
                title = { Text(stringResource(R.string.title_new_highlight), fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = newHighlightName,
                        onValueChange = { newHighlightName = it },
                        label = { Text(stringResource(R.string.label_highlight_name)) },
                        singleLine = true,
                        placeholder = { Text(stringResource(R.string.placeholder_highlight_name)) }
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newHighlightName.isNotBlank() && onAddHighlight != null) {
                                onAddHighlight(newHighlightName, currentStory.mediaPath)
                                newHighlightName = ""
                                showNewHighlightDialog = false
                                showHighlightsPopup = false
                            }
                        }
                    ) {
                        Text(stringResource(R.string.btn_create_add))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNewHighlightDialog = false }) {
                        Text(stringResource(R.string.btn_cancel))
                    }
                }
            )
        }

        
        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text(stringResource(R.string.title_delete_story), fontWeight = FontWeight.Bold) },
                text = { Text(stringResource(R.string.msg_delete_story)) },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        onClick = {
                            if (onDeleteStory != null) {
                                onDeleteStory(currentStory)
                            }
                            showDeleteConfirmation = false
                        }
                    ) {
                        Text(stringResource(R.string.btn_delete), color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = false }) {
                        Text(stringResource(R.string.btn_cancel))
                    }
                }
            )
        }
    }
}
