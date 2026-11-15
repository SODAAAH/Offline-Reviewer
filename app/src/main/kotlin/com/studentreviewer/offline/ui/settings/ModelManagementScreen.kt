package com.studentreviewer.offline.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studentreviewer.offline.ai.ModelInfo
import com.studentreviewer.offline.ai.ModelStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelManagementScreen(
    modelInfo: ModelInfo,
    onImportModelClick: () -> Unit,
    onLoadModelClick: () -> Unit,
    onUnloadModelClick: () -> Unit,
    onDeleteModelClick: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val sizeMb = modelInfo.fileSize / (1024 * 1024)
    val freeRamMb = modelInfo.availableRamBytes / (1024 * 1024)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Offline Model Configuration") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "MODEL SPECIFICATION",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Qwen3.5-2B (Q4_K_M)",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Surface(
                            color = when (modelInfo.status) {
                                ModelStatus.LOADED -> Color(0xFFDCFCE7)
                                ModelStatus.READY -> MaterialTheme.colorScheme.primaryContainer
                                ModelStatus.LOW_MEMORY -> Color(0xFFFEF3C7)
                                ModelStatus.ERROR -> Color(0xFFFEE2E2)
                                ModelStatus.NOT_INSTALLED -> MaterialTheme.colorScheme.surface
                            },
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = modelInfo.status.name.replace('_', ' '),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = when (modelInfo.status) {
                                    ModelStatus.LOADED -> Color(0xFF15803D)
                                    ModelStatus.READY -> MaterialTheme.colorScheme.onPrimaryContainer
                                    ModelStatus.LOW_MEMORY -> Color(0xFFB45309)
                                    ModelStatus.ERROR -> Color(0xFFB91C1C)
                                    ModelStatus.NOT_INSTALLED -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Disk Size", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text(if (sizeMb > 0) "${sizeMb} MB" else "Not installed", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                        Column {
                            Text("Quantization", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text(modelInfo.quantization ?: "Q4_K_M", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                        Column {
                            Text("Available RAM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text("${freeRamMb} MB", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                    }

                    if (modelInfo.errorMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.padding(start = 8.dp))
                            Text(
                                text = modelInfo.errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            when (modelInfo.status) {
                ModelStatus.NOT_INSTALLED -> {
                    Button(
                        onClick = onImportModelClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.padding(start = 8.dp))
                        Text("Import GGUF File via Storage Access")
                    }
                }

                ModelStatus.READY -> {
                    Button(
                        onClick = onLoadModelClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Memory, contentDescription = null)
                        Spacer(modifier = Modifier.padding(start = 8.dp))
                        Text("Load Model into RAM")
                    }

                    OutlinedButton(
                        onClick = onDeleteModelClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.padding(start = 8.dp))
                        Text("Delete Model File")
                    }
                }

                ModelStatus.LOADED -> {
                    Button(
                        onClick = onUnloadModelClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.padding(start = 8.dp))
                        Text("Unload Model (Reclaim RAM)")
                    }
                }

                ModelStatus.LOW_MEMORY -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFEF3C7)
                        )
                    ) {
                        Row(modifier = Modifier.padding(16.dp)) {
                            Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color(0xFFB45309))
                            Spacer(modifier = Modifier.padding(start = 12.dp))
                            Text(
                                text = "Device free memory (${freeRamMb}MB) is below the 2.5GB threshold required for safe inference. The application will continue using the rule-based question generator.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF92400E)
                            )
                        }
                    }
                }

                ModelStatus.ERROR -> {
                    Button(
                        onClick = onImportModelClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Re-import Model")
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.padding(start = 12.dp))
                    Column {
                        Text(
                            text = "Dual-Engine Architecture",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "This application operates 100% offline. The default deterministic algorithmic engine generates all questions, definitions, and flashcards instantly without requiring any AI model. The GGUF model is purely optional for advanced phrasing.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
