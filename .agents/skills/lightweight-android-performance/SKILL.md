---
name: lightweight-android-performance
description: Optimize Android application efficiency for budget hardware (2GB to 4GB RAM), governing memory budgets, CPU throttling prevention, cold startup, APK size reduction, R8 minification, and zero-leak coroutine lifecycles.
---

# Lightweight Android Performance

## 1. Core Objectives and Target Constraints

This skill governs optimization techniques that ensure the native Android student reviewer runs smoothly on entry-level hardware.

Hardware Target:
- Minimum Device: 2GB to 3GB RAM, Quad-Core ARM Cortex-A53 / A55 processor, Android 10 (API 29).
- Baseline Device: 4GB RAM, Octa-Core processor, Android 12+.

Key Metrics:
- Cold Startup Time: < 800ms to first interactive frame.
- Steady-State RAM Usage: < 120MB without AI model loaded; < 2.2GB during optional Qwen3.5-2B inference.
- Release APK Size: < 20MB base application (excluding optional on-demand AI model weights).
- Frame Rate: Consistent 60fps scrolling in Jetpack Compose feeds without dropped frames.

---

## 2. RAM and Garbage Collection Management

### Preventing Low Memory Kills (LMK)
1. Memory Trim Callback: Implement `ComponentCallbacks2` in `Application` class:

```kotlin
class ReviewerApp : Application(), ComponentCallbacks2 {
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            MemoryCacheManager.clearTransientCaches()
        }
    }
}
```

2. Native Memory Visibility: Native allocations (such as `llama.cpp` tensors) reside outside the ART JVM heap. Track native allocations using:
   `Debug.getNativeHeapAllocatedSize()`
3. Allocation Churn in Compose:
   - Never instantiate objects, lists, formatters, or date parsers directly within composable function bodies without `remember`.
   - Use primitive types (`Int`, `Long`, `Float`) over boxed wrappers (`Integer`, `Long`) in high-frequency loops.

---

## 3. CPU and Thermal Budgeting

### Threading Scopes
- `Dispatchers.Main`: Only lightweight UI state binding and interaction dispatch. Zero disk I/O, zero JSON parsing.
- `Dispatchers.Default`: CPU-bound algorithmic question extraction, similarity hashing, and scoring.
- `Dispatchers.IO`: Document file reading, SQLite/Room queries, and local export/import.
- Thread Throttling: When running multi-threaded operations (e.g. document batch processing), cap concurrent worker threads at `(availableProcessors / 2).coerceAtLeast(1)` to avoid thermal throttling and UI stutter.

---

## 4. Cold Startup Optimization

1. App Startup Library: Use `androidx.startup:startup-runtime` to consolidate initializers into a single ContentProvider, eliminating startup overhead.
2. Lazy Initialization: Defer initialization of database instances, document parsers, and native libraries until the exact moment they are first requested by user action.
3. Baseline Profiles: Generate and ship Baseline Profiles (`androidx.profileinstaller`) to pre-compile critical execution paths during app installation via ART Ahead-Of-Time (AOT) compilation.

---

## 5. APK Size Minimization and R8 Configuration

### Build Configuration (`app/build.gradle.kts`)

```kotlin
android {
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
        }
    }
    packaging {
        resources {
            excludes += listOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/*.version",
                "DebugProbesKt.bin"
            )
        }
    }
}
```

### R8 Full Mode
- Enable R8 full mode in `gradle.properties`:
  `android.enableR8.fullMode=true`
- Avoid broad wildcard ProGuard keep rules like `-keep class com.example.** { *; }`.
- Audit keep rules using the `r8-analyzer` skill.

---

## 6. Efficient Database Querying and UI Paging

1. Jetpack Paging 3: When displaying lists of questions, cards, or document history, use `Pager` with Room's `PagingSource`:
   - Page size: 20 to 30 items.
   - Pre-fetch distance: 10 items.
   - Avoid loading full question decks of 500+ items directly into memory.
2. Projection Queries:
   - Query only the required columns for list overviews (e.g., `SELECT id, stem, type FROM questions WHERE deckId = :deckId`).
   - Fetch complete details (explanations, full source excerpts) only when the card is active in the study viewport.

---

## 7. Strict Dependency Governance

Before adding any external library to `libs.versions.toml`:
1. Check Transitive Footprint: Verify the library does not pull in heavy transitive dependencies (such as Guava, Apache Commons, or Jackson).
2. Native Library Inspection: If a library contains `.so` files, verify that it supports 64-bit `arm64-v8a` and does not include unnecessary architectures.
3. Lightweight Alternatives:
   - Prefer Kotlin Serialization (`kotlinx.serialization`) over Jackson or Gson.
   - Prefer custom `XmlPullParser` over Apache POI for document extraction.
   - Prefer Kotlin Coroutines over RxJava.
