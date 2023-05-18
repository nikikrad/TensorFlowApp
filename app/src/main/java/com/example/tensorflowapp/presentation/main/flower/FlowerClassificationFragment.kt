package com.example.tensorflowapp.presentation.main.flower

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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tensorflowapp.MainActivity
import com.example.tensorflowapp.R
import com.example.tensorflowapp.databinding.FragmentFlowerClassificationBinding
import com.example.tensorflowapp.presentation.main.images.ImageWithText
import com.example.tensorflowapp.presentation.main.`object`.adapter.ClassificationAdapter
import com.example.tensorflowapp.presentation.main.`object`.model.ModelFirebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class FlowerClassificationFragment : Fragment() {

    private lateinit var binding: FragmentFlowerClassificationBinding
    private lateinit var photoFile: File
    private lateinit var flowerLabeler: ImageLabeler
    private val root = Firebase.database.reference
    private val reference = FirebaseStorage.getInstance().reference.child("Images")
    private var imageUri: Uri? = null
    private var auth = FirebaseAuth.getInstance()
    private var description: String = ""
    private val selectedImages: MutableList<Bitmap> = mutableListOf()
    private val bitmapList: MutableList<ImageWithText> = mutableListOf()
    private var adapter = ClassificationAdapter(bitmapList)

    private val REQUEST_PICK_IMAGE = 1000
    private val PICK_IMAGES_REQUEST_CODE = 100
    private val REQUEST_CAPTURE_IMAGE = 1001
    var imageSize = 32


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFlowerClassificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("Range")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val localModel = LocalModel.Builder().setAssetFilePath("model_flowers.tflite").build()
        val options = CustomImageLabelerOptions.Builder(localModel)
            .setConfidenceThreshold(0.7f)
            .setMaxResultCount(5)
            .build()
        flowerLabeler = ImageLabeling.getClient(options)

        binding.btnGalleryImage.setOnClickListener {
            binding.apply {
                ivImage.visibility = View.VISIBLE
                tvOutput.visibility = View.VISIBLE
                tvGroupOutput.visibility = View.INVISIBLE
                rvImages.visibility = View.INVISIBLE
            }
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
            binding.apply {
                ivImage.visibility = View.VISIBLE
                tvOutput.visibility = View.VISIBLE
                tvGroupOutput.visibility = View.INVISIBLE
                rvImages.visibility = View.INVISIBLE
            }
            onStartCamera()
        }
        binding.btnImages.setOnClickListener {
            val bundle = Bundle()
            bundle.putInt("TYPE", 4)
            findNavController().navigate(R.id.imagesFragment, bundle)
        }
        binding.btnGroupImages.setOnClickListener {
            binding.apply {
                ivImage.visibility = View.INVISIBLE
                tvOutput.visibility = View.INVISIBLE
                tvGroupOutput.visibility = View.VISIBLE
                rvImages.visibility = View.VISIBLE
                rvImages.layoutManager =
                    LinearLayoutManager(
                        activity?.applicationContext,
                        LinearLayoutManager.VERTICAL,
                        false
                    )
                rvImages.adapter = adapter
            }
            onPickGroupImages()
        }
    }

    private fun onPickGroupImages() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(
            Intent.createChooser(intent, "Select Pictures"),
            PICK_IMAGES_REQUEST_CODE
        )
    }

    private fun runDetection(bitmap: Bitmap) {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        flowerLabeler.process(inputImage).addOnSuccessListener { imageLabels: List<ImageLabel> ->
            val sb = StringBuilder()
            for (label in imageLabels) {
                sb.append(label.text).append(": ").append(label.confidence).append("\n")
            }
            if (imageLabels.isEmpty()) {
                binding.tvOutput.text = "Could not identify!!"
                description = "Could not identify!!"
            } else {
                binding.tvOutput.text = sb.toString()
                description = sb.toString()
            }
        }.addOnFailureListener { e: Exception -> e.printStackTrace() }
    }

    private fun runGroupDetection(bitmap: MutableList<Bitmap>) {
        var kostil = 0
        bitmapList.clear()
        bitmap.forEach {mBitmap ->
            val inputImage = InputImage.fromBitmap(mBitmap, 0)
            flowerLabeler.process(inputImage).addOnSuccessListener { imageLabels: List<ImageLabel> ->
                val sb = StringBuilder()
                for (label in imageLabels) {
                    sb.append(label.text).append(": ").append(label.confidence).append("\n")
                }
                if (imageLabels.isEmpty()) {
                    binding.tvOutput.text = "Could not identify!!"
                    description = "Could not identify!!"
                    bitmapList.add(ImageWithText(selectedImages[kostil], "Could not identify!!"))
                    adapter.notifyDataSetChanged()
                    kostil++
                } else {
                    binding.tvOutput.text = sb.toString()
                    description = sb.toString()
                    bitmapList.add(ImageWithText(selectedImages[kostil], sb.toString()))
                    adapter.notifyDataSetChanged()
                    kostil++
                }
            }.addOnFailureListener { e: Exception -> e.printStackTrace() }
        }
        binding.tvGroupOutput.text = "Count of group images ${bitmap.size}"
        kostil = 0
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
        imageUri = fileUri
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
        if (requestCode == PICK_IMAGES_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val clipData = data.clipData
            selectedImages.clear()
            if (clipData != null) {
                for (i in 0 until clipData.itemCount) {
                    val uri = clipData.getItemAt(i).uri
                    val inputStream =
                        (activity as MainActivity).contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    selectedImages.add(bitmap)
                }
                runGroupDetection(selectedImages)
            } else {
                val uri = data.data
                val inputStream = (activity as MainActivity).contentResolver.openInputStream(uri!!)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                selectedImages.add(bitmap)
                selectedImages.forEach {
                    bitmapList.add(ImageWithText(it, ""))
                }
            }
        } else {
            if (resultCode == Activity.RESULT_OK) {
                if (requestCode == REQUEST_PICK_IMAGE) {
                    val uri = data?.data
                    imageUri = uri
                    uploadToFirebase(uri!!)
                    val bitmap = loadFromUri(uri)
                    binding.ivImage.setImageBitmap(bitmap)
                    if (bitmap != null) {
                        runDetection(bitmap)
                    }
                } else if (requestCode == REQUEST_CAPTURE_IMAGE) {
                    uploadToFirebase(imageUri!!)
                    val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                    binding.ivImage.setImageBitmap(bitmap)
                    runDetection(bitmap)
                }
            }
        }
    }

    private fun uploadToFirebase(uri: Uri) {
        val fileRef: StorageReference =
            reference.child(System.currentTimeMillis().toString())
        try {
            fileRef.putFile(uri)
                .addOnSuccessListener {

                    fileRef.downloadUrl
                        .addOnSuccessListener { url ->

                            val modelId: String? = root.push().key
                            if (modelId != null) {
                                root.child(auth.currentUser?.email.toString().substringBefore("@"))
                                    .child("images")
                                    .child(modelId)
                                    .setValue(
                                        ModelFirebase(
                                            url = url.toString(),
                                            text = description,
                                            type = "4"
                                        )
                                    )
                            }
                            binding.progressBar.visibility = View.INVISIBLE
                            Toast.makeText(
                                requireContext(),
                                "Uploaded Successfully",
                                Toast.LENGTH_SHORT
                            ).show()

                        }

                }.addOnProgressListener {
                    binding.progressBar.setVisibility(View.VISIBLE)
                }.addOnFailureListener {
                    binding.progressBar.setVisibility(View.INVISIBLE)
                    Toast.makeText(requireContext(), "Uploading Failed !!", Toast.LENGTH_SHORT)
                        .show()
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }
}