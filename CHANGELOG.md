# Changelog

## 1.1.0 – Archive, UI tweaks, menu reorders

## 1.2.0 – AI Hashtags, DB v12

- AI: Added "Generate hashtags" to the editor AI menu
  - Prompt analyzes existing hashtags, keeps relevant, adds missing (1–5 total)
  - Client-side merge guard to avoid losing relevant existing tags
  - Version created with description "Добавлены хештеги"; hashtags shown in Version Info
- Note Info: hashtags shown as blue chips line; editor for manual tags (comma/space separated)
- Database v12:
  - New table `note_tags` (normalized storage, PK: noteId+tag, index on tag)
  - New column `note_versions.aiHashtags` for Info dialog
- Import (.db): imports `note_tags` and optional `aiHashtags` if present
- README updated (RU/EN)

- New Archive system:
  - Archive/unarchive notes; archived notes are hidden from main list and search
  - Archive screen with note preview (tap on card), per-note actions (Restore, Delete → moves to Trash)
  - Bulk actions in top bar: Restore all, Delete all (soft delete) with confirmation

- Favorites:
  - Mark/unmark notes as favorite in editor and in selection mode on the list
  - Favorites highlighted in the list


## 1.0.0 – Initial release

- Core notes, autosave with versioning
- Full-text search with LRU caching
- Import/Export (text, Markdown, ZIP, database)
- Trash with soft delete
- Theme and localization (RU/EN)

