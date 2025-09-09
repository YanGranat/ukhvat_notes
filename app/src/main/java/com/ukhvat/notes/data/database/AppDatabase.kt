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
        NoteVersionEntity::class,   // Остается: история версий
        NoteTagEntity::class        // Новая: нормализованные хештеги
    ],
    version = 13,  // v13: note_versions.diffOpsJson
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteMetadataDao(): NoteMetadataDao  // Новый DAO для метаданных
    abstract fun noteContentDao(): NoteContentDao    // Новый DAO для содержимого
    abstract fun noteVersionDao(): NoteVersionDao    // Остается без изменений
    abstract fun noteTagDao(): NoteTagDao            // DAO для хештегов

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

        // Миграция 10->11: добавление AI метаданных в note_versions
        val MIGRATION_10_11: Migration = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE note_versions ADD COLUMN aiProvider TEXT")
                db.execSQL("ALTER TABLE note_versions ADD COLUMN aiModel TEXT")
                db.execSQL("ALTER TABLE note_versions ADD COLUMN aiDurationMs INTEGER")
            }
        }

        // Миграция 11->12: таблица note_tags и колонка aiHashtags в note_versions
        val MIGRATION_11_12: Migration = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create note_tags table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS note_tags (
                        noteId INTEGER NOT NULL,
                        tag TEXT NOT NULL,
                        PRIMARY KEY(noteId, tag),
                        FOREIGN KEY(noteId) REFERENCES note_metadata(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_note_tags_tag ON note_tags(tag)")

                // Add aiHashtags column to note_versions
                db.execSQL("ALTER TABLE note_versions ADD COLUMN aiHashtags TEXT")
            }
        }

        // Миграция 12->13: колонка diffOpsJson для хранения журнала правок в note_versions
        val MIGRATION_12_13: Migration = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE note_versions ADD COLUMN diffOpsJson TEXT")
            }
        }
    }
} 