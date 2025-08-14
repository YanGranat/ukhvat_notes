package com.ukhvat.notes.data.export

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.room.withTransaction
import android.database.sqlite.SQLiteDatabase
import com.ukhvat.notes.data.database.NoteMetadataEntity
import com.ukhvat.notes.data.database.NoteContentEntity
import com.ukhvat.notes.data.database.NoteVersionEntity
import com.ukhvat.notes.data.database.AppDatabase
import com.ukhvat.notes.domain.util.Toaster
import java.io.File
import java.io.FileOutputStream
// Removed Hilt imports for Koin migration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.ukhvat.notes.R

/**
 * Notes importer from SQLite database.
 * 
 * Performance optimizations:
 * - **Batch note import**: insertNotesInBatch() for 10x acceleration
 * - **Batch version import**: createVersionsInBatch() for 5-10x acceleration
 * - **Atomicity**: Notes and versions imported in unified transactions
 * - **ID mapping**: Efficient correspondence of old and new note IDs
 * 
 * Import architecture:
 * 1. Load all data from imported DB
 * 2. Batch create notes with new ID generation
 * 3. Create correspondence map of old and new IDs
 * 4. Batch create all versions with new IDs
 * 
 * @author AI Assistant  
      * Database import
 */
/**
 * MIGRATED FROM HILT TO KOIN
 * 
 * Dependencies injected via Koin instead of @Inject constructor:
 * - NotesRepository: for importing notes
 * - Context: for file operations  
 * - Toaster: for user notifications
 */
