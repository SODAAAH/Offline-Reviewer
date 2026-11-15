# Offline Reviewer

A lightweight, 100% offline native Android learning companion that transforms study documents into active recall flashcards, spaced repetition schedules, and examination simulations without cloud dependencies, accounts, or telemetry.

---

## Overview

Offline Reviewer is built for students who need a reliable, private, and distraction-free study tool on Android. The application reads study materials directly from device storage and generates interactive reviewers through a fast, deterministic algorithmic engine. An optional local AI runtime is supported for devices with sufficient memory.

---

## Core Features

### 1. Document Ingestion
- Imports PDF, DOCX, PPTX, TXT, Markdown, and XLSX files through Android Storage Access Framework (SAF).
- Memory-safe streaming parsers with zero DOM tree overhead on budget hardware.
- Preserves document provenance (page numbers, slide numbers, and section headings).

### 2. Algorithmic Question Generation
- Default deterministic rule-based engine requiring 0MB model weights.
- Generates 6 question types:
  - **Multiple Choice**: 4 choices with 3 plausible, category-grounded distractors.
  - **True / False**: Verifiable factual statements and controlled concept mutations.
  - **Identification**: Subject-predicate definition recall.
  - **Cloze (Fill-in-the-Blank)**: Keyword masking in context sentences.
  - **Matching**: Term and definition pairing exercises.
  - **Sequencing**: Procedural and chronological step ordering.
- Jaccard similarity deduplication (threshold > 0.75) and verbatim source grounding.

### 3. Active Recall & Spaced Repetition
- Deterministic SuperMemo-2 (SM-2) interval algorithm calculating optimal review intervals.
- 3D interactive flashcard review with card-flip animation and rating controls (Again, Hard, Good, Easy).
- Prompt-first active recall mode with instant source excerpt verification.
- Feynman self-explanation mode with keyword coverage checks.

### 4. Examination Simulator
- Timed practice exams with customizable question counts (10, 20, 30) and durations (15, 30, 45 mins).
- Grid navigator sheet tracking answered, unanswered, and flagged questions.
- Detailed result summaries with percentage scores, pass/fail evaluation, and topic breakdowns.

### 5. Learning Analytics & Mastery Engine
- Calculates topic mastery from repeated historical recall rather than single-attempt scores.
- Tracks flashcard mastery, review streaks, and overall retention rate.
- Automatically flags weak areas for targeted re-practice.

### 6. Optional On-Device AI Engine
- Isolated `llama.cpp` runtime via Android NDK and JNI for `Qwen3.5-2B Q4_K_M` GGUF models.
- Binary header validation (`0x46554747` magic, version check, quantization verification).
- RAM pre-flight checks requiring at least 2.5 GB free memory before loading.
- Seamless automatic fallback to the algorithmic generator when memory is constrained.

### 7. Local Data Portability
- Atomic backup and restoration via versioned ZIP archives.
- Zero network permissions declared in `AndroidManifest.xml`.

---

## Technology Stack

| Layer | Technology |
|---|---|
| Platform | Native Android (API 29+ / Android 10+) |
| Language | Kotlin 2.3.20 |
| UI Framework | Jetpack Compose with Material 3 |
| Database | Room 2.6.1 / SQLite (WAL mode) |
| Preferences | Jetpack Preferences DataStore 1.1.1 |
| Async | Kotlin Coroutines & Flow |
| Native Runtime | C++17, CMake, Android NDK |
| Build System | Gradle 9.1.0 with Android Gradle Plugin 9.0.1 |

---

## Building and Running

### Prerequisites
- Android SDK with Platform 36 and Build-Tools 36.0.0.
- JDK 17.

### Build Commands

```bash
# Run automated test suite (28 tests)
./gradlew testDebugUnitTest

# Assemble debug APK
./gradlew assembleDebug

# Assemble release APK
./gradlew assembleRelease
```

The compiled APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## Privacy & Security

Offline Reviewer does not declare `android.permission.INTERNET`. All documents, generated question decks, exam scores, and study metrics remain exclusively on the user's physical device.
