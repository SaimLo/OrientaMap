package com.showroomprive.orientamap.data

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.showroomprive.orientamap.data.local.OrientaMapDatabase
import com.showroomprive.orientamap.data.repository.RoomMapProjectRepository
import com.showroomprive.orientamap.data.repository.RoomOfflinePackageRepository

class AppContainer(context: Context) {
    private val database = Room.databaseBuilder(
        context,
        OrientaMapDatabase::class.java,
        "orientamap.db"
    )
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
        .build()

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE map_projects ADD COLUMN boundsNorth REAL")
                db.execSQL("ALTER TABLE map_projects ADD COLUMN boundsSouth REAL")
                db.execSQL("ALTER TABLE map_projects ADD COLUMN boundsEast REAL")
                db.execSQL("ALTER TABLE map_projects ADD COLUMN boundsWest REAL")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE map_projects ADD COLUMN status TEXT NOT NULL DEFAULT 'BROUILLON'"
                )
                db.execSQL("""
                    UPDATE map_projects
                    SET status = 'ZONE_DEFINIE'
                    WHERE boundsNorth IS NOT NULL
                      AND boundsSouth IS NOT NULL
                      AND boundsEast IS NOT NULL
                      AND boundsWest IS NOT NULL
                """.trimIndent())
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS offline_packages (
                        id TEXT NOT NULL PRIMARY KEY,
                        projectId TEXT NOT NULL,
                        name TEXT NOT NULL,
                        boundsNorth REAL NOT NULL,
                        boundsSouth REAL NOT NULL,
                        boundsEast REAL NOT NULL,
                        boundsWest REAL NOT NULL,
                        minZoom INTEGER NOT NULL,
                        maxZoom INTEGER NOT NULL,
                        sourceId TEXT NOT NULL,
                        attribution TEXT NOT NULL,
                        status TEXT NOT NULL,
                        downloadedTiles INTEGER NOT NULL,
                        totalTiles INTEGER NOT NULL,
                        estimatedSizeBytes INTEGER,
                        localPath TEXT,
                        createdAtEpochMillis INTEGER NOT NULL,
                        completedAtEpochMillis INTEGER,
                        FOREIGN KEY(projectId) REFERENCES map_projects(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_offline_packages_projectId " +
                        "ON offline_packages(projectId)"
                )
            }
        }
    }

    val mapProjectRepository = RoomMapProjectRepository(database.mapProjectDao())
    val offlinePackageRepository = RoomOfflinePackageRepository(
        projectDao = database.mapProjectDao(),
        packageDao = database.offlinePackageDao()
    )
}
