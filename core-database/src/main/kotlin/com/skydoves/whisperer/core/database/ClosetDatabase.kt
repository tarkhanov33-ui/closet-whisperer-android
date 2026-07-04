package com.skydoves.whisperer.core.database

import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.RoomDatabase

@Entity(tableName = "dummy_table")
data class DummyEntity(
  @PrimaryKey val id: Int = 0,
  val value: String = ""
)

@Database(
  entities = [DummyEntity::class],
  version = 1,
  exportSchema = false,
)
abstract class ClosetDatabase : RoomDatabase() {
  
}

