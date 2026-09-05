package com.start.launcher.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.start.launcher.data.dao.AppDao
import com.start.launcher.data.dao.CategoryDao
import com.start.launcher.data.model.AppEntity
import com.start.launcher.data.model.Category
import com.start.launcher.data.model.SortType

class Converters {
    @TypeConverter
    fun fromSortType(value: SortType): String = value.name

    @TypeConverter
    fun toSortType(value: String): SortType = SortType.valueOf(value)
}

@Database(
    entities = [Category::class, AppEntity::class],
    version = 4,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "start_database",
                )
                    // 开发阶段：数据库结构变更直接清空重建
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}