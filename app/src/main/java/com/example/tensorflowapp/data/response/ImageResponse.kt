package com.example.tensorflowapp.data.response

import android.graphics.Bitmap

data class ImageResponse(
    var text: String = "",
    var byteImage: String,
    var type: Int = 0,
)
