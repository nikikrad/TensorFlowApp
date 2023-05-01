package com.example.tensorflowapp.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TensorEntity::class], version = 1, exportSchema = false)
abstract class TensorDatabase: RoomDatabase() {


    abstract fun TensorDao(): TensorDao

    companion object {
        @Volatile
        private var INSTANCE: TensorDatabase? = null

        fun getDatabase(context: Context): TensorDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }

            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TensorDatabase::class.java,
                    "tensor_database"
                ).build()
                INSTANCE = instance
                return instance
            }
        }
    }

}