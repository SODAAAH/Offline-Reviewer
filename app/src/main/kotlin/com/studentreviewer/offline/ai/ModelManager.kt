package com.studentreviewer.offline.ai

import android.app.ActivityManager
import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class ModelStatus {
    NOT_INSTALLED,
    READY,
    LOADED,
    LOW_MEMORY,
    ERROR
}

data class ModelInfo(
    val status: ModelStatus,
    val filePath: String? = null,
    val fileSize: Long = 0,
    val architecture: String? = null,
    val quantization: String? = null,
    val availableRamBytes: Long = 0,
    val requiredRamBytes: Long = 2_500_000_000L,
    val errorMessage: String? = null
)

class ModelManager(
    private val context: Context,
    private val llamaBridge: LlamaBridge = LlamaBridge()
) {
    private val _modelInfo = MutableStateFlow(ModelInfo(status = ModelStatus.NOT_INSTALLED))
    val modelInfo: StateFlow<ModelInfo> = _modelInfo.asStateFlow()

    private var nativeModelHandle: Long = 0

    init {
        refreshModelStatus()
    }

    fun getModelFile(): File {
        val dir = File(context.filesDir, "models")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "qwen3.5-2b-q4_k_m.gguf")
    }

    fun refreshModelStatus() {
        val file = getModelFile()
        val (hasRam, freeRam) = checkRamAvailability()

        if (!file.exists()) {
            _modelInfo.value = ModelInfo(
                status = ModelStatus.NOT_INSTALLED,
                availableRamBytes = freeRam
            )
            return
        }

        val metadata = GgufValidator.validateFile(file)
        if (!metadata.isValid) {
            _modelInfo.value = ModelInfo(
                status = ModelStatus.ERROR,
                filePath = file.absolutePath,
                fileSize = file.length(),
                availableRamBytes = freeRam,
                errorMessage = metadata.errorMessage
            )
            return
        }

        val currentStatus = if (nativeModelHandle != 0L) {
            ModelStatus.LOADED
        } else if (!hasRam) {
            ModelStatus.LOW_MEMORY
        } else {
            ModelStatus.READY
        }

        _modelInfo.value = ModelInfo(
            status = currentStatus,
            filePath = file.absolutePath,
            fileSize = file.length(),
            architecture = metadata.architecture,
            quantization = metadata.quantization,
            availableRamBytes = freeRam
        )
    }

    suspend fun importModelFromUri(uri: Uri): Result<ModelInfo> = withContext(Dispatchers.IO) {
        try {
            val destFile = getModelFile()
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(IllegalStateException("Failed to read from URI"))

            refreshModelStatus()
            Result.success(_modelInfo.value)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun loadModel(): Boolean {
        val file = getModelFile()
        if (!file.exists()) return false

        val (hasRam, freeRam) = checkRamAvailability()
        if (!hasRam) {
            _modelInfo.value = _modelInfo.value.copy(
                status = ModelStatus.LOW_MEMORY,
                errorMessage = "Insufficient free RAM ($freeRam bytes). Minimum 2.5GB required."
            )
            return false
        }

        if (llamaBridge.isAvailable()) {
            val threads = (Runtime.getRuntime().availableProcessors() - 2).coerceIn(2, 4)
            nativeModelHandle = llamaBridge.nativeLoadModel(file.absolutePath, 2048, threads)
            if (nativeModelHandle == 0L) {
                _modelInfo.value = _modelInfo.value.copy(
                    status = ModelStatus.ERROR,
                    errorMessage = "Failed to load model into native memory."
                )
                return false
            }
        }

        _modelInfo.value = _modelInfo.value.copy(status = ModelStatus.LOADED)
        return true
    }

    fun unloadModel() {
        if (nativeModelHandle != 0L && llamaBridge.isAvailable()) {
            llamaBridge.nativeFreeModel(nativeModelHandle)
            nativeModelHandle = 0L
        }
        refreshModelStatus()
    }

    fun deleteModel(): Boolean {
        unloadModel()
        val file = getModelFile()
        val deleted = if (file.exists()) file.delete() else true
        refreshModelStatus()
        return deleted
    }

    private fun checkRamAvailability(): Pair<Boolean, Long> {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return Pair(true, 4_000_000_000L)
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        val freeBytes = memInfo.availMem
        val requiredBytes = 2_500_000_000L
        val isSufficient = freeBytes >= requiredBytes && !memInfo.lowMemory
        return Pair(isSufficient, freeBytes)
    }
}
