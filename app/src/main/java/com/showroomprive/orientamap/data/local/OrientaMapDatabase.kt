package com.showroomprive.orientamap.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [MapProjectEntity::class, OfflinePackageEntity::class],
    version = 4,
    exportSchema = true
)
abstract class OrientaMapDatabase : RoomDatabase() {
    abstract fun mapProjectDao(): MapProjectDao
    abstract fun offlinePackageDao(): OfflinePackageDao
}