class DatabaseImporter(
    private val context: Context,
    private val toaster: Toaster,
    private val mainDatabase: AppDatabase
) {

    suspend fun importFromDatabase(fileUri: Uri): Int = withContext(Dispatchers.IO) {
        try {
            // Copy file to app databases folder with unique name
            val databasesDir = File(context.applicationInfo.dataDir, "databases")
            if (!databasesDir.exists()) {
                databasesDir.mkdirs()
            }
            
            val tempDbName = "import_temp_${System.currentTimeMillis()}.db"
            val tempFile = File(databasesDir, tempDbName)
            
            context.contentResolver.openInputStream(fileUri)?.use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            
            if (!tempFile.exists() || tempFile.length() == 0L) {
                throw Exception(context.getString(R.string.database_copy_failed))
            }

            
            // Check database schema via direct SQLite access
            try {
                val sqliteDb = SQLiteDatabase.openDatabase(tempFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
                
                val cursor = sqliteDb.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null)
                val tables = mutableListOf<String>()
                while (cursor.moveToNext()) {
                    tables.add(cursor.getString(0))
                }
                cursor.close()
                sqliteDb.close()
                
                if (!tables.contains("note_metadata") || !tables.contains("note_content")) {
                    throw Exception(context.getString(R.string.database_missing_tables))
                }
                
            } catch (e: Exception) {
                throw Exception(context.getString(R.string.database_structure_invalid, e.message ?: ""))
            }
            
            // MERGE IMPORT: Read from temp DB and append to current DB without overwriting existing data
            var importedCount: Int
            try {
                // Open source DB for reading
                val sourceDb = SQLiteDatabase.openDatabase(tempFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)

                // Helper: detect available columns in note_versions for backward compatibility
                fun hasColumn(table: String, column: String): Boolean {
                    return try {
                        sourceDb.rawQuery("PRAGMA table_info($table)", null).use { cursor ->
                            val nameIndex = cursor.getColumnIndex("name")
                            while (cursor.moveToNext()) {
                                if (nameIndex >= 0 && column.equals(cursor.getString(nameIndex), ignoreCase = true)) return true
                            }
                        }
                        false
                    } catch (_: Exception) {
                        false
                    }
                }

                val hasAiProvider = hasColumn("note_versions", "aiProvider")
                val hasAiModel = hasColumn("note_versions", "aiModel")
                val hasAiDuration = hasColumn("note_versions", "aiDurationMs")

                // Read metadata
                val importedMetadata = mutableListOf<Triple<Long, String, LongArray>>()
                // Store minimal primitives + characterCount/isFavorite/isDeleted/isArchived flags to reduce allocations
                val metadataQuery = """
                    SELECT id, title, createdAt, updatedAt, characterCount, maxVersions, isFavorite, isDeleted, deletedAt, isArchived, archivedAt
                    FROM note_metadata
                    ORDER BY id ASC
                """.trimIndent()
                sourceDb.rawQuery(metadataQuery, null).use { c ->
                    val oldIds = mutableListOf<Long>()
                    val titles = mutableListOf<String>()
                    val rows = mutableListOf<LongArray>()
                    while (c.moveToNext()) {
                        val id = c.getLong(0)
                        val title = c.getString(1) ?: ""
                        val createdAt = c.getLong(2)
                        val updatedAt = c.getLong(3)
                        val characterCount = c.getInt(4).toLong()
                        val maxVersions = c.getInt(5).toLong()
                        val isFavorite = c.getInt(6).toLong()
                        val isDeleted = c.getInt(7).toLong()
                        val deletedAt = if (!c.isNull(8)) c.getLong(8) else -1L
                        val isArchived = c.getInt(9).toLong()
                        val archivedAt = if (!c.isNull(10)) c.getLong(10) else -1L
                        oldIds.add(id)
                        titles.add(title)
                        rows.add(longArrayOf(createdAt, updatedAt, characterCount, maxVersions, isFavorite, isDeleted, deletedAt, isArchived, archivedAt))
                    }
                    for (i in oldIds.indices) {
                        importedMetadata.add(Triple(oldIds[i], titles[i], rows[i]))
                    }
                }

                // Read content into map: oldId -> content
                val contentMap = HashMap<Long, String>(importedMetadata.size)
                sourceDb.rawQuery("SELECT noteId, content FROM note_content", null).use { c ->
                    while (c.moveToNext()) {
                        val noteId = c.getLong(0)
                        val content = c.getString(1) ?: ""
                        contentMap[noteId] = content
                    }
                }

                // Read versions, keep in memory for later remap
                data class ImportedVersion(
                    val oldNoteId: Long,
                    val content: String,
                    val timestamp: Long,
                    val changeDescription: String?,
                    val customName: String?,
                    val isForcedSave: Boolean,
                    val aiProvider: String?,
                    val aiModel: String?,
                    val aiDurationMs: Long?,
                    val aiHashtags: String?
                )
                val importedVersions = mutableListOf<ImportedVersion>()
                val baseVersionSelect = StringBuilder()
                baseVersionSelect.append("SELECT noteId, content, timestamp, changeDescription, customName, isForcedSave")
                if (hasAiProvider) baseVersionSelect.append(", aiProvider") else baseVersionSelect.append(", NULL AS aiProvider")
                if (hasAiModel) baseVersionSelect.append(", aiModel") else baseVersionSelect.append(", NULL AS aiModel")
                if (hasAiDuration) baseVersionSelect.append(", aiDurationMs") else baseVersionSelect.append(", NULL AS aiDurationMs")
                val hasAiHashtags = hasColumn("note_versions", "aiHashtags")
                if (hasAiHashtags) baseVersionSelect.append(", aiHashtags") else baseVersionSelect.append(", NULL AS aiHashtags")
                baseVersionSelect.append(" FROM note_versions ORDER BY timestamp ASC")
                sourceDb.rawQuery(baseVersionSelect.toString(), null).use { c ->
                    while (c.moveToNext()) {
                        val oldNoteId = c.getLong(0)
                        val vContent = c.getString(1) ?: ""
                        val ts = c.getLong(2)
                        val desc = if (!c.isNull(3)) c.getString(3) else null
                        val custom = if (!c.isNull(4)) c.getString(4) else null
                        val forced = c.getInt(5) == 1
                        val provider = if (!c.isNull(6)) c.getString(6) else null
                        val model = if (!c.isNull(7)) c.getString(7) else null
                        val dur = if (!c.isNull(8)) c.getLong(8) else null
                        val hashtags = if (!c.isNull(9)) c.getString(9) else null
                        importedVersions.add(ImportedVersion(oldNoteId, vContent, ts, desc, custom, forced, provider, model, dur, hashtags))
                    }
                }

                // Read tags if table exists (backward compatible)
                fun hasTable(table: String): Boolean {
                    return try {
                        sourceDb.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?", arrayOf(table)).use { c ->
                            c.moveToFirst()
                        }
                    } catch (_: Exception) { false }
                }
                val importedTags = mutableListOf<Pair<Long, String>>()
                if (hasTable("note_tags")) {
                    sourceDb.rawQuery("SELECT noteId, tag FROM note_tags", null).use { c ->
                        while (c.moveToNext()) {
                            val oldId = c.getLong(0)
                            val tag = c.getString(1) ?: ""
                            if (tag.isNotBlank()) importedTags.add(oldId to tag)
                        }
                    }
                }

                sourceDb.close()

                // Nothing to import
                if (importedMetadata.isEmpty()) {
                    tempFile.delete()
                    return@withContext 0
                }

                // Merge into main DB atomically
                val metadataDao = mainDatabase.noteMetadataDao()
                val contentDao = mainDatabase.noteContentDao()
                val versionDao = mainDatabase.noteVersionDao()
                val tagDao = mainDatabase.noteTagDao()

                importedCount = mainDatabase.withTransaction {
                    // Safety: normalize source-derived booleans from timestamps before insert
                    // (if imported rows have archivedAt/deletedAt but flags are 0)
                    // Prepare metadata entities with autogenerated IDs (id = 0)
                    val metadataEntities = importedMetadata.map { triple ->
                        val (oldId, title, row) = triple
                        val createdAt = row[0]
                        val updatedAt = row[1]
                        val characterCount = row[2].toInt()
                        val maxVersions = row[3].toInt()
                        val isFavorite = row[4] == 1L
                        val deletedAt = if (row[6] >= 0) row[6] else null
                        val archivedAt = if (row[8] >= 0) row[8] else null
                        val isDeleted = if (deletedAt != null) true else (row[5] == 1L)
                        val isArchived = if (archivedAt != null) true else (row[7] == 1L)
                        com.ukhvat.notes.data.database.NoteMetadataEntity(
                            id = 0,
                            title = title,
                            createdAt = createdAt,
                            updatedAt = updatedAt,
                            characterCount = characterCount,
                            maxVersions = maxVersions,
                            isFavorite = isFavorite,
                            isDeleted = isDeleted,
                            deletedAt = deletedAt,
                            isArchived = isArchived,
                            archivedAt = archivedAt
                        )
                    }

                    // Insert metadata batch → get new IDs in order
                    val newIds = metadataDao.insertMetadataBatch(metadataEntities)

                    // Build id map old -> new using positions (both lists are aligned)
                    val idMap = HashMap<Long, Long>(newIds.size)
                    for (i in importedMetadata.indices) {
                        idMap[importedMetadata[i].first] = newIds[i]
                    }

                    // Prepare content batch with mapped IDs (skip entries without content)
                    val contentEntities = importedMetadata.mapNotNull { (oldId, _, _) ->
                        val newId = idMap[oldId] ?: return@mapNotNull null
                        val content = contentMap[oldId] ?: ""
                        com.ukhvat.notes.data.database.NoteContentEntity(
                            noteId = newId,
                            content = content
                        )
                    }
                    if (contentEntities.isNotEmpty()) {
                        contentDao.insertContentBatch(contentEntities)
                    }

                    // Prepare versions batch with mapped note IDs
                    if (importedVersions.isNotEmpty()) {
                        val versionEntities = importedVersions.mapNotNull { v ->
                            val newNoteId = idMap[v.oldNoteId] ?: return@mapNotNull null
                            com.ukhvat.notes.data.database.NoteVersionEntity(
                                id = 0,
                                noteId = newNoteId,
                                content = v.content,
                                timestamp = v.timestamp,
                                changeDescription = v.changeDescription,
                                customName = v.customName,
                                isForcedSave = v.isForcedSave,
                                aiProvider = v.aiProvider,
                                aiModel = v.aiModel,
                                aiDurationMs = v.aiDurationMs,
                                aiHashtags = v.aiHashtags
                            )
                        }
                        if (versionEntities.isNotEmpty()) {
                            versionDao.insertVersionsBatch(versionEntities)
                        }
                    }

                    // Insert note tags mapped to new IDs
                    if (importedTags.isNotEmpty()) {
                        val tagEntities = importedTags.mapNotNull { (oldId, tag) ->
                            val newId = idMap[oldId] ?: return@mapNotNull null
                            com.ukhvat.notes.data.database.NoteTagEntity(noteId = newId, tag = tag)
                        }
                        if (tagEntities.isNotEmpty()) {
                            tagDao.insertTags(tagEntities)
                        }
                    }

                    // Normalize flags just in case
                    try {
                        metadataDao.normalizeDeletedFlags()
                        metadataDao.normalizeArchivedFlags()
                    } catch (_: Exception) { }

                    newIds.size
                }
            } finally {
                tempFile.delete()
            }

            return@withContext importedCount
            
        } catch (e: Exception) {
            throw Exception(context.getString(R.string.database_import_error, e.message ?: ""), e)
        }
    }
    
    fun getImportInfo(): String {
        return """
            Import from database file (.db):

• Select SQLite database file (.db)
• All notes with their content will be imported
• Note version history will also be restored (timestamps, names, flags)
• Creation/modification timestamps and flags (favorites/archive) preserved
• Notes will get new IDs in your database
• Duplicates may appear on repeated import

Compatibility:
✓ Files exported from this app
✓ Standard SQLite databases with compatible structure
        
        ${context.getString(R.string.select_db_file_prompt)}
        """.trimIndent()
    }
} 