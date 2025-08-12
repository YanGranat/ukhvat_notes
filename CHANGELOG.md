# Changelog

## 1.1.0 – Archive, UI tweaks, menu reorders

- Database v10 migration:
  - Added `isArchived`, `archivedAt`, and index `index_isArchived` to `note_metadata`
  - Migration 9→10 enabled in DI
- New Archive system:
  - Archive/unarchive notes; archived notes are hidden from main list and search
  - Archive screen with note preview (tap on card), per-note actions (Restore, Delete → moves to Trash)
  - Bulk actions in top bar: Restore all, Delete all (soft delete) with confirmation
- Editor menu order updated:
  - Info → Favorite → Version history → Export → Archive → Delete
- Main menu order updated:
  - Language → Import → Export → About → Archive → Trash
- Selection toolbar (multi-select) tweaks:
  - Buttons order preserved; spacing adjusted (Select all +6dp, Export/Favorite/Archive +2dp)
- Toasts:
  - Show "Архивировано" on archiving (editor and multi-select)
- Misc:
  - Added icons `ic_archive`, `ic_restore_all`, vector `ic_delete_all`
  - Bug fixes and minor UI polish

## 1.0.0 – Initial release

- Core notes, autosave with versioning
- Full-text search with LRU caching
- Import/Export (text, Markdown, ZIP, database)
- Trash with soft delete
- Theme and localization (RU/EN)

