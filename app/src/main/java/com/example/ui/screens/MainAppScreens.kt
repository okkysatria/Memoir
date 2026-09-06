package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.components.AddPostScreen
import com.example.ui.components.StoryCreatorDialog
import com.example.ui.components.StoryViewer
import com.example.ui.viewmodel.MemoirViewModel
import com.example.ui.viewmodel.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoirApp(viewModel: MemoirViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val addSheetOpen by viewModel.addSheetOpen.collectAsStateWithLifecycle()
    val storyViewerOpen by viewModel.storyViewerOpen.collectAsStateWithLifecycle()
    val activeStories by viewModel.activeStories.collectAsStateWithLifecycle()
    val activeStoryIndex by viewModel.activeStoryIndex.collectAsStateWithLifecycle()
    val isAppLocked by viewModel.isAppLocked.collectAsStateWithLifecycle()
    val highlights by viewModel.highlights.collectAsStateWithLifecycle()
    val profileImagePath by viewModel.profileImagePath.collectAsStateWithLifecycle()
    val screenStack by viewModel.screenStack.collectAsStateWithLifecycle()
    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var showStoryCreator by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    val hasSeenOnboarding by viewModel.hasSeenOnboarding.collectAsStateWithLifecycle()
    val hasGrantedPermissions by viewModel.hasGrantedPermissions.collectAsStateWithLifecycle()
    BackHandler {
        if (screenStack.size > 1) {
            viewModel.navigateBack()
        } else if (currentTab != "home") {
            viewModel.selectTab("home")
        } else {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastBackPressTime < 2000) {
                (context as? android.app.Activity)?.finish()
            } else {
                lastBackPressTime = currentTime
                val msg = context.getString(R.string.msg_back_exit)
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        if (!hasSeenOnboarding) {
            OnboardingScreen(
                onFinishOnboarding = { viewModel.finishOnboarding() }
            )
        } else if (!hasGrantedPermissions) {
            PermissionsSetupScreen(
                onFinish = { viewModel.finishPermissions() }
            )
        } else if (isAppLocked) {
            val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsStateWithLifecycle()
            LockScreen(
                isBiometricEnabled = isBiometricEnabled,
                onUnlockAttempt = { pin ->
                    if (viewModel.unlockApp(pin)) {
                        val msg = context.getString(R.string.msg_unlocked)
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    } else {
                        val msg = context.getString(R.string.msg_wrong_pin)
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                },
                onBiometricSuccess = {
                    viewModel.unlockAppWithBiometric()
                    val msg = context.getString(R.string.msg_unlocked)
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            Scaffold(
                bottomBar = {
                    if (currentScreen !is Screen.AddPost && currentScreen !is Screen.EditPost) {
                        Surface(
                            shadowElevation = 4.dp,
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 0.dp,
                                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                            ) {
                                NavigationBarItem(
                                    selected = currentTab == "home",
                                    onClick = { 
                                        if (currentTab == "home") viewModel.triggerScrollToTop()
                                        else viewModel.selectTab("home") 
                                    },
                                    icon = { Icon(imageVector = if (currentTab == "home") Icons.Default.Home else Icons.Outlined.Home, contentDescription = stringResource(R.string.nav_home)) },
                                    label = { Text(stringResource(R.string.nav_home), fontSize = 11.sp, fontWeight = if (currentTab == "home") FontWeight.Bold else FontWeight.Medium) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                    ),
                                    modifier = Modifier.testTag("nav_home")
                                )
                                NavigationBarItem(
                                    selected = currentTab == "explore",
                                    onClick = { 
                                        if (currentTab == "explore") viewModel.triggerScrollToTop()
                                        else viewModel.selectTab("explore") 
                                    },
                                    icon = { Icon(imageVector = Icons.Default.Search, contentDescription = stringResource(R.string.nav_explore)) },
                                    label = { Text(stringResource(R.string.nav_explore), fontSize = 11.sp, fontWeight = if (currentTab == "explore") FontWeight.Bold else FontWeight.Medium) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                    ),
                                    modifier = Modifier.testTag("nav_explore")
                                )
                                NavigationBarItem(
                                    selected = currentScreen is Screen.AddPost,
                                    onClick = { viewModel.selectTab("add") },
                                    icon = {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(
                                                    Brush.linearGradient(
                                                        listOf(
                                                            MaterialTheme.colorScheme.primary,
                                                            MaterialTheme.colorScheme.secondary
                                                        )
                                                    ),
                                                    shape = RoundedCornerShape(12.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = stringResource(R.string.btn_add_memory),
                                                tint = Color.White,
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }
                                    },
                                    label = null,
                                    modifier = Modifier.testTag("nav_add")
                                )
                                NavigationBarItem(
                                    selected = currentTab == "album",
                                    onClick = { 
                                        if (currentTab == "album") viewModel.triggerScrollToTop()
                                        else viewModel.selectTab("album") 
                                    },
                                    icon = { Icon(imageVector = if (currentTab == "album") Icons.Default.PhotoLibrary else Icons.Outlined.PhotoLibrary, contentDescription = stringResource(R.string.nav_albums)) },
                                    label = { Text(stringResource(R.string.nav_albums), fontSize = 11.sp, fontWeight = if (currentTab == "album") FontWeight.Bold else FontWeight.Medium) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                    ),
                                    modifier = Modifier.testTag("nav_album")
                                )
                                NavigationBarItem(
                                    selected = currentTab == "profile",
                                    onClick = { 
                                        if (currentTab == "profile") viewModel.triggerScrollToTop()
                                        else viewModel.selectTab("profile") 
                                    },
                                    icon = { Icon(imageVector = if (currentTab == "profile") Icons.Default.Person else Icons.Outlined.Person, contentDescription = stringResource(R.string.nav_profile)) },
                                    label = { Text(stringResource(R.string.nav_profile), fontSize = 11.sp, fontWeight = if (currentTab == "profile") FontWeight.Bold else FontWeight.Medium) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                    ),
                                    modifier = Modifier.testTag("nav_profile")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (currentScreen is Screen.AddPost || currentScreen is Screen.EditPost) PaddingValues(0.dp) else innerPadding)
                ) {
                    val navState = remember(currentScreen) { currentScreen to screenStack.size }
                    AnimatedContent(
                        targetState = navState,
                        transitionSpec = {
                            val initialDepth = initialState.second
                            val targetDepth = targetState.second
                            if (initialDepth <= 1 && targetDepth <= 1) {
                                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                            } else if (targetDepth > initialDepth) {
                                (scaleIn(initialScale = 0.92f, animationSpec = tween(350, easing = LinearOutSlowInEasing)) + fadeIn(animationSpec = tween(350)))
                                    .togetherWith(scaleOut(targetScale = 1.08f, animationSpec = tween(350)) + fadeOut(animationSpec = tween(350)))
                            } else {
                                (scaleIn(initialScale = 1.08f, animationSpec = tween(350, easing = LinearOutSlowInEasing)) + fadeIn(animationSpec = tween(350)))
                                    .togetherWith(scaleOut(targetScale = 0.92f, animationSpec = tween(350)) + fadeOut(animationSpec = tween(350)))
                            }
                        },
                        label = "ScreenTransition"
                    ) { (targetScreen, _) ->
                        when (targetScreen) {
                            is Screen.Home -> HomeScreen(viewModel, onAddStoryClick = { showStoryCreator = true })
                            is Screen.Explore -> ExploreScreen(viewModel)
                            is Screen.AlbumList -> AlbumListScreen(viewModel)
                            is Screen.Profile -> ProfileScreen(viewModel, onAddStoryClick = { showStoryCreator = true })
                            is Screen.AlbumDetail -> AlbumDetailScreen(targetScreen.albumId, viewModel)
                            is Screen.Settings -> SettingsScreen(viewModel)
                            is Screen.ManageConnections -> ManageConnectionsScreen(viewModel)
                            is Screen.Backup -> BackupScreen(viewModel, onBack = { viewModel.navigateBack() })
                            is Screen.SmbExplorer -> SmbExplorerScreen(viewModel)
                            is Screen.AddPost -> AddPostScreen(
                                viewModel = viewModel,
                                onDismiss = { viewModel.navigateBack() }
                            )
                            is Screen.EditPost -> AddPostScreen(
                                viewModel = viewModel,
                                initialPost = targetScreen.post,
                                onDismiss = { viewModel.navigateBack() }
                            )
                            is Screen.ProfileFeed -> ProfileFeedScreen(
                                initialIndex = targetScreen.initialIndex,
                                isSavedTab = targetScreen.isSavedTab,
                                viewModel = viewModel
                            )
                            is Screen.ExploreFeed -> ExploreFeedScreen(
                                postIds = targetScreen.postIds,
                                initialIndex = targetScreen.initialIndex,
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
            if (storyViewerOpen) {
                StoryViewer(
                    stories = activeStories,
                    currentIndex = activeStoryIndex,
                    onNext = { viewModel.nextStory() },
                    onPrev = { viewModel.prevStory() },
                    onClose = { viewModel.closeStoryViewer() },
                    onDeleteStory = { story ->
                        viewModel.deleteStory(story)
                    },
                    highlights = highlights,
                    onAddToHighlight = { story, highlightId ->
                        if (story.highlightId == highlightId) {
                            viewModel.linkStoryToHighlight(story, null)
                            val msg = context.getString(R.string.msg_story_removed)
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.linkStoryToHighlight(story, highlightId)
                            val msg = context.getString(R.string.msg_story_added)
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onAddHighlight = { name, coverPath ->
                        val story = activeStories.getOrNull(activeStoryIndex)
                        if (story != null) {
                            viewModel.addHighlightWithStories(name, coverPath, listOf(story.id))
                            val msg = context.getString(R.string.msg_highlight_created, name)
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.addHighlight(name, coverPath)
                            val msg = context.getString(R.string.msg_highlight_created_only, name)
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
            if (showStoryCreator) {
                StoryCreatorDialog(
                    onDismiss = { showStoryCreator = false },
                    onPublish = { mediaPath, caption, textColor, textStyle, createdAt ->
                        viewModel.publishStory(mediaPath, caption, textColor, textStyle, createdAt)
                        showStoryCreator = false
                        val msg = context.getString(R.string.msg_story_published)
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    },
                    profileImagePath = profileImagePath
                )
            }
        }
    }
}

@Composable
fun LockScreen(
    isBiometricEnabled: Boolean = false,
    onUnlockAttempt: (String) -> Unit,
    onBiometricSuccess: () -> Unit = {}
) {
    var pin by remember { mutableStateOf("") }
    val context = LocalContext.current
    val triggerBiometric = remember(context, isBiometricEnabled) {
        {
            if (isBiometricEnabled && context is androidx.fragment.app.FragmentActivity) {
                com.example.utils.BiometricHelper.showBiometricPrompt(
                    activity = context,
                    onSuccess = onBiometricSuccess,
                    onError = { }
                )
            }
        }
    }
    LaunchedEffect(Unit) {
        if (isBiometricEnabled) {
            triggerBiometric()
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(RoundedCornerShape(22.dp))
                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(22.dp))
        ) {
            Image(
                painter = painterResource(id = R.drawable.app_logo_user),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.lock_title),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.lock_desc),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = { if (it.length <= 4) pin = it },
            label = { Text(stringResource(R.string.label_enter_pin)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .width(240.dp)
                .testTag("pin_lock_input")
        )
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.width(240.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    onUnlockAttempt(pin)
                    pin = ""
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("pin_lock_submit")
            ) {
                Text(stringResource(R.string.btn_unlock), fontWeight = FontWeight.SemiBold)
            }
            if (isBiometricEnabled && com.example.utils.BiometricHelper.isBiometricAvailable(context)) {
                IconButton(
                    onClick = { triggerBiometric() },
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(14.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = stringResource(R.string.pref_biometric),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}
