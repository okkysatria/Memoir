package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.data.database.AlbumEntity
import com.example.ui.components.MediaView
import com.example.ui.viewmodel.MemoirViewModel
import com.example.ui.viewmodel.Screen
import java.util.Calendar

@Composable
fun AlbumListScreen(viewModel: MemoirViewModel) {
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val activeSortOrder by viewModel.albumSortOrder.collectAsStateWithLifecycle()
    val activeSourceFilter by viewModel.albumSourceFilter.collectAsStateWithLifecycle()
    val activeMonth by viewModel.albumSelectedMonth.collectAsStateWithLifecycle()
    val activeYear by viewModel.albumSelectedYear.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var showFilterMenu by remember { mutableStateOf(false) }
    var isTimeSelectionExpanded by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newAlbumName by remember { mutableStateOf("") }
    var selectedSource by remember { mutableStateOf("LOCAL") }
    var customSourcePath by remember { mutableStateOf("/sdcard/DCIM/Memoirs") }
    var selectedCover by remember { mutableStateOf("gradient_sunset") }

    val presetCovers = listOf("gradient_sunset", "gradient_blue", "gradient_orange", "gradient_nature", "gradient_temple", "gradient_purple", "gradient_slate")

    val calendar = Calendar.getInstance()
    val filteredAlbums = remember(albums, searchQuery, activeSortOrder, activeSourceFilter, activeMonth, activeYear) {
        val filtered = albums.filter {
            val matchSearch = it.name.contains(searchQuery, ignoreCase = true)
            val matchSource = activeSourceFilter == "ALL" || it.sourceType == activeSourceFilter
            
            var m = true
            var y = true
            if (activeMonth != null || activeYear != null) {
                calendar.timeInMillis = it.createdAt
                if (activeMonth != null) m = (calendar.get(Calendar.MONTH) + 1) == activeMonth
                if (activeYear != null) y = calendar.get(Calendar.YEAR) == activeYear
            }
            matchSearch && matchSource && m && y
        }

        when (activeSortOrder) {
            "OLDEST" -> filtered.sortedBy { it.createdAt }
            "NAME_AZ" -> filtered.sortedBy { it.name }
            "NAME_ZA" -> filtered.sortedByDescending { it.name }
            else -> filtered.sortedByDescending { it.createdAt }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { newAlbumName = ""; selectedSource = "LOCAL"; customSourcePath = "/sdcard/DCIM/Memoirs"; selectedCover = "gradient_sunset"; showCreateDialog = true }, containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.testTag("add_album_fab")) {
                Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.btn_create_album))
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(paddingValues)) {
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 0.5.dp) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = stringResource(R.string.album_archive), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Box {
                        IconButton(onClick = { showFilterMenu = true }) { Icon(Icons.Default.FilterList, stringResource(R.string.nav_explore)) }
                        DropdownMenu(expanded = showFilterMenu, onDismissRequest = { showFilterMenu = false }) {
                            Text(stringResource(R.string.filter_sort), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                            val sortOptions = listOf(
                                "LATEST" to R.string.filter_latest,
                                "OLDEST" to R.string.filter_oldest,
                                "NAME_AZ" to null,
                                "NAME_ZA" to null
                            )
                            sortOptions.forEach { (o, resId) ->
                                DropdownMenuItem(text = { Text(if (resId != null) stringResource(resId) else if (o == "NAME_AZ") "A-Z" else "Z-A") }, onClick = { viewModel.setAlbumSortOrder(o); showFilterMenu = false }, leadingIcon = { if (activeSortOrder == o) Icon(Icons.Default.Check, null) })
                            }
                            HorizontalDivider()
                            Text(stringResource(R.string.label_storage_source), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                            listOf("ALL", "LOCAL", "SMB", "CLOUD").forEach { src ->
                                DropdownMenuItem(text = { Text(if (src == "ALL") stringResource(R.string.cat_all) else src) }, onClick = { viewModel.setAlbumSourceFilter(src); showFilterMenu = false }, leadingIcon = { if (activeSourceFilter == src) Icon(Icons.Default.Check, null) })
                            }
                            HorizontalDivider()
                            
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.filter_custom_time)) }, 
                                onClick = { isTimeSelectionExpanded = !isTimeSelectionExpanded }, 
                                trailingIcon = { Icon(if (isTimeSelectionExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null) },
                                leadingIcon = { if (activeMonth != null || activeYear != null) Icon(Icons.Default.DateRange, null, tint = MaterialTheme.colorScheme.primary) }
                            )

                            if (isTimeSelectionExpanded) {
                                Text(stringResource(R.string.filter_month), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp))
                                val currentLocale = LocalContext.current.resources.configuration.locales[0]
                                val monthFormat = java.text.SimpleDateFormat("MMM", currentLocale)
                                Row(
                                    modifier = Modifier
                                        .width(240.dp)
                                        .horizontalScroll(rememberScrollState())
                                        .padding(horizontal = 16.dp)
                                ) {
                                    for (idx in 0 until 12) {
                                        val m = idx + 1
                                        val isSel = activeMonth == m
                                        val calendarMonth = java.util.Calendar.getInstance().apply { set(java.util.Calendar.MONTH, idx) }
                                        val monthName = monthFormat.format(calendarMonth.time)
                                        FilterChip(
                                            selected = isSel, 
                                            onClick = { viewModel.setAlbumSelectedMonth(if (isSel) null else m) }, 
                                            label = { Text(monthName, fontSize = 10.sp) }, 
                                            modifier = Modifier.padding(horizontal = 2.dp)
                                        )
                                    }
                                }
                                Text(stringResource(R.string.filter_year), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp))
                                val years = (2023..Calendar.getInstance().get(Calendar.YEAR)).toList().reversed()
                                Row(
                                    modifier = Modifier
                                        .width(240.dp)
                                        .horizontalScroll(rememberScrollState())
                                        .padding(horizontal = 16.dp)
                                ) {
                                    years.forEach { year ->
                                        val isSel = activeYear == year
                                        FilterChip(
                                            selected = isSel, 
                                            onClick = { viewModel.setAlbumSelectedYear(if (isSel) null else year) }, 
                                            label = { Text(year.toString(), fontSize = 10.sp) }, 
                                            modifier = Modifier.padding(horizontal = 2.dp)
                                        )
                                    }
                                }
                            }
                            HorizontalDivider()
                            DropdownMenuItem(text = { Text(stringResource(R.string.filter_reset_time)) }, onClick = { viewModel.setAlbumSelectedMonth(null); viewModel.setAlbumSelectedYear(null); isTimeSelectionExpanded = false; showFilterMenu = false }, leadingIcon = { Icon(Icons.Default.Refresh, null) })
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, placeholder = { Text(stringResource(R.string.search_placeholder), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) }, leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) }, trailingIcon = { if (searchQuery.isNotEmpty()) IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)) } }, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)), singleLine = true, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 12.dp))

            if (albums.isEmpty()) {
                Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.PhotoLibrary, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.msg_no_album_yet), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else if (filteredAlbums.isEmpty()) {
                Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.SearchOff, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.msg_album_not_found), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { searchQuery = ""; viewModel.setAlbumSourceFilter("ALL"); viewModel.setAlbumSelectedMonth(null); viewModel.setAlbumSelectedYear(null) }) { Text(stringResource(R.string.btn_reset_filter)) }
                }
            } else {
                LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredAlbums) { album ->
                        AlbumGridCard(album = album, viewModel = viewModel, onClick = { viewModel.navigateTo(Screen.AlbumDetail(album.id)) })
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text(stringResource(R.string.new_album_title), fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = newAlbumName, onValueChange = { newAlbumName = it }, label = { Text(stringResource(R.string.label_album_name)) }, placeholder = { Text(stringResource(R.string.placeholder_album_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text(stringResource(R.string.label_cover_gradient), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().height(48.dp)) {
                        items(presetCovers) { cov ->
                            val isSelected = selectedCover == cov
                            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).clickable { selectedCover = cov }.border(width = if (isSelected) 3.dp else 1.dp, color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp))) {
                                MediaView(path = cov, modifier = Modifier.fillMaxSize())
                                if (isSelected) Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
                            }
                        }
                    }
                    Text(stringResource(R.string.label_storage_source), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("LOCAL", "SMB", "CLOUD").forEach { src ->
                            val isSelected = selectedSource == src
                            Button(onClick = { selectedSource = src; customSourcePath = when (src) { "SMB" -> "smb://192.168.1.100/Photos/Bali"; "CLOUD" -> "cloud://drive/Memoirs"; else -> "/sdcard/DCIM/Memoirs" } }, colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant), modifier = Modifier.weight(1f)) { Text(src, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
                        }
                    }
                    OutlinedTextField(value = customSourcePath, onValueChange = { customSourcePath = it }, label = { Text(stringResource(R.string.label_source_path)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = { Button(onClick = { if (newAlbumName.isNotBlank()) { viewModel.createAlbum(name = newAlbumName, coverPath = selectedCover, sourceType = selectedSource, sourcePath = customSourcePath); newAlbumName = ""; showCreateDialog = false } }) { Text(stringResource(R.string.btn_create)) } },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text(stringResource(R.string.btn_cancel)) } }
        )
    }
}

@Composable
fun AlbumGridCard(album: AlbumEntity, viewModel: MemoirViewModel, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val items by viewModel.getItemsForAlbum(album.id).collectAsStateWithLifecycle(initialValue = emptyList())
    Card(onClick = onClick, modifier = modifier.fillMaxWidth().aspectRatio(0.85f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))) {
                MediaView(path = album.coverPath, modifier = Modifier.fillMaxSize())
                Box(modifier = Modifier.align(Alignment.TopStart).padding(8.dp).background(color = when (album.sourceType) { "SMB" -> Color(0xFF4CAF50); "CLOUD" -> Color(0xFF2196F3); else -> MaterialTheme.colorScheme.primary }, shape = RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) { Text(text = album.sourceType, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(text = album.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(imageVector = when (album.sourceType) { "SMB" -> Icons.Default.Dns; "CLOUD" -> Icons.Default.Cloud; else -> Icons.Default.Folder }, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
                    Text(text = stringResource(R.string.media_count, items.size) + " • " + (if (album.sourceType == "LOCAL") stringResource(R.string.status_offline) else stringResource(R.string.status_online)), fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
