package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
  entities = [
    AppSettingsEntity::class,
    TasbihStateEntity::class,
    AdhkarProgressEntity::class,
  ],
  version = 4,
  exportSchema = false,
)
abstract class AthanDatabase : RoomDatabase() {
  abstract fun athanDao(): AthanDao

  companion object {
    @Volatile
    private var INSTANCE: AthanDatabase? = null

    fun getInstance(context: Context): AthanDatabase {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: Room.databaseBuilder(
          context.applicationContext,
          AthanDatabase::class.java,
          "athan_offline_db",
        ).fallbackToDestructiveMigration(true)
          .build()
          .also { INSTANCE = it }
      }
    }
  }
}
