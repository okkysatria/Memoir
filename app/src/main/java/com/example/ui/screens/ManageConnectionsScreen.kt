package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.database.SourceConnectionEntity
import com.example.ui.viewmodel.MemoirViewModel
import com.example.utils.DiscoveredServer
import com.example.utils.SmbHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageConnectionsScreen(viewModel: MemoirViewModel) {
    val connections by viewModel.connections.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var serverName by remember { mutableStateOf("") }
    var serverHost by remember { mutableStateOf("192.168.1.100") }
    var shareName by remember { mutableStateOf("Photos") }
    var folderPath by remember { mutableStateOf("Memoir_Backup") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isGuestMode by remember { mutableStateOf(false) }

    
    var isDiscovering by remember { mutableStateOf(false) }
    var discoveredServers by remember { mutableStateOf<List<DiscoveredServer>>(emptyList()) }

    
    var isTestingConnection by remember { mutableStateOf(false) }
    var syncStatusText by remember { mutableStateOf<String?>(null) }
    var connectionToDelete by remember { mutableStateOf<SourceConnectionEntity?>(null) }

    
    var showFolderBrowser by remember { mutableStateOf(false) }
    var browserFolders by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoadingFolders by remember { mutableStateOf(false) }

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
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.btn_cancel),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = stringResource(R.string.title_manage_nas),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Radar,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.label_auto_discovery),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            FilledTonalButton(
                                onClick = {
                                    if (!isDiscovering) {
                                        isDiscovering = true
                                        discoveredServers = emptyList()
                                        scope.launch {
                                            SmbHelper.discoverSmbServers(context) { server ->
                                                if (!discoveredServers.any { it.ip == server.ip }) {
                                                    discoveredServers = discoveredServers + server
                                                }
                                            }
                                            isDiscovering = false
                                            if (discoveredServers.isEmpty()) {
                                                val msg = context.getString(R.string.msg_no_smb_found)
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                },
                                enabled = !isDiscovering,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                if (isDiscovering) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.msg_scanning), fontSize = 12.sp)
                                } else {
                                    Icon(Icons.Default.Search, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.btn_scan_network), fontSize = 12.sp)
                                }
                            }
                        }

                        if (discoveredServers.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.label_smb_found),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(discoveredServers) { server ->
                                    FilterChip(
                                        selected = serverHost == server.ip,
                                        onClick = {
                                            serverHost = server.ip
                                            if (serverName.isBlank()) serverName = "NAS (${server.ip})"
                                            val msg = context.getString(R.string.msg_host_set, server.ip)
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        },
                                        label = { Text(server.ip) },
                                        leadingIcon = {
                                            Icon(Icons.Default.Dns, null, modifier = Modifier.size(14.dp))
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Dns,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.label_smb_config),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }

                            
                            FilterChip(
                                selected = isGuestMode,
                                onClick = {
                                    isGuestMode = !isGuestMode
                                    if (isGuestMode) {
                                        username = ""
                                        password = ""
                                        val msg = context.getString(R.string.msg_guest_active)
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                label = { Text(stringResource(R.string.label_guest_mode), fontSize = 11.sp) },
                                leadingIcon = {
                                    if (isGuestMode) Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp))
                                }
                            )
                        }

                        OutlinedTextField(
                            value = serverName,
                            onValueChange = { serverName = it },
                            label = { Text(stringResource(R.string.label_server_name)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = serverHost,
                                onValueChange = { serverHost = it },
                                label = { Text(stringResource(R.string.label_ip_host)) },
                                placeholder = { Text(stringResource(R.string.placeholder_ip)) },
                                modifier = Modifier.weight(1.2f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = shareName,
                                onValueChange = { shareName = it },
                                label = { Text(stringResource(R.string.label_share_name)) },
                                placeholder = { Text(stringResource(R.string.placeholder_share)) },
                                modifier = Modifier.weight(0.8f),
                                singleLine = true
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = folderPath,
                                onValueChange = { folderPath = it },
                                label = { Text(stringResource(R.string.label_target_folder)) },
                                placeholder = { Text(stringResource(R.string.placeholder_target)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )

                            IconButton(
                                onClick = {
                                    if (serverHost.isBlank() || shareName.isBlank()) {
                                        val msg = context.getString(R.string.msg_fill_host_share)
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        return@IconButton
                                    }
                                    isLoadingFolders = true
                                    showFolderBrowser = true
                                    scope.launch {
                                        val res = SmbHelper.listSubfoldersInShare(
                                            host = serverHost,
                                            shareName = shareName,
                                            username = if (isGuestMode) "" else username,
                                            password = if (isGuestMode) "" else password
                                        )
                                        isLoadingFolders = false
                                        res.fold(
                                            onSuccess = { browserFolders = it },
                                            onFailure = {
                                                browserFolders = emptyList()
                                                val msg = context.getString(R.string.msg_read_folder_failed, it.localizedMessage)
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                },
                                modifier = Modifier.padding(top = 6.dp)
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = stringResource(R.string.item_smb), tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        if (!isGuestMode) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = username,
                                    onValueChange = { username = it },
                                    label = { Text(stringResource(R.string.label_username)) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = password,
                                    onValueChange = { password = it },
                                    label = { Text(stringResource(R.string.label_enter_pin).split(" ")[1]) },
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (serverHost.isBlank() || shareName.isBlank()) {
                                        val msg = context.getString(R.string.msg_fill_host_share_test)
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        return@OutlinedButton
                                    }
                                    isTestingConnection = true
                                    viewModel.testSmbConnection(
                                        host = serverHost,
                                        shareName = shareName,
                                        username = if (isGuestMode) "" else username,
                                        password = if (isGuestMode) "" else password
                                    ) { success, msg ->
                                        isTestingConnection = false
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !isTestingConnection
                            ) {
                                if (isTestingConnection) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.btn_test_conn), fontSize = 12.sp)
                                }
                            }

                            Button(
                                onClick = {
                                    if (serverName.isNotBlank() && serverHost.isNotBlank()) {
                                        viewModel.addConnection(
                                            name = serverName,
                                            type = "SMB",
                                            host = serverHost,
                                            folderPath = if (folderPath.isNotBlank()) "$shareName/$folderPath" else shareName,
                                            credentials = if (!isGuestMode && username.isNotBlank()) "$username:$password" else ""
                                        )
                                        val msg = context.getString(R.string.msg_server_saved)
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        serverName = ""
                                    } else {
                                        val msg = context.getString(R.string.msg_fill_name_host)
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.btn_save_server), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            
            syncStatusText?.let { status ->
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = status, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            
            item {
                Text(
                    text = stringResource(R.string.label_connected_servers, connections.size),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            if (connections.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.msg_no_conn_saved),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                items(connections, key = { it.id }) { conn ->
                    val credParts = conn.credentials.split(":")
                    val user = credParts.getOrNull(0) ?: ""
                    val pass = credParts.getOrNull(1) ?: ""

                    val pathParts = conn.folderPath.split("/")
                    val sName = pathParts.firstOrNull() ?: "Photos"
                    val subFolder = pathParts.drop(1).joinToString("/").ifBlank { "Memoir_Backup" }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF4CAF50).copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Dns, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                                    }
                                    Column {
                                        Text(conn.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("smb://${conn.host}/$sName/$subFolder", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                IconButton(onClick = { connectionToDelete = conn }) {
                                    Icon(Icons.Default.Delete, stringResource(R.string.btn_delete), tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f))
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))

                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.navigateTo(com.example.ui.viewmodel.Screen.SmbExplorer) },
                                    modifier = Modifier.weight(1.2f),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Explore, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.btn_open_explorer), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                FilledTonalButton(
                                    onClick = {
                                        viewModel.syncSmbMedia(
                                            connection = conn.copy(folderPath = subFolder),
                                            shareName = sName,
                                            username = user,
                                            password = pass,
                                            onProgress = { syncStatusText = it }
                                        )
                                    },
                                    modifier = Modifier.weight(0.8f),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Sync, null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.label_media), fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    
    if (showFolderBrowser) {
        AlertDialog(
            onDismissRequest = { showFolderBrowser = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FolderOpen, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.title_choose_folder, shareName), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                    if (isLoadingFolders) {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (browserFolders.isEmpty()) {
                        Text(stringResource(R.string.msg_no_subfolder), fontSize = 12.sp, color = Color.Gray)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(browserFolders) { folder ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            folderPath = folder
                                            showFolderBrowser = false
                                            val msg = context.getString(R.string.msg_folder_selected, folder)
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Text(folder, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFolderBrowser = false }) {
                    Text(stringResource(R.string.btn_done))
                }
            }
        )
    }

    
    connectionToDelete?.let { conn ->
        AlertDialog(
            onDismissRequest = { connectionToDelete = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.title_delete_conn)) },
            text = { Text(stringResource(R.string.msg_delete_conn, conn.name, conn.host)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteConnection(conn)
                        connectionToDelete = null
                        val msg = context.getString(R.string.msg_conn_deleted)
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(stringResource(R.string.btn_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { connectionToDelete = null }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}

