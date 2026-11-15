package com.studentreviewer.offline.exam

import com.studentreviewer.offline.data.dao.ExamDao
import com.studentreviewer.offline.data.entity.ExamResponseEntity
import com.studentreviewer.offline.data.entity.ExamSessionEntity
import com.studentreviewer.offline.data.entity.QuestionEntity
import com.studentreviewer.offline.data.entity.QuestionOptionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Random

data class ExamQuestionItem(
    val question: QuestionEntity,
    val options: List<String>,
    var selectedAnswer: String = "",
    var isMarkedForReview: Boolean = false,
    var timeSpentSeconds: Int = 0
)

data class TopicScore(
    val topic: String,
    val total: Int,
    val correct: Int,
    val percentage: Float
)

data class ExamEvaluation(
    val examSessionId: Long,
    val totalQuestions: Int,
    val correctCount: Int,
    val incorrectCount: Int,
    val scorePercentage: Float,
    val isPassed: Boolean,
    val durationSeconds: Int,
    val topicBreakdown: List<TopicScore>,
    val responseEntities: List<ExamResponseEntity>
)

class ExamEngine {
    fun prepareExam(
        questions: List<QuestionEntity>,
        optionsMap: Map<Long, List<QuestionOptionEntity>>,
        questionLimit: Int = 20,
        shuffleQuestions: Boolean = true,
        shuffleOptions: Boolean = true,
        random: Random = Random()
    ): List<ExamQuestionItem> {
        val selected = if (shuffleQuestions) {
            questions.shuffled(random).take(questionLimit)
        } else {
            questions.take(questionLimit)
        }

        return selected.map { q ->
            val opts = optionsMap[q.id]?.map { it.optionText } ?: emptyList()
            val finalOptions = if (shuffleOptions && opts.isNotEmpty()) {
                opts.shuffled(random)
            } else {
                opts
            }
            ExamQuestionItem(
                question = q,
                options = finalOptions
            )
        }
    }

    suspend fun evaluateAndPersist(
        examDao: ExamDao,
        title: String,
        durationMinutes: Int,
        timeElapsedSeconds: Int,
        items: List<ExamQuestionItem>
    ): ExamEvaluation = withContext(Dispatchers.IO) {
        var correctCount = 0
        val topicMap = mutableMapOf<String, Pair<Int, Int>>()

        val responseEntities = mutableListOf<ExamResponseEntity>()

        for (item in items) {
            val isCorrect = item.selectedAnswer.trim().equals(item.question.correctAnswer.trim(), ignoreCase = true)
            if (isCorrect) {
                correctCount++
            }

            val topic = item.question.explanation.substringBefore('.').takeIf { it.isNotBlank() } ?: "General"
            val current = topicMap.getOrDefault(topic, Pair(0, 0))
            val updatedTotal = current.first + 1
            val updatedCorrect = if (isCorrect) current.second + 1 else current.second
            topicMap[topic] = Pair(updatedTotal, updatedCorrect)
        }

        val total = items.size
        val incorrectCount = total - correctCount
        val scorePercentage = if (total > 0) (correctCount.toFloat() / total.toFloat()) * 100f else 0f
        val isPassed = scorePercentage >= 70f

        val sessionEntity = ExamSessionEntity(
            title = title,
            totalQuestions = total,
            durationMinutes = durationMinutes,
            scorePercentage = scorePercentage,
            correctCount = correctCount,
            incorrectCount = incorrectCount,
            startedAt = System.currentTimeMillis() - (timeElapsedSeconds * 1000L),
            submittedAt = System.currentTimeMillis(),
            isCompleted = true
        )

        val insertedSessionId = examDao.insertExamSession(sessionEntity)

        for (item in items) {
            val isCorrect = item.selectedAnswer.trim().equals(item.question.correctAnswer.trim(), ignoreCase = true)
            val resp = ExamResponseEntity(
                examSessionId = insertedSessionId,
                questionId = item.question.id,
                questionStem = item.question.stem,
                selectedAnswer = item.selectedAnswer,
                correctAnswer = item.question.correctAnswer,
                isCorrect = isCorrect,
                isMarkedForReview = item.isMarkedForReview,
                timeSpentSeconds = item.timeSpentSeconds
            )
            responseEntities.add(resp)
        }

        examDao.insertResponses(responseEntities)

        val topicBreakdowns = topicMap.map { (topic, pair) ->
            TopicScore(
                topic = topic,
                total = pair.first,
                correct = pair.second,
                percentage = if (pair.first > 0) (pair.second.toFloat() / pair.first.toFloat()) * 100f else 0f
            )
        }

        ExamEvaluation(
            examSessionId = insertedSessionId,
            totalQuestions = total,
            correctCount = correctCount,
            incorrectCount = incorrectCount,
            scorePercentage = scorePercentage,
            isPassed = isPassed,
            durationSeconds = timeElapsedSeconds,
            topicBreakdown = topicBreakdowns,
            responseEntities = responseEntities
        )
    }
}
