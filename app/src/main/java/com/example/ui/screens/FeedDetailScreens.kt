package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.ui.components.CommentsBottomSheet
import com.example.ui.components.FeedPostCard
import com.example.ui.viewmodel.MemoirViewModel
import com.example.ui.viewmodel.Screen

@Composable
fun ExploreFeedScreen(
    postIds: List<Long>,
    initialIndex: Int,
    viewModel: MemoirViewModel
) {
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val displayPosts = remember(posts, postIds) {
        val postsMap = posts.associateBy { it.id }
        postIds.mapNotNull { postsMap[it] }
    }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialIndex.coerceIn(0, (displayPosts.size - 1).coerceAtLeast(0))
    )
    var selectedPostForComments by remember { mutableStateOf<com.example.data.database.PostEntity?>(null) }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateBack() },
                modifier = Modifier.testTag("explore_feed_back_button")
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_to_explore))
            }
            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                Text(
                    text = stringResource(R.string.label_exploration),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = stringResource(R.string.posts),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
        if (displayPosts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.msg_no_post),
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(displayPosts, key = { it.id }) { post ->
                    FeedPostCard(
                        post = post,
                        viewModel = viewModel,
                        onCommentsClick = { selectedPostForComments = post },
                        onAlbumClick = { albumId ->
                            viewModel.navigateTo(Screen.AlbumDetail(albumId))
                        }
                    )
                }
            }
        }
    }
    selectedPostForComments?.let { post ->
        CommentsBottomSheet(
            post = post,
            viewModel = viewModel,
            onDismiss = { selectedPostForComments = null }
        )
    }
}

@Composable
fun ProfileFeedScreen(
    initialIndex: Int,
    isSavedTab: Boolean,
    viewModel: MemoirViewModel
) {
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val username by viewModel.username.collectAsStateWithLifecycle()
    val displayPosts = remember(posts, isSavedTab) {
        if (isSavedTab) posts.filter { it.isLiked } else posts
    }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialIndex.coerceIn(0, (displayPosts.size - 1).coerceAtLeast(0))
    )
    var selectedPostForComments by remember { mutableStateOf<com.example.data.database.PostEntity?>(null) }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(Screen.Profile) },
                modifier = Modifier.testTag("profile_feed_back_button")
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_to_profile))
            }
            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                Text(
                    text = username.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = if (isSavedTab) stringResource(R.string.saved) else stringResource(R.string.posts),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
        if (displayPosts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (isSavedTab) stringResource(R.string.msg_no_saved_post) else stringResource(R.string.msg_no_post),
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(displayPosts, key = { it.id }) { post ->
                    FeedPostCard(
                        post = post,
                        viewModel = viewModel,
                        onCommentsClick = { selectedPostForComments = post },
                        onAlbumClick = { albumId ->
                            viewModel.navigateTo(Screen.AlbumDetail(albumId))
                        }
                    )
                }
            }
        }
    }
    selectedPostForComments?.let { post ->
        CommentsBottomSheet(
            post = post,
            viewModel = viewModel,
            onDismiss = { selectedPostForComments = null }
        )
    }
}
