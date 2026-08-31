package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.database.StoryEntity
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StoriesTray(
    stories: List<StoryEntity>,
    allStories: List<StoryEntity>,
    onStoryClick: (Int) -> Unit,
    onAddStoryClick: () -> Unit,
    onKilasBalikClick: (StoryEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    
    val randomStory = remember(allStories) {
        allStories.filter { it.isWatched }.randomOrNull()
            ?: allStories.randomOrNull()
    }

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onAddStoryClick() }
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .border(
                            BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                            CircleShape
                        )
                        .padding(3.dp)
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
                            contentDescription = stringResource(R.string.btn_add_story),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.btn_add_story),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )
            }
        }

        if (randomStory != null) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onKilasBalikClick(randomStory) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .border(
                                BorderStroke(
                                    2.5.dp,
                                    Brush.linearGradient(
                                        listOf(
                                            Color(0xFFFFD700),
                                            Color(0xFFFF5722),
                                            Color(0xFF9C27B0)
                                        )
                                    )
                                ),
                                CircleShape
                            )
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            MediaView(path = randomStory.mediaPath, modifier = Modifier.fillMaxSize())
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.35f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "✨",
                                    fontSize = 18.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.kilas_balik),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        itemsIndexed(
            items = stories,
            key = { _, story -> story.id }
        ) { index, story ->
            val ringBrush = if (story.isWatched) {
                Brush.linearGradient(listOf(Color.LightGray, Color.Gray))
            } else {
                Brush.linearGradient(listOf(Color(0xFFE1306C), Color(0xFFC13584), Color(0xFFF77737)))
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onStoryClick(index) }
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .border(BorderStroke(2.5.dp, ringBrush), CircleShape)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        MediaView(path = story.mediaPath, modifier = Modifier.fillMaxSize())
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = run {
                        val cap = story.caption
                        if (!cap.isNullOrBlank()) {
                            val maxChars = 14
                            if (cap.length > maxChars) cap.take(maxChars) + "…" else cap
                        } else {
                            val currentLocale = context.resources.configuration.locales[0]
                            val sdf = SimpleDateFormat("d MMM", currentLocale)
                            sdf.format(Date(story.createdAt))
                        }
                    },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    maxLines = 1
                )
            }
        }
    }
}
