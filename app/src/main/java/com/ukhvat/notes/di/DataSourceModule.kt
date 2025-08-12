package com.ukhvat.notes.di

import com.ukhvat.notes.data.datasource.NoteDataSourceImpl
import com.ukhvat.notes.data.datasource.ArchiveDataSourceImpl
import com.ukhvat.notes.data.datasource.SearchDataSourceImpl
import com.ukhvat.notes.data.datasource.VersionDataSourceImpl
import com.ukhvat.notes.data.datasource.TrashDataSourceImpl
import com.ukhvat.notes.data.datasource.PreferencesDataSourceImpl
import com.ukhvat.notes.domain.datasource.NoteDataSource
import com.ukhvat.notes.domain.datasource.ArchiveDataSource
import com.ukhvat.notes.domain.datasource.SearchDataSource
import com.ukhvat.notes.domain.datasource.VersionDataSource
import com.ukhvat.notes.domain.datasource.TrashDataSource
import com.ukhvat.notes.domain.datasource.PreferencesDataSource
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * KOIN MODULE FOR NEW DATASOURCE CLASSES
 * 
 * Module contains new DataSource architecture to replace God Object Repository.
 * Each DataSource has clearly defined responsibility:
 * 
 * - NoteDataSource: basic CRUD operations
 * - SearchDataSource: search logic with LRU cache (critical to preserve)
 * - VersionDataSource: versioning system
 * - TrashDataSource: trash management
 * - PreferencesDataSource: app settings
 * 
 * Important: This is an ADDITIONAL module to existing appModule.
 * Old NotesRepositoryImpl continues to work for backward compatibility.
 */
val dataSourceModule = module {
    
    // ============ DATASOURCE IMPLEMENTATIONS ============
    
    /**
     * MAIN DATASOURCE FOR NOTES
     * Contains basic CRUD operations and batch processing
     */
    single<NoteDataSource> { 
        NoteDataSourceImpl(
            metadataDao = get(),  // NoteMetadataDao from main module
            contentDao = get()    // NoteContentDao from main module
        ) 
    }
    
    /**
     * Search DataSource (critically important)
     * MIGRATION: removed dependency on LegacyNotesRepositoryImpl.
     * Uses only noteContentDao for optimal search.
     */
    single<SearchDataSource> { 
        SearchDataSourceImpl(
            noteContentDao = get()     // NoteContentDao for JOIN optimization
        ) 
    }
    
    /**
     * DATASOURCE FOR VERSIONING
     * Intelligent version system with auto-cleanup
     */
    single<VersionDataSource> { 
        VersionDataSourceImpl(
            versionDao = get()  // NoteVersionDao from main module
        ) 
    }
    
    /**
     * DATASOURCE FOR TRASH
     * Soft delete and deleted notes management
     */
    single<TrashDataSource> { 
        TrashDataSourceImpl(
            metadataDao = get(),  // NoteMetadataDao
            contentDao = get(),   // NoteContentDao  
            versionDao = get()    // NoteVersionDao
        ) 
    }

    // Archive DataSource
    single<ArchiveDataSource> {
        ArchiveDataSourceImpl(
            metadataDao = get(),
            contentDao = get()
        )
    }
    
    /**
     * DATASOURCE FOR SETTINGS
     * SharedPreferences and user settings
     */
    single<PreferencesDataSource> { 
        PreferencesDataSourceImpl(
            context = androidContext()
        ) 
    }
}