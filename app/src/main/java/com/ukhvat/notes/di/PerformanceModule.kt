package com.ukhvat.notes.di

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

/**
 * Performance optimization module
 * 
 * Creates specialized thread pools and coroutine dispatchers
 * for different operation types instead of using common Dispatchers.IO.
 * 
 * GOAL: Eliminate contention and improve app responsiveness.
 * 
 * ARCHITECTURE:
 * - Database operations: separate thread pool (Room already configured)
 * - File I/O operations: dedicated thread pool for export/import
 * - Heavy computations: background thread pool for search
 * - Network operations: separate pool (if needed)
 * 
 * PERFORMANCE IMPACT:
 * - Reduced context switching overhead
 * - Better CPU core utilization
 * - UI thread blocking prevention
 */
val performanceModule = module {
    
    // ============ SPECIALIZED THREAD POOLS ============
    
    /**
     * THREAD POOL FOR FILE OPERATIONS
     * 
     * Used for file export/import, DocumentFile operations.
     * Separated from database operations to prevent contention.
     * 
     * Pool size: 2 threads - sufficient for file operations
     */
    single(named("file_io_executor")) {
        Executors.newFixedThreadPool(2) { runnable ->
            Thread(runnable, "file-io-${Thread.currentThread().id}").apply {
                isDaemon = true
                priority = Thread.NORM_PRIORITY - 1 // Slightly lower priority than DB
            }
        }
    }
    
    /**
     * DISPATCHER FOR FILE OPERATIONS
     * Wrapper over file_io_executor for coroutine usage
     */
    single<CoroutineDispatcher>(named("file_io_dispatcher")) {
        get<java.util.concurrent.ExecutorService>(named("file_io_executor")).asCoroutineDispatcher()
    }
    
    /**
     * THREAD POOL FOR HEAVY COMPUTATIONS
     * 
     * Used for:
     * - Search algorithms (findMatches, createHighlightedContext)
     * - Note batch processing
     * - Complex data transformations
     * 
     * Pool size: CPU cores - optimal for CPU-intensive tasks
     */
    single(named("computation_executor")) {
        val cpuCores = Runtime.getRuntime().availableProcessors()
        Executors.newFixedThreadPool(cpuCores) { runnable ->
            Thread(runnable, "computation-${Thread.currentThread().id}").apply {
                isDaemon = true
                priority = Thread.NORM_PRIORITY
            }
        }
    }
    
    /**
     * DISPATCHER FOR HEAVY COMPUTATIONS
     */
    single<CoroutineDispatcher>(named("computation_dispatcher")) {
        get<java.util.concurrent.ExecutorService>(named("computation_executor")).asCoroutineDispatcher()
    }
    
    /**
     * SINGLE THREAD FOR SHAREDPREFERENCES
     * 
     * SharedPreferences not thread-safe for writes, use single thread
     * for all app settings operations.
     */
    single(named("preferences_executor")) {
        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "preferences-io").apply {
                isDaemon = true
                priority = Thread.NORM_PRIORITY - 2 // Lowest priority
            }
        }
    }
    
    /**
     * DISPATCHER FOR SHAREDPREFERENCES
     */
    single<CoroutineDispatcher>(named("preferences_dispatcher")) {
        get<java.util.concurrent.ExecutorService>(named("preferences_executor")).asCoroutineDispatcher()
    }

    // NETWORK EXECUTOR/DISPATCHER (for AI requests and future network I/O)
    single(named("network_executor")) {
        Executors.newFixedThreadPool(4) { runnable ->
            Thread(runnable, "network-${Thread.currentThread().id}").apply {
                isDaemon = true
                priority = Thread.NORM_PRIORITY - 1
            }
        }
    }

    single<CoroutineDispatcher>(named("network_dispatcher")) {
        get<java.util.concurrent.ExecutorService>(named("network_executor")).asCoroutineDispatcher()
    }
    
    // ============ SPECIALIZED COROUTINE SCOPES ============
    
    /**
     * SCOPE FOR BACKGROUND OPERATIONS
     * 
     * Used for long-running operations not tied to UI lifecycle:
     * - Automatic version cleanup
     * - Periodic trash cleanup
     * - Background synchronization (if needed)
     * 
     * SupervisorJob: error in one operation doesn't affect others
     */
    single<CoroutineScope>(named("background_scope")) {
        CoroutineScope(
            SupervisorJob() + 
            get<CoroutineDispatcher>(named("computation_dispatcher"))
        )
    }
    
    /**
     * SCOPE FOR FILE OPERATIONS
     * Separated from background scope for better control
     */
    single<CoroutineScope>(named("file_io_scope")) {
        CoroutineScope(
            SupervisorJob() + 
            get<CoroutineDispatcher>(named("file_io_dispatcher"))
        )
    }
}