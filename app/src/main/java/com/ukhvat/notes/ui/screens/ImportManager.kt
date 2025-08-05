package com.ukhvat.notes.ui.screens

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.ukhvat.notes.R
import com.ukhvat.notes.data.database.AppDatabase
import com.ukhvat.notes.data.export.ArchiveImporter
import com.ukhvat.notes.data.export.DatabaseImporter
import com.ukhvat.notes.data.export.FolderImporter
import com.ukhvat.notes.domain.model.Note
import com.ukhvat.notes.domain.repository.NotesRepository
import com.ukhvat.notes.domain.util.Toaster


/**
 * Note import manager
 * 
 * Centralized class for all note import operations from various sources.
 * Extracted from NotesListViewModel for architecture and testability.
 * 
 * Batch optimizations:
 * - Uses existing importers with optimized methods
 * - DatabaseImporter returns number of imported notes
 * - ArchiveImporter and FolderImporter work similarly
 * - Automatic Toast notifications and error handling
 * 
 * Supported import formats:
 * - SQLite database (.db files) - full recovery with versions
 * - ZIP archives (.zip) - .md and .txt file extraction with UTF-8 support
 * - Folders with MD and TXT files - recursive structure scanning
 * 
 * Data security and integrity:
 * - Automatic ID conflict resolution via timestamp-based generation
 * - Filtering corrupted/empty files during import
 * - Toast notifications with detailed import statistics
 * - Graceful error handling without app crashes
 *
 * @param databaseImporter Importer for SQLite databases
 * @param archiveImporter Importer for ZIP archives
 * @param folderImporter Importer for folders with MD and TXT files
 * @param toaster Service for user notifications
 */
class ImportManager(
    private val databaseImporter: DatabaseImporter,
    private val archiveImporter: ArchiveImporter,
    private val folderImporter: FolderImporter,
    private val toaster: Toaster,
    private val context: Context
) {
    
    /**
     * SQLite database import
     * 
     * Uses existing DatabaseImporter.importFromDatabase()
     * which returns number of imported notes.
     * 
     * @param uri URI of selected .db file
     * @param onSuccess Callback on successful import with note count
     * @param onError Callback on import error
     */
    suspend fun importDatabaseFile(
        uri: Uri,
        onSuccess: (Int) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val importedCount = databaseImporter.importFromDatabase(uri)
            
            if (importedCount > 0) {
                toaster.toast(R.string.import_database_success, importedCount)
                onSuccess(importedCount)
            } else {
                toaster.toast(R.string.import_no_notes_found)
                onSuccess(0)
            }
        } catch (e: Exception) {
            val errorMessage = context.getString(R.string.import_failed_database, e.localizedMessage ?: "")
            toaster.toast(errorMessage)
            onError(errorMessage)
        }
    }
    
    /**
     * ZIP archive import with MD files
     * 
     * Uses existing ArchiveImporter.importFromArchive()
     * which returns number of imported notes.
     * 
     * @param uri URI of selected .zip file
     * @param onSuccess Callback on successful import with note count
     * @param onError Callback on import error
     */
    suspend fun importArchiveFile(
        uri: Uri,
        onSuccess: (Int) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val importedCount = archiveImporter.importFromArchive(uri)
            
            if (importedCount > 0) {
                toaster.toast(R.string.import_archive_success, importedCount)
                onSuccess(importedCount)
            } else {
                toaster.toast(R.string.import_no_notes_found)
                onSuccess(0)
            }
        } catch (e: Exception) {
            val errorMessage = context.getString(R.string.import_failed_archive, e.localizedMessage ?: "")
            toaster.toast(errorMessage)
            onError(errorMessage)
        }
    }
    
    /**
     * Import from folder with MD files
     * 
     * Uses existing FolderImporter.importFromFolder()
     * which returns number of imported notes.
     * 
     * @param uri URI of selected folder
     * @param onSuccess Callback on successful import with note count
     * @param onError Callback on import error
     */
    suspend fun importFromFolderUri(
        uri: Uri,
        onSuccess: (Int) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val importedCount = folderImporter.importFromFolder(uri)
            
            if (importedCount > 0) {
                toaster.toast(R.string.import_folder_success, importedCount)
                onSuccess(importedCount)
            } else {
                toaster.toast(R.string.import_no_notes_found)
                onSuccess(0)
            }
        } catch (e: Exception) {
            val errorMessage = context.getString(R.string.import_failed_folder, e.localizedMessage ?: "")
            toaster.toast(errorMessage)
            onError(errorMessage)
        }
    }
} 