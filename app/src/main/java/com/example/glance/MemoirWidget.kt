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





class MemoirWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        
        val state = MemoirWidgetState.load(context)
        provideContent {
            GlanceTheme {
                WidgetContent(state = state, context = context)
            }
        }
    }
}

@Composable
private fun WidgetContent(state: MemoirWidgetState, context: Context) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color(0xFF121212)))
            .cornerRadius(16.dp)
            .clickable(actionStartActivity<MainActivity>()),
        contentAlignment = Alignment.TopStart
    ) {
        if (state.photoPath.isNotBlank()) {
            
            Image(
                provider = ImageProvider(R.drawable.ic_memoir_widget_placeholder),
                contentDescription = context.getString(R.string.widget_title_memory),
                contentScale = ContentScale.Crop,
                modifier = GlanceModifier.fillMaxSize()
            )
            
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .background(ColorProvider(Color(0x99000000)))
            ) {}
        }

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    provider = ImageProvider(R.drawable.ic_memoir_widget_placeholder),
                    contentDescription = context.getString(R.string.app_name),
                    modifier = GlanceModifier.size(18.dp)
                )
                Spacer(modifier = GlanceModifier.width(4.dp))
                Text(
                    text = "MEMOIR",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFE040FB)),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(4.dp))

            
            Text(
                text = if (state.hasMemory) context.getString(R.string.widget_title_memory) else context.getString(R.string.widget_title_welcome),
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontSize = 13.sp,
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
                    color = ColorProvider(Color(0xCCFFFFFF)),
                    fontSize = 10.sp
                )
            )

            if (state.hasMemory && state.caption.isNotBlank()) {
                Spacer(modifier = GlanceModifier.height(2.dp))
                Text(
                    text = "\"${state.caption.take(60)}${if (state.caption.length > 60) "…" else ""}\"",
                    style = TextStyle(
                        color = ColorProvider(Color(0xAAFFFFFF)),
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}
