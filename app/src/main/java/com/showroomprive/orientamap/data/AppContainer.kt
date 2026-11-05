package com.showroomprive.orientamap.data

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.showroomprive.orientamap.data.local.OrientaMapDatabase
import com.showroomprive.orientamap.data.repository.RoomMapProjectRepository

class AppContainer(context: Context) {
    private val database = Room.databaseBuilder(
        context,
        OrientaMapDatabase::class.java,
        "orientamap.db"
    )
        .addMigrations(MIGRATION_1_2)
        .build()

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE map_projects ADD COLUMN boundsNorth REAL")
                database.execSQL("ALTER TABLE map_projects ADD COLUMN boundsSouth REAL")
                database.execSQL("ALTER TABLE map_projects ADD COLUMN boundsEast REAL")
                database.execSQL("ALTER TABLE map_projects ADD COLUMN boundsWest REAL")
            }
        }
    }

    val mapProjectRepository = RoomMapProjectRepository(database.mapProjectDao())
}
