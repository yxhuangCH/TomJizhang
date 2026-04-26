package com.yxhuang.jizhang.feature.analytics.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yxhuang.jizhang.feature.analytics.engine.CategoryBreakdown

@Composable
fun CategoryPieChart(
    data: List<CategoryBreakdown>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        Text(
            text = "暂无分类数据",
            style = MaterialTheme.typography.bodyMedium,
            modifier = modifier.padding(16.dp)
        )
        return
    }

    val colors = listOf(
        Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFFFF9800),
        Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF00BCD4),
        Color(0xFF795548), Color(0xFF607D8B), Color(0xFFFFC107)
    )

    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            text = "分类支出占比",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Canvas(modifier = Modifier.size(160.dp).align(Alignment.CenterHorizontally).padding(8.dp)) {
            val total = data.sumOf { it.amount }
            var startAngle = -90f
            data.forEachIndexed { index, item ->
                val sweep = if (total > 0) (item.amount / total * 360).toFloat() else 0f
                drawArc(
                    color = colors[index % colors.size],
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = true,
                    topLeft = Offset.Zero,
                    size = Size(size.width, size.height)
                )
                startAngle += sweep
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            data.forEachIndexed { index, item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(colors[index % colors.size], shape = CircleShape)
                    )
                    Text(
                        text = "${item.category}: %.0f元 (%.1f%%)".format(item.amount, item.percentage * 100),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}
