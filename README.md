# Ukhvat

**Grasp your ideas** - A simple yet powerful note-taking app for Android

*Simple outside, powerful inside*

---

**Language versions:** [🇺🇸 English](#) | [🇷🇺 Русский](README_RU.md)

---

## 📸 Screenshots

![Ukhvat Screenshots](screenshots/ukhvat_v1_Screenshots.jpg)

*Main interface: Notes List, Note Editor, Search, and Trash*

## ✨ Features

- **📝 Instant note creation** - Start writing immediately with auto-save
- **🔍 Full-text search** - Find anything across all notes with highlighting  
- **📚 Version history** - Track changes and restore previous versions (clean preview in list; full view shows precise additions/removals)
- **🗑️ Smart trash** - Soft delete with recovery options
- **📤 Multiple export formats** - Markdown, ZIP archives, database backups
- **📥 Flexible import** - From databases, archives, or folders
- **🌓 Theme support** - Dark and light modes
- **🌍 Bilingual** - English and Russian interface
- **⭐ Favorites** - Mark important notes and see them highlighted in the list
- **🗄️ Archive** - Archive notes to hide them from the main list and search. Manage them in a separate screen.
- **🤖 AI Features (new)** - Intelligent tools to work with your text:
    - **Fix Errors**: Corrects text using an LLM. Works on a selected fragment or the entire note.
    - **Generate Title**: Creates a concise title (up to 50 characters) and inserts it at the top of the note.
    - **Generate Hashtags**: Analyzes the note and suggests 1-5 relevant hashtags, preserving existing ones.

## 📱 What it does

Ukhvat is designed for anyone who needs to capture thoughts quickly and reliably. Whether you're a student taking lecture notes, a writer drafting ideas, or someone who just needs a dependable digital notepad - Ukhvat focuses on the essentials while providing powerful features under the hood.

**Core philosophy:** No clutter, no distractions, just your thoughts and the tools to manage them effectively.

## 🚀 Getting Started

### Requirements
- Android 7.0 (API 24) or higher
- ~10MB storage space

### Installation
1. Download the latest APK from [Releases](../../releases)
2. Install on your Android device
3. Start taking notes immediately!

### Building from Source
```bash
git clone https://github.com/YanGranat/ukhvat_notes.git
cd ukhvat_notes
./gradlew assembleDebug
```

### Secure release signing
1. Create `signing.properties` in project root (DO NOT commit):
```
keyAlias=ukhvat
keyPassword=********
storeFile=keys/ukhvat-release-key.jks
storePassword=********
```
2. Place the keystore at the given path.
3. Build release: `./gradlew assembleRelease` (APK) or `./gradlew bundleRelease` (AAB).
4. `signing.properties` and `keys/` are git-ignored.

## 🏗️ Technical Details

**Architecture:** Clean MVVM with Repository pattern  
**Database:** Room with three-table optimization  
**Migrations:** Non-destructive schema migrations (e.g., v8→v9 adds Favorites without data loss)  
v9→v10 adds Archive (isArchived, archivedAt, index) with migration
**UI:** Jetpack Compose with Material Design 3  
**Language:** 100% Kotlin  
**DI:** Koin for lightweight dependency injection (modules: `appModule`, `dataSourceModule`, `performanceModule`)  

### Key Technical Features
- **Reactive data flow** with StateFlow and Compose
- **Optimized database queries** - separate metadata/content tables
- **Batch operations** for high-performance import/export
- **Archive data flow** - new `ArchiveDataSource`, repository methods, and UI navigation
- **AI integration** - Support for OpenAI, Gemini, Anthropic, and OpenRouter. API keys are stored locally.
- **Adaptive text processing** with advanced search algorithms

## 📄 Export & Import

**Export formats:**
- Individual Markdown files  
- ZIP archives with separate files
- Single combined Markdown file
- Complete SQLite database backup

**Import sources:**
- Database files (full restore)
- ZIP archives (bulk import)  
- Folder with text/markdown files

## 🛠️ Development

Built with modern Android development tools:
- **Kotlin** - Primary language
- **Jetpack Compose** - Declarative UI
- **Room** - Local database with migrations
- **Koin** - Dependency injection
- **Coroutines** - Async operations

## 📝 License

MIT License - see [LICENSE](LICENSE) for details

---

*Ukhvat (Ухват) - A traditional Russian oven tool for handling pots. Just like the tool helps manage heavy cookware, this app helps you handle your heavy thoughts and ideas in your head..*