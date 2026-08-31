package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.components.MediaView
import com.example.ui.viewmodel.MemoirViewModel
import com.example.ui.viewmodel.Screen
import java.text.SimpleDateFormat
import java.util.*

enum class NotificationType {
    MEMORY, MILESTONE, STORY_EXPIRED, NEW_POST, SECURITY
}

data class ActivityItem(
    val id: String,
    val type: NotificationType,
    val title: String,
    val description: String,
    val timestamp: Long,
    val mediaPath: String? = null,
    val onClick: () -> Unit = {}
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(viewModel: MemoirViewModel) {
    val context = LocalContext.current
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val stories by viewModel.stories.collectAsStateWithLifecycle()

    val activityItems = remember(posts, stories, context) {
        val list = mutableListOf<ActivityItem>()
        val now = System.currentTimeMillis()
        val currentLocale = context.resources.configuration.locales[0]
        val sdf = SimpleDateFormat("d MMM yyyy", currentLocale)

        val todayCal = Calendar.getInstance()
        val todayMonth = todayCal.get(Calendar.MONTH)
        val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)
        val todayYear = todayCal.get(Calendar.YEAR)

        
        posts.forEach { post ->
            val cal = Calendar.getInstance().apply { timeInMillis = post.createdAt }
            val monthDiff = (todayYear * 12 + todayMonth) - (cal.get(Calendar.YEAR) * 12 + cal.get(Calendar.MONTH))
            val yearDiff = todayYear - cal.get(Calendar.YEAR)

            val badge = when {
                monthDiff == 1 && cal.get(Calendar.DAY_OF_MONTH) == todayDay -> context.getString(R.string.one_month_ago)
                yearDiff >= 1 && cal.get(Calendar.MONTH) == todayMonth && cal.get(Calendar.DAY_OF_MONTH) == todayDay -> context.getString(R.string.years_ago, yearDiff)
                else -> null
            }

            if (badge != null) {
                list.add(
                    ActivityItem(
                        id = "mem_${post.id}",
                        type = NotificationType.MEMORY,
                        title = context.getString(R.string.label_mem_flashback, badge),
                        description = post.caption.ifBlank { context.getString(R.string.desc_mem_date, sdf.format(Date(post.createdAt))) },
                        timestamp = post.createdAt,
                        mediaPath = post.mediaPaths.firstOrNull(),
                        onClick = { viewModel.navigateTo(Screen.ExploreFeed(listOf(post.id), 0)) }
                    )
                )
            }
        }

        
        posts.take(5).forEach { post ->
            list.add(
                ActivityItem(
                    id = "post_${post.id}",
                    type = NotificationType.NEW_POST,
                    title = "📸 " + context.getString(R.string.title_mem_added),
                    description = post.caption.ifBlank { context.getString(R.string.desc_mem_added) },
                    timestamp = post.createdAt,
                    mediaPath = post.mediaPaths.firstOrNull(),
                    onClick = { viewModel.navigateTo(Screen.ExploreFeed(listOf(post.id), 0)) }
                )
            )
        }

        
        val twoHoursMs = 2 * 60 * 60 * 1000L
        stories.filter { story ->
            val ageMs = now - story.createdAt
            val dayMs = 24 * 60 * 60 * 1000L
            ageMs in (dayMs - twoHoursMs)..dayMs
        }.forEach { story ->
            val remainMins = ((story.createdAt + 24 * 60 * 60 * 1000L) - now) / 60000
            list.add(
                ActivityItem(
                    id = "story_exp_${story.id}",
                    type = NotificationType.STORY_EXPIRED,
                    title = "⏰ " + context.getString(R.string.title_story_expiring),
                    description = context.getString(R.string.desc_story_expiring, story.caption ?: context.getString(R.string.label_your_story), remainMins),
                    timestamp = story.createdAt,
                    mediaPath = story.mediaPath
                )
            )
        }

        
        val totalPosts = posts.size
        if (totalPosts > 0) {
            list.add(
                ActivityItem(
                    id = "milestone_posts",
                    type = NotificationType.MILESTONE,
                    title = "🏆 " + context.getString(R.string.title_mem_collection),
                    description = context.getString(R.string.desc_mem_collection, totalPosts),
                    timestamp = now
                )
            )
        }

        
        list.add(
            ActivityItem(
                id = "sec_info",
                type = NotificationType.SECURITY,
                title = "🔒 " + context.getString(R.string.title_local_sec),
                description = context.getString(R.string.desc_local_sec),
                timestamp = now
            )
        )

        list.distinctBy { it.id }.sortedByDescending { it.timestamp }
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
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.btn_cancel),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = stringResource(R.string.title_notifications),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (activityItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.msg_no_activity),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                itemsIndexed(activityItems) { _, item ->
                    ActivityRowItem(item = item)
                    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                }
            }
        }
    }
}

@Composable
fun ActivityRowItem(item: ActivityItem) {
    val iconColor = when (item.type) {
        NotificationType.MEMORY -> MaterialTheme.colorScheme.tertiary
        NotificationType.MILESTONE -> MaterialTheme.colorScheme.primary
        NotificationType.STORY_EXPIRED -> MaterialTheme.colorScheme.error
        NotificationType.NEW_POST -> MaterialTheme.colorScheme.secondary
        NotificationType.SECURITY -> Color(0xFF4CAF50)
    }

    val icon: ImageVector = when (item.type) {
        NotificationType.MEMORY -> Icons.Default.Favorite
        NotificationType.MILESTONE -> Icons.Default.EmojiEvents
        NotificationType.STORY_EXPIRED -> Icons.Default.Timer
        NotificationType.NEW_POST -> Icons.Default.PhotoCamera
        NotificationType.SECURITY -> Icons.Default.Shield
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { item.onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(item.timestamp)),
                fontSize = 10.sp,
                color = Color.Gray
            )
        }

        if (item.mediaPath != null) {
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                MediaView(
                    path = item.mediaPath,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
