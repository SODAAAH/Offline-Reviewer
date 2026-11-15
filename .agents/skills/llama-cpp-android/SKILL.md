---
name: llama-cpp-android
description: Guide offline on-device LLM inference using llama.cpp via Android NDK and JNI with Qwen3.5-2B Q4_K_M GGUF, covering native builds, memory management, structured JSON generation, cancellation, and algorithmic fallback.
---

# llama.cpp Native Android Integration

## 1. Architectural Overview

This skill provides the blueprint for running optional, 100% offline local inference on Android devices using `llama.cpp` compiled as a native shared library (`.so`) linked through JNI.

Strict constraints:
- Official Model: Qwen3.5-2B (Q4_K_M quantization, GGUF format).
- Zero cloud AI APIs, remote endpoints, or background sync.
- Zero heavyweight runtime frameworks: do not use Ollama, PyTorch Mobile, ONNX Runtime, or full Python bridges.
- Pure NDK + JNI architecture: lightweight C++17/C++20 native wrapper around core `llama.cpp`.
- Algorithmic fallback: the application must remain 100% functional via deterministic rule-based parsing even if the local AI model is uninstalled or unsupported.

---

## 2. GGUF Model Architecture and Validation

### Model Specification
- Architecture: Qwen 3.5 / Qwen 2.5 dense transformer.
- Quantization: Q4_K_M (4-bit medium k-quantization, striking optimal balance between perplexity and RAM footprint).
- Typical File Size: ~1.4 GB to 1.6 GB.
- RAM Overhead: ~1.8 GB to 2.2 GB working set memory during inference.

### File Validation Pre-flight
Before loading into native memory, validate the file integrity:
1. File Existence & Read Permission: Ensure readable file descriptor via Android Storage Access Framework or application-internal storage.
2. GGUF Magic Header: Verify the first 4 bytes match `0x46554747` (`GGUF` in ASCII little-endian).
3. Version Check: Ensure GGUF version is 2 or 3.
4. Tensor & Metadata Inspection: Read metadata keys (`general.architecture`, `tokenizer.ggml.model`). Ensure architecture matches `qwen2` or `qwen3`.
5. Quantization Verification: Confirm tensor type represents `Q4_K_M` to prevent attempting to load unquantized FP16/FP32 models that cause immediate OOM.

---

## 3. Android NDK Build Configuration

### CMake Configuration (`CMakeLists.txt`)
Configure `llama.cpp` for 64-bit ARM (`arm64-v8a`):

```cmake
cmake_minimum_required(VERSION 3.22.1)
project(llama_android LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(LLAMA_BUILD_COMMON OFF CACHE BOOL "common" FORCE)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "tests" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "examples" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "server" FORCE)

if (${ANDROID_ABI} STREQUAL "arm64-v8a")
    add_compile_options(-march=armv8.2-a+fp16+dotprod)
    set(GGML_OPENMP ON CACHE BOOL "OpenMP" FORCE)
endif()

add_subdirectory(llama.cpp)

add_library(llama_jni SHARED
    llama-bridge.cpp
)

find_library(log-lib log)
find_library(android-lib android)

target_link_libraries(llama_jni
    llama
    ggml
    ${log-lib}
    ${android-lib}
)
```

### ABI Targeting
- Only target `arm64-v8a` (64-bit).
- Explicitly exclude `armeabi-v7a` (32-bit): 32-bit processes are capped at 2GB to 3GB address space and lack ARMv8.2 dot-product acceleration.
- In `app/build.gradle.kts`:

```kotlin
android {
    defaultConfig {
        ndk {
            abiFilters.clear()
            abiFilters.add("arm64-v8a")
        }
    }
}
```

---

## 4. JNI Native Bridge and Lifecycle Management

### Native Wrapper Responsibilities
1. Model Lifecycle:
   - `nativeLoadModel(modelPath: String, contextSize: Int, threadCount: Int): Long` -> returns native handle pointer.
   - `nativeFreeModel(modelHandle: Long): Unit` -> releases `llama_model` and `llama_context`.
2. Inference Execution:
   - `nativeGenerate(modelHandle: Long, prompt: String, grammar: String?, callback: TokenCallback): String` -> streams tokens and returns final text.
3. Cancellation:
   - `nativeInterrupt(modelHandle: Long): Unit` -> sets an atomic cancellation flag checked inside the token sampling loop.

### C++ Lifecycle Implementation Details

