---
name: offline-first-android
description: Enforce local-first Android persistence architecture using Room, SQLite WAL, DataStore, and Storage Access Framework with zero cloud dependencies, complete privacy, safe database migrations, and local backup/restore.
---

# Offline-First Android Architecture

## 1. Architectural Principles

This skill enforces a 100% offline, local-first foundation for Android applications.

Strict constraints:
- Zero Cloud Database: No Firebase, Supabase, AWS Amplify, Realm Sync, or REST backend syncing.
- Zero Remote Authentication: No sign-in gates, OAuth user accounts, email verification, or phone number requirements.
- Zero Telemetry / Analytics: No tracking SDKs, crashlytics reporting to remote servers, or usage beacons.
- Permission Minimization: Eliminate legacy external storage permissions (`READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`). Rely strictly on Android Storage Access Framework (SAF) and app-internal storage directories.
- 100% Functional Offline: All features (importing, parsing, question generation, study sessions, spaced repetition scheduling, statistics) must operate without network connectivity.

---

## 2. Room Database and SQLite Optimization

### Configuration and Tuning
- WAL Mode (Write-Ahead Logging): Enable WAL mode on the Room SQLite database to allow concurrent reads and writes:

```kotlin
Room.databaseBuilder(
    context.applicationContext,
    AppDatabase::class.java,
    "reviewer_database.db"
)
.setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
.build()
```

- SQLite Pragmas:
  - `PRAGMA foreign_keys = ON;` (enforce relational integrity).
  - `PRAGMA synchronous = NORMAL;` (optimal speed with durability in WAL mode).

### Schema Design and Indexing
- Primary Keys: Use autoincrementing 64-bit integer IDs (`Long`) for fast join performance and compact b-tree indices.
- Indices: Index foreign keys and columns frequently used in query filters (`WHERE documentId = :id`, `WHERE nextReviewDate <= :now`).
- Full-Text Search (FTS5): For fast keyword searches across thousands of flashcards or questions, leverage Room's `@Fts4` or `@Fts5` virtual tables:

```kotlin
@Entity(tableName = "questions_fts")
@Fts4(contentEntity = QuestionEntity::class)
data class QuestionFtsEntity(
    val stem: String,
    val explanation: String
)
```

### Relational Mapping
- Use `@Embedded` and `@Relation` for 1-to-many structures (e.g., `DocumentWithDecks`, `DeckWithQuestions`).
- Use `@Transaction` on DAO methods returning relational POJOs to guarantee consistency across queries.

---

## 3. Jetpack DataStore

Use Jetpack Preferences DataStore (or Proto DataStore) for application configuration and study preferences:
- Study session preferences (daily goal, question batch size, shuffle settings).
- Spaced repetition parameters (retention rate target, initial interval, maximum interval).
- UI preferences (theme mode, font scaling, reviewer card layout).
- Model configuration (selected model path, context size, thread count).

Never use legacy `SharedPreferences`: DataStore prevents UI thread disk I/O blocking through its asynchronous Coroutine Flow interface.

---

## 4. Storage Access Framework (SAF) and Local File Storage

### Internal App Storage
- Store working files, extracted text caches, and generated question databases in `context.filesDir` or `context.noBackupFilesDir`.
- These locations are private to the app sandbox, encrypted by Android file-based encryption (FBE), and require zero runtime permissions.

### SAF for Importing and Exporting
- Ingestion: Use `ActivityResultContracts.OpenDocument` to allow users to select study files from any provider (local storage, SD card, or USB drive).
- Persisting URI Permissions: Immediately take persistable permissions if the file path needs to be reopened later:

```kotlin
val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
context.contentResolver.takePersistableUriPermission(selectedUri, takeFlags)
```

- Content Streaming: Access document data strictly via `contentResolver.openInputStream(uri)`. Never attempt to convert content URIs into direct file paths via string manipulation.

---

## 5. Database Migrations and Integrity

### Migration Governance
- Never use `fallbackToDestructiveMigration()` in production builds: data loss of user study history is unacceptable.
- Export Schema: Set `room.schemaLocation` in `build.gradle.kts` and check generated JSON schemas into version control.
- Write explicit `Migration(from, to)` classes or utilize Room `AutoMigration` with validated `@RenameColumn` or `@DeleteColumn` specs.
- Test every migration using `MigrationTestHelper` in instrumented tests.

---

## 6. Backup, Restoration, and Data Portability

### Export Format
- Provide an encrypted or checksum-validated ZIP archive containing:
  1. `database_dump.json` (canonical JSON representation of documents, decks, questions, review logs).
  2. `preferences.json` (DataStore settings).
- Use `ActivityResultContracts.CreateDocument` to let the user save their backup to any folder.

### Restoration Safeguards
1. Verify Archive Header and CRC32 / SHA-256 checksum.
2. Validate Schema Version: confirm the backup version is `<= currentAppDatabaseVersion`.
3. Atomic Transaction: Perform restoration inside a Room transaction (`database.withTransaction { ... }`); if any record fails validation, roll back completely to leave the existing database untouched.
4. Clean Cache: Clear transient document caches after successful restoration.
