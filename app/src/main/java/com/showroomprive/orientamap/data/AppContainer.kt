package com.showroomprive.orientamap.data

import android.content.Context
import androidx.room.Room
import com.showroomprive.orientamap.data.local.OrientaMapDatabase
import com.showroomprive.orientamap.data.repository.RoomMapProjectRepository

class AppContainer(context: Context) {
    private val database = Room.databaseBuilder(
        context,
        OrientaMapDatabase::class.java,
        "orientamap.db"
    ).build()

    val mapProjectRepository = RoomMapProjectRepository(database.mapProjectDao())
}
