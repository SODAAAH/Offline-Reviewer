package com.studentreviewer.offline.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Library : Screen("library", "Library", Icons.Default.CollectionsBookmark)
    object Study : Screen("study", "Study", Icons.AutoMirrored.Filled.MenuBook)
    object Practice : Screen("practice", "Practice", Icons.Default.Quiz)
    object Progress : Screen("progress", "Progress", Icons.Default.Assessment)

    object Settings : Screen("settings", "Settings")
    object ModelManagement : Screen("model_management", "Model Configuration")
    object FlashcardStudy : Screen("study_flashcards", "Flashcard Review")
    object ActiveRecall : Screen("study_active_recall", "Active Recall")
    object Feynman : Screen("study_feynman", "Feynman Technique")
    object ActiveExam : Screen("exam_active", "Examination")
    object ExamResult : Screen("exam_result", "Exam Results")
}
