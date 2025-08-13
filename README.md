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
- **📚 Version history** - Track changes and restore previous versions
- **🗑️ Smart trash** - Soft delete with recovery options
- **📤 Multiple export formats** - Markdown, ZIP archives, database backups
- **📥 Flexible import** - From databases, archives, or folders
- **🌓 Theme support** - Dark and light modes
- **🌍 Bilingual** - English and Russian interface
- **⭐ Favorites** - Mark important notes and see them highlighted in the list
- **🗄️ Archive (new)** - Archive/unarchive notes (hidden from main list and search), Archive screen with preview, per-note Restore/Delete (to Trash), and bulk actions (Restore all / Delete all)
- **🤖 AI proofreading (new)** - In-editor AI menu with "Fix errors". If there is a text selection, only the selected fragment is corrected in-place; otherwise the entire note is corrected. Supports OpenAI/Gemini/Anthropic/OpenRouter; API keys in Settings → API Keys; strict provider/model usage (no fallbacks). 180s network timeouts. Version preview highlights per-character changes (green = added vs previous; red = removed vs next).

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
**DI:** Koin for lightweight dependency injection  

### Key Technical Features
- **Reactive data flow** with StateFlow and Compose
- **Optimized database queries** - separate metadata/content tables
- **Batch operations** for high-performance import/export
- **Archive data flow** - new `ArchiveDataSource`, repository methods, and UI navigation
- **AI integration** - OkHttp client; strict provider/model selection (no fallbacks); default models (gpt-5, gemini-2.5-flash, claude-3-7-sonnet-thinking) + extras (OpenAI: gpt-4.1-2025-04-14, o3-deep-research-2025-06-26; Gemini: gemini-2.5-flash-lite; OpenRouter: x-ai/grok-4, qwen/qwen3-coder); local API key storage; per-version AI metadata (provider/model/duration)
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