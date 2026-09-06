package com.example.data.repository

import com.example.data.database.*
import com.example.data.backup.MemoirBackupPayload
import kotlinx.coroutines.flow.Flow

class MemoirRepository(private val memoirDao: MemoirDao) {
    val allPosts: Flow<List<PostEntity>> = memoirDao.getAllPosts()
    val allStories: Flow<List<StoryEntity>> = memoirDao.getAllStories()
    val allAlbums: Flow<List<AlbumEntity>> = memoirDao.getAllAlbums()
    val allConnections: Flow<List<SourceConnectionEntity>> = memoirDao.getAllConnections()
    val allHighlights: Flow<List<StoryHighlightEntity>> = memoirDao.getAllHighlights()

    fun getPostById(postId: Long): Flow<PostEntity?> = memoirDao.getPostById(postId)
    suspend fun getPostCount(): Int = memoirDao.getPostCount()
    fun getStoriesForHighlight(highlightId: Long): Flow<List<StoryEntity>> = memoirDao.getStoriesForHighlight(highlightId)
    fun getAlbumById(albumId: Long): Flow<AlbumEntity?> = memoirDao.getAlbumById(albumId)
    fun getItemsForAlbum(albumId: Long): Flow<List<AlbumItemEntity>> = memoirDao.getItemsForAlbum(albumId)
    fun getCommentsForPost(postId: Long): Flow<List<CommentEntity>> = memoirDao.getCommentsForPost(postId)

    suspend fun insertPost(post: PostEntity): Long = memoirDao.insertPost(post)
    suspend fun updatePost(post: PostEntity) = memoirDao.updatePost(post)
    suspend fun deletePost(post: PostEntity) = memoirDao.deletePost(post)

    suspend fun insertStory(story: StoryEntity): Long = memoirDao.insertStory(story)
    suspend fun updateStory(story: StoryEntity) = memoirDao.updateStory(story)
    suspend fun deleteStory(story: StoryEntity) = memoirDao.deleteStory(story)
    suspend fun getExpiredStories(now: Long = System.currentTimeMillis()): List<StoryEntity> = memoirDao.getExpiredStories(now)
    suspend fun deleteExpiredStories(now: Long = System.currentTimeMillis()) = memoirDao.deleteExpiredStories(now)

    suspend fun insertAlbum(album: AlbumEntity): Long = memoirDao.insertAlbum(album)
    suspend fun updateAlbum(album: AlbumEntity) = memoirDao.updateAlbum(album)
    suspend fun deleteAlbum(album: AlbumEntity) {
        memoirDao.deleteAlbum(album)
        memoirDao.deleteItemsForAlbum(album.id)
    }

    suspend fun insertAlbumItem(item: AlbumItemEntity): Long = memoirDao.insertAlbumItem(item)
    suspend fun insertAlbumItems(items: List<AlbumItemEntity>) = memoirDao.insertAlbumItems(items)
    suspend fun deleteAlbumItem(item: AlbumItemEntity) = memoirDao.deleteAlbumItem(item)

    suspend fun insertConnection(conn: SourceConnectionEntity): Long = memoirDao.insertConnection(conn)
    suspend fun updateConnection(conn: SourceConnectionEntity) = memoirDao.updateConnection(conn)
    suspend fun deleteConnection(conn: SourceConnectionEntity) = memoirDao.deleteConnection(conn)

    suspend fun insertComment(comment: CommentEntity): Long = memoirDao.insertComment(comment)
    suspend fun deleteComment(comment: CommentEntity) = memoirDao.deleteComment(comment)

    suspend fun insertHighlight(highlight: StoryHighlightEntity): Long = memoirDao.insertHighlight(highlight)
    suspend fun deleteHighlight(highlight: StoryHighlightEntity) = memoirDao.deleteHighlight(highlight)

    suspend fun clearAllDatabase() {
        memoirDao.clearPosts()
        memoirDao.clearStories()
        memoirDao.clearAlbums()
        memoirDao.clearAlbumItems()
        memoirDao.clearConnections()
        memoirDao.clearComments()
        memoirDao.clearHighlights()
    }

    suspend fun restoreBackup(payload: MemoirBackupPayload, replaceExisting: Boolean = false) {
        if (replaceExisting) {
            clearAllDatabase()
        }
        payload.posts.forEach { memoirDao.insertPost(it) }
        payload.stories.forEach { memoirDao.insertStory(it) }
        payload.albums.forEach { memoirDao.insertAlbum(it) }
        payload.albumItems.forEach { memoirDao.insertAlbumItem(it) }
        payload.highlights.forEach { memoirDao.insertHighlight(it) }
        payload.comments.forEach { memoirDao.insertComment(it) }
        payload.connections.forEach { memoirDao.insertConnection(it) }
    }
}
