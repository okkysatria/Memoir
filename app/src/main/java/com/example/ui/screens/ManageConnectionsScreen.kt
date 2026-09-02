package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
    var currentStep by remember { mutableIntStateOf(1) }
    var serverName by remember { mutableStateOf("") }
    var serverHost by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var domain by remember { mutableStateOf("") }
    var isGuestMode by remember { mutableStateOf(false) }
    var isDiscovering by remember { mutableStateOf(false) }
    var discoveredServers by remember { mutableStateOf<List<DiscoveredServer>>(emptyList()) }
    var isVerifying by remember { mutableStateOf(false) }
    var shareNameInput by remember { mutableStateOf("") }
    var browserFolders by remember { mutableStateOf<List<String>>(emptyList()) }
    var browserCurrentPath by remember { mutableStateOf("") }
    var isLoadingBrowser by remember { mutableStateOf(false) }
    var selectedPathResult by remember { mutableStateOf("") }
    var connectionToDelete by remember { mutableStateOf<SourceConnectionEntity?>(null) }
    fun refreshBrowser(path: String) {
        if (serverHost.isBlank() || shareNameInput.isBlank()) return
        isLoadingBrowser = true
        browserCurrentPath = path
        scope.launch {
            val res = SmbHelper.listSubfoldersInShare(
                host = serverHost,
                shareName = shareNameInput,
                parentFolder = path,
                username = if (isGuestMode) "" else username,
                password = if (isGuestMode) "" else password,
                domain = domain
            )
            isLoadingBrowser = false
            res.fold(
                onSuccess = { browserFolders = it },
                onFailure = {
                    Toast.makeText(context, "Error: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_manage_nas), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (currentStep > 1) currentStep-- else viewModel.navigateBack() 
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            LinearProgressIndicator(
                progress = { if (currentStep == 1) 0.5f else 1f },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (currentStep == 1) {
                    item {
                        Text(
                            stringResource(R.string.ob_title_2),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Radar, null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.width(8.dp))
                                        Text(stringResource(R.string.label_auto_discovery), fontWeight = FontWeight.Bold)
                                    }
                                    TextButton(onClick = {
                                        isDiscovering = true
                                        discoveredServers = emptyList()
                                        scope.launch {
                                            SmbHelper.discoverSmbServers(context) { s ->
                                                if (!discoveredServers.any { it.ip == s.ip }) discoveredServers = discoveredServers + s
                                            }
                                            isDiscovering = false
                                        }
                                    }, enabled = !isDiscovering) {
                                        if (isDiscovering) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                        else Text(stringResource(R.string.btn_scan_network))
                                    }
                                }
                                if (discoveredServers.isNotEmpty()) {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(discoveredServers) { s ->
                                            FilterChip(
                                                selected = serverHost == s.ip,
                                                onClick = { 
                                                    serverHost = s.ip
                                                    if (serverName.isBlank()) serverName = "NAS ${s.ip}"
                                                },
                                                label = { Text(s.ip) },
                                                leadingIcon = { Icon(Icons.Default.Dns, null, Modifier.size(14.dp)) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = serverHost, onValueChange = { serverHost = it },
                                    label = { Text(stringResource(R.string.label_ip_hostname)) },
                                    placeholder = { Text(stringResource(R.string.placeholder_ip)) },
                                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                                    leadingIcon = { Icon(Icons.Default.Lan, null) }
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = isGuestMode, 
                                        onCheckedChange = { 
                                            isGuestMode = it
                                            if (it) {
                                                username = ""
                                                password = ""
                                                domain = ""
                                            }
                                        }
                                    )
                                    Text(stringResource(R.string.label_guest_access), fontSize = 14.sp)
                                }
                                if (!isGuestMode) {
                                    OutlinedTextField(
                                        value = username, onValueChange = { username = it },
                                        label = { Text(stringResource(R.string.label_user)) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = password, onValueChange = { password = it },
                                            label = { Text(stringResource(R.string.label_pass)) }, modifier = Modifier.weight(1f),
                                            visualTransformation = PasswordVisualTransformation(),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        OutlinedTextField(
                                            value = domain, onValueChange = { domain = it },
                                            label = { Text(stringResource(R.string.label_domain)) }, placeholder = { Text("optional") },
                                            modifier = Modifier.weight(0.7f), shape = RoundedCornerShape(12.dp)
                                        )
                                    }
                                }
                                Button(
                                    onClick = {
                                        if (serverHost.isBlank()) {
                                            Toast.makeText(context, context.getString(R.string.placeholder_ip), Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        if (!isGuestMode && username.isBlank()) {
                                            Toast.makeText(context, context.getString(R.string.msg_fill_name_host), Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        isVerifying = true
                                        viewModel.testSmbConnection(
                                            host = serverHost,
                                            shareName = "",
                                            username = username,
                                            password = password,
                                            domain = domain
                                        ) { success, msg ->
                                            isVerifying = false
                                            if (success) {
                                                currentStep = 2
                                            } else {
                                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = !isVerifying
                                ) {
                                    if (isVerifying) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White)
                                    else {
                                        Icon(Icons.Default.Link, null)
                                        Spacer(Modifier.width(8.dp))
                                        Text(stringResource(R.string.btn_connect_explore), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    item {
                        Text(
                            stringResource(R.string.title_explore_folders),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    item {
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text(
                                    "${stringResource(R.string.status_connected)}: $serverHost", 
                                    fontSize = 12.sp, 
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                OutlinedTextField(
                                    value = shareNameInput, 
                                    onValueChange = { shareNameInput = it },
                                    label = { Text(stringResource(R.string.label_share_hint)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    trailingIcon = {
                                        if (shareNameInput.isNotBlank()) {
                                            IconButton(onClick = { refreshBrowser("") }) {
                                                Icon(Icons.Default.Search, null)
                                            }
                                        }
                                    }
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 100.dp, max = 300.dp)
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                ) {
                                    if (shareNameInput.isBlank()) {
                                        Text(
                                            stringResource(R.string.msg_fill_host_share),
                                            modifier = Modifier.align(Alignment.Center).padding(20.dp),
                                            textAlign = TextAlign.Center,
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    } else if (isLoadingBrowser) {
                                        CircularProgressIndicator(Modifier.align(Alignment.Center))
                                    } else {
                                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                                            if (browserCurrentPath.isNotEmpty()) {
                                                item {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable {
                                                                val parent = if (browserCurrentPath.contains("/")) browserCurrentPath.substringBeforeLast("/") else ""
                                                                refreshBrowser(parent)
                                                            }
                                                            .padding(12.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(Icons.Default.ArrowUpward, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                                        Spacer(Modifier.width(12.dp))
                                                        Text(".. ${stringResource(R.string.btn_back)}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    }
                                                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                                                }
                                            }
                                            if (browserFolders.isEmpty()) {
                                                item {
                                                    Text(stringResource(R.string.msg_no_folders), modifier = Modifier.padding(20.dp).fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 11.sp)
                                                }
                                            } else {
                                                items(browserFolders) { folder ->
                                                    ListItem(
                                                        headlineContent = { Text(folder, fontSize = 14.sp) },
                                                        leadingContent = { Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp)) },
                                                        trailingContent = {
                                                            RadioButton(
                                                                selected = selectedPathResult == (if(browserCurrentPath.isEmpty()) folder else "$browserCurrentPath/$folder"),
                                                                onClick = { selectedPathResult = if(browserCurrentPath.isEmpty()) folder else "$browserCurrentPath/$folder" }
                                                            )
                                                        },
                                                        modifier = Modifier.clickable {
                                                            val nextPath = if (browserCurrentPath.isEmpty()) folder else "$browserCurrentPath/$folder"
                                                            refreshBrowser(nextPath)
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                if (selectedPathResult.isNotBlank() || (shareNameInput.isNotBlank() && browserCurrentPath.isEmpty())) {
                                    val finalPath = if(selectedPathResult.isNotBlank()) "$shareNameInput/$selectedPathResult" else shareNameInput
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            stringResource(R.string.label_path_prefix, finalPath),
                                            modifier = Modifier.padding(12.dp),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = serverName, onValueChange = { serverName = it },
                                        label = { Text(stringResource(R.string.label_friendly_name)) },
                                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                                    )
                                    Button(
                                        onClick = {
                                            if (serverName.isBlank()) serverName = "NAS $serverHost"
                                            val finalPath = if(selectedPathResult.isNotBlank()) "$shareNameInput/$selectedPathResult" else shareNameInput
                                            val creds = if (!isGuestMode) {
                                                if (domain.isNotBlank()) "$username:$password:$domain" else "$username:$password"
                                            } else ""
                                            viewModel.addConnection(
                                                name = serverName,
                                                type = "SMB",
                                                host = serverHost,
                                                folderPath = finalPath,
                                                credentials = creds
                                            )
                                            Toast.makeText(context, context.getString(R.string.msg_server_saved), Toast.LENGTH_SHORT).show()
                                            serverName = ""; serverHost = ""; shareNameInput = ""; selectedPathResult = ""; username = ""; password = ""; domain = ""; currentStep = 1
                                        },
                                        modifier = Modifier.fillMaxWidth().height(52.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        enabled = shareNameInput.isNotBlank()
                                    ) {
                                        Text(stringResource(R.string.btn_finalize_save), fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    Text(stringResource(R.string.nav_albums), fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
                }
                if (connections.isEmpty()) {
                    item {
                        Text(stringResource(R.string.msg_no_conn_saved), modifier = Modifier.fillMaxWidth().padding(32.dp), textAlign = TextAlign.Center, color = Color.Gray)
                    }
                } else {
                    items(connections) { conn ->
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Dns, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(16.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(conn.name, fontWeight = FontWeight.Bold)
                                    Text("smb://${conn.host}/${conn.folderPath}", style = MaterialTheme.typography.bodySmall, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                IconButton(onClick = { connectionToDelete = conn }) {
                                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(40.dp)) }
            }
        }
    }
    connectionToDelete?.let { conn ->
        AlertDialog(
            onDismissRequest = { connectionToDelete = null },
            title = { Text(stringResource(R.string.title_delete_conn)) },
            text = { Text(stringResource(R.string.msg_delete_conn, conn.name, conn.host)) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteConnection(conn); connectionToDelete = null }) {
                    Text(stringResource(R.string.btn_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { connectionToDelete = null }) { Text(stringResource(R.string.btn_cancel)) }
            }
        )
    }
}

@Composable
fun StepIndicator(step: Int, active: Boolean, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(60.dp)) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
            contentAlignment = Alignment.Center
        ) {
            Text("$step", color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
        }
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (active) MaterialTheme.colorScheme.primary else Color.Gray)
    }
}
