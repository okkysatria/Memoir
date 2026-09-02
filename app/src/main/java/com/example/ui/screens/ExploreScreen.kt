package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.data.database.AlbumEntity
import com.example.data.database.PostEntity
import com.example.data.database.StoryEntity
import com.example.ui.components.MediaView
import com.example.ui.viewmodel.MemoirViewModel
import com.example.ui.viewmodel.Screen
import com.example.utils.FileUtils
import java.util.Calendar

data class ExploreMediaItem(
    val path: String,
    val type: String, 
    val title: String,
    val location: String = "",
    val date: Long = 0L,
    val associatedId: Long = 0L,
    val isMultiMedia: Boolean = false,
    val postEntity: PostEntity? = null,
    val storyEntity: StoryEntity? = null,
    val albumEntity: AlbumEntity? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(viewModel: MemoirViewModel) {
    val context = LocalContext.current
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val stories by viewModel.stories.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val activeSortOrder by viewModel.exploreSortOrder.collectAsStateWithLifecycle()
    val activeDateFilter by viewModel.exploreDateFilter.collectAsStateWithLifecycle()
    val activeMonth by viewModel.exploreSelectedMonth.collectAsStateWithLifecycle()
    val activeYear by viewModel.exploreSelectedYear.collectAsStateWithLifecycle()
    var selectedCategoryTab by remember { mutableStateOf("ALL") }
    var showFilterMenu by remember { mutableStateOf(false) }
    var isTimeSelectionExpanded by remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState()
    val scrollToTopTrigger by viewModel.scrollToTopTrigger.collectAsStateWithLifecycle()
    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger > 0) {
            gridState.scrollToItem(0)
        }
    }
    val randomSeed = remember { mutableLongStateOf(System.currentTimeMillis()) }
    val categoryOptions = listOf(
        "ALL" to R.string.cat_all,
        "PHOTO" to R.string.cat_photo,
        "VIDEO" to R.string.cat_video,
        "STORY" to R.string.cat_story,
        "ALBUM" to R.string.cat_album
    )
    val allMedia = remember(posts, stories, albums, searchQuery, selectedCategoryTab, activeSortOrder, activeDateFilter, activeMonth, activeYear, randomSeed.longValue) {
        val list = mutableListOf<ExploreMediaItem>()
        posts.forEach { post ->
            val firstMedia = post.mediaPaths.firstOrNull()
            if (!firstMedia.isNullOrBlank()) {
                val hasMultiple = post.mediaPaths.size > 1
                val isVideo = FileUtils.isVideoPath(firstMedia)
                list.add(ExploreMediaItem(firstMedia, if (isVideo && !hasMultiple) "REEL" else "POST", post.caption, post.location, post.createdAt, post.id, hasMultiple, postEntity = post))
            }
        }
        stories.forEach { story ->
            val storyLabel = context.getString(R.string.cat_story).replace("✨ ", "")
            list.add(ExploreMediaItem(story.mediaPath, "STORY", story.caption ?: storyLabel, "", story.createdAt, story.id, false, storyEntity = story))
        }
        albums.forEach { album ->
            if (album.coverPath.isNotBlank()) {
                list.add(ExploreMediaItem(album.coverPath, "ALBUM", album.name, album.sourceType, album.createdAt, album.id, true, albumEntity = album))
            }
        }
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        val filtered = list.filter { item ->
            val matchChip = when (selectedCategoryTab) {
                "PHOTO" -> item.type == "POST"
                "VIDEO" -> item.type == "REEL"
                "STORY" -> item.type == "STORY"
                "ALBUM" -> item.type == "ALBUM"
                else -> true
            }
            val matchSearch = if (searchQuery.isNotBlank()) {
                val q = searchQuery.lowercase().trim()
                item.title.lowercase().contains(q) || item.location.lowercase().contains(q)
            } else true
            val matchDate = when (activeDateFilter) {
                "THIS_WEEK" -> item.date > now - 7L * 24 * 60 * 60 * 1000
                "THIS_MONTH" -> item.date > now - 30L * 24 * 60 * 60 * 1000
                "THIS_YEAR" -> item.date > now - 365L * 24 * 60 * 60 * 1000
                "CUSTOM" -> {
                    var m = true
                    var y = true
                    if (activeMonth != null || activeYear != null) {
                        calendar.timeInMillis = item.date
                        if (activeMonth != null) m = (calendar.get(Calendar.MONTH) + 1) == activeMonth
                        if (activeYear != null) y = calendar.get(Calendar.YEAR) == activeYear
                    }
                    m && y
                }
                else -> true
            }
            matchChip && matchSearch && matchDate
        }
        when (activeSortOrder) {
            "OLDEST" -> filtered.sortedBy { it.date }
            "RANDOM" -> filtered.shuffled(java.util.Random(randomSeed.longValue))
            else -> filtered.sortedByDescending { it.date }
        }
    }
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 0.5.dp) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text(stringResource(R.string.search_placeholder), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) },
                    trailingIcon = { if (searchQuery.isNotEmpty()) IconButton(onClick = { viewModel.setSearchQuery("") }) { Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)) } },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )
                Box {
                    IconButton(onClick = { showFilterMenu = true }, modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))) {
                        Icon(Icons.Default.FilterList, stringResource(R.string.nav_explore), tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                    }
                    DropdownMenu(expanded = showFilterMenu, onDismissRequest = { showFilterMenu = false }) {
                        Text(stringResource(R.string.filter_sort), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                        DropdownMenuItem(text = { Text(stringResource(R.string.filter_latest)) }, onClick = { viewModel.setExploreSortOrder("LATEST"); showFilterMenu = false }, leadingIcon = { if (activeSortOrder == "LATEST") Icon(Icons.Default.Check, null) })
                        DropdownMenuItem(text = { Text(stringResource(R.string.filter_oldest)) }, onClick = { viewModel.setExploreSortOrder("OLDEST"); showFilterMenu = false }, leadingIcon = { if (activeSortOrder == "OLDEST") Icon(Icons.Default.Check, null) })
                        DropdownMenuItem(text = { Text(stringResource(R.string.filter_random)) }, onClick = { viewModel.setExploreSortOrder("RANDOM"); showFilterMenu = false }, leadingIcon = { if (activeSortOrder == "RANDOM") Icon(Icons.Default.Check, null) })
                        HorizontalDivider()
                        Text(stringResource(R.string.filter_quick_time), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                        DropdownMenuItem(text = { Text(stringResource(R.string.filter_this_week)) }, onClick = { viewModel.setExploreDateFilter("THIS_WEEK"); showFilterMenu = false }, leadingIcon = { if (activeDateFilter == "THIS_WEEK") Icon(Icons.Default.Check, null) })
                        DropdownMenuItem(text = { Text(stringResource(R.string.filter_this_month)) }, onClick = { viewModel.setExploreDateFilter("THIS_MONTH"); showFilterMenu = false }, leadingIcon = { if (activeDateFilter == "THIS_MONTH") Icon(Icons.Default.Check, null) })
                        DropdownMenuItem(text = { Text(stringResource(R.string.filter_this_year)) }, onClick = { viewModel.setExploreDateFilter("THIS_YEAR"); showFilterMenu = false }, leadingIcon = { if (activeDateFilter == "THIS_YEAR") Icon(Icons.Default.Check, null) })
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.filter_custom_time)) }, 
                            onClick = { isTimeSelectionExpanded = !isTimeSelectionExpanded }, 
                            trailingIcon = { Icon(if (isTimeSelectionExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null) },
                            leadingIcon = { if (activeDateFilter == "CUSTOM") Icon(Icons.Default.DateRange, null, tint = MaterialTheme.colorScheme.primary) }
                        )
                        if (isTimeSelectionExpanded) {
                            Text(stringResource(R.string.filter_month), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp))
                            val currentLocale = LocalContext.current.resources.configuration.locales[0]
                            val monthFormat = java.text.SimpleDateFormat("MMM", currentLocale)
                            Row(modifier = Modifier.width(240.dp).horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                                for (idx in 0 until 12) {
                                    val m = idx + 1
                                    val isSel = activeMonth == m
                                    val calendarMonth = java.util.Calendar.getInstance().apply { set(java.util.Calendar.MONTH, idx) }
                                    val monthName = monthFormat.format(calendarMonth.time)
                                    FilterChip(
                                        selected = isSel, 
                                        onClick = { 
                                            viewModel.setExploreSelectedMonth(if (isSel) null else m)
                                            viewModel.setExploreDateFilter("CUSTOM")
                                        }, 
                                        label = { Text(monthName, fontSize = 10.sp) }, 
                                        modifier = Modifier.padding(horizontal = 2.dp)
                                    )
                                }
                            }
                            Text(stringResource(R.string.filter_year), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp))
                            val years = (2023..Calendar.getInstance().get(Calendar.YEAR)).toList().reversed()
                            Row(modifier = Modifier.width(240.dp).horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                                years.forEach { year ->
                                    val isSel = activeYear == year
                                    FilterChip(
                                        selected = isSel, 
                                        onClick = { 
                                            viewModel.setExploreSelectedYear(if (isSel) null else year)
                                            viewModel.setExploreDateFilter("CUSTOM")
                                        }, 
                                        label = { Text(year.toString(), fontSize = 10.sp) }, 
                                        modifier = Modifier.padding(horizontal = 2.dp)
                                    )
                                }
                            }
                        }
                        HorizontalDivider()
                        DropdownMenuItem(text = { Text(stringResource(R.string.filter_reset_time)) }, onClick = { viewModel.setExploreDateFilter("ALL_TIME"); isTimeSelectionExpanded = false; showFilterMenu = false }, leadingIcon = { Icon(Icons.Default.Refresh, null) })
                    }
                }
            }
        }
        LazyRow(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), contentPadding = PaddingValues(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categoryOptions) { (key, resId) ->
                val isSelected = selectedCategoryTab == key
                Surface(shape = RoundedCornerShape(12.dp), color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.clickable { selectedCategoryTab = key }) {
                    Text(text = stringResource(resId), fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp))
                }
            }
        }
        if (allMedia.isEmpty()) {
            Column(modifier = Modifier.weight(1f).fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Default.SearchOff, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(16.dp))
                Text(if (searchQuery.isNotBlank()) stringResource(R.string.no_search_results) else stringResource(R.string.no_media_yet), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (searchQuery.isNotBlank() || selectedCategoryTab != "ALL") {
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { viewModel.setSearchQuery(""); selectedCategoryTab = "ALL"; viewModel.setExploreDateFilter("ALL_TIME") }) { Text(stringResource(R.string.btn_reset_filter)) }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3), 
                modifier = Modifier.weight(1f).fillMaxWidth(), 
                state = gridState,
                verticalArrangement = Arrangement.spacedBy(2.dp), 
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(allMedia) { item ->
                    Box(modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(1.dp))
                        .clickable {
                        when (item.type) {
                            "ALBUM" -> viewModel.navigateTo(Screen.AlbumDetail(item.associatedId))
                            "STORY" -> {
                                val sList = stories.ifEmpty { item.storyEntity?.let { listOf(it) } ?: emptyList() }
                                val idx = sList.indexOfFirst { it.id == item.associatedId }.coerceAtLeast(0)
                                if (sList.isNotEmpty()) viewModel.openStoryViewer(sList, idx)
                            }
                            else -> {
                                val pList = posts
                                val idx = pList.indexOfFirst { it.id == item.associatedId }.coerceAtLeast(0)
                                if (pList.isNotEmpty()) viewModel.navigateTo(Screen.ExploreFeed(pList.map { it.id }, idx))
                            }
                        }
                    }) {
                        MediaView(
                            path = item.path, 
                            autoPlay = false,
                            allowManualPlay = false,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (item.type == "REEL") {
                            Box(modifier = Modifier.align(Alignment.TopEnd).padding(5.dp).background(Color.Black.copy(alpha = 0.6f), CircleShape).padding(4.dp)) { Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(13.dp)) }
                        } else if (item.type == "STORY") {
                            Surface(modifier = Modifier.align(Alignment.TopStart).padding(5.dp), shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)) { 
                                val storyLabel = stringResource(R.string.cat_story).replace("✨ ", "")
                                Text(storyLabel, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) 
                            }
                        } else if (item.type == "ALBUM") {
                            Surface(modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)))).padding(6.dp), color = Color.Transparent) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) { Icon(Icons.Default.Folder, null, tint = Color.White, modifier = Modifier.size(12.dp)); Text(item.title, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            }
                        } else if (item.isMultiMedia) {
                            Box(modifier = Modifier.align(Alignment.TopEnd).padding(5.dp).background(Color.Black.copy(alpha = 0.6f), CircleShape).padding(4.dp)) { Icon(Icons.Default.Collections, null, tint = Color.White, modifier = Modifier.size(13.dp)) }
                        }
                    }
                }
            }
        }
    }
}
