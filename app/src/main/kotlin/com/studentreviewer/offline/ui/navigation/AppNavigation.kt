package com.studentreviewer.offline.ui.navigation

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.studentreviewer.offline.analytics.MasteryCalculator
import com.studentreviewer.offline.backup.BackupManager
import com.studentreviewer.offline.core.di.AppContainer
import com.studentreviewer.offline.data.entity.FlashcardEntity
import com.studentreviewer.offline.data.entity.QuestionEntity
import com.studentreviewer.offline.exam.ExamEngine
import com.studentreviewer.offline.exam.ExamEvaluation
import com.studentreviewer.offline.exam.ExamQuestionItem
import com.studentreviewer.offline.study.StudySessionManager
import com.studentreviewer.offline.ui.home.HomeScreen
import com.studentreviewer.offline.ui.library.LibraryScreen
import com.studentreviewer.offline.ui.practice.ActiveExamScreen
import com.studentreviewer.offline.ui.practice.ExamResultScreen
import com.studentreviewer.offline.ui.practice.PracticeHomeScreen
import com.studentreviewer.offline.ui.progress.ProgressHomeScreen
import com.studentreviewer.offline.ui.settings.ModelManagementScreen
import com.studentreviewer.offline.ui.settings.SettingsScreen
import com.studentreviewer.offline.ui.study.ActiveRecallScreen
import com.studentreviewer.offline.ui.study.FeynmanScreen
import com.studentreviewer.offline.ui.study.FlashcardStudyScreen
import com.studentreviewer.offline.ui.study.StudyHomeScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun MainAppNavigation(
    container: AppContainer,
    currentTheme: String,
    onThemeChange: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val database = container.database
    val documentImportManager = container.documentImportManager
    val questionEngine = container.questionEngine
    val modelManager = container.modelManager
    val backupManager = remember { BackupManager(database) }
    val examEngine = remember { ExamEngine() }
    val studySessionManager = remember { StudySessionManager(database.studyDao()) }

    val bottomNavItems = listOf(
        Screen.Home,
        Screen.Library,
        Screen.Study,
        Screen.Practice,
        Screen.Progress
    )

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    val materials by database.materialDao().getAllMaterials().collectAsState(initial = emptyList())
    val reviewSets by database.reviewSetDao().getAllReviewSets().collectAsState(initial = emptyList())
    val dueFlashcards by database.studyDao().getDueFlashcards(System.currentTimeMillis()).collectAsState(initial = emptyList())
    val dueFlashcardsCount by database.studyDao().getDueFlashcardCount(System.currentTimeMillis()).collectAsState(initial = 0)
    val examSessions by database.examDao().getAllExamSessions().collectAsState(initial = emptyList())
    val avgScore by database.examDao().getAverageExamScore().collectAsState(initial = 0f)
    val completedExamsCount by database.examDao().getCompletedExamCount().collectAsState(initial = 0)
    val allQuestionsList by database.questionDao().getAllQuestions().collectAsState(initial = emptyList())
    val modelInfo by modelManager.modelInfo.collectAsState()

    var activeExamQuestions by remember { mutableStateOf<List<ExamQuestionItem>>(emptyList()) }
    var activeExamTitle by remember { mutableStateOf("Mock Examination") }
    var activeExamDurationMinutes by remember { mutableStateOf(15) }
    var lastExamEvaluation by remember { mutableStateOf<ExamEvaluation?>(null) }

    var activeStudyFlashcards by remember { mutableStateOf<List<FlashcardEntity>>(emptyList()) }
    var activeStudyTitle by remember { mutableStateOf("Flashcard Review") }
    var activeRecallQuestions by remember { mutableStateOf<List<QuestionEntity>>(emptyList()) }

    var feynmanConcept by remember { mutableStateOf("Concept") }
    var feynmanDefinition by remember { mutableStateOf("") }
    var feynmanKeywords by remember { mutableStateOf<List<String>>(emptyList()) }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val result = documentImportManager.importDocument(uri, null)
                result.onSuccess { material ->
                    Toast.makeText(context, "Imported ${material.title} (${material.totalChunks} chunks)", Toast.LENGTH_SHORT).show()
                    val rsId = questionEngine.generateAndPersist(
                        database = database,
                        materialId = material.id,
                        subjectId = material.subjectId,
                        title = "${material.title} Reviewer"
                    )
                    Toast.makeText(context, "Generated questions and flashcards deck.", Toast.LENGTH_SHORT).show()
                }.onFailure { err ->
                    Toast.makeText(context, "Import failed: ${err.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val modelPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val res = modelManager.importModelFromUri(uri)
                res.onSuccess {
                    Toast.makeText(context, "Model imported successfully.", Toast.LENGTH_SHORT).show()
                }.onFailure { err ->
                    Toast.makeText(context, "Model import failed: ${err.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val backupExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    backupManager.exportBackup(out)
                        .onSuccess { Toast.makeText(context, "Backup saved successfully.", Toast.LENGTH_SHORT).show() }
                        .onFailure { Toast.makeText(context, "Backup failed: ${it.message}", Toast.LENGTH_LONG).show() }
                }
            }
        }
    }

    val backupRestoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                context.contentResolver.openInputStream(uri)?.use { inp ->
                    backupManager.importBackup(inp)
                        .onSuccess { Toast.makeText(context, "Restored $it records.", Toast.LENGTH_SHORT).show() }
                        .onFailure { Toast.makeText(context, "Restore failed: ${it.message}", Toast.LENGTH_LONG).show() }
                }
            }
        }
    }

    when (currentScreen) {
        Screen.Settings -> {
            SettingsScreen(
                currentTheme = currentTheme,
                onThemeChange = onThemeChange,
                onModelManagementClick = { currentScreen = Screen.ModelManagement },
                onBackupClick = { backupExportLauncher.launch("offline_reviewer_backup_${System.currentTimeMillis()}.zip") },
                onRestoreClick = { backupRestoreLauncher.launch(arrayOf("application/zip", "application/octet-stream")) },
                onBackClick = { currentScreen = Screen.Home }
            )
        }

        Screen.ModelManagement -> {
            ModelManagementScreen(
                modelInfo = modelInfo,
                onImportModelClick = { modelPickerLauncher.launch(arrayOf("*/*")) },
                onLoadModelClick = {
                    val loaded = modelManager.loadModel()
                    if (loaded) {
                        Toast.makeText(context, "Model loaded into memory.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Model load prevented: check memory budget.", Toast.LENGTH_LONG).show()
                    }
                },
                onUnloadModelClick = {
                    modelManager.unloadModel()
                    Toast.makeText(context, "Model unloaded.", Toast.LENGTH_SHORT).show()
                },
                onDeleteModelClick = {
                    modelManager.deleteModel()
                    Toast.makeText(context, "Model deleted.", Toast.LENGTH_SHORT).show()
                },
                onNavigateBack = { currentScreen = Screen.Settings }
            )
        }

        Screen.FlashcardStudy -> {
            FlashcardStudyScreen(
                deckTitle = activeStudyTitle,
                flashcards = activeStudyFlashcards,
                onRateCard = { card, rating ->
                    scope.launch {
                        studySessionManager.recordReview(0L, card, rating)
                    }
                },
                onNavigateBack = { currentScreen = Screen.Study }
            )
        }

        Screen.ActiveRecall -> {
            ActiveRecallScreen(
                questions = activeRecallQuestions,
                onComplete = { currentScreen = Screen.Study },
                onNavigateBack = { currentScreen = Screen.Study }
            )
        }

        Screen.Feynman -> {
            FeynmanScreen(
                conceptTitle = feynmanConcept,
                referenceDefinition = feynmanDefinition,
                keyKeywords = feynmanKeywords,
                onComplete = { currentScreen = Screen.Study },
                onNavigateBack = { currentScreen = Screen.Study }
            )
        }

        Screen.ActiveExam -> {
            ActiveExamScreen(
                examTitle = activeExamTitle,
                durationMinutes = activeExamDurationMinutes,
                examQuestions = activeExamQuestions,
                onSubmitExam = { timeSpentSeconds ->
                    scope.launch {
                        val eval = examEngine.evaluateAndPersist(
                            examDao = database.examDao(),
                            title = activeExamTitle,
                            durationMinutes = activeExamDurationMinutes,
                            timeElapsedSeconds = timeSpentSeconds,
                            items = activeExamQuestions
                        )
                        lastExamEvaluation = eval
                        currentScreen = Screen.ExamResult
                    }
                },
                onCancelExam = { currentScreen = Screen.Practice }
            )
        }

        Screen.ExamResult -> {
            lastExamEvaluation?.let { evaluation ->
                ExamResultScreen(
                    evaluation = evaluation,
                    onDone = { currentScreen = Screen.Practice }
                )
            } ?: run {
                currentScreen = Screen.Practice
            }
        }

        else -> {
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        bottomNavItems.forEach { screen ->
                            NavigationBarItem(
                                icon = {
                                    screen.icon?.let { Icon(it, contentDescription = screen.title) }
                                },
                                label = { Text(screen.title) },
                                selected = currentScreen == screen,
                                onClick = { currentScreen = screen }
                            )
                        }
                    }
                }
            ) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                    when (currentScreen) {
                        Screen.Home -> HomeScreen(
                            dueReviewsCount = dueFlashcardsCount,
                            totalMaterialsCount = materials.size,
                            totalQuestionsCount = allQuestionsList.size,
                            onImportClick = { documentPickerLauncher.launch(arrayOf("*/*")) },
                            onStartStudyClick = {
                                if (dueFlashcards.isNotEmpty()) {
                                    activeStudyFlashcards = dueFlashcards
                                    activeStudyTitle = "Due Flashcards (${dueFlashcards.size})"
                                    currentScreen = Screen.FlashcardStudy
                                } else {
                                    currentScreen = Screen.Study
                                }
                            },
                            onSettingsClick = { currentScreen = Screen.Settings }
                        )

                        Screen.Library -> LibraryScreen(
                            materials = materials,
                            reviewSets = reviewSets,
                            onImportClick = { documentPickerLauncher.launch(arrayOf("*/*")) },
                            onMaterialClick = { matId ->
                                scope.launch {
                                    val mat = materials.firstOrNull { it.id == matId }
                                    if (mat != null) {
                                        questionEngine.generateAndPersist(
                                            database = database,
                                            materialId = mat.id,
                                            subjectId = mat.subjectId,
                                            title = "${mat.title} Reviewer"
                                        )
                                        Toast.makeText(context, "Generated questions from ${mat.title}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onReviewSetClick = { rsId ->
                                scope.launch {
                                    val rs = reviewSets.firstOrNull { it.id == rsId }
                                    val cards = database.studyDao().getFlashcardsForSet(rsId).first()
                                    if (cards.isNotEmpty()) {
                                        activeStudyFlashcards = cards
                                        activeStudyTitle = rs?.title ?: "Flashcard Review"
                                        currentScreen = Screen.FlashcardStudy
                                    } else {
                                        val questions = database.questionDao().getQuestionsForSet(rsId).first()
                                        if (questions.isNotEmpty()) {
                                            activeRecallQuestions = questions
                                            currentScreen = Screen.ActiveRecall
                                        }
                                    }
                                }
                            }
                        )

                        Screen.Study -> StudyHomeScreen(
                            dueCount = dueFlashcardsCount,
                            onMethodSelect = { method ->
                                when (method) {
                                    "FLASHCARDS" -> {
                                        scope.launch {
                                            val cards = if (dueFlashcards.isNotEmpty()) dueFlashcards else database.studyDao().getDueFlashcards(System.currentTimeMillis() + 86400000000L).first()
                                            activeStudyFlashcards = cards
                                            activeStudyTitle = "Flashcard Session"
                                            currentScreen = Screen.FlashcardStudy
                                        }
                                    }
                                    "ACTIVE_RECALL" -> {
                                        scope.launch {
                                            val allQuestions = database.questionDao().getAllQuestions().first()
                                            activeRecallQuestions = allQuestions
                                            currentScreen = Screen.ActiveRecall
                                        }
                                    }
                                    "FEYNMAN" -> {
                                        scope.launch {
                                            val cards = database.studyDao().getDueFlashcards(System.currentTimeMillis() + 86400000000L).first()
                                            if (cards.isNotEmpty()) {
                                                val chosen = cards.first()
                                                feynmanConcept = chosen.frontText
                                                feynmanDefinition = chosen.backText
                                                feynmanKeywords = chosen.backText.split(' ').filter { it.length > 5 }.take(4)
                                            } else {
                                                feynmanConcept = "Process Scheduling"
                                                feynmanDefinition = "Method by which work is assigned to resources that complete the work."
                                                feynmanKeywords = listOf("Method", "assigned", "resources", "complete")
                                            }
                                            currentScreen = Screen.Feynman
                                        }
                                    }
                                }
                            }
                        )

                        Screen.Practice -> PracticeHomeScreen(
                            pastExams = examSessions,
                            onStartExamClick = { count, timeMin ->
                                scope.launch {
                                    val questions = database.questionDao().getAllQuestions().first()
                                    if (questions.isNotEmpty()) {
                                        val optionsMap = mutableMapOf<Long, List<com.studentreviewer.offline.data.entity.QuestionOptionEntity>>()
                                        for (q in questions) {
                                            optionsMap[q.id] = database.questionDao().getOptionsForQuestion(q.id)
                                        }
                                        activeExamQuestions = examEngine.prepareExam(
                                            questions = questions,
                                            optionsMap = optionsMap,
                                            questionLimit = count
                                        )
                                        activeExamTitle = "Practice Examination ($count Questions)"
                                        activeExamDurationMinutes = timeMin
                                        currentScreen = Screen.ActiveExam
                                    } else {
                                        Toast.makeText(context, "Import documents in Library first to generate exam questions.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        )

                        Screen.Progress -> {
                            val allCards by database.studyDao().getDueFlashcards(System.currentTimeMillis() + 86400000000L).collectAsState(initial = emptyList())
                            val allQuestions by database.questionDao().getAllQuestions().collectAsState(initial = emptyList())
                            val analytics = MasteryCalculator.calculateOverallAnalytics(allCards, allQuestions)

                            ProgressHomeScreen(
                                totalAnswered = allQuestions.sumOf { it.timesAnswered } + allCards.sumOf { it.repetitionNumber },
                                overallAccuracy = avgScore ?: 0f,
                                completedExamsCount = completedExamsCount,
                                analytics = analytics
                            )
                        }

                        else -> {}
                    }
                }
            }
        }
    }
}
