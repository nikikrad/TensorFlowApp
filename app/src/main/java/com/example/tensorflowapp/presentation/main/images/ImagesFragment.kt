package com.example.tensorflowapp.presentation.main.images

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.Image
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tensorflowapp.data.database.TensorDatabase
import com.example.tensorflowapp.data.response.ImageResponse
import com.example.tensorflowapp.databinding.FragmentImagesBinding
import com.example.tensorflowapp.databinding.FragmentObjectDetectionBinding
import com.example.tensorflowapp.presentation.main.images.adapter.ImagesAdapter
import com.example.tensorflowapp.presentation.main.`object`.model.ModelFirebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.objects.ObjectDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.time.temporal.ValueRange

class ImagesFragment : Fragment() {

    private lateinit var binding: FragmentImagesBinding
    private val reference = FirebaseStorage.getInstance().reference.child("Images")
    private val root = Firebase.database.reference
    private var auth = FirebaseAuth.getInstance()
    private val imageList = mutableListOf<ModelFirebase>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentImagesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        var adapter = ImagesAdapter(imageList)
        val type = arguments?.getInt("TYPE")!!


        root.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                root.child(auth.currentUser?.email.toString().substringBefore("@"))
                    .child("images")
                    .get()
                    .addOnSuccessListener {
                        it.children.forEach {
                            if (it.child("type").value.toString().toInt() == type) {
                                imageList.add(
                                    ModelFirebase(
                                        url = it.child("url").value.toString(),
                                        text = it.child("text").value.toString(),
                                        type = it.child("type").value.toString()
                                    )
                                )
                            }
                        }
                        lifecycleScope.launch(Dispatchers.Main) {
                            adapter = ImagesAdapter(imageList)//getImage(type)
                            binding.rvImages.layoutManager =
                                LinearLayoutManager(
                                    activity?.applicationContext,
                                    LinearLayoutManager.VERTICAL,
                                    false
                                )
                            binding.rvImages.adapter = adapter
                        }
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })


    }
}

//    private suspend fun getImageFromRealtimeDatabase(type: Int): MutableList<ImageWithText> {
//        val imageList = mutableListOf<ImageWithText>()
//        var auth = FirebaseAuth.getInstance()
//        var database = Firebase.database.reference
//        database.child(auth.currentUser?.email.toString().substringBefore("@"))
//            .child("2")
//            .get()
//            .addOnSuccessListener {
//                it.children.forEach {
//                    Log.e("TAG", "getImageFromRealtimeDatabase: ${it.child("byteImage").value.toString().toByteArray()}", )
//                    imageList.add(
//                        ImageWithText(
//                            image =
//                            BitmapFactory.decodeByteArray(
//                                it.child("byteImage").value.toString().toByteArray(),
//                                0,
//                                it.child("byteImage").value.toString().toByteArray().size
//                            ),
//                            text = it.child("text").value.toString()
//                        )
//                    )
//                    Log.e("TAG", "getImageFromRealtimeDatabase: ${imageList}")
//                }
//            }
//        return imageList
//
//    }
//
//    private suspend fun getImage(type: Int): MutableList<ImageWithText> {
//        val imageList = mutableListOf<ImageWithText>()
//        val dao = TensorDatabase.getDatabase(requireContext()).TensorDao()
//        val data = dao.getAllObjectImages(type)
//        data.forEach {
//            imageList.add(
//                ImageWithText(
//                    image = BitmapFactory.decodeByteArray(
//                        it.byteImage,
//                        0,
//                        it.byteImage.size
//                    ), text = it.text
//                )
//            )
//        }
//        return imageList
//    }
//}