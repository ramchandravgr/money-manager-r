package com.rrparallelstudio.miapp.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object MmrMigrations {
    val MIGRATION_26_27 =
        object : Migration(26, 27) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE lan_sync_log_entries ADD COLUMN unchangedCount INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    "ALTER TABLE lan_sync_log_entries ADD COLUMN addedCount INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    "ALTER TABLE lan_sync_log_entries ADD COLUMN overridesJson TEXT NOT NULL DEFAULT '[]'",
                )
            }
        }
}
