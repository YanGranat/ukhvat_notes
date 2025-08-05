package com.ukhvat.notes.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

@Database(
    entities = [
        NoteMetadataEntity::class,  // Новая: легкие метаданные
        NoteContentEntity::class,   // Новая: тяжелое содержимое  
        NoteVersionEntity::class    // Остается: история версий
    ],
    version = 8,  // Версия 8: добавлены поля корзины isDeleted, deletedAt и индекс isDeleted
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteMetadataDao(): NoteMetadataDao  // Новый DAO для метаданных
    abstract fun noteContentDao(): NoteContentDao    // Новый DAO для содержимого
    abstract fun noteVersionDao(): NoteVersionDao    // Остается без изменений

    companion object {
        const val DATABASE_NAME = "note_database"
        
        // Миграции убраны - в разработке используем fallbackToDestructiveMigration
        // Когда будут пользователи, добавим миграции обратно
    }
} 