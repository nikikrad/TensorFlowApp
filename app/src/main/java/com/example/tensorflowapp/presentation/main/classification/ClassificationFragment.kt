package com.example.tensorflowapp.presentation.main.classification

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.tensorflowapp.MainActivity
import com.example.tensorflowapp.R
import com.example.tensorflowapp.databinding.FragmentClassificationBinding
import com.example.tensorflowapp.databinding.FragmentMainBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ClassificationFragment : Fragment() {

    private lateinit var binding: FragmentClassificationBinding
    private lateinit var imageLabeler: ImageLabeler
    private lateinit var photoFile: File

    private val REQUEST_PICK_IMAGE = 1000
    private val REQUEST_CAPTURE_IMAGE = 1001

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentClassificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("Range")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnGalleryImage.setOnClickListener {
            onPickImage()
        }
        binding.btnCameraImage.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED
            ) {
                ActivityCompat.requestPermissions(
                    activity as MainActivity,
                    arrayOf(Manifest.permission.CAMERA),
                    PackageManager.PERMISSION_DENIED
                )
            }
            onStartCamera()

        }
        imageLabeler = ImageLabeling.getClient(
            ImageLabelerOptions.Builder()
                .setConfidenceThreshold(0.7f)
                .build()
        )
    }

    private fun onPickImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"

        startActivityForResult(intent, REQUEST_PICK_IMAGE)
    }

    private fun onStartCamera() {
        photoFile = createPhotoFile()
        val fileUri =
            context?.let { FileProvider.getUriForFile(it, "com.iago.fileprovider", photoFile) }

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri)

        startActivityForResult(intent, REQUEST_CAPTURE_IMAGE)
    }

    private fun createPhotoFile(): File {
        val photoFileDir = File(
            (activity as MainActivity).getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "SIEGA"
        )

        if (!photoFileDir.exists()) {
            photoFileDir.mkdirs()
        }

        val name = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val file = File(photoFileDir.path + File.separator + name)
        return file
    }

    private fun runClassification(bitmap: Bitmap) {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        imageLabeler.process(inputImage).addOnSuccessListener {
            if (it.size > 0) {
                val builder = StringBuilder()
                it.forEach {
                    builder.append(it.text)
                        .append(" : ")
                        .append(it.confidence)
                        .append("\n")
                }
                binding.tvOutput.text = builder.toString()
            } else {
                binding.tvOutput.text = "Could not classify"
            }
        }.addOnFailureListener {
            it.printStackTrace()
        }
    }

    private fun loadFromUri(uri: Uri): Bitmap? {
        var bitmap: Bitmap? = null
        try {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1) {
                val source =
                    ImageDecoder.createSource((activity as MainActivity).contentResolver, uri)
                bitmap = ImageDecoder.decodeBitmap(source)
            } else {
                bitmap = MediaStore.Images.Media.getBitmap(
                    (activity as MainActivity).contentResolver,
                    uri
                )
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return bitmap
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_PICK_IMAGE) {
                val uri = data?.data
                val bitmap = loadFromUri(uri!!)
                binding.ivImage.setImageBitmap(bitmap)
                if (bitmap != null) {
                    runClassification(bitmap)
                }
            } else if (requestCode == REQUEST_CAPTURE_IMAGE) {
                val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                binding.ivImage.setImageBitmap(bitmap)
                runClassification(bitmap)
            }
        }

    }

}