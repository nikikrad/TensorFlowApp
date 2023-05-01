package com.example.tensorflowapp.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TensorDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addImage(tensorEntity: TensorEntity)

    @Query("DELETE FROM tensor_table WHERE id = :id")
    suspend fun deleteImage(id: String)

    @Query("SELECT * FROM tensor_table")
    suspend fun readAllData(): List<TensorEntity>
}