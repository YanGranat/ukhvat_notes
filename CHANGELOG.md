# Changelog

## 1.2.6 – Quick Note notification resiliency

- Strengthened quick note notification with explicit no-clear flags to prevent swipe dismissal.
- Added automatic restart when the notification gets cleared by the system or user on certain devices.
- Ensures persistent quick note access even on devices with aggressive notification handling.

## 1.2.5 – AI prompts folder + Translate

- Moved all AI prompts to `assets/prompts` (Markdown) for easy editing.
- Added AI "Translate" (RU/EN) with a language picker dialog.

## 1.2.4 – Home screen note widget (minor)

- Added a simple home screen widget that shows the content of a selected note.
- Tapping the widget opens the note in the editor; updates automatically on save.
- Respects light/dark theme; minimal resources and manifest updates.

## 1.2.3 – Versioning settings and fixes

- Added versioning settings (interval, min chars, limit), info dialog, and minor UI tweaks.
- Stability fixes for diff highlighting and resource strings.

## 1.2.2 – Quick Note from Notification: Reliable Clipboard

- Notification action "Создать с текстом": redesigned to read clipboard at action time in foreground `MainActivity` for Android 10+ reliability.
- Removed embedding clipboard text into PendingIntent extras to avoid stale values and caching side-effects.
- Eliminated BroadcastReceiver path for clipboard; simplified flow to single Activity-based handling.
- Ensures correct, current clipboard content is used; creates empty note if clipboard is empty.
- Minor refactors and stability improvements in notification code.

## 1.2.1 – Versioning reliability and backup rules

- Versioning: guaranteed first-version on exit for new notes (trim ≥3), race safety.
- Versioning: faster existence check (`hasAnyVersion`) to reduce I/O in UI.
- Backup: `app/src/main/java/.../BackupHelper.kt` supports external storage, keeps versions.
- Export tweaks: markdown export header uses title from first line, metadata more robust.

## 1.2.0 – Archive improvements + color updates

- Added Archive screen, navigation entry, bulk restore/delete actions.
- Metadata entity now includes `isArchived`/`archivedAt` fields with index.
- Repository/DataSource updates for archive operations.
- UI adjustments for archive actions in selection and menus.

## 1.1.9 – Search performance boost

- Optimized search caching with selective invalidation for updates/deletes.
- Added `invalidateNoteInCache` to `SearchDataSource` and repository integration.
- Maintains 70-80% cache hit under active editing.

## 1.1.8 – Version cache optimization

- Added version cache in repository to reduce DB calls during autosave.
- Invalidation hooks in note update/delete/version operations.
- Removes getLatestVersionForNote DB calls per autosave tick.

## 1.1.7 – Accessibility pass

- Added content descriptions for TalkBack navigation across key screens.
- Respect system font scaling in TextField, list items, dialogues.

## 1.1.6 – Database preloading

- Preload Room database on app start to remove cold start lag.
- Lightweight async warmup of metadata/content/version DAOs.

## 1.1.5 – Undo/Redo stability

- Stabilized TextController integration with TextFieldState undo stack.
- Prevented cursor jumps and flickering during fast edits.

## 1.1.4 – Export/Import improvements

- Improved Markdown export formatting and metadata header.
- Added folder import with batching and progress feedback.

## 1.1.3 – Widget polish

- Added support for multi-note widget configuration.
- Automatic widget refresh on note save.

## 1.1.2 – AI integration

- Added AI proofread action with provider selection and API key storage.
- Handles long content with 180s network timeouts.

## 1.1.1 – Favorites + migrations

- Added `isFavorite` flag, index, and migration.
- UI updates for favorite toggles and selection mode.

## 1.1.0 – Modular repository refactor

- Removed LegacyNotesRepositoryImpl, completed ModularNotesRepository.
- VersionDataSource additions for latest version fetch and batch creation.
- DI cleanup removing legacy bindings.

## 1.0.5 – Export reliability

- ExportManager now validates content size before generating share Intent.
- Added handling for empty note export edge cases.

## 1.0.4 – Search UX adjustments

- Stabilized search results ordering and highlight behavior.
- Prevented search state from reapplying after user clears query.

## 1.0.3 – Autosave tune-up

- Debounce adjustments for new vs existing notes, race condition fixes.
- Improved TextController to prevent cursor resets.

## 1.0.2 – Initial release polish

- Added version history view, trash management, and UI theming tweaks.

## 1.0.1 – Initial release