```cpp
#include <jni.h>
#include <string>
#include <atomic>
#include "llama.h"

struct AndroidLlamaContext {
    llama_model* model = nullptr;
    llama_context* ctx = nullptr;
    std::atomic<bool> cancelled{false};
};

extern "C" JNIEXPORT jlong JNICALL
Java_com_offline_reviewer_ai_LlamaBridge_nativeLoadModel(
    JNIEnv* env,
    jobject /* thiz */,
    jstring jModelPath,
    jint nCtx,
    jint nThreads
) {
    const char* modelPath = env->GetStringUTFChars(jModelPath, nullptr);
    llama_backend_init();

    llama_model_params modelParams = llama_model_default_params();
    modelParams.use_mmap = true;

    llama_model* model = llama_model_load_from_file(modelPath, modelParams);
    env->ReleaseStringUTFChars(jModelPath, modelPath);

    if (!model) {
        return 0;
    }

    llama_context_params ctxParams = llama_context_default_params();
    ctxParams.n_ctx = static_cast<uint32_t>(nCtx);
    ctxParams.n_batch = 512;
    ctxParams.n_threads = nThreads;
    ctxParams.n_threads_batch = nThreads;

    llama_context* ctx = llama_init_from_model(model, ctxParams);
    if (!ctx) {
        llama_model_free(model);
        return 0;
    }

    auto* wrapper = new AndroidLlamaContext();
    wrapper->model = model;
    wrapper->ctx = ctx;
    wrapper->cancelled.store(false);

    return reinterpret_cast<jlong>(wrapper);
}

extern "C" JNIEXPORT void JNICALL
Java_com_offline_reviewer_ai_LlamaBridge_nativeFreeModel(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong handle
) {
    auto* wrapper = reinterpret_cast<AndroidLlamaContext*>(handle);
    if (wrapper) {
        if (wrapper->ctx) {
            llama_free(wrapper->ctx);
        }
        if (wrapper->model) {
            llama_model_free(wrapper->model);
        }
        delete wrapper;
    }
    llama_backend_free();
}

extern "C" JNIEXPORT void JNICALL
Java_com_offline_reviewer_ai_LlamaBridge_nativeInterrupt(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jlong handle
) {
    auto* wrapper = reinterpret_cast<AndroidLlamaContext*>(handle);
    if (wrapper) {
        wrapper->cancelled.store(true);
    }
}
```

---

## 5. Token Generation, Prompt Templating, and Structured JSON

### Qwen Chat Template
Format prompts using the official Qwen chat format:

```text
<|im_start|>system
You are a precise educational question generator. You only generate valid JSON adhering to the provided schema. You base every question strictly on the provided context.<|im_end|>
<|im_start|>user
Context: [EXTRACTED TEXT]
Generate 3 multiple choice questions.<|im_end|>
<|im_start|>assistant
```

### Context Size and Thread Tuning
- Context Length (`n_ctx`): Cap at 2,048 tokens on devices with 4GB - 6GB RAM; cap at 4,096 tokens on devices with 8GB+ RAM.
- Batch Size (`n_batch`): Set to 512 for prompt processing.
- Thread Count (`n_threads`): Dynamically detect physical performance cores:
  `val threads = (Runtime.getRuntime().availableProcessors() - 2).coerceIn(2, 4)`
  Do not consume all CPU cores, preserving system responsiveness for the UI.

### Structured Output via GBNF Grammar
To guarantee valid JSON without parsing failures:
- Pass a GBNF (GGML BNF) grammar to `llama_sampler_init_grammar`.
- Restrict output strictly to the schema of question arrays, keys, and values.

---

## 6. Algorithmic Fallback and Error Resilience

Inference safety matrix:
1. Low-Memory Condition: If `ActivityManager.MemoryInfo.lowMemory` is true, or available RAM < 2.5 GB, refuse model load and route to the deterministic rule-based extractor.
2. Background Lifecycle: If the user backgrounds the application, do not keep the heavy llama.cpp context locked in memory; invoke `nativeFreeModel()` after saving generated state.
3. Cancellation Responsiveness: When user clicks "Stop Generation" or navigates back, invoke `nativeInterrupt()` immediately to release CPU cycles.
4. Fallback Execution: If `nativeLoadModel` returns 0, cleanly log the cause and seamlessly invoke the rule-based reviewer generation pipeline without presenting crash dialogs.
