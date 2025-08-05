# Ukhvat

**Grasp your ideas** - A simple yet powerful note-taking app for Android

*Simple outside, powerful inside*

---

**Language versions:** [🇺🇸 English](#) | [🇷🇺 Русский](README_RU.md)

---

## ✨ Features

- **📝 Instant note creation** - Start writing immediately with auto-save
- **🔍 Full-text search** - Find anything across all notes with highlighting  
- **📚 Version history** - Track changes and restore previous versions
- **🗑️ Smart trash** - Soft delete with recovery options
- **📤 Multiple export formats** - Markdown, ZIP archives, database backups
- **📥 Flexible import** - From databases, archives, or folders
- **🌓 Theme support** - Dark and light modes
- **🌍 Bilingual** - English and Russian interface

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

## 🏗️ Technical Details

**Architecture:** Clean MVVM with Repository pattern  
**Database:** Room with three-table optimization  
**UI:** Jetpack Compose with Material Design 3  
**Language:** 100% Kotlin  
**DI:** Koin for lightweight dependency injection  

### Key Technical Features
- **Reactive data flow** with StateFlow and Compose
- **Optimized database queries** - separate metadata/content tables
- **Batch operations** for high-performance import/export
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
- **Room** - Local database
- **Koin** - Dependency injection
- **Coroutines** - Async operations

## 📝 License

MIT License - see [LICENSE](LICENSE) for details

---

*Ukhvat (Ухват) - A traditional Russian oven tool for handling pots. Just like the tool helps manage heavy cookware, this app helps you handle your heavy thoughts and ideas in your head..*