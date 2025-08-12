package com.ukhvat.notes.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        NoteMetadataEntity::class,  // Новая: легкие метаданные
        NoteContentEntity::class,   // Новая: тяжелое содержимое  
        NoteVersionEntity::class    // Остается: история версий
    ],
    version = 10,  // Версия 10: архивирование (isArchived, archivedAt) + индекс
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteMetadataDao(): NoteMetadataDao  // Новый DAO для метаданных
    abstract fun noteContentDao(): NoteContentDao    // Новый DAO для содержимого
    abstract fun noteVersionDao(): NoteVersionDao    // Остается без изменений

    companion object {
        const val DATABASE_NAME = "note_database"
        
        // Миграция 8->9: добавлен флаг избранного и индекс
        val MIGRATION_8_9: Migration = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE note_metadata ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_isFavorite ON note_metadata(isFavorite)")
            }
        }

        // Миграция 9->10: архивирование заметок
        val MIGRATION_9_10: Migration = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE note_metadata ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE note_metadata ADD COLUMN archivedAt INTEGER")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_isArchived ON note_metadata(isArchived)")
            }
        }
    }
} 