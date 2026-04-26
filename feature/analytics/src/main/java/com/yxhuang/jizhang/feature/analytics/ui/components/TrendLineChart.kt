package com.yxhuang.jizhang.feature.analytics.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yxhuang.jizhang.feature.analytics.engine.TrendPoint

@Composable
fun TrendLineChart(
    data: List<TrendPoint>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        Text(
            text = "暂无趋势数据",
            style = MaterialTheme.typography.bodyMedium,
            modifier = modifier.padding(16.dp)
        )
        return
    }

    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            text = "日支出趋势",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Canvas(modifier = Modifier.fillMaxWidth().height(120.dp).padding(vertical = 8.dp)) {
            val maxAmount = data.maxOfOrNull { it.amount } ?: 1.0
            val stepX = size.width / (data.size.coerceAtLeast(2) - 1).coerceAtLeast(1)

            // Draw line
            val points = data.mapIndexed { index, point ->
                val x = index * stepX
                val y = size.height - ((point.amount / maxAmount) * size.height).toFloat()
                Offset(x, y)
            }

            for (i in 0 until points.size - 1) {
                drawLine(
                    color = Color(0xFF2196F3),
                    start = points[i],
                    end = points[i + 1],
                    strokeWidth = 3f
                )
            }

            // Draw points
            points.forEach { point ->
                drawCircle(
                    color = Color(0xFF2196F3),
                    radius = 4f,
                    center = point
                )
            }
        }
    }
}
