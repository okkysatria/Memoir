package com.example.ui.components

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ModeComment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.database.PostEntity
import com.example.data.database.StoryEntity
import com.example.ui.viewmodel.MemoirViewModel
import com.example.utils.FileUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RandomStoryCard(
    stories: List<StoryEntity>,
    onViewStory: (StoryEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    if (stories.isEmpty()) return

    val randomStory = remember(stories) {
        stories.randomOrNull()
    } ?: return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("random_story_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onViewStory(randomStory) }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                MediaView(path = randomStory.mediaPath, modifier = Modifier.fillMaxSize())
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                )
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "✨ " + stringResource(R.string.kilas_balik),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = randomStory.caption ?: stringResource(R.string.msg_story_random_hint),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun FeedPostCard(
    post: PostEntity,
    viewModel: MemoirViewModel,
    onCommentsClick: () -> Unit,
    onAlbumClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var scaleHeart by remember { mutableStateOf(1f) }
    var showBigHeart by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val profileImagePath by viewModel.profileImagePath.collectAsStateWithLifecycle()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(0.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .zIndex(200f),
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
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!profileImagePath.isNullOrEmpty()) {
                            MediaView(path = profileImagePath!!, modifier = Modifier.fillMaxSize())
                        } else {
                            Text(stringResource(R.string.label_me), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = stringResource(R.string.my_memoir), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            if (post.isPinned) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = stringResource(R.string.desc_pinned),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (post.location.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(end = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = post.location,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Box {
                        IconButton(onClick = { expandedMenu = true }) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = stringResource(R.string.desc_menu))
                        }
                        DropdownMenu(
                            expanded = expandedMenu,
                            onDismissRequest = { expandedMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (post.isPinned) stringResource(R.string.item_unpin) else stringResource(R.string.item_pin)) },
                                onClick = {
                                    viewModel.togglePin(post)
                                    expandedMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.PushPin, null) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.item_edit_memory)) },
                                onClick = {
                                    expandedMenu = false
                                    viewModel.navigateTo(com.example.ui.viewmodel.Screen.EditPost(post))
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, null) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.item_delete_memory)) },
                                onClick = {
                                    showDeleteConfirmation = true
                                    expandedMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                            )
                        }
                    }
                }
            }

            val baseRatio = remember(post.aspectRatio) {
                when {
                    post.aspectRatio.startsWith("Asli") -> {
                        val parts = post.aspectRatio.split(":")
                        if (parts.size >= 2) parts[1].toFloatOrNull() ?: 1f else 1f
                    }
                    post.aspectRatio == "4:5" -> 4f / 5f
                    post.aspectRatio == "5:4" -> 5f / 4f
                    post.aspectRatio == "16:9" -> 16f / 9f
                    post.aspectRatio == "9:16" -> 9f / 16f
                    else -> 1f
                }
            }

            data class CardTransform(
                val scale: Float = 1f,
                val rotation: Float = 0f,
                val offsetX: Float = 0f,
                val offsetY: Float = 0f
            )

            val parsedTransforms = remember(post.mediaTransformsJson) {
                if (post.mediaTransformsJson.isBlank()) emptyList()
                else try {
                    val list = mutableListOf<CardTransform>()
                    val arr = org.json.JSONArray(post.mediaTransformsJson)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val s = obj.optDouble("scale", 1.0).toFloat()
                        val r = obj.optDouble("rotation", 0.0).toFloat()
                        val ox = obj.optDouble("offsetX", 0.0).toFloat()
                        val oy = obj.optDouble("offsetY", 0.0).toFloat()
                        list.add(CardTransform(s, r, ox, oy))
                    }
                    list
                } catch (e: Exception) {
                    emptyList()
                }
            }

            val isCarousel = post.mediaPaths.size > 1
            val pagerState = if (isCarousel) rememberPagerState(pageCount = { post.mediaPaths.size }) else null

            val activePage = pagerState?.currentPage ?: 0
            val activePath = post.mediaPaths.getOrNull(activePage) ?: ""

            val activePhotoRatio = remember(activePath) {
                if (activePath.isNotBlank()) FileUtils.getImageAspectRatio(context, activePath)
                else baseRatio
            }

            val targetRatio = if (post.aspectRatio.startsWith("Asli") || post.aspectRatio == "Asli" || post.aspectRatio.startsWith("Original") || post.aspectRatio == "Original") activePhotoRatio else baseRatio
            val animatedRatio by animateFloatAsState(
                targetValue = targetRatio.coerceIn(0.4f, 2.5f),
                animationSpec = tween(durationMillis = 250),
                label = "cardRatioAnim"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(animatedRatio)
                    .clipToBounds()
            ) {
                if (isCarousel && pagerState != null) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        val transform = parsedTransforms.getOrNull(page) ?: CardTransform()
                        val itemPath = post.mediaPaths[page]
                        val itemRatio = remember(itemPath) { FileUtils.getImageAspectRatio(context, itemPath) }
                        val isRatioDifferent = Math.abs(itemRatio - animatedRatio) > 0.05f
                        val isDefaultTransform = transform.scale == 1.0f && transform.rotation == 0.0f && transform.offsetX == 0.0f && transform.offsetY == 0.0f
                        val contentScaleToUse = if (isRatioDifferent && isDefaultTransform) ContentScale.Fit else ContentScale.Crop

                        ZoomableBox(
                            modifier = Modifier.fillMaxSize(),
                            onDoubleClick = {
                                if (!post.isLiked) {
                                    viewModel.toggleLike(post)
                                }
                                coroutineScope.launch {
                                    showBigHeart = true
                                    scaleHeart = 1.6f
                                    delay(300)
                                    scaleHeart = 1.0f
                                    delay(100)
                                    showBigHeart = false
                                }
                            }
                        ) {
                            MediaView(
                                path = itemPath,
                                contentScale = contentScaleToUse,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = transform.scale
                                        scaleY = transform.scale
                                        rotationZ = transform.rotation
                                        translationX = transform.offsetX
                                        translationY = transform.offsetY
                                    }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                            .background(Color.Black.copy(alpha = 0.4f), shape = RoundedCornerShape(100.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0 until post.mediaPaths.size) {
                            val active = i == pagerState.currentPage
                            Box(
                                modifier = Modifier
                                    .size(if (active) 6.dp else 4.dp)
                                    .clip(CircleShape)
                                    .background(if (active) Color.White else Color.White.copy(alpha = 0.5f))
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${pagerState.currentPage + 1}/${post.mediaPaths.size}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    val transform = parsedTransforms.firstOrNull() ?: CardTransform()
                    ZoomableBox(
                        modifier = Modifier.fillMaxSize(),
                        onDoubleClick = {
                            if (!post.isLiked) {
                                viewModel.toggleLike(post)
                            }
                            coroutineScope.launch {
                                showBigHeart = true
                                scaleHeart = 1.6f
                                delay(300)
                                scaleHeart = 1.0f
                                delay(100)
                                showBigHeart = false
                            }
                        }
                    ) {
                        MediaView(
                            path = post.mediaPaths.firstOrNull() ?: "gradient_slate",
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = transform.scale
                                    scaleY = transform.scale
                                    rotationZ = transform.rotation
                                    translationX = transform.offsetX
                                    translationY = transform.offsetY
                                }
                        )
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = showBigHeart,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(100.dp)
                            .scale(scaleHeart)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .zIndex(200f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            viewModel.toggleLike(post)
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = stringResource(R.string.desc_like),
                            tint = if (post.isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = onCommentsClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ModeComment,
                            contentDescription = stringResource(R.string.desc_notes)
                        )
                    }

                    IconButton(
                        onClick = {
                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                val shareText = buildString {
                                    if (post.caption.isNotBlank()) append(post.caption)
                                    if (post.location.isNotBlank()) append("\n📍 ${post.location}")
                                    append(context.getString(R.string.msg_share_footer))
                                }
                                putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(android.content.Intent.createChooser(shareIntent, context.getString(R.string.title_share)))
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(R.string.title_share)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            viewModel.toggleSave(post)
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (post.isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = stringResource(R.string.saved),
                            tint = if (post.isSaved) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (post.linkedAlbumId != null) {
                SuggestionChip(
                    onClick = { onAlbumClick(post.linkedAlbumId) },
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(stringResource(R.string.btn_view_album), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                        labelColor = MaterialTheme.colorScheme.secondary
                    ),
                    border = null,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp)
            ) {
                Text(
                    text = if (post.isLiked) stringResource(R.string.label_liked) else stringResource(R.string.label_add_favorite),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    Text(
                        text = stringResource(R.string.label_me) + " ",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = post.caption,
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.label_view_journal),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.clickable { onCommentsClick() }
                )
                Spacer(modifier = Modifier.height(6.dp))
                val currentLocale = context.resources.configuration.locales[0]
                Text(
                    text = SimpleDateFormat("dd MMMM yyyy", currentLocale).format(Date(post.createdAt)),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(R.string.title_delete_memory)) },
            text = { Text(stringResource(R.string.msg_delete_memory)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePost(post)
                        showDeleteConfirmation = false
                        val msg = context.getString(R.string.msg_memory_deleted)
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.btn_delete))
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
