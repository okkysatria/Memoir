package com.example.data.backup

import com.example.data.database.*
import org.json.JSONArray
import org.json.JSONObject

data class MemoirBackupPayload(
    val version: Int = 2,
    val exportedAt: Long = System.currentTimeMillis(),
    val appName: String = "Memoir",
    val profileName: String = "",
    val username: String = "",
    val profileBio: String = "",
    val posts: List<PostEntity> = emptyList(),
    val stories: List<StoryEntity> = emptyList(),
    val albums: List<AlbumEntity> = emptyList(),
    val albumItems: List<AlbumItemEntity> = emptyList(),
    val highlights: List<StoryHighlightEntity> = emptyList(),
    val comments: List<CommentEntity> = emptyList(),
    val connections: List<SourceConnectionEntity> = emptyList()
) {
    


    fun toJson(): String {
        val root = JSONObject()
        root.put("version", version)
        root.put("exportedAt", exportedAt)
        root.put("appName", appName)
        root.put("profileName", profileName)
        root.put("username", username)
        root.put("profileBio", profileBio)

        
        val postsArr = JSONArray()
        posts.forEach { post ->
            val obj = JSONObject()
            obj.put("id", post.id)
            obj.put("caption", post.caption)
            obj.put("location", post.location)
            obj.put("createdAt", post.createdAt)
            obj.put("aspectRatio", post.aspectRatio)
            obj.put("isLiked", post.isLiked)
            obj.put("isSaved", post.isSaved)
            obj.put("isPinned", post.isPinned)
            obj.put("isLocked", post.isLocked)
            obj.put("mediaTransformsJson", post.mediaTransformsJson)
            val pathsArr = JSONArray()
            post.mediaPaths.forEach { pathsArr.put(it) }
            obj.put("mediaPaths", pathsArr)
            if (post.linkedAlbumId != null) obj.put("linkedAlbumId", post.linkedAlbumId)
            postsArr.put(obj)
        }
        root.put("posts", postsArr)

        
        val storiesArr = JSONArray()
        stories.forEach { story ->
            val obj = JSONObject()
            obj.put("id", story.id)
            obj.put("mediaPath", story.mediaPath)
            obj.put("createdAt", story.createdAt)
            obj.put("expiresAt", story.expiresAt)
            obj.put("isWatched", story.isWatched)
            if (story.caption != null) obj.put("caption", story.caption)
            if (story.textColor != null) obj.put("textColor", story.textColor)
            if (story.textStyle != null) obj.put("textStyle", story.textStyle)
            if (story.highlightId != null) obj.put("highlightId", story.highlightId)
            storiesArr.put(obj)
        }
        root.put("stories", storiesArr)

        
        val albumsArr = JSONArray()
        albums.forEach { album ->
            val obj = JSONObject()
            obj.put("id", album.id)
            obj.put("name", album.name)
            obj.put("coverPath", album.coverPath)
            obj.put("sourceType", album.sourceType)
            obj.put("sourcePath", album.sourcePath)
            obj.put("createdAt", album.createdAt)
            albumsArr.put(obj)
        }
        root.put("albums", albumsArr)

        
        val highlightsArr = JSONArray()
        highlights.forEach { h ->
            val obj = JSONObject()
            obj.put("id", h.id)
            obj.put("name", h.name)
            obj.put("coverPath", h.coverPath)
            highlightsArr.put(obj)
        }
        root.put("highlights", highlightsArr)

        return root.toString(2)
    }

    companion object {
        


        fun fromJson(json: String): MemoirBackupPayload? {
            return try {
                val root = JSONObject(json)
                val profileName = if (root.has("profileName")) root.getString("profileName") else ""
                val username = if (root.has("username")) root.getString("username") else ""
                val profileBio = if (root.has("profileBio")) root.getString("profileBio") else ""

                
                val posts = mutableListOf<PostEntity>()
                if (root.has("posts")) {
                    val arr = root.getJSONArray("posts")
                    for (i in 0 until arr.length()) {
                        val p = arr.getJSONObject(i)
                        val mediaPaths = mutableListOf<String>()
                        if (p.has("mediaPaths")) {
                            val mp = p.getJSONArray("mediaPaths")
                            for (j in 0 until mp.length()) mediaPaths.add(mp.getString(j))
                        } else if (p.has("mediaPath")) mediaPaths.add(p.getString("mediaPath"))
                        if (mediaPaths.isEmpty()) mediaPaths.add("gradient_purple")

                        posts.add(PostEntity(
                            caption = if (p.has("caption")) p.getString("caption") else "",
                            location = if (p.has("location")) p.getString("location") else "",
                            mediaPaths = mediaPaths,
                            createdAt = if (p.has("createdAt")) p.getLong("createdAt") else System.currentTimeMillis(),
                            aspectRatio = if (p.has("aspectRatio")) p.getString("aspectRatio") else "1:1",
                            isLiked = if (p.has("isLiked")) p.getBoolean("isLiked") else false,
                            isSaved = if (p.has("isSaved")) p.getBoolean("isSaved") else false,
                            isPinned = if (p.has("isPinned")) p.getBoolean("isPinned") else false,
                            isLocked = if (p.has("isLocked")) p.getBoolean("isLocked") else false,
                            mediaTransformsJson = if (p.has("mediaTransformsJson")) p.getString("mediaTransformsJson") else "",
                            linkedAlbumId = if (p.has("linkedAlbumId")) p.getLong("linkedAlbumId") else null
                        ))
                    }
                }

                
                val stories = mutableListOf<StoryEntity>()
                if (root.has("stories")) {
                    val arr = root.getJSONArray("stories")
                    for (i in 0 until arr.length()) {
                        val s = arr.getJSONObject(i)
                        stories.add(StoryEntity(
                            mediaPath = if (s.has("mediaPath")) s.getString("mediaPath") else "gradient_purple",
                            createdAt = if (s.has("createdAt")) s.getLong("createdAt") else System.currentTimeMillis(),
                            expiresAt = if (s.has("expiresAt")) s.getLong("expiresAt") else System.currentTimeMillis() + 24 * 60 * 60 * 1000L,
                            isWatched = if (s.has("isWatched")) s.getBoolean("isWatched") else false,
                            caption = if (s.has("caption")) s.getString("caption") else null,
                            textColor = if (s.has("textColor")) s.getString("textColor") else null,
                            textStyle = if (s.has("textStyle")) s.getString("textStyle") else null,
                            highlightId = if (s.has("highlightId")) s.getLong("highlightId") else null
                        ))
                    }
                }

                
                val albums = mutableListOf<AlbumEntity>()
                if (root.has("albums")) {
                    val arr = root.getJSONArray("albums")
                    for (i in 0 until arr.length()) {
                        val a = arr.getJSONObject(i)
                        albums.add(AlbumEntity(
                            name = if (a.has("name")) a.getString("name") else "",
                            coverPath = if (a.has("coverPath")) a.getString("coverPath") else "",
                            sourceType = if (a.has("sourceType")) a.getString("sourceType") else "LOCAL",
                            sourcePath = if (a.has("sourcePath")) a.getString("sourcePath") else "",
                            createdAt = if (a.has("createdAt")) a.getLong("createdAt") else System.currentTimeMillis()
                        ))
                    }
                }

                
                val highlights = mutableListOf<StoryHighlightEntity>()
                if (root.has("highlights")) {
                    val arr = root.getJSONArray("highlights")
                    for (i in 0 until arr.length()) {
                        val h = arr.getJSONObject(i)
                        highlights.add(StoryHighlightEntity(
                            name = if (h.has("name")) h.getString("name") else "",
                            coverPath = if (h.has("coverPath")) h.getString("coverPath") else ""
                        ))
                    }
                }

                MemoirBackupPayload(
                    profileName = profileName,
                    username = username,
                    profileBio = profileBio,
                    posts = posts,
                    stories = stories,
                    albums = albums,
                    highlights = highlights
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}
