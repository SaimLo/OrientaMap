package com.showroomprive.orientamap.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [MapProjectEntity::class],
    version = 3,
    exportSchema = true
)
abstract class OrientaMapDatabase : RoomDatabase() {
    abstract fun mapProjectDao(): MapProjectDao
}
