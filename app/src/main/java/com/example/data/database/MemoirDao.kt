package com.example.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.paging.PagingSource
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoirDao {
    @Query("SELECT * FROM posts ORDER BY isPinned DESC, createdAt DESC")
    fun getAllPosts(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts")
    suspend fun getAllPostsList(): List<PostEntity>

    @Query("SELECT * FROM posts ORDER BY isPinned DESC, createdAt DESC")
    fun getPagedPosts(): PagingSource<Int, PostEntity>

    @Query("SELECT COUNT(*) FROM posts")
    suspend fun getPostCount(): Int

    @Query("SELECT * FROM posts WHERE id = :postId")
    fun getPostById(postId: Long): Flow<PostEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity): Long

    @Update
    suspend fun updatePost(post: PostEntity)

    @Delete
    suspend fun deletePost(post: PostEntity)

    @Query("SELECT * FROM stories ORDER BY createdAt DESC")
    fun getAllStories(): Flow<List<StoryEntity>>

    @Query("SELECT * FROM stories")
    suspend fun getAllStoriesList(): List<StoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStory(story: StoryEntity): Long

    @Update
    suspend fun updateStory(story: StoryEntity)

    @Delete
    suspend fun deleteStory(story: StoryEntity)

    @Query("SELECT * FROM stories WHERE expiresAt < :now AND highlightId IS NULL")
    suspend fun getExpiredStories(now: Long = System.currentTimeMillis()): List<StoryEntity>

    @Query("DELETE FROM stories WHERE expiresAt < :now AND highlightId IS NULL")
    suspend fun deleteExpiredStories(now: Long = System.currentTimeMillis())

    @Query("SELECT * FROM stories WHERE highlightId = :highlightId")
    fun getStoriesForHighlight(highlightId: Long): Flow<List<StoryEntity>>

    @Query("SELECT * FROM albums ORDER BY createdAt DESC")
    fun getAllAlbums(): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM albums WHERE id = :albumId")
    fun getAlbumById(albumId: Long): Flow<AlbumEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbum(album: AlbumEntity): Long

    @Update
    suspend fun updateAlbum(album: AlbumEntity)

    @Delete
    suspend fun deleteAlbum(album: AlbumEntity)

    @Query("SELECT * FROM album_items WHERE albumId = :albumId")
    fun getItemsForAlbum(albumId: Long): Flow<List<AlbumItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbumItem(item: AlbumItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbumItems(items: List<AlbumItemEntity>)

    @Delete
    suspend fun deleteAlbumItem(item: AlbumItemEntity)

    @Query("DELETE FROM album_items WHERE albumId = :albumId")
    suspend fun deleteItemsForAlbum(albumId: Long)

    @Query("SELECT * FROM source_connections ORDER BY id DESC")
    fun getAllConnections(): Flow<List<SourceConnectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConnection(connection: SourceConnectionEntity): Long

    @Update
    suspend fun updateConnection(connection: SourceConnectionEntity)

    @Delete
    suspend fun deleteConnection(connection: SourceConnectionEntity)

    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY createdAt ASC")
    fun getCommentsForPost(postId: Long): Flow<List<CommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity): Long

    @Delete
    suspend fun deleteComment(comment: CommentEntity)

    @Query("SELECT * FROM story_highlights ORDER BY id DESC")
    fun getAllHighlights(): Flow<List<StoryHighlightEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighlight(highlight: StoryHighlightEntity): Long

    @Delete
    suspend fun deleteHighlight(highlight: StoryHighlightEntity)

    @Query("DELETE FROM posts")
    suspend fun clearPosts()

    @Query("DELETE FROM stories")
    suspend fun clearStories()

    @Query("DELETE FROM albums")
    suspend fun clearAlbums()

    @Query("DELETE FROM album_items")
    suspend fun clearAlbumItems()

    @Query("DELETE FROM source_connections")
    suspend fun clearConnections()

    @Query("DELETE FROM comments")
    suspend fun clearComments()

    @Query("DELETE FROM story_highlights")
    suspend fun clearHighlights()

    @Query("SELECT * FROM post_tags WHERE postId = :postId ORDER BY taggedAt ASC")
    fun getTagsForPost(postId: Long): Flow<List<PostTagEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: PostTagEntity): Long

    @Delete
    suspend fun deleteTag(tag: PostTagEntity)

    @Query("DELETE FROM post_tags WHERE postId = :postId")
    suspend fun deleteTagsForPost(postId: Long)
}
