---
name: offline-document-processing
description: Guide local-only, zero-cloud document importing, parsing, and text extraction on Android across PDF, DOCX, PPTX, TXT, Markdown, and XLSX files with page/slide provenance and memory safety.
---

# Offline Document Processing for Android

## 1. Architectural Principles

This skill governs local, completely offline ingestion and extraction of study materials on Android devices.
Strict constraints:
- Zero network requests or cloud document APIs.
- Pure on-device extraction using Android SDK APIs, streaming XML parsing, or lightweight embedded Java/Kotlin parsers.
- Strict memory bounds: never load an entire multi-megabyte file or document object model directly into memory as a monolithic byte array.
- Absolute provenance: every extracted text block or question-candidate paragraph must retain its source metadata (document URI/ID, page number, slide number, or section header).

---

## 2. Supported Document Formats and Extraction Strategies

### Plain Text (.txt)
- Implementation: Read via `InputStreamReader` wrapped in `BufferedReader` with explicit `StandardCharsets.UTF_8` fallback detection.
- Stream line-by-line or in fixed character blocks (e.g., 2,048 to 4,096 characters) respecting paragraph boundaries (`\n\n`).
- Maintain line numbers and running character offsets.

### Markdown (.md)
- Implementation: Stream lines with lightweight stateful parser or lightweight CommonMark AST parser.
- Identify heading levels (`#`, `##`, `###`) to create logical section hierarchies.
- Preserve list markers, bullet points, and code spans for contextual preservation during reviewer generation.

### PDF Documents (.pdf)
- Extraction Approaches:
  1. For text content extraction: Use lightweight Android-compatible text extraction engines (such as Android PdfBox / `com.tom-roush:pdfbox-android` or pure C++ `poppler`/`pdfium` via NDK).
  2. For page rendering / thumbnails: Utilize native Android `android.graphics.pdf.PdfRenderer` (available from API 21+).
- Page-by-page streaming: Open `PdfRenderer` or `PDDocument`, process single page index `p`, extract text, associate with `pageNumber = p + 1`, and recycle/close page buffers immediately before advancing.
- Guard against raster-only scanned PDFs: detect when page text length is 0 or near zero, indicating an image-only document where OCR or user notification is required.

### Microsoft Word (.docx)
- Format nature: An Open Packaging Conventions (OPC) ZIP archive containing XML parts.
- Lightweight parsing without heavy desktop POI dependencies:
  1. Open via `java.util.zip.ZipInputStream` or `ZipFile`.
  2. Locate `word/document.xml`.
  3. Parse using Android's native `XmlPullParser` (`org.xmlpull.v1.XmlPullParser`).
  4. Collect text elements within `<w:p>` (paragraphs) and `<w:t>` (runs).
  5. Distinguish headings by inspecting `<w:pStyle w:val="Heading1"/>`.
- Memory impact: Zero DOM tree overhead. Stream through the XML tags directly.

### Microsoft PowerPoint (.pptx)
- Format nature: OPC ZIP archive containing individual slide files (`ppt/slides/slide1.xml`, `slide2.xml`, etc.).
- Lightweight parsing:
  1. Open via `ZipFile`.
  2. Inspect slide entries sorted by slide index number.
  3. Stream each slide XML using `XmlPullParser`.
  4. Extract text contained inside `<a:p>` and `<a:t>`.
  5. Assign provenance: `slideNumber = index + 1`.

### Tabular Reviewer Files (.xlsx)
- Format nature: OPC ZIP archive with `xl/sharedStrings.xml` and `xl/worksheets/sheet1.xml`.
- Stream `sharedStrings.xml` to build a string pool index or map if memory allows, or stream row-by-row with `XmlPullParser`.
- Map column A (Question/Term), column B (Answer/Definition), column C (Optional Explanation/Category).

---

## 3. Provenance and Metadata Retention

Every extracted text segment must be emitted as a structured data class:

```kotlin
data class ExtractedDocumentChunk(
    val documentId: Long,
    val documentName: String,
    val chunkIndex: Int,
    val content: String,
    val pageNumber: Int?,
    val slideNumber: Int?,
    val sectionHeader: String?,
    val characterCount: Int
)
```

Rules:
- Never strip or lose `pageNumber` or `slideNumber`.
- Clean non-semantic whitespace: normalize multi-spaces to single spaces, normalize multiple consecutive empty lines to double newline, and strip trailing whitespace.
- De-hyphenate line-wrapped words (e.g., `oper- \n ation` -> `operation`).
- Exclude repeated running headers and footers by detecting identical top/bottom lines appearing on >=3 consecutive pages.

---

## 4. Background Concurrency and Memory Management

### Thread Scoping
- All file I/O and parsing must run strictly on `Dispatchers.IO`.
- Long-running multi-page documents must execute within a cancelable coroutine job, checking `coroutineContext.isActive` between pages or paragraphs.
- For user-initiated background imports that must survive app navigation, dispatch tasks using `androidx.work.WorkManager` with `CoroutineWorker`.

### Memory Guardrails for 2GB - 4GB RAM Devices
- Do not buffer large binary streams into `ByteArrayOutputStream` or `ByteArray`.
- Close all `ZipFile`, `InputStream`, `ParcelFileDescriptor`, and parser handles inside `use { ... }` blocks or `try-finally`.
- When using `Bitmap` rendering via `PdfRenderer.Page.render()`, allocate only a single reusable bitmap matching screen viewport dimensions, never full uncompressed print resolution. Recycle bitmaps explicitly when discarded.

---

## 5. Document Error Handling

Handle the following failure modes gracefully without crashing the application:

1. Password-Protected Documents:
   - Catch encryption exceptions (`EncryptedDocumentException` / `SecurityException`).
   - Prompt the user to enter the document decryption password or skip the file.

2. Malformed or Corrupted Archives:
   - Catch `ZipException`, `EOFException`, and `XmlPullParserException`.
   - Report the exact corrupted segment and salvage any previously parsed chunks.

3. Out of Memory Mitigation:
   - If large embedded images or deep XML nesting threatens memory, abort chunk processing, log recovery state, and notify the user with actionable instructions.

4. Unreadable Encodings:
   - Handle fallback from UTF-8 to ISO-8859-1 or Windows-1252 for legacy text files.
