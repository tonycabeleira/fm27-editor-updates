package com.cabincrew.academy.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        CategoryEntity::class,
        QuestionEntity::class,
        QuestionProgressEntity::class,
        ExamResultEntity::class,
        ExamAnswerEntity::class,
        DailyActivityEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun questionDao(): QuestionDao
    abstract fun progressDao(): ProgressDao
    abstract fun examDao(): ExamDao
    abstract fun dailyActivityDao(): DailyActivityDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "cabin_crew_academy.db")
                .build()
    }
}
