package com.ukhvat.notes.data.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.room.withTransaction
import com.ukhvat.notes.data.database.AppDatabase
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import com.ukhvat.notes.R
// Removed Hilt imports for Koin migration

/**
 * SQLite database exporter.
 * 
 * Creates full database copy for backup.
 * Includes all notes, version history and metadata.
 * 
 * @author AI Assistant
      * Database export
 */
/**
 * MIGRATED FROM HILT TO KOIN: No dependencies, simple constructor
 */
class DatabaseExporter(private val database: AppDatabase) {
    
    suspend fun exportDatabase(context: Context): Intent? = withContext(Dispatchers.IO) {
        try {
            // Barrier: ensure all pending Room writes are flushed before checkpoint
            try {
                database.withTransaction { /* no-op barrier */ }
            } catch (_: Exception) { }
            
            // Get database file path
            val databasePath = context.getDatabasePath(AppDatabase.DATABASE_NAME)
            
            if (!databasePath.exists()) {
                return@withContext null
            }
            
            // Ensure WAL is checkpointed using the same connection pool used by Room
            try {
                val supportDb = database.openHelper.writableDatabase
                supportDb.execSQL("PRAGMA wal_checkpoint(TRUNCATE)")
                supportDb.execSQL("PRAGMA optimize")
            } catch (ignored: Exception) {
                // Best-effort; if fails, proceed with file copy
            }

            // Create DB copy in cache folder for export
            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
            val exportFileName = "note_database_${dateFormat.format(Date())}.db"
            val exportFile = File(context.cacheDir, exportFileName)
            
            // Copy database file
            copyDatabaseFile(databasePath, exportFile)
            
            // Create Intent for sending
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                exportFile
            )
            
            Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.database_export_subject, dateFormat.format(Date())))
                putExtra(Intent.EXTRA_TEXT, buildDatabaseInfo(context))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
        } catch (e: Exception) {
            null
        }
    }
    
    private suspend fun copyDatabaseFile(source: File, destination: File) = withContext(Dispatchers.IO) {
        FileInputStream(source).use { input ->
            FileOutputStream(destination).use { output ->
                input.copyTo(output, bufferSize = 8192)
            }
        }
    }
    
    private fun buildDatabaseInfo(context: Context): String {
        return try {
            val databasePath = context.getDatabasePath(AppDatabase.DATABASE_NAME)
            val fileSize = databasePath.length() / 1024 // KB
            val versionName = context.packageManager.getPackageInfo(context.packageName, 0).versionName
            
            """
            |📱 ${context.resources.getString(R.string.database_export_header)}
            |
            |File information:
            |• Size: ${fileSize}KB
|• App version: $versionName
|• Export date: ${SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date())}
|• Format: SQLite Database (.db)
            |
            |Content:
            |• All notes with full text
|• Note version history
|• Creation and modification timestamps
            |• Cached note titles
            |
            |How to open:
            |• DB Browser for SQLite (free)
            |• SQLite Expert
            |• Any SQLite database software
            |
            |📂 Table structure:
|• note_metadata - note metadata (titles, dates)
|• note_content - full note content
|• note_versions - change history
            """.trimMargin()
        } catch (e: Exception) {
                            context.getString(R.string.database_export_success)
        }
    }
    
    suspend fun getDatabaseInfo(context: Context): String = withContext(Dispatchers.IO) {
        try {
            val databasePath = context.getDatabasePath(AppDatabase.DATABASE_NAME)
            val fileSize = if (databasePath.exists()) {
                databasePath.length() / 1024 // KB
            } else {
                0
            }
            
            """
            Database: ${AppDatabase.DATABASE_NAME}
File size: ${fileSize}KB
Path: ${databasePath.absolutePath}
Exists: ${databasePath.exists()}
            """.trimIndent()
        } catch (e: Exception) {
                            context.getString(R.string.database_info_failed)
        }
    }
} 