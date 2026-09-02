package com.example.glance

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.MainActivity
import com.example.R
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.data.database.MemoirDatabase
import com.example.utils.FileUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MemoirWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = MemoirDatabase.getDatabase(context)
        val dao = database.memoirDao()
        
        val allPosts = dao.getAllPostsList()
        val allStories = dao.getAllStoriesList()
        
        val today = Calendar.getInstance()
        val tDay = today.get(Calendar.DAY_OF_MONTH)
        val tMonth = today.get(Calendar.MONTH)
        val tYear = today.get(Calendar.YEAR)
        
        val candidates = mutableListOf<MemoirWidgetState>()
        
        fun addCandidate(createdAt: Long, path: String, caption: String) {
            val cal = Calendar.getInstance().apply { timeInMillis = createdAt }
            if (cal.get(Calendar.DAY_OF_MONTH) == tDay && cal.get(Calendar.MONTH) == tMonth && cal.get(Calendar.YEAR) < tYear) {
                candidates.add(
                    MemoirWidgetState(
                        photoPath = path,
                        caption = caption,
                        dateLabel = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(createdAt)),
                        yearsAgo = tYear - cal.get(Calendar.YEAR),
                        hasMemory = true
                    )
                )
            }
        }
        
        allPosts.forEach { addCandidate(it.createdAt, it.mediaPaths.firstOrNull() ?: "", it.caption) }
        allStories.forEach { addCandidate(it.createdAt, it.mediaPath, it.caption ?: "") }
        
        val state = candidates.randomOrNull() ?: MemoirWidgetState(hasMemory = false)
        
        val bitmap = if (state.hasMemory && state.photoPath.isNotBlank()) {
            loadWidgetBitmap(context, state.photoPath)
        } else null

        provideContent {
            GlanceTheme {
                WidgetContent(state = state, context = context, bitmap = bitmap)
            }
        }
    }

    private fun loadWidgetBitmap(context: Context, path: String): Bitmap? {
        return try {
            val file = FileUtils.getLocalFile(context, path)
            if (file.exists()) {
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(file.absolutePath, options)
                options.inSampleSize = calculateInSampleSize(options, 400, 400)
                options.inJustDecodeBounds = false
                BitmapFactory.decodeFile(file.absolutePath, options)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}

@Composable
private fun WidgetContent(state: MemoirWidgetState, context: Context, bitmap: Bitmap?) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color(0xFF121212)))
            .cornerRadius(16.dp)
            .clickable(actionStartActivity<MainActivity>()),
        contentAlignment = Alignment.TopStart
    ) {
        if (bitmap != null) {
            Image(
                provider = ImageProvider(bitmap),
                contentDescription = context.getString(R.string.widget_title_memory),
                contentScale = ContentScale.Crop,
                modifier = GlanceModifier.fillMaxSize()
            )
            
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .background(ColorProvider(Color(0x66000000)))
            ) {}
        } else {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(Color(0xFF2D1B4D)))
            ) {}
        }

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = GlanceModifier
                        .size(16.dp)
                        .background(ColorProvider(Color(0xFFE040FB)))
                        .cornerRadius(4.dp)
                ) {}
                
                Spacer(modifier = GlanceModifier.width(6.dp))
                Text(
                    text = "MEMOIR",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFE040FB)),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(6.dp))

            Text(
                text = if (state.hasMemory) context.getString(R.string.widget_title_memory) else context.getString(R.string.widget_title_welcome),
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = GlanceModifier.height(2.dp))

            Text(
                text = if (state.hasMemory)
                    context.getString(R.string.widget_desc_years_ago, state.yearsAgo, state.dateLabel)
                else
                    context.getString(R.string.widget_desc_empty),
                style = TextStyle(
                    color = ColorProvider(Color(0xEEFFFFFF)),
                    fontSize = 11.sp
                )
            )

            if (state.hasMemory && state.caption.isNotBlank()) {
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = "\"${state.caption.take(80)}${if (state.caption.length > 80) "…" else ""}\"",
                    style = TextStyle(
                        color = ColorProvider(Color(0xCCFFFFFF)),
                        fontSize = 10.sp,
                        fontStyle = androidx.glance.text.FontStyle.Italic
                    )
                )
            }
        }
    }
}
