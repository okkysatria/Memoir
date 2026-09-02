package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.viewmodel.MemoirViewModel
import com.example.ui.viewmodel.Screen
import com.example.ui.theme.MemoirViolet
import com.example.ui.theme.NavyPrimary
import com.example.ui.components.MediaView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MemoirViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val currentPin by viewModel.appPin.collectAsStateWithLifecycle()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val selectedColorTheme by viewModel.selectedColorTheme.collectAsStateWithLifecycle()
    val cacheSizeMb by viewModel.cacheSizeMb.collectAsStateWithLifecycle()
    val profileName by viewModel.profileName.collectAsStateWithLifecycle()
    val username by viewModel.username.collectAsStateWithLifecycle()
    val profileImagePath by viewModel.profileImagePath.collectAsStateWithLifecycle()
    val currentLanguage by viewModel.language.collectAsStateWithLifecycle()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showResetPinDialog by remember { mutableStateOf(false) }
    var resetPinInput by remember { mutableStateOf("") }
    var showClearCacheConfirm by remember { mutableStateOf(false) }
    var isClearingCache by remember { mutableStateOf(false) }

    val backupRepo = remember {
        com.example.data.repository.BackupRepository(
            context,
            com.example.data.database.MemoirDatabase.getDatabase(context).memoirDao()
        )
    }

    val exportFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            scope.launch {
                val res = backupRepo.exportMemoirDataToDocumentTree(context, it)
                res.fold(
                    onSuccess = { count ->
                        Toast.makeText(context, context.getString(R.string.msg_success_export, count), Toast.LENGTH_LONG).show()
                    },
                    onFailure = { err ->
                        Toast.makeText(context, context.getString(R.string.msg_failed_export, err.localizedMessage), Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.settings_title), 
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold 
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!profileImagePath.isNullOrEmpty()) {
                            MediaView(path = profileImagePath!!, modifier = Modifier.fillMaxSize())
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(profileName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("@$username", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(
                        onClick = { showEditProfileDialog = true },
                        modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    ) {
                        Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                }
            }

            SettingsGroup(title = stringResource(R.string.sec_appearance)) {
                LanguagePicker(
                    currentLanguage = currentLanguage,
                    onLanguageSelected = { viewModel.changeLanguage(it) }
                )

                SettingsToggleItem(
                    title = stringResource(R.string.pref_dark_mode),
                    icon = Icons.Default.Brightness4,
                    checked = isDarkMode,
                    onCheckedChange = { viewModel.toggleDarkMode() }
                )

                ThemeColorPicker(
                    selectedTheme = selectedColorTheme,
                    onThemeSelected = { viewModel.setColorTheme(it) }
                )
            }

            SettingsGroup(title = stringResource(R.string.sec_security)) {
                SettingsActionItem(
                    title = stringResource(R.string.pref_pin_lock),
                    icon = Icons.Default.Lock,
                    subtitle = if (currentPin != null) stringResource(R.string.status_active) else stringResource(R.string.status_inactive),
                    statusColor = if (currentPin != null) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = { }
                )
                
                Box(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        var inlinePin by remember { mutableStateOf("") }
                        OutlinedTextField(
                            value = inlinePin,
                            onValueChange = { if (it.length <= 4) inlinePin = it },
                            placeholder = { Text(stringResource(R.string.placeholder_new_pin), fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(52.dp)
                        )
                        if (currentPin != null) {
                            IconButton(onClick = { viewModel.setAppPin(null) }) {
                                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        Button(
                            onClick = { if (inlinePin.length == 4) { viewModel.setAppPin(inlinePin); inlinePin = "" } },
                            shape = RoundedCornerShape(12.dp),
                            enabled = inlinePin.length == 4
                        ) {
                            Text(stringResource(R.string.btn_save), fontSize = 12.sp)
                        }
                    }
                }

                SettingsToggleItem(
                    title = stringResource(R.string.pref_biometric),
                    icon = Icons.Default.Fingerprint,
                    checked = isBiometricEnabled,
                    isLocked = currentPin == null,
                    onCheckedChange = { 
                        if (currentPin != null) {
                            viewModel.toggleBiometric() 
                        } else {
                            Toast.makeText(context, context.getString(R.string.msg_set_pin_first), Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            SettingsGroup(title = stringResource(R.string.sec_storage)) {
                SettingsActionItem(
                    title = stringResource(R.string.pref_smb_explorer),
                    icon = Icons.Default.Explore,
                    subtitle = stringResource(R.string.pref_smb_explorer_desc),
                    onClick = { viewModel.navigateTo(Screen.SmbExplorer) }
                )
                SettingsActionItem(
                    title = stringResource(R.string.pref_manage_smb),
                    icon = Icons.Default.Dns,
                    subtitle = stringResource(R.string.desc_manage_smb),
                    onClick = { viewModel.navigateTo(Screen.ManageConnections) }
                )
                SettingsActionItem(
                    title = stringResource(R.string.pref_manual_backup),
                    icon = Icons.Default.FolderZip,
                    subtitle = stringResource(R.string.desc_manual_backup),
                    trailingIcon = Icons.Default.FileUpload,
                    onClick = { exportFolderLauncher.launch(null) }
                )
            }

            SettingsGroup(title = stringResource(R.string.sec_maintenance)) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.pref_clear_cache), fontSize = 14.sp, fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text("${String.format("%.1f", cacheSizeMb)} MB", fontSize = 12.sp) },
                    leadingContent = { Icon(Icons.Default.CleaningServices, null, tint = MaterialTheme.colorScheme.primary) },
                    trailingContent = { 
                        if (isClearingCache) {
                            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp)
                        } else {
                            OutlinedButton(
                                onClick = { showClearCacheConfirm = true },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(stringResource(R.string.btn_clean), fontSize = 11.sp)
                            }
                        }
                    }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showResetPinDialog = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            stringResource(R.string.pref_reset_app),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            stringResource(R.string.msg_total_wipe_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.memoir_version),
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }

    if (showClearCacheConfirm) {
        AlertDialog(
            onDismissRequest = { showClearCacheConfirm = false },
            title = { Text(stringResource(R.string.pref_clear_cache)) },
            text = { Text("Membersihkan cache akan menghapus file sementara. Lanjutkan?") },
            confirmButton = {
                TextButton(onClick = {
                    showClearCacheConfirm = false
                    isClearingCache = true
                    scope.launch {
                        viewModel.clearCache()
                        delay(1000)
                        isClearingCache = false
                        Toast.makeText(context, "Cache dibersihkan", Toast.LENGTH_SHORT).show()
                    }
                }) { Text(stringResource(R.string.btn_clean)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheConfirm = false }) { Text(stringResource(R.string.btn_cancel)) }
            }
        )
    }

    if (showResetPinDialog) {
        AlertDialog(
            onDismissRequest = { showResetPinDialog = false; resetPinInput = "" },
            title = { 
                Text(
                    if (currentPin != null) stringResource(R.string.label_verify_pin_reset) else stringResource(R.string.confirm_reset_title),
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.msg_total_wipe_desc))
                    if (currentPin != null) {
                        OutlinedTextField(
                            value = resetPinInput,
                            onValueChange = { if (it.length <= 4) resetPinInput = it },
                            label = { Text("PIN") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (currentPin == null || resetPinInput == currentPin) {
                            viewModel.resetDatabaseAndReSeed()
                            showResetPinDialog = false
                            Toast.makeText(context, context.getString(R.string.msg_reset_success), Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, context.getString(R.string.msg_wrong_pin), Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.btn_total_reset))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetPinDialog = false; resetPinInput = "" }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    if (showEditProfileDialog) {
        com.example.ui.screens.profile.EditProfileDialog(
            currentName = profileName,
            currentUsername = username,
            currentBio = viewModel.profileBio.collectAsStateWithLifecycle().value,
            currentImagePath = profileImagePath,
            onDismiss = { showEditProfileDialog = false },
            onSave = { name, uname, bio, imagePath ->
                viewModel.updateProfile(name, uname, bio, imagePath)
                showEditProfileDialog = false
                Toast.makeText(context, context.getString(R.string.msg_profile_updated), Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp, bottom = 10.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SettingsToggleItem(title: String, icon: ImageVector, checked: Boolean, isLocked: Boolean = false, onCheckedChange: (Boolean) -> Unit) {
    val alpha = if (isLocked) 0.38f else 1.0f
    ListItem(
        headlineContent = { 
            Text(
                title, 
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
            ) 
        },
        leadingContent = { 
            Icon(
                icon, 
                null, 
                tint = if (isLocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.38f) else MaterialTheme.colorScheme.primary, 
                modifier = Modifier.size(20.dp)
            ) 
        },
        trailingContent = { 
            Switch(
                checked = checked, 
                onCheckedChange = onCheckedChange,
                thumbContent = if (checked) {
                    { Icon(Icons.Default.Check, null, Modifier.size(SwitchDefaults.IconSize)) }
                } else null
            ) 
        },
        modifier = Modifier.clickable { onCheckedChange(!checked) }
    )
}

@Composable
fun SettingsActionItem(
    title: String, 
    icon: ImageVector, 
    subtitle: String? = null, 
    trailingIcon: ImageVector = Icons.Default.ChevronRight,
    statusColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title, fontSize = 14.sp) },
        supportingContent = subtitle?.let { { Text(it, fontSize = 11.sp, color = statusColor) } },
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) },
        trailingContent = { Icon(trailingIcon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(18.dp)) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
fun LanguagePicker(currentLanguage: String, onLanguageSelected: (String) -> Unit) {
    var showLangMenu by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text(stringResource(R.string.pref_language), fontSize = 14.sp) },
        leadingContent = { Icon(Icons.Default.Language, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) },
        trailingContent = {
            Box {
                TextButton(onClick = { showLangMenu = true }) {
                    val displayLang = when {
                        currentLanguage.startsWith("id") || currentLanguage.startsWith("in") -> "Indonesia"
                        currentLanguage.startsWith("zh") -> "中文"
                        currentLanguage.startsWith("ja") -> "日本語"
                        currentLanguage.startsWith("ru") -> "Русский"
                        currentLanguage.startsWith("ar") -> "العربية"
                        else -> "English"
                    }
                    Text(displayLang, fontSize = 13.sp)
                    Icon(Icons.Default.ArrowDropDown, null)
                }
                DropdownMenu(expanded = showLangMenu, onDismissRequest = { showLangMenu = false }) {
                    listOf("en" to R.string.lang_en, "id" to R.string.lang_id, "zh" to R.string.lang_zh, "ja" to R.string.lang_ja, "ru" to R.string.lang_ru, "ar" to R.string.lang_ar).forEach { (code, res) ->
                        DropdownMenuItem(
                            text = { Text(stringResource(res)) }, 
                            onClick = { onLanguageSelected(code); showLangMenu = false },
                            leadingIcon = { if (currentLanguage.startsWith(code)) Icon(Icons.Default.Check, null) }
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun ThemeColorPicker(selectedTheme: String, onThemeSelected: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text(stringResource(R.string.pref_theme_color), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        val themeOptions = listOf(
            Triple("DEFAULT", R.string.theme_signature, MemoirViolet),
            Triple("NAVY", R.string.theme_navy, NavyPrimary),
            Triple("MONOCHROME", R.string.theme_charcoal, Color(0xFF2D3142)),
            Triple("DEFAULT_ROSE", R.string.theme_rose, Color(0xFFD81B60)),
            Triple("OCEAN", R.string.theme_ocean, Color(0xFF4A7B9D)),
            Triple("EMERALD", R.string.theme_emerald, Color(0xFF4A7C59)),
            Triple("SUNSET", R.string.theme_sunset, Color(0xFFC86D51)),
            Triple("PURPLE", R.string.theme_violet, Color(0xFF7A6C9B))
        )
        
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(themeOptions) { option ->
                val (key, resId, color) = option
                val isSelected = selectedTheme == key
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onThemeSelected(key) }) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(if (isSelected) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    Text(stringResource(resId), fontSize = 9.sp, modifier = Modifier.padding(top = 4.dp), color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray)
                }
            }
        }
    }
}
