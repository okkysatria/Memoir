package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.database.PostEntity
import com.example.data.database.StoryEntity
import com.example.data.database.StoryHighlightEntity
import com.example.ui.components.MediaView
import com.example.ui.screens.profile.AddHighlightDialog
import com.example.ui.screens.profile.EditHighlightDialog
import com.example.ui.screens.profile.EditProfileDialog
import com.example.ui.viewmodel.MemoirViewModel
import com.example.ui.viewmodel.Screen
import com.example.utils.FileUtils
import java.text.SimpleDateFormat
import java.util.*

data class MemoryItem(
    val mediaPath: String,
    val caption: String,
    val timeBadge: String,
    val dateString: String = "",
    val category: String = "Kenangan",
    val isVideo: Boolean = false,
    val isMultiMedia: Boolean = false,
    val storyEntity: StoryEntity? = null,
    val postEntity: PostEntity? = null
)

@Composable
fun ProfileScreen(viewModel: MemoirViewModel, onAddStoryClick: () -> Unit) {
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val stories by viewModel.stories.collectAsStateWithLifecycle()
    val highlights by viewModel.highlights.collectAsStateWithLifecycle()
    val profileName by viewModel.profileName.collectAsStateWithLifecycle()
    val username by viewModel.username.collectAsStateWithLifecycle()
    val profileBio by viewModel.profileBio.collectAsStateWithLifecycle()
    val profileImagePath by viewModel.profileImagePath.collectAsStateWithLifecycle()

    var activeProfileSubTab by remember { mutableStateOf("grid") }
    var selectedMemoryFilter by remember { mutableStateOf("ALL") }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showAddHighlightDialog by remember { mutableStateOf(false) }
    var editingHighlight by remember { mutableStateOf<StoryHighlightEntity?>(null) }

    val gridState = rememberLazyGridState()
    val albumGridState = rememberLazyGridState()
    val storyGridState = rememberLazyGridState()
    val scrollToTopTrigger by viewModel.scrollToTopTrigger.collectAsStateWithLifecycle()

    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger > 0) {
            when (activeProfileSubTab) {
                "grid" -> gridState.scrollToItem(0)
                "album" -> albumGridState.scrollToItem(0)
                "stories" -> storyGridState.scrollToItem(0)
            }
        }
    }

    val context = LocalContext.current
    val todayCal = remember { Calendar.getInstance() }
    val todayYear = todayCal.get(Calendar.YEAR)
    val todayMonth = todayCal.get(Calendar.MONTH)
    val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)

    val onThisDayMemories = remember(stories, posts, todayYear, todayMonth, todayDay, context) {
        val list = mutableListOf<MemoryItem>()
        val now = System.currentTimeMillis()

        fun calculateMemoryCategory(createdAt: Long): Pair<String, String>? {
            if (createdAt > now) return null
            val diffMillis = now - createdAt
            val daysDiff = diffMillis / (1000L * 60 * 60 * 24)

            val itemCal = Calendar.getInstance().apply { timeInMillis = createdAt }
            val itemYear = itemCal.get(Calendar.YEAR)
            val itemMonth = itemCal.get(Calendar.MONTH)

            val monthDiff = (todayYear * 12 + todayMonth) - (itemYear * 12 + itemMonth)
            val yearDiff = todayYear - itemYear

            if (monthDiff == 1 || daysDiff in 25..40) {
                return Pair(context.getString(R.string.one_month_ago), context.getString(R.string.one_month))
            }
            if (monthDiff == 6 || daysDiff in 165..200) {
                return Pair(context.getString(R.string.six_months_ago), context.getString(R.string.six_months))
            }
            if (yearDiff == 1 || daysDiff in 345..395) {
                return Pair(context.getString(R.string.years_ago, 1), context.getString(R.string.years, 1))
            }
            if (yearDiff >= 2 || daysDiff >= 700) {
                val calculatedYears = if (yearDiff >= 2) yearDiff else (daysDiff / 365).toInt().coerceAtLeast(2)
                return Pair(context.getString(R.string.years_ago, calculatedYears), context.getString(R.string.years, calculatedYears))
            }
            return null
        }

        stories.forEach { story ->
            val result = calculateMemoryCategory(story.createdAt)
            if (result != null) {
                val currentLocale = context.resources.configuration.locales[0]
                val dateStr = SimpleDateFormat("d MMM yyyy", currentLocale).format(Date(story.createdAt))
                val isVid = FileUtils.isVideoPath(story.mediaPath)
                list.add(
                    MemoryItem(
                        mediaPath = story.mediaPath,
                        caption = story.caption ?: context.getString(R.string.kilas_balik),
                        timeBadge = result.first,
                        category = result.second,
                        dateString = dateStr,
                        isVideo = isVid,
                        isMultiMedia = false,
                        storyEntity = story
                    )
                )
            }
        }

        posts.forEach { post ->
            val result = calculateMemoryCategory(post.createdAt)
            if (result != null) {
                val currentLocale = context.resources.configuration.locales[0]
                val dateStr = SimpleDateFormat("d MMM yyyy", currentLocale).format(Date(post.createdAt))
                val firstMedia = post.mediaPaths.firstOrNull() ?: ""
                val isVid = FileUtils.isVideoPath(firstMedia)
                list.add(
                    MemoryItem(
                        mediaPath = firstMedia,
                        caption = post.caption,
                        timeBadge = result.first,
                        category = result.second,
                        dateString = dateStr,
                        isVideo = isVid,
                        isMultiMedia = post.mediaPaths.size > 1,
                        postEntity = post
                    )
                )
            }
        }

        list.sortByDescending { it.storyEntity?.createdAt ?: it.postEntity?.createdAt ?: 0L }
        list
    }

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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = username,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.Settings) },
                        modifier = Modifier.size(38.dp).testTag("profile_settings_button")
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .border(
                        BorderStroke(
                            2.dp,
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        ),
                        CircleShape
                    )
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable {
                        val activeUserStories = stories.filter { it.expiresAt > System.currentTimeMillis() }
                        if (activeUserStories.isNotEmpty()) {
                            viewModel.openStoryViewer(activeUserStories, 0)
                        } else {
                            onAddStoryClick()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (!profileImagePath.isNullOrEmpty()) {
                    MediaView(
                        path = profileImagePath!!,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                ProfileStat(
                    count = posts.size,
                    label = stringResource(R.string.posts),
                    onClick = { activeProfileSubTab = "grid" }
                )
                ProfileStat(
                    count = albums.size,
                    label = stringResource(R.string.nav_albums),
                    onClick = { activeProfileSubTab = "album" }
                )
                ProfileStat(
                    count = stories.size,
                    label = stringResource(R.string.cat_story).replace("✨ ", ""),
                    onClick = {
                        if (stories.isNotEmpty()) {
                            viewModel.openStoryViewer(stories, 0)
                        }
                    }
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = profileName,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (profileBio.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = profileBio,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { showAddHighlightDialog = true }
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .border(1.dp, Color.LightGray, CircleShape)
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.new_highlight),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = stringResource(R.string.new_highlight), fontSize = 10.sp, fontWeight = FontWeight.Medium)
                }
            }

            items(highlights) { highlight ->
                var highlightMenuExpanded by remember { mutableStateOf(false) }

                Box {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = {
                                        val highlightStories = stories.filter { it.highlightId == highlight.id }
                                        if (highlightStories.isNotEmpty()) {
                                            viewModel.openStoryViewer(highlightStories, 0)
                                        } else {
                                            val msg = context.getString(R.string.msg_no_story_highlight_edit)
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onLongPress = {
                                        highlightMenuExpanded = true
                                    }
                                )
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape)
                                .padding(3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                MediaView(path = highlight.coverPath, modifier = Modifier.fillMaxSize())
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = highlight.name, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                    }

                    DropdownMenu(
                        expanded = highlightMenuExpanded,
                        onDismissRequest = { highlightMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.btn_view_highlight)) },
                            onClick = {
                                highlightMenuExpanded = false
                                val highlightStories = stories.filter { it.highlightId == highlight.id }
                                if (highlightStories.isNotEmpty()) {
                                    viewModel.openStoryViewer(highlightStories, 0)
                                } else {
                                    val msg = context.getString(R.string.msg_no_story_highlight_edit)
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            leadingIcon = { Icon(Icons.Default.PlayCircle, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.btn_edit_highlight)) },
                            onClick = {
                                highlightMenuExpanded = false
                                editingHighlight = highlight
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.btn_delete_highlight)) },
                            onClick = {
                                highlightMenuExpanded = false
                                viewModel.deleteHighlight(highlight)
                                val msg = context.getString(R.string.msg_highlight_deleted)
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            TabButton(
                selected = activeProfileSubTab == "grid",
                icon = Icons.Default.GridView,
                onClick = { activeProfileSubTab = "grid" }
            )
            TabButton(
                selected = activeProfileSubTab == "album",
                icon = Icons.Default.PhotoLibrary,
                onClick = { activeProfileSubTab = "album" }
            )
            TabButton(
                selected = activeProfileSubTab == "stories",
                icon = Icons.Default.History,
                onClick = { activeProfileSubTab = "stories" }
            )
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AnimatedContent(
                targetState = activeProfileSubTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "ProfileTabTransition"
            ) { currentTab ->
                when (currentTab) {
                    "grid" -> {
                        if (posts.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(stringResource(R.string.msg_no_post), color = Color.Gray, fontSize = 13.sp)
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                modifier = Modifier.fillMaxSize(),
                                state = gridState,
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                items(posts.size) { index ->
                                    val post = posts[index]
                                    Box(
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(1.dp))
                                            .clickable {
                                                viewModel.navigateTo(
                                                    Screen.ProfileFeed(
                                                        initialIndex = index,
                                                        isSavedTab = false
                                                    )
                                                )
                                            }
                                    ) {
                                        MediaView(
                                            path = post.mediaPaths.firstOrNull() ?: "gradient_slate",
                                            autoPlay = false,
                                            allowManualPlay = false,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        if (post.isPinned) {
                                            Icon(
                                                imageVector = Icons.Default.PushPin,
                                                contentDescription = stringResource(R.string.desc_pinned),
                                                tint = Color.White,
                                                modifier = Modifier
                                                    .align(Alignment.TopStart)
                                                    .padding(6.dp)
                                                    .size(16.dp)
                                                    .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                                                    .padding(2.dp)
                                            )
                                        }

                                        val firstMedia = post.mediaPaths.firstOrNull() ?: ""
                                        if (FileUtils.isVideoPath(firstMedia)) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = stringResource(R.string.label_video),
                                                tint = Color.White,
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(6.dp)
                                                    .size(16.dp)
                                                    .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                                                    .padding(2.dp)
                                            )
                                        } else if (post.mediaPaths.size > 1) {
                                            Icon(
                                                imageVector = Icons.Default.Collections,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(6.dp)
                                                    .size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "album" -> {
                        if (albums.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(stringResource(R.string.msg_no_album), color = Color.Gray, fontSize = 13.sp)
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                state = albumGridState,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(albums) { album ->
                                    AlbumGridCard(
                                        album = album,
                                        viewModel = viewModel,
                                        onClick = { viewModel.navigateTo(Screen.AlbumDetail(album.id)) }
                                    )
                                }
                            }
                        }
                    }
                    "stories" -> {
                        val availableCategories = remember(onThisDayMemories) {
                            val baseList = mutableListOf("ALL")
                            val distinctCats = onThisDayMemories.map { it.category }.distinct()
                            baseList.addAll(distinctCats)
                            baseList
                        }

                        val displayedMemories = remember(onThisDayMemories, selectedMemoryFilter) {
                            if (selectedMemoryFilter == "ALL") onThisDayMemories
                            else onThisDayMemories.filter { it.category == selectedMemoryFilter }
                        }

                        Column(modifier = Modifier.fillMaxSize()) {
                            if (onThisDayMemories.isNotEmpty()) {
                                LazyRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(availableCategories) { cat ->
                                        val isSelected = selectedMemoryFilter == cat
                                        val count = if (cat == "ALL") onThisDayMemories.size else onThisDayMemories.count { it.category == cat }
                                        val displayCat = if (cat == "ALL") stringResource(R.string.cat_all) else cat
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.clickable { selectedMemoryFilter = cat }
                                        ) {
                                            Text(
                                                text = "$displayCat ($count)",
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (displayedMemories.isEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = if (onThisDayMemories.isEmpty()) stringResource(R.string.msg_no_memories) else stringResource(R.string.msg_no_memories_cat),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (onThisDayMemories.isEmpty())
                                            stringResource(R.string.msg_memories_desc)
                                        else
                                            stringResource(R.string.msg_memories_filter_desc),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(3),
                                    modifier = Modifier.fillMaxSize(),
                                    state = storyGridState,
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    items(displayedMemories) { memory ->
                                        Box(
                                            modifier = Modifier
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(1.dp))
                                                .clickable {
                                                    if (memory.storyEntity != null) {
                                                        val storyList = stories.ifEmpty { listOf(memory.storyEntity) }
                                                        val sIndex = storyList.indexOfFirst { it.id == memory.storyEntity.id }.coerceAtLeast(0)
                                                        viewModel.openStoryViewer(storyList, sIndex)
                                                    } else if (memory.postEntity != null) {
                                                        val pIndex = posts.indexOfFirst { it.id == memory.postEntity.id }
                                                        if (pIndex >= 0) {
                                                            viewModel.navigateTo(Screen.ProfileFeed(initialIndex = pIndex))
                                                        }
                                                    }
                                                }
                                        ) {
                                            MediaView(
                                                path = memory.mediaPath,
                                                autoPlay = false,
                                                allowManualPlay = false,
                                                modifier = Modifier.fillMaxSize()
                                            )

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        Brush.verticalGradient(
                                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)),
                                                            startY = 80f
                                                        )
                                                    )
                                            )

                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopStart)
                                                    .padding(5.dp)
                                                    .background(
                                                        Brush.linearGradient(
                                                            listOf(Color(0xFF7C3AED), Color(0xFFEC4899))
                                                        ),
                                                        shape = RoundedCornerShape(6.dp)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.5.dp)
                                            ) {
                                                Text(
                                                    text = memory.timeBadge,
                                                    color = Color.White,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            if (memory.isVideo) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(5.dp)
                                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                                        .padding(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.PlayArrow,
                                                        contentDescription = stringResource(R.string.label_video),
                                                        tint = Color.White,
                                                        modifier = Modifier.size(11.dp)
                                                    )
                                                }
                                            } else if (memory.isMultiMedia) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(5.dp)
                                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                                        .padding(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Collections,
                                                        contentDescription = stringResource(R.string.label_multi_photo),
                                                        tint = Color.White,
                                                        modifier = Modifier.size(11.dp)
                                                    )
                                                }
                                            }

                                            Text(
                                                text = memory.dateString,
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                modifier = Modifier
                                                    .align(Alignment.BottomStart)
                                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditProfileDialog) {
        EditProfileDialog(
            currentName = profileName,
            currentUsername = username,
            currentBio = profileBio,
            currentImagePath = profileImagePath,
            onDismiss = { showEditProfileDialog = false },
            onSave = { name, uname, bio, imagePath ->
                viewModel.updateProfile(name, uname, bio, imagePath)
                showEditProfileDialog = false
                val msg = context.getString(R.string.msg_profile_updated)
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showAddHighlightDialog) {
        AddHighlightDialog(
            viewModel = viewModel,
            onDismiss = { showAddHighlightDialog = false },
            onSave = { name, coverPath, storyIds ->
                viewModel.addHighlightWithStories(name, coverPath, storyIds)
                showAddHighlightDialog = false
                val msg = context.getString(R.string.msg_highlight_added, name)
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        )
    }

    editingHighlight?.let { highlight ->
        EditHighlightDialog(
            highlight = highlight,
            viewModel = viewModel,
            onDismiss = { editingHighlight = null },
            onSave = { newName, newCover ->
                viewModel.updateHighlight(highlight, newName, newCover)
                editingHighlight = null
                val msg = context.getString(R.string.msg_highlight_updated)
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            },
            onDelete = {
                viewModel.deleteHighlight(highlight)
                editingHighlight = null
                val msg = context.getString(R.string.msg_highlight_deleted)
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun ProfileStat(count: Int, label: String, onClick: () -> Unit = {}) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = count.toString(),
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TabButton(selected: Boolean, icon: ImageVector, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.height(44.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(22.dp)
        )
    }
}
