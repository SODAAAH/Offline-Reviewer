package com.studentreviewer.offline.core.di

import android.content.Context
import com.studentreviewer.offline.ai.ModelManager
import com.studentreviewer.offline.core.preferences.UserPreferences
import com.studentreviewer.offline.data.database.AppDatabase
import com.studentreviewer.offline.document.DocumentImportManager
import com.studentreviewer.offline.generator.algorithmic.AlgorithmicQuestionEngine

interface AppContainer {
    val database: AppDatabase
    val userPreferences: UserPreferences
    val documentImportManager: DocumentImportManager
    val questionEngine: AlgorithmicQuestionEngine
    val modelManager: ModelManager
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    override val database: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    override val userPreferences: UserPreferences by lazy {
        UserPreferences(context)
    }

    override val documentImportManager: DocumentImportManager by lazy {
        DocumentImportManager(context, database.materialDao())
    }

    override val questionEngine: AlgorithmicQuestionEngine by lazy {
        AlgorithmicQuestionEngine()
    }

    override val modelManager: ModelManager by lazy {
        ModelManager(context)
    }
}
