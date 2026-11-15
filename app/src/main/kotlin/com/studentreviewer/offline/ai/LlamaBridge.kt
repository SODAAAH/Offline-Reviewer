package com.studentreviewer.offline.ai

class LlamaBridge {
    private var isNativeLoaded = false

    init {
        try {
            System.loadLibrary("llama_jni")
            isNativeLoaded = true
        } catch (_: Throwable) {
            isNativeLoaded = false
        }
    }

    fun isAvailable(): Boolean = isNativeLoaded

    external fun nativeValidateGguf(path: String): Boolean
    external fun nativeLoadModel(modelPath: String, contextSize: Int, threadCount: Int): Long
    external fun nativeFreeModel(handle: Long)
    external fun nativeInterrupt(handle: Long)
    external fun nativeGenerate(handle: Long, prompt: String, grammar: String?): String
}
