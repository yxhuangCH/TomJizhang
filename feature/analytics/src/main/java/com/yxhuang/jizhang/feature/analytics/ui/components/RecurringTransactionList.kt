package com.yxhuang.jizhang.feature.analytics.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yxhuang.jizhang.feature.analytics.engine.RecurringFrequency
import com.yxhuang.jizhang.feature.analytics.engine.RecurringPattern
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun RecurringTransactionList(
    patterns: List<RecurringPattern>,
    modifier: Modifier = Modifier,
    onPatternClick: (RecurringPattern) -> Unit = {}
) {
    if (patterns.isEmpty()) {
        EmptyRecurringMessage(modifier = modifier)
        return
    }

    LazyColumn(modifier = modifier) {
        items(
            items = patterns,
            key = { it.merchant }
        ) { pattern ->
            RecurringPatternCard(
                pattern = pattern,
                onClick = { onPatternClick(pattern) }
            )
        }
    }
}

@Composable
private fun EmptyRecurringMessage(modifier: Modifier) {
    Text(
        text = "尚未发现周期性交易，持续记账后将自动识别",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun RecurringPatternCard(
    pattern: RecurringPattern,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pattern.merchant,
                        style = MaterialTheme.typography.titleMedium
                    )
                    pattern.category?.let { category ->
                        Text(
                            text = category,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "-${String.format("%.2f", pattern.averageAmount)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = frequencyLabel(pattern.frequency),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "上次: ${formatTimestamp(pattern.lastOccurrence)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(
                        text = "预计: ${formatTimestamp(pattern.nextEstimatedOccurrence)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            ConfidenceBar(confidence = pattern.confidence)
        }
    }
}

@Composable
private fun ConfidenceBar(confidence: Float) {
    val color = when {
        confidence >= 0.7f -> Color(0xFF4CAF50)
        confidence >= 0.4f -> Color(0xFFFFC107)
        else -> Color(0xFFFF9800)
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "置信度",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${(confidence * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { confidence.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

private fun frequencyLabel(frequency: RecurringFrequency): String = when (frequency) {
    RecurringFrequency.DAILY -> "每日"
    RecurringFrequency.WEEKLY -> "每周"
    RecurringFrequency.MONTHLY -> "每月"
}

private fun formatTimestamp(millis: Long): String {
    val instant = Instant.ofEpochMilli(millis)
    val localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
    return localDate.format(DateTimeFormatter.ofPattern("MM-dd"))
}
