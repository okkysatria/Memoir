package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val caption: String,
    val createdAt: Long = System.currentTimeMillis(),
    val location: String = "",
    val mediaPaths: List<String>,
    val linkedAlbumId: Long? = null,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val isPinned: Boolean = false,
    val isLocked: Boolean = false,
    
    val aspectRatio: String = "1:1",
    
    
    val mediaTransformsJson: String = ""
)

@Entity(tableName = "stories")
data class StoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mediaPath: String,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L, 
    val highlightId: Long? = null,
    val isWatched: Boolean = false,
    val caption: String? = null,
    val textColor: String? = null,
    val textStyle: String? = null
)

@Entity(tableName = "albums")
data class AlbumEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val coverPath: String = "",
    val sourceType: String = "LOCAL", 
    val sourcePath: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "album_items")
data class AlbumItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val albumId: Long,
    val mediaPath: String,
    val sourceType: String = "LOCAL",
    val mimeType: String = "image" 
)

@Entity(tableName = "source_connections")
data class SourceConnectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, 
    val name: String,
    val host: String = "",
    val folderPath: String = "",
    val credentials: String = "", 
    val status: String = "ONLINE" 
)

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val postId: Long,
    val text: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "story_highlights")
data class StoryHighlightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val coverPath: String = ""
)
