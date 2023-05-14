package com.example.tensorflowapp.presentation.main.face

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.example.tensorflowapp.presentation.main.images.ImageWithText
import com.example.tensorflowapp.presentation.main.`object`.BoxWithText
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetector
import java.util.ArrayList

class FaceDetectionViewModel: ViewModel() {

    private var description: String = ""
    private val selectedImages: MutableList<Bitmap> = mutableListOf()
    private val bitmapList: MutableList<ImageWithText> = mutableListOf()
    private lateinit var faceDetector: FaceDetector

    private fun runGroupDetection(bitmap: MutableList<Bitmap>) {
        var kostil = 0
        bitmapList.clear()
        bitmap.forEach {mBitmap ->
            val finalBitmap = mBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val image = InputImage.fromBitmap(finalBitmap, 0)
            faceDetector.process(image)
                .addOnFailureListener { error: Exception -> error.printStackTrace() }
                .addOnSuccessListener { faces: List<Face> ->
                    if (faces.isEmpty()) {
//                        binding.tvOutput.text = "No faces detected"
                        description = "No faces detected"
                        bitmapList.add(ImageWithText(selectedImages[kostil], "No faces detected"))
//                        adapter.notifyDataSetChanged()
                        kostil++
                    } else {
//                        binding.tvOutput.text = String.format("%d faces detected", faces.size)
                        description = String.format("%d faces detected", faces.size)
                        val boxes: MutableList<BoxWithText?> = ArrayList()
                        for (face in faces) {
                            boxes.add(BoxWithText(face.trackingId.toString() + "", face.boundingBox))
                        }
//                        binding.ivImage.setImageBitmap(drawDetectionResult(finalBitmap, boxes))
//                        bitmapList.add(
//                            ImageWithText((drawDetectionResult(selectedImages[kostil].copy(
//                                Bitmap.Config.ARGB_8888, true), boxes))!!, String.format("%d faces detected", faces.size))
//                        )
//                        adapter.notifyDataSetChanged()
                        kostil++
                    }
                }
        }
        kostil = 0
    }
}