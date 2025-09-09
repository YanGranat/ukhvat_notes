package com.ukhvat.notes.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ukhvat.notes.R
import com.ukhvat.notes.domain.model.NoteVersion
import com.ukhvat.notes.domain.repository.NotesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
// Removed Hilt imports for Koin migration

/**
 * MIGRATED FROM HILT TO KOIN
 * 
 * Dependencies injected via Koin instead of @Inject constructor:
 * - NotesRepository: for version history operations
 */
class VersionHistoryViewModel(
    private val repository: NotesRepository,
    private val context: Context
) : ViewModel() {
    
    fun getVersionsForNote(noteId: Long): Flow<List<NoteVersion>> {
        return repository.getVersionsForNote(noteId)
    }
    
    suspend fun getVersionById(versionId: Long): NoteVersion? {
        return repository.getVersionById(versionId)
    }
    
    /**
     * Creates new note based on selected version
     */
    suspend fun createNoteFromVersion(versionId: Long): Long? {
        return try {
            val version = repository.getVersionById(versionId)
            if (version != null) {
                val currentTime = System.currentTimeMillis()
                val newNote = com.ukhvat.notes.domain.model.Note(
                    content = version.content,
                    createdAt = currentTime,
                    updatedAt = currentTime
                )
                repository.insertNote(newNote)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Deletes note version
     */
    suspend fun deleteVersion(versionId: Long): Boolean {
        return repository.deleteVersion(versionId)
    }

    /**
     * Deletes all versions for given note by iterating existing versions.
     * Uses repository.deleteVersion() to preserve cache invalidation logic.
     * @return number of deleted versions
     */
    suspend fun deleteAllVersions(noteId: Long): Int {
        return try {
            val versions = repository.getVersionsForNoteList(noteId)
            var deleted = 0
            for (v in versions) {
                if (repository.deleteVersion(v.id)) deleted++
            }
            deleted
        } catch (e: Exception) {
            0
        }
    }

    suspend fun getMaxVersions(noteId: Long): Int? {
        return repository.getMaxVersions(noteId)
    }

    // ============ VERSIONING SETTINGS (GLOBAL) ============
    suspend fun getVersioningAutoEnabled(): Boolean = repository.getVersioningAutoEnabled()
    suspend fun setVersioningAutoEnabled(enabled: Boolean) = repository.setVersioningAutoEnabled(enabled)
    suspend fun getVersioningIntervalMs(): Long = repository.getVersioningIntervalMs()
    suspend fun setVersioningIntervalMs(intervalMs: Long) = repository.setVersioningIntervalMs(intervalMs)
    suspend fun getVersioningMinChangeChars(): Int = repository.getVersioningMinChangeChars()
    suspend fun setVersioningMinChangeChars(chars: Int) = repository.setVersioningMinChangeChars(chars)
    suspend fun getVersioningMaxRegularVersions(): Int = repository.getVersioningMaxRegularVersions()
    suspend fun setVersioningMaxRegularVersions(max: Int) = repository.setVersioningMaxRegularVersions(max)

    suspend fun cleanupVersionsNow(noteId: Long, keepCount: Int) {
        // Use DataSource path through repository if exposed in future; for now delete old manually
        val versions = repository.getVersionsForNoteList(noteId)
        if (versions.size > keepCount) {
            val toDelete = versions.drop(keepCount)
            toDelete.forEach { repository.deleteVersion(it.id) }
        }
    }

    /**
     * Updates user-defined version name
     */
    suspend fun updateVersionName(versionId: Long, customName: String?) {
        repository.updateVersionCustomName(versionId, customName)
    }

    /**
     * Exports note version history
     * Moved from Repository to follow Clean Architecture principles
     */
    suspend fun exportVersionHistory(noteId: Long, format: String): String {
        val versions = repository.getVersionsForNoteList(noteId)
        val note = repository.getNoteById(noteId)
        
        return when (format.lowercase()) {
            "markdown" -> {
                buildString {
                    appendLine("# ${context.resources.getString(R.string.version_history_title, note?.title ?: context.resources.getString(R.string.note_fallback))}")
                    appendLine()
                    
                    versions.forEach { version ->
                        appendLine("## ${version.formattedDate}")
                        version.customName?.let { name ->
                            appendLine("**$name**")
                            appendLine()
                        }
                        version.changeDescription?.let { desc ->
                            appendLine("*$desc*")
                            appendLine()
                        }
                        appendLine("```")
                        appendLine(version.content)
                        appendLine("```")
                        appendLine()
                        appendLine("---")
                        appendLine()
                    }
                }
            }
            else -> { // "text"
                buildString {
                    appendLine(context.resources.getString(R.string.version_history_title))
                    appendLine("=".repeat(50))
                    appendLine()
                    
                    versions.forEach { version ->
                        appendLine(context.getString(R.string.version_date_label, version.formattedDate))
                        version.customName?.let { name ->
                            appendLine(context.getString(R.string.version_title_label, name))
                        }
                        version.changeDescription?.let { desc ->
                            appendLine(context.getString(R.string.version_description_label, desc))
                        }
                        appendLine()
                        appendLine(version.content)
                        appendLine()
                        appendLine("-".repeat(50))
                        appendLine()
                    }
                }
            }
        }
    }

        /**
     * Rolls back current note to selected version
     * 
     * Fixed: Uses current content from DB (autosave ensures accuracy)
     * Creates backup before rollback to prevent data loss.
     */
    suspend fun rollbackToVersion(noteId: Long, versionId: Long, currentUiContent: String? = null): Boolean {
        return try {
            val version = repository.getVersionById(versionId)
            val currentNote = repository.getNoteById(noteId)
            
            if (version != null && currentNote != null) {
                // Create version of current state before rollback
                val backupContent = currentUiContent ?: currentNote.content
                repository.createVersion(
                    noteId = noteId,
                    content = backupContent, // Use UI content if available, otherwise from DB
                    changeDescription = context.getString(R.string.backup_before_rollback)
                )
                
                // Update note with content from version
                val updatedNote = currentNote.copy(
                    content = version.content,
                    updatedAt = System.currentTimeMillis()
                )
                repository.updateNote(updatedNote)
                
                // Create version with rollback indication
                repository.createVersion(
                    noteId = noteId,
                    content = version.content,
                                            changeDescription = context.getString(R.string.rollback_to_version, version.formattedDate)
                )
                
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
} 