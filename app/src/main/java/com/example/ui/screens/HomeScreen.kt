package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import com.example.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.CommentsBottomSheet
import com.example.ui.components.FeedPostCard
import com.example.ui.components.MediaView
import com.example.ui.components.StoriesTray
import com.example.ui.viewmodel.MemoirViewModel
import com.example.ui.viewmodel.Screen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MemoirViewModel, onAddStoryClick: () -> Unit) {
    val context = LocalContext.current
    val posts by viewModel.homePosts.collectAsStateWithLifecycle()
    val stories by viewModel.homeStories.collectAsStateWithLifecycle()
    var selectedPostForComments by remember { mutableStateOf<com.example.data.database.PostEntity?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val orderedPosts = posts
    val listState = rememberLazyListState()
    val scrollToTopTrigger by viewModel.scrollToTopTrigger.collectAsStateWithLifecycle()
    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger > 0) {
            listState.scrollToItem(0)
        }
    }
    val todayMemories = remember(posts, stories) {
        val list = mutableListOf<MemoryItem>()
        val todayCal = Calendar.getInstance()
        val todayYear = todayCal.get(Calendar.YEAR)
        val todayMonth = todayCal.get(Calendar.MONTH)
        val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)
        val currentLocale = context.resources.configuration.locales[0]
        val sdf = SimpleDateFormat("d MMM yyyy", currentLocale)
        fun badge(createdAt: Long): String? {
            val cal = Calendar.getInstance().apply { timeInMillis = createdAt }
            val monthDiff = (todayYear * 12 + todayMonth) - (cal.get(Calendar.YEAR) * 12 + cal.get(Calendar.MONTH))
            val yearDiff = todayYear - cal.get(Calendar.YEAR)
            return when {
                monthDiff == 1 && cal.get(Calendar.DAY_OF_MONTH) == todayDay -> context.getString(R.string.one_month_ago)
                yearDiff >= 1 && cal.get(Calendar.MONTH) == todayMonth && cal.get(Calendar.DAY_OF_MONTH) == todayDay -> context.getString(R.string.years_ago, yearDiff)
                else -> null
            }
        }
        val storyKenanganLabel = context.getString(R.string.cat_story).replace("✨ ", "")
        stories.forEach { story ->
            val b = badge(story.createdAt)
            if (b != null) list.add(MemoryItem(story.mediaPath, story.caption ?: storyKenanganLabel, b, sdf.format(Date(story.createdAt)), storyEntity = story))
        }
        posts.forEach { post ->
            val b = badge(post.createdAt)
            if (b != null) list.add(MemoryItem(post.mediaPaths.firstOrNull() ?: "gradient_sunset", post.caption, b, sdf.format(Date(post.createdAt)), postEntity = post))
        }
        list
    }
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                viewModel.reshuffle()
                delay(800) 
                isRefreshing = false
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 0.5.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.app_name),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = (-0.5).sp
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { viewModel.navigateTo(Screen.Settings) },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = stringResource(R.string.settings_title),
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState
            ) {
                item {
                    StoriesTray(
                        stories = stories, 
                        allStories = stories,
                        onStoryClick = { index ->
                            viewModel.openStoryViewer(stories, index)
                        },
                        onAddStoryClick = onAddStoryClick,
                        onKilasBalikClick = { story ->
                            val index = stories.indexOf(story)
                            if (index >= 0) {
                                viewModel.openStoryViewer(stories, index)
                            }
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                }
                if (todayMemories.isNotEmpty()) {
                    item {
                        OnThisDayHomeBanner(
                            memories = todayMemories,
                            onMemoryClick = { post ->
                                viewModel.navigateTo(Screen.ExploreFeed(listOf(post.id), 0))
                            },
                            onStoryClick = { story ->
                                viewModel.openStoryViewer(listOf(story), 0)
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                    }
                }
                if (orderedPosts.isEmpty()) {
                    item {
                        EmptyHomeState(onAddClick = { viewModel.setAddSheetOpen(true) })
                    }
                } else {
                    items(orderedPosts, key = { it.id }) { post ->
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
fun OnThisDayHomeBanner(
    memories: List<MemoryItem>,
    onMemoryClick: (com.example.data.database.PostEntity) -> Unit,
    onStoryClick: (com.example.data.database.StoryEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "🗓️ " + stringResource(R.string.on_this_day),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.memories_found, memories.size),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(memories) { memory ->
                MemoryCard(
                    memory = memory,
                    onClick = {
                        if (memory.postEntity != null) {
                            onMemoryClick(memory.postEntity)
                        } else if (memory.storyEntity != null) {
                            onStoryClick(memory.storyEntity)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun MemoryCard(memory: MemoryItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(180.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            MediaView(
                path = memory.mediaPath,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                        )
                    )
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
            ) {
                Text(
                    text = memory.timeBadge,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = memory.caption,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = memory.dateString,
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun EmptyHomeState(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.empty_home_title_alt),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.empty_home_desc_alt),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onAddClick,
            modifier = Modifier.testTag("create_first_post_button")
        ) {
            Text(stringResource(R.string.btn_add_memory))
        }
    }
}
