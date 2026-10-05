package com.example.spaceplan.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.spaceplan.data.HistoryRecord
import com.example.spaceplan.domain.EdgeGapMode
import com.example.spaceplan.domain.ResultText
import com.example.spaceplan.domain.SpacePlanMode
import com.example.spaceplan.ui.SpacePlanViewModel
import com.example.spaceplan.ui.components.ScreenHeader
import com.example.spaceplan.ui.components.SectionLabel
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryScreen(
    viewModel: SpacePlanViewModel,
    onOpen: (HistoryRecord) -> Unit,
    showMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val history by viewModel.history.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Row(Modifier.widthIn(max = 720.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                ScreenHeader("History", "Saved calculations · up to 50", Modifier.weight(1f))
                if (history.isNotEmpty()) {
                    TextButton(onClick = { confirmClear = true }) { Text("Clear all") }
                }
            }
        }
        if (history.isEmpty()) {
            item { EmptyHistory() }
        }
        items(history, key = { it.id }) { record ->
            HistoryItem(
                record = record,
                decimals = settings.decimalPlaces,
                date = dateFormat.format(Date(record.timestampMillis)),
                onOpen = { onOpen(record) },
                onDelete = {
                    viewModel.deleteHistory(record.id)
                    showMessage("Calculation deleted")
                },
                modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(),
            )
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear history?") },
            text = { Text("All saved calculations will be removed. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    viewModel.clearHistory()
                    showMessage("History cleared")
                }) { Text("Clear all") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun EmptyHistory() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            Icons.Outlined.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(40.dp),
        )
        Text("No saved calculations", style = MaterialTheme.typography.titleMedium)
        Text(
            "Use Save on the Calculator to keep a result here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HistoryItem(
    record: HistoryRecord,
    decimals: Int,
    date: String,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = remember(record.unit, decimals) { ResultText(record.unit, decimals) }
    val lines = describe(record, t)
    OutlinedCard(
        onClick = onOpen,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.semantics { contentDescription = "Open ${lines.joinToString(", ")}, saved $date" },
    ) {
        Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SectionLabel(record.mode.label)
                Spacer(Modifier.height(2.dp))
                Text(lines.first(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                lines.drop(1).forEach {
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
                Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete calculation")
            }
        }
    }
}

/** Headline result first, then the inputs. */
private fun describe(r: HistoryRecord, t: ResultText): List<String> = when (r.mode) {
    SpacePlanMode.FIT_COUNT -> listOfNotNull(
        when {
            r.doesNotFit -> "0 objects fit"
            r.remainingMm.signum() == 0 -> "${r.objectsFit} objects · exact fit"
            else -> "${r.objectsFit} objects · ${t.m(r.remainingMm)} remaining"
        },
        "${t.m(r.availableMm)} space",
        "${t.m(r.objectMm)} objects · ${t.m(r.gapMm)} gap",
        if (r.edgeGapMode == EdgeGapMode.EQUAL) "${t.m(r.edgeGapMm)} edge gaps" else null,
    )
    SpacePlanMode.EVEN_DISTRIBUTION -> listOf(
        when {
            r.doesNotFit -> "Does not fit"
            r.equalGapMm == null -> "No internal spacing"
            else -> "${t.m(r.equalGapMm)} gap" + if (r.edgeGapMode == EdgeGapMode.EQUAL) " · equal edges" else ""
        },
        "${t.m(r.availableMm)} space",
        "${r.objectCount} objects · ${t.m(r.objectMm)} each",
    )
}
