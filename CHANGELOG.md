# Changelog

## 1.2.0 – Architecture, Performance, and AI Features

A major update focused on architectural simplification, critical performance optimizations, and expanded AI capabilities.

### ✨ Features

- **AI - Error Correction**: Added AI-powered proofreading. The feature is context-aware: it corrects only the selected text or the entire note if there is no selection.
- **AI - Generate Title**: Added a feature to generate a concise, one-line title (up to 50 characters) for a note. The generated title is inserted at the beginning of the note.
- **AI - Hashtag Generation**: Added "Generate hashtags" to the editor's AI menu. The prompt analyzes existing tags, keeps relevant ones, and adds missing ones (1–5 total).
- **Archive System**: Implemented a full-featured archive. Archived notes are hidden from the main list and search results. The archive screen includes per-note actions (Restore, Delete) and bulk operations (Restore All, Delete All).
- **Favorites**: Added the ability to mark notes as "favorite" in the editor or from the main list in selection mode. Favorite notes are highlighted for easy identification.

### 🚀 Improvements & Optimizations

- **Architectural Refactoring**: Completed the migration to a unified `ModularNotesRepository`, removing the legacy repository and deleting over 900 lines of duplicate and dead code. This simplifies the architecture and improves maintainability.
- **Performance - UI Scrolling**: Fixed a critical issue causing UI freezes and lag when scrolling the notes list for the first time. Implemented a global color caching architecture (`GlobalColorBundle`) to prevent expensive calculations during recomposition.
- **Performance - App Startup**: Significantly improved app launch speed by adding a database index for sorting, pre-loading the database on a background thread, and implementing staged data loading to prioritize UI responsiveness.
- **Performance - Search Cache**: Optimized the search cache to use selective invalidation. Instead of clearing the entire cache on every change, it now only removes relevant entries, preserving a high cache-hit rate during note editing.
- **Performance - Versioning Cache**: Implemented a cache for version lookups to reduce database queries during frequent auto-save operations.
- **Reliability - Import/Export**: Fixed a bug that could cause a visual glitch ("minus one note") after importing notes from a ZIP or folder. Improved database export/import reliability with WAL checkpointing and a safer merge strategy.
- **Accessibility**:
    - Added content descriptions to all icons and buttons for full navigation support via the **TalkBack** screen reader.
    - Implemented **Font Scaling**, allowing the app's text to resize according to the user's system-level font size settings.
- **UX - Note Sorting**: Opening and closing a note without making any changes no longer updates its timestamp, preventing it from incorrectly moving to the top of the list.

### 🐛 Fixes

- **Critical Fix - Data Integrity**: Added `@Transaction` annotations to multi-table database operations, preventing potential data corruption and ensuring atomicity.
- **Critical Fix - Editor**: Re-enabled automatic capitalization of sentences in the text editor, which had been accidentally disabled during diagnostics.
- **Database**: Fixed an inefficient batch update operation, reducing the number of SQL calls from O(n) to O(2).
- **Refactoring**: Removed duplicated constants and unused code.

## 1.1.0 – Archive & Favorites

This version introduced the Archive and Favorites systems, along with corresponding database migrations and UI enhancements.

### ✨ Features

- **Archive System**:
    - Added functionality to archive and unarchive notes.
    - Created a separate "Archive" screen to view and manage archived notes.
    - Implemented per-note actions (Restore, Delete) and bulk operations (Restore All, Delete All).
    - Archived notes are excluded from the main list and search results.
- **Favorites**:
    - Added the ability to mark notes as "favorite".
    - Favorites are highlighted in the main notes list for better visibility.
    - Added options to toggle favorite status in the editor and in selection mode.

### 🚀 Improvements & Optimizations

- **UI Tweaks**: Reordered menu items in the editor and main screen for a more logical flow. Adjusted spacing in the selection toolbar.

### 💾 Database

- **DB Migration (9 -> 10)**: Added `isArchived` and `archivedAt` fields to support the archive system.
- **DB Migration (8 -> 9)**: Added the `isFavorite` flag to support the favorites system. Both migrations are non-destructive.

## 1.0.0 – Initial release

- Core notes, autosave with versioning
- Full-text search with LRU caching
- Import/Export (text, Markdown, ZIP, database)
- Trash with soft delete
- Theme and localization (RU/EN)

