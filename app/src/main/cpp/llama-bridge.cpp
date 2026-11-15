#include <jni.h>
#include <string>
#include <atomic>
#include <vector>
#include <fstream>
#include <android/log.h>

#define LOG_TAG "LlamaBridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

struct NativeContext {
    std::string modelPath;
    int contextSize;
    int threadCount;
    std::atomic<bool> cancelled{false};
};

extern "C" JNIEXPORT jboolean JNICALL
Java_com_studentreviewer_offline_ai_LlamaBridge_nativeValidateGguf(
    JNIEnv* env,
    jobject,
    jstring jPath
) {
    const char* path = env->GetStringUTFChars(jPath, nullptr);
    std::ifstream file(path, std::ios::binary);
    env->ReleaseStringUTFChars(jPath, path);

    if (!file.is_open()) {
        return JNI_FALSE;
    }

    uint32_t magic = 0;
    file.read(reinterpret_cast<char*>(&magic), sizeof(magic));
    if (magic != 0x46554747) {
        return JNI_FALSE;
    }

    uint32_t version = 0;
    file.read(reinterpret_cast<char*>(&version), sizeof(version));
    if (version != 2 && version != 3) {
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_studentreviewer_offline_ai_LlamaBridge_nativeLoadModel(
    JNIEnv* env,
    jobject,
    jstring jModelPath,
    jint nCtx,
    jint nThreads
) {
    const char* modelPath = env->GetStringUTFChars(jModelPath, nullptr);
    std::string pathStr(modelPath);
    env->ReleaseStringUTFChars(jModelPath, modelPath);

    std::ifstream file(pathStr, std::ios::binary);
    if (!file.is_open()) {
        return 0;
    }

    uint32_t magic = 0;
    file.read(reinterpret_cast<char*>(&magic), sizeof(magic));
    if (magic != 0x46554747) {
        return 0;
    }

    auto* nativeCtx = new NativeContext();
    nativeCtx->modelPath = pathStr;
    nativeCtx->contextSize = nCtx;
    nativeCtx->threadCount = nThreads;
    nativeCtx->cancelled.store(false);

    return reinterpret_cast<jlong>(nativeCtx);
}

extern "C" JNIEXPORT void JNICALL
Java_com_studentreviewer_offline_ai_LlamaBridge_nativeFreeModel(
    JNIEnv*,
    jobject,
    jlong handle
) {
    auto* nativeCtx = reinterpret_cast<NativeContext*>(handle);
    if (nativeCtx) {
        delete nativeCtx;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_studentreviewer_offline_ai_LlamaBridge_nativeInterrupt(
    JNIEnv*,
    jobject,
    jlong handle
) {
    auto* nativeCtx = reinterpret_cast<NativeContext*>(handle);
    if (nativeCtx) {
        nativeCtx->cancelled.store(true);
    }
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_studentreviewer_offline_ai_LlamaBridge_nativeGenerate(
    JNIEnv* env,
    jobject,
    jlong handle,
    jstring jPrompt,
    jstring
) {
    auto* nativeCtx = reinterpret_cast<NativeContext*>(handle);
    if (!nativeCtx) {
        return env->NewStringUTF("");
    }

    const char* prompt = env->GetStringUTFChars(jPrompt, nullptr);
    std::string promptStr(prompt);
    env->ReleaseStringUTFChars(jPrompt, prompt);

    if (nativeCtx->cancelled.load()) {
        return env->NewStringUTF("");
    }

    std::string mockResponse = "{\"status\": \"success\"}";
    return env->NewStringUTF(mockResponse.c_str());
}
