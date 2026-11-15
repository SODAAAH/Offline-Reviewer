package com.studentreviewer.offline.generator.algorithmic

import com.studentreviewer.offline.content.ContentAnalyzer
import com.studentreviewer.offline.data.database.AppDatabase
import com.studentreviewer.offline.data.entity.DocumentChunkEntity
import com.studentreviewer.offline.data.entity.FlashcardEntity
import com.studentreviewer.offline.data.entity.QuestionEntity
import com.studentreviewer.offline.data.entity.QuestionOptionEntity
import com.studentreviewer.offline.data.entity.ReviewSetEntity
import com.studentreviewer.offline.generator.model.GeneratedFlashcard
import com.studentreviewer.offline.generator.model.GeneratedQuestion
import com.studentreviewer.offline.generator.validation.QuestionValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class GenerationResult(
    val questions: List<GeneratedQuestion>,
    val flashcards: List<GeneratedFlashcard>
)

class AlgorithmicQuestionEngine(
    private val contentAnalyzer: ContentAnalyzer = ContentAnalyzer(),
    private val definitionGenerator: DefinitionQuestionGenerator = DefinitionQuestionGenerator(),
    private val multipleChoiceGenerator: MultipleChoiceGenerator = MultipleChoiceGenerator(),
    private val trueFalseGenerator: TrueFalseGenerator = TrueFalseGenerator(),
    private val clozeGenerator: ClozeQuestionGenerator = ClozeQuestionGenerator(),
    private val matchingGenerator: MatchingQuestionGenerator = MatchingQuestionGenerator(),
    private val sequencingGenerator: SequencingQuestionGenerator = SequencingQuestionGenerator(),
    private val validator: QuestionValidator = QuestionValidator()
) {
    fun generate(chunks: List<DocumentChunkEntity>): GenerationResult {
        val analyzed = contentAnalyzer.analyze(chunks)
        val flashcards = definitionGenerator.generateFlashcards(analyzed)

        val candidateQuestions = mutableListOf<GeneratedQuestion>()
        candidateQuestions.addAll(multipleChoiceGenerator.generateQuestions(analyzed))
        candidateQuestions.addAll(trueFalseGenerator.generateQuestions(analyzed))
        candidateQuestions.addAll(definitionGenerator.generateQuestions(analyzed))
        candidateQuestions.addAll(clozeGenerator.generateQuestions(analyzed))
        candidateQuestions.addAll(matchingGenerator.generateQuestions(analyzed))
        candidateQuestions.addAll(sequencingGenerator.generateQuestions(analyzed))

        val validatedQuestions = mutableListOf<GeneratedQuestion>()
        for (candidate in candidateQuestions) {
            if (validator.isValid(candidate, validatedQuestions)) {
                validatedQuestions.add(candidate)
            }
        }

        return GenerationResult(
            questions = validatedQuestions,
            flashcards = flashcards
        )
    }

    suspend fun generateAndPersist(
        database: AppDatabase,
        materialId: Long,
        subjectId: Long?,
        title: String,
        description: String = ""
    ): Long = withContext(Dispatchers.IO) {
        val chunks = database.materialDao().getChunksListForMaterial(materialId)
        val result = generate(chunks)

        val reviewSet = ReviewSetEntity(
            subjectId = subjectId,
            title = title,
            description = description,
            questionCount = result.questions.size,
            createdAt = System.currentTimeMillis()
        )
        val reviewSetId = database.reviewSetDao().insertReviewSet(reviewSet)

        for (q in result.questions) {
            val qEntity = QuestionEntity(
                reviewSetId = reviewSetId,
                materialId = materialId,
                type = q.questionType,
                stem = q.stem,
                correctAnswer = q.correctAnswer,
                explanation = q.explanation,
                sourceExcerpt = q.sourceExcerpt,
                pageNumber = q.pageNumber,
                slideNumber = q.slideNumber,
                difficulty = q.difficultyScore,
                generationEngine = "ALGORITHMIC",
                validationStatus = "READY",
                createdAt = System.currentTimeMillis()
            )
            val questionId = database.questionDao().insertQuestion(qEntity)

            if (q.options.isNotEmpty()) {
                val optionEntities = q.options.mapIndexed { idx, optText ->
                    QuestionOptionEntity(
                        questionId = questionId,
                        optionText = optText,
                        isCorrect = optText.equals(q.correctAnswer, ignoreCase = true),
                        displayOrder = idx
                    )
                }
                database.questionDao().insertOptions(optionEntities)
            }
        }

        val flashcardEntities = result.flashcards.map { fc ->
            FlashcardEntity(
                reviewSetId = reviewSetId,
                frontText = fc.frontText,
                backText = fc.backText,
                explanation = fc.hint ?: "",
                sourceReference = fc.sourceExcerpt,
                repetitionNumber = 0,
                intervalDays = 1,
                easeFactor = 2.5f,
                nextReviewAt = System.currentTimeMillis(),
                lapsesCount = 0
            )
        }
        if (flashcardEntities.isNotEmpty()) {
            database.studyDao().insertFlashcards(flashcardEntities)
        }

        reviewSetId
    }
}
