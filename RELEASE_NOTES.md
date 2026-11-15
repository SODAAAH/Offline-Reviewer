# Offline Reviewer v1.0.0

Offline Reviewer is a lightweight, 100% offline native Android learning companion that transforms study documents into active recall flashcards, SM-2 spaced repetition schedules, and examination simulations without cloud dependencies, accounts, or telemetry.

### What's New in v1.0.0

- **Multi-Format Document Parsing**: Streaming extraction for PDF, Word (.docx), PowerPoint (.pptx), Excel (.xlsx), Markdown (.md), and plain text (.txt) files via Storage Access Framework (SAF).
- **Algorithmic Question Generator**: Generates Multiple Choice (with 3 category-grounded distractors), True/False, Identification, Cloze, Matching, and Sequencing questions with source grounding and Jaccard deduplication.
- **Spaced Repetition & Study Modes**:
  - Interactive 3D flashcards with SuperMemo-2 (SM-2) scheduling.
  - Active recall prompt verification with source excerpts.
  - Feynman explanation practice with keyword coverage checks.
- **Examination Simulator**: Timed mock exams with question flag navigator, score percentages, and topic breakdowns.
- **Mastery Analytics**: Retention rate calculations, flashcard mastery tracking, and weak area detection.
- **Optional Local AI**: Support for `Qwen3.5-2B Q4_K_M` GGUF models executed locally via `llama.cpp` over Android NDK/JNI with automatic fallback to the algorithmic engine.
- **Data Portability**: Atomic ZIP backup and restore.
- **100% Offline**: Zero internet permissions declared in manifest.

### APK Download
- `app-debug.apk` (Android 10+, API 29+)
