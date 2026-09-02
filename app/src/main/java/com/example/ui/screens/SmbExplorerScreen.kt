package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.database.SourceConnectionEntity
import com.example.ui.viewmodel.MemoirViewModel
import com.example.utils.SmbHelper
import com.example.utils.SmbMediaItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmbExplorerScreen(viewModel: MemoirViewModel) {
    val connections by viewModel.connections.collectAsStateWithLifecycle()
    val username by viewModel.username.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedConnection by remember { mutableStateOf<SourceConnectionEntity?>(null) }
    var currentPath by remember { mutableStateOf("") }
    var folders by remember { mutableStateOf<List<String>>(emptyList()) }
    var mediaFiles by remember { mutableStateOf<List<SmbMediaItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var syncStatusText by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(connections) {
        if (connections.size == 1 && selectedConnection == null) {
            selectedConnection = connections.first()
        }
    }
    fun loadContent(conn: SourceConnectionEntity, path: String) {
        val credParts = conn.credentials.split(":")
        val user = credParts.getOrNull(0) ?: ""
        val pass = credParts.getOrNull(1) ?: ""
        val dom = credParts.getOrNull(2) ?: ""
        val shareName = conn.folderPath.split("/").firstOrNull() ?: ""
        isLoading = true
        scope.launch {
            val foldersRes = SmbHelper.listSubfoldersInShare(
                host = conn.host,
                shareName = shareName,
                parentFolder = path,
                username = user,
                password = pass,
                domain = dom
            )
            val filesRes = SmbHelper.listMediaFiles(
                host = conn.host,
                shareName = shareName,
                folderPath = path,
                username = user,
                password = pass,
                domain = dom
            )
            isLoading = false
            folders = foldersRes.getOrDefault(emptyList())
            mediaFiles = filesRes.getOrDefault(emptyList())
            if (foldersRes.isFailure && filesRes.isFailure) {
                val msg = context.getString(R.string.msg_smb_load_failed)
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }
    LaunchedEffect(selectedConnection, currentPath) {
        selectedConnection?.let { loadContent(it, currentPath) }
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
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateBack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.btn_cancel))
                }
                Text(
                    text = stringResource(R.string.title_smb_explorer),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        if (connections.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(Icons.Default.Dns, null, modifier = Modifier.size(64.dp), tint = Color.Gray.copy(alpha = 0.5f))
                    Text(stringResource(R.string.msg_no_smb_saved), color = Color.Gray)
                    Button(onClick = { viewModel.navigateTo(com.example.ui.viewmodel.Screen.ManageConnections) }) {
                        Text(stringResource(R.string.pref_manage_smb))
                    }
                }
            }
        } else {
            Column(Modifier.padding(16.dp)) {
                if (connections.size > 1) {
                    Text(stringResource(R.string.label_select_server), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    LazyRow(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(connections) { conn ->
                            FilterChip(
                                selected = selectedConnection?.id == conn.id,
                                onClick = { selectedConnection = conn; currentPath = "" },
                                label = { Text(conn.name) },
                                leadingIcon = { if (selectedConnection?.id == conn.id) Icon(Icons.Default.Check, null, Modifier.size(14.dp)) }
                            )
                        }
                    }
                }
                selectedConnection?.let { conn ->
                    val shareName = conn.folderPath.split("/").firstOrNull() ?: ""
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            val locPath = "\\\\${conn.host}\\$shareName\\${currentPath.replace("/", "\\")}"
                            Text(
                                text = stringResource(R.string.label_location_path, locPath),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                if (currentPath.isNotBlank()) {
                                    IconButton(
                                        onClick = { 
                                            currentPath = if (currentPath.contains("/")) currentPath.substringBeforeLast("/") else "" 
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowUpward, stringResource(R.string.btn_up), tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                Spacer(Modifier.weight(1f))
                                val credParts = conn.credentials.split(":")
                                val user = credParts.getOrNull(0) ?: ""
                                val pass = credParts.getOrNull(1) ?: ""
                                Button(
                                    onClick = {
                                        viewModel.exportMemoirDataToSmb(
                                            host = conn.host,
                                            shareName = shareName,
                                            remoteFolder = currentPath,
                                            username = user,
                                            password = pass,
                                            onProgress = { syncStatusText = it },
                                            onResult = { success, msg ->
                                                syncStatusText = null
                                                Toast.makeText(context, msg, if (success) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
                                                loadContent(conn, currentPath)
                                            }
                                        )
                                    },
                                    enabled = syncStatusText == null,
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.CloudUpload, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(R.string.btn_export_here), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                syncStatusText?.let {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(12.dp))
                            Text(it, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    AnimatedContent(
                        targetState = currentPath,
                        transitionSpec = {
                            val initialDepth = initialState.count { it == '/' } + (if (initialState.isEmpty()) 0 else 1)
                            val targetDepth = targetState.count { it == '/' } + (if (targetState.isEmpty()) 0 else 1)
                            if (targetDepth > initialDepth || (initialState.isEmpty() && targetState.isNotEmpty())) {
                                (scaleIn(initialScale = 0.92f, animationSpec = tween(350, easing = LinearOutSlowInEasing)) + fadeIn(animationSpec = tween(350)))
                                    .togetherWith(scaleOut(targetScale = 1.08f, animationSpec = tween(350)) + fadeOut(animationSpec = tween(350)))
                            } else {
                                (scaleIn(initialScale = 1.08f, animationSpec = tween(350, easing = LinearOutSlowInEasing)) + fadeIn(animationSpec = tween(350)))
                                    .togetherWith(scaleOut(targetScale = 0.92f, animationSpec = tween(350)) + fadeOut(animationSpec = tween(350)))
                            }
                        },
                        label = "SmbFolderTransition"
                    ) { targetPath ->
                        Box(Modifier.fillMaxSize()) {
                            if (isLoading) {
                                CircularProgressIndicator(Modifier.align(Alignment.Center))
                            } else if (folders.isEmpty() && mediaFiles.isEmpty()) {
                                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.FolderOpen, null, Modifier.size(48.dp), tint = Color.Gray.copy(alpha = 0.3f))
                                    Text(stringResource(R.string.msg_empty_folder), color = Color.Gray, fontSize = 13.sp)
                                }
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                                    items(folders) { folder ->
                                        val userFolderName = stringResource(R.string.label_memoir_user, username)
                                        val isMemoirFolder = folder == userFolderName
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            onClick = {
                                                currentPath = if (targetPath.isBlank()) folder else "$targetPath/$folder"
                                            },
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isMemoirFolder) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surface
                                            ),
                                            border = if (isMemoirFolder) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = if (isMemoirFolder) Icons.Default.AutoAwesome else Icons.Default.Folder,
                                                    contentDescription = null,
                                                    tint = if (isMemoirFolder) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(Modifier.width(14.dp))
                                                Column(Modifier.weight(1f)) {
                                                    Text(folder, fontWeight = if (isMemoirFolder) FontWeight.Bold else FontWeight.Medium)
                                                    if (isMemoirFolder) {
                                                        Text(stringResource(R.string.label_memoir_archive), fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                                    }
                                                }
                                                if (isMemoirFolder) {
                                                    Button(
                                                        onClick = {
                                                            selectedConnection?.let { conn ->
                                                                val shareName = conn.folderPath.split("/").firstOrNull() ?: ""
                                                                val credParts = conn.credentials.split(":")
                                                                val user = credParts.getOrNull(0) ?: ""
                                                                val pass = credParts.getOrNull(1) ?: ""
                                                                val fullFolderPath = if (targetPath.isBlank()) folder else "$targetPath/$folder"
                                                                viewModel.importMemoirDataFromSmbUserFolder(
                                                                    host = conn.host,
                                                                    shareName = shareName,
                                                                    remoteUserFolder = fullFolderPath,
                                                                    username = user,
                                                                    password = pass,
                                                                    onProgress = { syncStatusText = it },
                                                                    onResult = { success, msg ->
                                                                        syncStatusText = null
                                                                        Toast.makeText(context, msg, if (success) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
                                                                    }
                                                                )
                                                            }
                                                        },
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                                    ) {
                                                        Icon(Icons.Default.CloudDownload, null, Modifier.size(14.dp))
                                                        Spacer(Modifier.width(4.dp))
                                                        Text(stringResource(R.string.btn_import), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                } else {
                                                    Icon(Icons.Default.ChevronRight, null, tint = Color.Gray.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                                                }
                                            }
                                        }
                                    }
                                    items(mediaFiles) { file ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 4.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (file.isVideo) Icons.Default.Videocam else Icons.Default.Image,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(Modifier.width(16.dp))
                                            Text(
                                                text = file.name,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface
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
}
