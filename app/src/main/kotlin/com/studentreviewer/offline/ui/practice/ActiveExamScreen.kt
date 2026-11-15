package com.studentreviewer.offline.ui.practice

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studentreviewer.offline.exam.ExamQuestionItem
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ActiveExamScreen(
    examTitle: String,
    durationMinutes: Int,
    examQuestions: List<ExamQuestionItem>,
    onSubmitExam: (timeSpentSeconds: Int) -> Unit,
    onCancelExam: () -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    var secondsRemaining by remember { mutableIntStateOf(durationMinutes * 60) }
    var timeElapsedSeconds by remember { mutableIntStateOf(0) }
    var isSubmitDialogOpen by remember { mutableStateOf(false) }
    var isNavigatorSheetOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (secondsRemaining > 0) {
            delay(1000L)
            secondsRemaining--
            timeElapsedSeconds++
        }
        if (secondsRemaining == 0) {
            onSubmitExam(timeElapsedSeconds)
        }
    }

    val minutes = secondsRemaining / 60
    val seconds = secondsRemaining % 60
    val timerText = String.format("%02d:%02d", minutes, seconds)
    val isTimerLow = secondsRemaining < 120

    if (examQuestions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No questions in this examination.")
        }
        return
    }

    val currentItem = examQuestions[currentIndex]

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(examTitle, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Question ${currentIndex + 1} of ${examQuestions.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onCancelExam) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Exit Exam")
                    }
                },
                actions = {
                    Surface(
                        color = if (isTimerLow) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isTimerLow) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.padding(start = 4.dp))
                            Text(
                                text = timerText,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isTimerLow) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = { currentItem.isMarkedForReview = !currentItem.isMarkedForReview }) {
                        Icon(
                            imageVector = if (currentItem.isMarkedForReview) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Flag for review",
                            tint = if (currentItem.isMarkedForReview) Color(0xFFD97706) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = { isNavigatorSheetOpen = true }) {
                        Icon(imageVector = Icons.Default.GridView, contentDescription = "Question grid")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { if (currentIndex > 0) currentIndex-- },
                        enabled = currentIndex > 0
                    ) {
                        Text("Previous")
                    }

                    if (currentIndex < examQuestions.size - 1) {
                        Button(onClick = { currentIndex++ }) {
                            Text("Next")
                        }
                    } else {
                        Button(onClick = { isSubmitDialogOpen = true }) {
                            Text("Submit Exam")
                        }
                    }
                }
            }
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
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = currentItem.question.type.replace('_', ' '),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        if (currentItem.isMarkedForReview) {
                            Text(
                                text = "FLAGGED FOR REVIEW",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFD97706),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = currentItem.question.stem,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (currentItem.options.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (opt in currentItem.options) {
                        val isSelected = currentItem.selectedAnswer.equals(opt, ignoreCase = true)
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currentItem.selectedAnswer = opt
                                },
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier.size(20.dp),
                                    shape = MaterialTheme.shapes.extraSmall,
                                    border = BorderStroke(
                                        width = 2.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    ),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                ) {}
                                Spacer(modifier = Modifier.padding(start = 12.dp))
                                Text(
                                    text = opt,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = currentItem.selectedAnswer,
                    onValueChange = { currentItem.selectedAnswer = it },
                    label = { Text("Your Answer") },
                    placeholder = { Text("Type your answer here...") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (isSubmitDialogOpen) {
        val unansweredCount = examQuestions.count { it.selectedAnswer.isBlank() }
        AlertDialog(
            onDismissRequest = { isSubmitDialogOpen = false },
            title = { Text("Submit Examination") },
            text = {
                if (unansweredCount > 0) {
                    Text("You have $unansweredCount unanswered questions. Are you sure you want to submit your exam now?")
                } else {
                    Text("You have answered all questions. Submit your examination for scoring?")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSubmitDialogOpen = false
                        onSubmitExam(timeElapsedSeconds)
                    }
                ) {
                    Text("Confirm & Submit")
                }
            },
            dismissButton = {
                TextButton(onClick = { isSubmitDialogOpen = false }) {
                    Text("Continue Exam")
                }
            }
        )
    }

    if (isNavigatorSheetOpen) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { isNavigatorSheetOpen = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Question Overview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(16.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    examQuestions.forEachIndexed { idx, q ->
                        val isCurrent = idx == currentIndex
                        val isAnswered = q.selectedAnswer.isNotBlank()
                        val isFlagged = q.isMarkedForReview

                        Surface(
                            modifier = Modifier
                                .size(44.dp)
                                .clickable {
                                    currentIndex = idx
                                    isNavigatorSheetOpen = false
                                },
                            shape = MaterialTheme.shapes.small,
                            border = BorderStroke(
                                width = if (isCurrent) 2.dp else 1.dp,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            color = when {
                                isFlagged -> Color(0xFFFEF3C7)
                                isAnswered -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surface
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${idx + 1}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    color = when {
                                        isFlagged -> Color(0xFF92400E)
                                        isAnswered -> MaterialTheme.colorScheme.onPrimaryContainer
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
