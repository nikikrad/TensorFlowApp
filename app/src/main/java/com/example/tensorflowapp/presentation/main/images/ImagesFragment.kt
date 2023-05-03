package com.example.tensorflowapp.presentation.main.images

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tensorflowapp.data.database.TensorDatabase
import com.example.tensorflowapp.databinding.FragmentImagesBinding
import com.example.tensorflowapp.databinding.FragmentObjectDetectionBinding
import com.example.tensorflowapp.presentation.main.images.adapter.ImagesAdapter
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.objects.ObjectDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class ImagesFragment : Fragment() {

    private lateinit var binding: FragmentImagesBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentImagesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val type = arguments?.getInt("TYPE")!!
        lifecycleScope.launch(Dispatchers.Main) {
            val adapter = ImagesAdapter(getImage(type))
            binding.rvImages.layoutManager =
                LinearLayoutManager(
                    activity?.applicationContext,
                    LinearLayoutManager.VERTICAL,
                    false
                )
            binding.rvImages.adapter = adapter
        }
    }

    private suspend fun getImage(type: Int): MutableList<ImageWithText> {
        val imageList = mutableListOf<ImageWithText>()
        val dao = TensorDatabase.getDatabase(requireContext()).TensorDao()
        val data = dao.getAllObjectImages(type)
        data.forEach {
            imageList.add(
                ImageWithText(
                    image = BitmapFactory.decodeByteArray(
                        it.byteImage,
                        0,
                        it.byteImage.size
                    ), text = it.text
                )
            )
        }
        return imageList
    }
}