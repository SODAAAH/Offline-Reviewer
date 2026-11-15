package com.studentreviewer.offline.ui.practice

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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
import com.studentreviewer.offline.exam.ExamEvaluation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamResultScreen(
    evaluation: ExamEvaluation,
    onDone: () -> Unit
) {
    val durationMinutes = evaluation.durationSeconds / 60
    val durationSeconds = evaluation.durationSeconds % 60
    val timeFormatted = "${durationMinutes}m ${durationSeconds}s"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Examination Results") }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = onDone,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Return to Practice")
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (evaluation.isPassed) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (evaluation.isPassed) "EXAMINATION PASSED" else "NEEDS IMPROVEMENT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (evaluation.isPassed) Color(0xFF15803D) else Color(0xFFB91C1C)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${String.format("%.1f", evaluation.scorePercentage)}%",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (evaluation.isPassed) Color(0xFF15803D) else Color(0xFFB91C1C)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${evaluation.correctCount} of ${evaluation.totalQuestions} questions correct",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Time elapsed: $timeFormatted",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            if (evaluation.topicBreakdown.isNotEmpty()) {
                item {
                    Text(
                        text = "Topic Mastery Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            for (t in evaluation.topicBreakdown) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = t.topic,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "${t.correct}/${t.total} (${String.format("%.0f", t.percentage)}%)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { t.percentage / 100f },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Question Review",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            itemsIndexed(evaluation.responseEntities) { index, resp ->
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (resp.isCorrect) Color(0xFF86EFAC) else Color(0xFFFCA5A5)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Question ${index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (resp.isCorrect) Icons.Default.Check else Icons.Default.Close,
                                    contentDescription = null,
                                    tint = if (resp.isCorrect) Color(0xFF16A34A) else MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.padding(start = 4.dp))
                                Text(
                                    text = if (resp.isCorrect) "Correct" else "Incorrect",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (resp.isCorrect) Color(0xFF16A34A) else MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = resp.questionStem,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Your Answer: ${if (resp.selectedAnswer.isBlank()) "(Unanswered)" else resp.selectedAnswer}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (resp.isCorrect) Color(0xFF16A34A) else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )

                        if (!resp.isCorrect) {
                            Text(
                                text = "Correct Answer: ${resp.correctAnswer}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
