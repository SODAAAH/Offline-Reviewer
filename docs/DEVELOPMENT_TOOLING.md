# Development Tooling Guide

## 1. Project Context and Tooling Architecture

This environment is configured for developing a lightweight, fully offline native Android student reviewer application.

### Core Stack
- Platform: Native Android (Kotlin, Jetpack Compose, Material 3)
- Local Persistence: Room (SQLite WAL, FTS5), Jetpack DataStore, Storage Access Framework (SAF)
- Document Processing: Pure on-device streaming parsers (PDF, DOCX, PPTX, TXT, Markdown, XLSX)
- Reviewer Engine: Algorithmic deterministic extraction for definition, enumeration, multiple choice, true/false, cloze, matching, and sequencing with source grounding
- Optional Local AI: llama.cpp compiled via Android NDK/JNI running Qwen3.5-2B Q4_K_M GGUF with algorithmic fallback
- Constraints: 100% offline, privacy-first, zero telemetry, optimized for 2GB to 4GB RAM Android devices

---

## 2. Model Context Protocol (MCP) Configuration

Workspace configuration location:
`.agents/mcp_config.json`

### A. Context7 MCP
- Purpose: Retrieve authoritative, up-to-date documentation for Kotlin, Android SDK, Jetpack Compose, Room, NDK, and related libraries.
- Transport: Streamable HTTP (Remote OAuth)
- Endpoint: `https://mcp.context7.com/mcp/oauth`
- Toolset: `resolve-library-id`, `query-docs`, `query-code`
- Operational Rules:
  - Query documentation rather than hallucinating API signatures or using outdated examples.
  - Never transmit proprietary source code or user data to external endpoints; use strictly for API documentation retrieval.

### B. Android MCP Server
- Repository: `https://github.com/martingeidobler/android-mcp-server`
- Package: `android-mcp-server`
- Transport: Stdio via `npx.cmd -y android-mcp-server`
- Discovered Environment Variables:
  - `ANDROID_HOME`: `C:\Users\jahnm\AppData\Local\Android\Sdk`
  - ADB Binary: `C:\Users\jahnm\AppData\Local\Android\Sdk\platform-tools\adb.exe`
- Toolset:
  - Device Management: `list_devices`, `list_avds`, `start_emulator`
  - UI Analysis: `screenshot`, `get_ui_tree`
  - Interaction: `tap`, `tap_element`, `tap_and_wait`, `type_text`, `press_key`, `swipe`, `scroll_to_element`
  - Diagnostics: `get_logs`, `clear_logs`, `get_device_info`
  - App Lifecycle: `launch_app`, `install_apk`, `get_current_activity`
- Operational Rules:
  - Use for emulator control, automated UI testing, and crash logcat inspection.
  - Never install APKs or mutate device state without explicit developer confirmation.

---

## 3. Official Android Development Skills

Installed directly from `https://github.com/android/skills` via the Android CLI into `.agents/skills/`:

### 1. `testing-setup`
- Path: `.agents/skills/testing-setup/SKILL.md`
- Scope: Unit testing, integration testing, Compose UI testing (`androidx.compose.ui:ui-test-*`), Robolectric test harnesses, and screenshot testing.

### 2. `android-profiler`
- Path: `.agents/skills/android-profiler/SKILL.md`
- Scope: Diagnosing CPU call stacks, memory leaks, Low Memory Kills (LMK), allocation churn, UI jank, and frame drops using Perfetto and Android Studio profilers.

### 3. `r8-analyzer`
- Path: `.agents/skills/r8-analyzer/SKILL.md`
- Scope: Analyzing Android release build files and ProGuard/R8 keep rules to identify redundancies, eliminate overly broad rules, and shrink release APK footprint.

### 4. `jetpack-compose-performance`
- Path: `.agents/skills/jetpack-compose-performance/SKILL.md`
- Scope: Compose phase deferral, recomposition optimization, stability and skipping analysis, lazy layout keys/contentType specifications, and subcomposition overhead elimination. Note: In the official Android skills catalog, this represents the verified identifier for Compose UI performance and architecture guidance.

---

## 4. Project-Specific Custom Skills

Configured in `.agents/skills/`:

### 1. `offline-document-processing`
- Path: `.agents/skills/offline-document-processing/SKILL.md`
- Scope: Purely local streaming extraction across PDF (PdfRenderer), DOCX (`XmlPullParser`), PPTX, TXT, Markdown, and XLSX. Enforces page/slide provenance, non-blocking coroutines (`Dispatchers.IO`), and memory guardrails.

### 2. `reviewer-generation-quality`
- Path: `.agents/skills/reviewer-generation-quality/SKILL.md`
- Scope: High-precision question construction covering definition detection, enumeration, identification, multiple choice, true/false, cloze, matching, and sequencing. Enforces strict source grounding, duplicate elimination, and distractor plausibility.

### 3. `llama-cpp-android`
- Path: `.agents/skills/llama-cpp-android/SKILL.md`
- Scope: Optional offline LLM inference using Qwen3.5-2B Q4_K_M GGUF via `llama.cpp` over Android NDK and JNI. Covers 64-bit `arm64-v8a` CMake setup, JNI memory management, prompt formatting, GBNF structured JSON output, cancellation, and algorithmic fallback.

### 4. `offline-first-android`
- Path: `.agents/skills/offline-first-android/SKILL.md`
- Scope: Local-first persistence using Room with SQLite WAL and FTS4/FTS5, Jetpack DataStore, Storage Access Framework, non-destructive migrations, and atomic backup/restoration.

### 5. `lightweight-android-performance`
- Path: `.agents/skills/lightweight-android-performance/SKILL.md`
- Scope: Hardware optimization for 2GB to 4GB RAM devices. Covers `ComponentCallbacks2` memory trimming, cold startup (< 800ms) with App Startup and Baseline Profiles, R8 full mode, APK size reduction, and Paging 3 query projection.

---

## 5. Development Workflows

### Documentation Verification Workflow
1. Identify the target library or Android API.
2. Resolve library identifier using Context7 (`resolve-library-id`).
3. Query specific documentation topic (`query-docs`).
4. Read source signatures (`query-code`) if exact parameter types or defaults require clarification.

### Device Automation and Testing Workflow
1. List available devices/emulators:
   `adb devices` or Android MCP `list_devices`.
2. Inspect UI hierarchy:
   Android MCP `get_ui_tree`.
3. Capture visual snapshot:
   Android MCP `screenshot`.
4. Capture logs during test execution:
   Android MCP `get_logs`.

### Code Review and Optimization Workflow
1. Compose UI audit: Activate `jetpack-compose-performance`.
2. Memory and trace analysis: Activate `android-profiler`.
3. Keep rule and APK shrinking audit: Activate `r8-analyzer`.
4. Testing setup verification: Activate `testing-setup`.
