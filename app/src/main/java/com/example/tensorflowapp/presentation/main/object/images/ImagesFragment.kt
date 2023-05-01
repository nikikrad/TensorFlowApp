package com.example.tensorflowapp.presentation.main.`object`.images

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.tensorflowapp.data.database.TensorDatabase
import com.example.tensorflowapp.databinding.FragmentImagesBinding
import com.example.tensorflowapp.databinding.FragmentObjectDetectionBinding
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.objects.ObjectDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class ImagesFragment : Fragment() {

    private lateinit var binding: FragmentImagesBinding
    private lateinit var objectDetector: ObjectDetector
    private lateinit var photoFile: File
    private lateinit var imageLabeler: ImageLabeler

    private val REQUEST_PICK_IMAGE = 1000
    private val REQUEST_CAPTURE_IMAGE = 1001

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentImagesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch(Dispatchers.Main) {
            binding.ivImage.setImageBitmap(getImage())
        }
    }

    private suspend fun getImage(): Bitmap? {
        val dao = TensorDatabase.getDatabase(requireContext()).TensorDao()
        val data = dao.readAllData()
        val image = BitmapFactory.decodeByteArray(data[0].byteImage, 0, data[0].byteImage.size)
        return image
    }
}