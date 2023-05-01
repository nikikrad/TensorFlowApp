package com.example.tensorflowapp.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "tensor_table", indices = [Index(value = ["byteImage"], unique = true)])
class TensorEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    var text: String = "",
    var byteImage: ByteArray,
    var type: Int = 0,
)