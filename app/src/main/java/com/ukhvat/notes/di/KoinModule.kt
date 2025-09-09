package com.ukhvat.notes.di

import androidx.room.Room
import com.ukhvat.notes.data.database.AppDatabase
import com.ukhvat.notes.data.database.NoteMetadataDao
import com.ukhvat.notes.data.database.NoteContentDao
import com.ukhvat.notes.data.database.NoteVersionDao
// MIGRATION: LegacyNotesRepositoryImpl import removed
import com.ukhvat.notes.domain.repository.NotesRepository
import com.ukhvat.notes.data.util.ToasterImpl
import com.ukhvat.notes.domain.util.Toaster
import com.ukhvat.notes.domain.util.NotificationService
import com.ukhvat.notes.data.services.NotificationServiceImpl

import com.ukhvat.notes.ui.screens.ExportManager
import com.ukhvat.notes.ui.screens.ImportManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * KOIN DI MODULE - MIGRATION FROM HILT
 * 
 * Unified dependency injection module replacing Hilt modules:
 * - DatabaseModule.kt functionality (Room configuration + DAOs)
 * - ToasterModule.kt functionality (Toaster interface binding)
 * - Manager classes (SearchManager, ExportManager, ImportManager)
 * 
 * Key optimizations preserved from Hilt configuration:
 * - Room background query executor (critical for startup performance)
 * - Database configuration optimizations
 * - Singleton scoping for performance-critical components
 */
val appModule = module {
    
    /**
     * Optimized Room database configuration (preserved from Hilt)
     * 
     * CRITICAL PERFORMANCE SETTINGS:
     * - setQueryExecutor(Dispatchers.IO.asExecutor()): all DB queries in background thread
     * - fallbackToDestructiveMigration(): recreate DB on schema changes (dev mode)
     * 
     * STARTUP LAG ELIMINATION:
     * Executor configuration ensures Room initialization and all queries 
     * don't block main UI thread, providing smooth scrolling from first second.
     */
    single<AppDatabase> {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
         .addMigrations(AppDatabase.MIGRATION_8_9, AppDatabase.MIGRATION_9_10, AppDatabase.MIGRATION_10_11, AppDatabase.MIGRATION_11_12, AppDatabase.MIGRATION_12_13)
         .setQueryExecutor(Dispatchers.IO.asExecutor())  // DB operations in background thread
         .build()
    }
    
    // DAOs for split architecture (preserving exact functionality)
    single<NoteMetadataDao> { get<AppDatabase>().noteMetadataDao() }
    single<NoteContentDao> { get<AppDatabase>().noteContentDao() }
    single<NoteVersionDao> { get<AppDatabase>().noteVersionDao() }
    single<com.ukhvat.notes.data.database.NoteTagDao> { get<AppDatabase>().noteTagDao() }
    
    // Repository interface binding - NEW MODULAR ARCHITECTURE
    single<NotesRepository> { 
        com.ukhvat.notes.data.repository.ModularNotesRepository(
            noteDataSource = get(),        // NoteDataSource
            searchDataSource = get(),      // SearchDataSource (CRITICAL: preserves LRU cache)
            versionDataSource = get(),     // VersionDataSource  
            trashDataSource = get(),       // TrashDataSource
            archiveDataSource = get(),     // ArchiveDataSource
            preferencesDataSource = get(), // PreferencesDataSource
            tagDataSource = get()          // TagDataSource
        )
    }
    
    // MIGRATION: LegacyNotesRepositoryImpl removed, using only ModularNotesRepository
    
    // Toaster interface binding (exact same as Hilt ToasterModule)
    single<Toaster> { 
        ToasterImpl(androidContext()) 
    }
    
    // LocaleManager for language switching
    single<com.ukhvat.notes.data.LocaleManager> {
        com.ukhvat.notes.data.LocaleManager(androidContext())
    }

    // Notification service for quick note creation from notification drawer
    single<NotificationService> {
        NotificationServiceImpl(androidContext(), get())
    }
    
    // Export utilities (migrated from Hilt @Inject constructors)
    single<com.ukhvat.notes.data.export.MarkdownExporter> { 
        com.ukhvat.notes.data.export.MarkdownExporter() 
    }
    
    single<com.ukhvat.notes.data.export.DatabaseExporter> { 
        com.ukhvat.notes.data.export.DatabaseExporter(get()) 
    }
    
    single<com.ukhvat.notes.data.export.IndividualMarkdownExporter> { 
        com.ukhvat.notes.data.export.IndividualMarkdownExporter(androidContext()) 
    }
    
    // Import utilities (migrated from Hilt @Inject constructors)
    single<com.ukhvat.notes.data.export.DatabaseImporter> { 
        com.ukhvat.notes.data.export.DatabaseImporter(androidContext(), get(), get()) // Context + Toaster + AppDatabase
    }
    
    single<com.ukhvat.notes.data.export.ArchiveImporter> { 
        com.ukhvat.notes.data.export.ArchiveImporter(androidContext(), get<NotesRepository>()) // Context + NotesRepository interface
    }
    
    single<com.ukhvat.notes.data.export.FolderImporter> { 
        com.ukhvat.notes.data.export.FolderImporter(androidContext(), get<NotesRepository>()) // Context + NotesRepository interface
    }
    
    // Manager classes (CRITICAL: SearchManager functionality moved to SearchDataSourceImpl)
    single<ExportManager> { 
        ExportManager(get(), get(), get(), get(), androidContext(), get(named("file_io_dispatcher"))) // MarkdownExporter + DatabaseExporter + IndividualMarkdownExporter + Toaster + Context + FileIoDispatcher
    }
    
    single<ImportManager> { 
        ImportManager(get(), get(), get(), get(), androidContext()) // DatabaseImporter + ArchiveImporter + FolderImporter + Toaster + Context
    }
    
    // ViewModels (migrating one by one)
    factory<com.ukhvat.notes.ui.screens.TrashViewModel> { 
        com.ukhvat.notes.ui.screens.TrashViewModel(get(), get()) // NotesRepository + Toaster
    }

    factory<com.ukhvat.notes.ui.screens.ArchiveViewModel> {
        com.ukhvat.notes.ui.screens.ArchiveViewModel(get(), get()) // NotesRepository + Toaster
    }
    
    factory<com.ukhvat.notes.ui.screens.VersionHistoryViewModel> { 
        com.ukhvat.notes.ui.screens.VersionHistoryViewModel(get(), androidContext()) // NotesRepository + Context
    }
    
    factory<com.ukhvat.notes.ui.screens.NoteEditViewModel> { 
        com.ukhvat.notes.ui.screens.NoteEditViewModel(get(), get(), androidContext(), get()) // NotesRepository + Toaster + Context + AiDataSource
    }
    
    factory<com.ukhvat.notes.ui.screens.NotesListViewModel> {
        com.ukhvat.notes.ui.screens.NotesListViewModel(
            get(), // NotesRepository
            get(), // Toaster
            androidContext(), // Context
            get(), // ExportManager
            get(), // ImportManager
            get(), // SearchDataSource - CRITICAL: preserves advanced search with LRU cache
            get(), // LocaleManager - for language switching
            get()  // NotificationService - for quick note creation
        )
    }
}