package com.example.spaceplan.ui.about

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.spaceplan.ui.components.ScreenHeader
import com.example.spaceplan.ui.components.SectionLabel

private val FEATURES = listOf(
    "Fit Count", "Even Distribution", "Gap Calculation", "Remaining Space", "Edge Gaps", "Multiple Units",
    "Visual Layout", "Calculation Breakdown", "History", "Copy Results", "Offline",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(Modifier.widthIn(max = 720.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ScreenHeader("SpacePlan", "Version 1.0")
            Text(
                "An offline utility for calculating how many equally sized objects fit within a given space " +
                    "and how to distribute them evenly.",
                style = MaterialTheme.typography.bodyLarge,
            )
            SectionLabel("Features")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FEATURES.forEach { feature ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.padding(vertical = 4.dp),
                    ) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(feature, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
            SectionLabel("How it works")
            Text(
                "Without edge gaps, N objects need N × size + (N − 1) × gap. With edge gaps, two edge gaps are added. " +
                    "Even Distribution divides the space left after the objects into N − 1 equal gaps, or N + 1 when edge gaps are equal. " +
                    "All calculations run in millimeters with exact decimal arithmetic; values are rounded only for display.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Text(
                        "SpacePlan performs mathematical layout calculations. Verify physical dimensions before cutting, " +
                            "drilling, installation, or permanent placement.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "It does not assess structural safety, engineering suitability, or building-code compliance, " +
                            "and cannot guarantee a physical fit.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                "Works fully offline. No accounts, no network access, no tracking.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }
    }
}
