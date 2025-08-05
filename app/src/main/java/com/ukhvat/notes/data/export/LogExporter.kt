package com.ukhvat.notes.data.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import com.ukhvat.notes.R

class LogExporter(private val context: Context) {
    
    suspend fun exportApplicationLogs(): Intent? = withContext(Dispatchers.IO) {
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
            val fileName = "note_app_logs_${dateFormat.format(Date())}.txt"
            
            val logsFile = File(context.cacheDir, fileName)
            
            // Get app logs via logcat
            val process = Runtime.getRuntime().exec(arrayOf(
                "logcat", "-d", "-v", "time", 
                "--pid=${android.os.Process.myPid()}"
            ))
            
            val logs = process.inputStream.bufferedReader().use { it.readText() }
            
            // Add header with app information
            val header = """
                |# Ukhvat Logs Export
                    |**Export Date:** ${SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date())}
    |**App Version:** ${context.packageManager.getPackageInfo(context.packageName, 0).versionName}
    |**Android Version:** ${android.os.Build.VERSION.RELEASE}
    |**Device:** ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}
                |
                |---
                |
            """.trimMargin()
            
            logsFile.writeText(header + logs)
            
            // Create Intent for sending
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                logsFile
            )
            
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Ukhvat Logs - ${dateFormat.format(Date())}")
                putExtra(Intent.EXTRA_TEXT, context.getString(R.string.log_app_text))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
        } catch (e: IOException) {
            null
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun getLogsSummary(): String = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf(
                "logcat", "-d", "-v", "brief", 
                "--pid=${android.os.Process.myPid()}"
            ))
            
            val logs = process.inputStream.bufferedReader().use { it.readLines() }
            
            val errorCount = logs.count { it.contains(" E/") }
            val warningCount = logs.count { it.contains(" W/") }
            val infoCount = logs.count { it.contains(" I/") }
            
            context.getString(R.string.logs_summary, logs.size, errorCount, warningCount, infoCount)
            
        } catch (e: Exception) {
            context.getString(R.string.logs_info_failed)
        }
    }
} 