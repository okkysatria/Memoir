package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.MediaView
import com.example.ui.viewmodel.MemoirViewModel
import com.example.ui.viewmodel.Screen
import androidx.compose.ui.res.stringResource
import com.example.R

@Composable
fun AlbumDetailScreen(albumId: Long, viewModel: MemoirViewModel) {
    val album by viewModel.getAlbumById(albumId).collectAsStateWithLifecycle(initialValue = null)
    val items by viewModel.getItemsForAlbum(albumId).collectAsStateWithLifecycle(initialValue = emptyList())
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val stories by viewModel.stories.collectAsStateWithLifecycle()
    var showAddMediaDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf("feed") }
    val context = LocalContext.current
    val presetGradients = listOf(
        "gradient_sunset", "gradient_blue", "gradient_orange", "gradient_nature", 
        "gradient_temple", "gradient_purple", "gradient_slate"
    )
    album?.let { currentAlbum ->
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddMediaDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.btn_add_memory))
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 0.5.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = { viewModel.navigateBack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.btn_back),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = currentAlbum.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            textAlign = TextAlign.Center
                        )
                        IconButton(
                            onClick = {
                                viewModel.deleteAlbum(currentAlbum)
                                viewModel.navigateTo(Screen.AlbumList)
                                val msg = context.getString(R.string.msg_album_deleted, currentAlbum.name)
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("delete_album_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.btn_delete_album),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .background(
                            color = when (currentAlbum.sourceType) {
                                "SMB" -> Color(0xFFE8F5E9)
                                "CLOUD" -> Color(0xFFE3F2FD)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = when (currentAlbum.sourceType) {
                                "SMB" -> Color(0xFFC8E6C9)
                                "CLOUD" -> Color(0xFFBBDEFB)
                                else -> Color.LightGray.copy(alpha = 0.3f)
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = when (currentAlbum.sourceType) {
                                "SMB" -> Icons.Default.Dns
                                "CLOUD" -> Icons.Default.Cloud
                                else -> Icons.Default.Folder
                            },
                            contentDescription = null,
                            tint = when (currentAlbum.sourceType) {
                                "SMB" -> Color(0xFF2E7D32)
                                "CLOUD" -> Color(0xFF1565C0)
                                else -> MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.label_conn_source, currentAlbum.sourceType),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = currentAlbum.sourcePath,
                                fontSize = 10.sp,
                                color = Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .background(
                                    if (currentAlbum.sourceType == "LOCAL") Color(0xFFEEEEEE) else Color(0xFFE8F5E9),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (currentAlbum.sourceType == "LOCAL") stringResource(R.string.status_offline).uppercase() else stringResource(R.string.status_connected).uppercase(),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentAlbum.sourceType == "LOCAL") Color.DarkGray else Color(0xFF2E7D32)
                            )
                        }
                    }
                }
                if (items.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.title_album_empty),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.desc_album_empty),
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(items) { mediaItem ->
                            Box(modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(1.dp))
                            ) {
                                MediaView(
                                    path = mediaItem.mediaPath, 
                                    autoPlay = false,
                                    allowManualPlay = false,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (com.example.utils.FileUtils.isVideoPath(mediaItem.mediaPath)) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(6.dp)
                                            .size(18.dp)
                                            .background(Color.Black.copy(alpha = 0.4f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .clickable {
                                            viewModel.deleteAlbumItem(mediaItem)
                                            val msg = context.getString(R.string.msg_item_removed_album)
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.btn_delete_item),
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
    if (showAddMediaDialog) {
        AlertDialog(
            onDismissRequest = { showAddMediaDialog = false },
            title = { Text(stringResource(R.string.title_add_to_album), fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("feed" to stringResource(R.string.tab_posts), "story" to stringResource(R.string.tab_stories), "preset" to stringResource(R.string.tab_gradients)).forEach { (tabKey, tabLabel) ->
                            val isSel = selectedTab == tabKey
                            Button(
                                onClick = { selectedTab = tabKey },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f).height(32.dp)
                            ) {
                                Text(tabLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                        when (selectedTab) {
                            "feed" -> {
                                val allFeedMedias = posts.flatMap { it.mediaPaths }.distinct()
                                if (allFeedMedias.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(stringResource(R.string.msg_no_feed_media), fontSize = 11.sp, color = Color.Gray)
                                    }
                                } else {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(allFeedMedias) { mediaPath ->
                                            Box(
                                                modifier = Modifier
                                                    .size(90.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        viewModel.addItemsToAlbum(
                                                            albumId = albumId,
                                                            mediaPaths = listOf(mediaPath),
                                                            sourceType = album?.sourceType ?: "LOCAL"
                                                        )
                                                        val msg = context.getString(R.string.msg_photo_added_album)
                                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                    }
                                            ) {
                                                MediaView(path = mediaPath, modifier = Modifier.fillMaxSize())
                                                Box(
                                                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.15f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(imageVector = Icons.Default.AddCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            "story" -> {
                                val allStoryMedias = stories.map { it.mediaPath }.distinct()
                                if (allStoryMedias.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(stringResource(R.string.msg_no_story_media), fontSize = 11.sp, color = Color.Gray)
                                    }
                                } else {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(allStoryMedias) { mediaPath ->
                                            Box(
                                                modifier = Modifier
                                                    .size(90.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        viewModel.addItemsToAlbum(
                                                            albumId = albumId,
                                                            mediaPaths = listOf(mediaPath),
                                                            sourceType = album?.sourceType ?: "LOCAL"
                                                        )
                                                        val msg = context.getString(R.string.msg_story_added_album)
                                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                    }
                                            ) {
                                                MediaView(path = mediaPath, modifier = Modifier.fillMaxSize())
                                                Box(
                                                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.15f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(imageVector = Icons.Default.AddCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            "preset" -> {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(presetGradients) { path ->
                                        Box(
                                            modifier = Modifier
                                                .size(90.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                                .clickable {
                                                    viewModel.addItemsToAlbum(
                                                        albumId = albumId,
                                                        mediaPaths = listOf(path),
                                                        sourceType = album?.sourceType ?: "LOCAL"
                                                    )
                                                    val msg = context.getString(R.string.msg_gradient_added_album)
                                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                }
                                        ) {
                                            MediaView(path = path, modifier = Modifier.fillMaxSize())
                                            Box(
                                                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(imageVector = Icons.Default.AddCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Text(
                        text = stringResource(R.string.desc_tap_to_add),
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showAddMediaDialog = false }) {
                    Text(stringResource(R.string.btn_done))
                }
            }
        )
    }
}
