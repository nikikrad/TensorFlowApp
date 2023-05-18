package com.example.tensorflowapp.presentation.main.face

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
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
import com.example.tensorflowapp.databinding.FragmentFaceDetectionBinding
import com.example.tensorflowapp.presentation.main.images.ImageWithText
import com.example.tensorflowapp.presentation.main.`object`.BoxWithText
import com.example.tensorflowapp.presentation.main.`object`.adapter.ClassificationAdapter
import com.example.tensorflowapp.presentation.main.`object`.model.ModelFirebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class FaceDetectionFragment : Fragment() {

    private lateinit var binding: FragmentFaceDetectionBinding
    private lateinit var faceDetector: FaceDetector
    private lateinit var photoFile: File
    private lateinit var imageLabeler: ImageLabeler
    private val root = Firebase.database.reference
    private val reference = FirebaseStorage.getInstance().reference.child("Images")
    private var imageUri: Uri? = null
    private var auth = FirebaseAuth.getInstance()
    private var description: String = ""
    private val selectedImages: MutableList<Bitmap> = mutableListOf()
    private val bitmapList: MutableList<ImageWithText> = mutableListOf()
    private val viewModel: FaceDetectionViewModel = FaceDetectionViewModel()
    private var adapter = ClassificationAdapter(bitmapList)
    private val REQUEST_PICK_IMAGE = 1000
    private val PICK_IMAGES_REQUEST_CODE = 100
    private val REQUEST_CAPTURE_IMAGE = 1001

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFaceDetectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("Range")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imageLabeler = ImageLabeling.getClient(
            ImageLabelerOptions.Builder()
                .setConfidenceThreshold(0.7f)
                .build()
        )

        val highAccuracyOpts = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .enableTracking()
            .build()

        faceDetector = FaceDetection.getClient(highAccuracyOpts)


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
            bundle.putInt("TYPE", 3)
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
        val finalBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val image = InputImage.fromBitmap(finalBitmap, 0)
        faceDetector.process(image)
            .addOnFailureListener { error: Exception -> error.printStackTrace() }
            .addOnSuccessListener { faces: List<Face> ->
                if (faces.isEmpty()) {
                    binding.tvOutput.text = "No faces detected"
                    description = "No faces detected"
                } else {
                    binding.tvOutput.text = String.format("%d faces detected", faces.size)
                    description = String.format("%d faces detected", faces.size)
                    val boxes: MutableList<BoxWithText?> = ArrayList()
                    for (face in faces) {
                        boxes.add(BoxWithText(face.trackingId.toString() + "", face.boundingBox))
                    }
                    binding.ivImage.setImageBitmap(drawDetectionResult(finalBitmap, boxes))
                }
            }
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
                    val bitmap = loadFromUri(uri!!)
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

    private fun drawDetectionResult(bitmap: Bitmap,detectionResults: List<BoxWithText?>): Bitmap? {
        val outputBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(outputBitmap)
        val pen = Paint()
        pen.setTextAlign(Paint.Align.LEFT)
        for (box in detectionResults) {
            // draw bounding box
            pen.setColor(Color.RED)
            pen.setStrokeWidth(4f)
            pen.setStyle(Paint.Style.STROKE)
            if (box != null) {
                canvas.drawRect(box.rect, pen)
            }
            val tagSize = Rect(0, 0, 0, 0)

            // calculate the right font size
            pen.setStyle(Paint.Style.FILL_AND_STROKE)
            pen.setColor(Color.YELLOW)
            pen.setStrokeWidth(1f)
            pen.setTextSize(96f)
            if (box != null) {
                pen.getTextBounds(box.text, 0, box.text.length, tagSize)
            }
            val fontSize: Float = pen.getTextSize() * box?.rect!!.width() / tagSize.width()

            // adjust the font size so texts are inside the bounding box
            if (fontSize < pen.getTextSize()) {
                pen.setTextSize(fontSize)
            }
            var margin: Float = (box.rect.width() - tagSize.width()) / 2.0f
            if (margin < 0f) margin = 0f
        }
        return outputBitmap
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
                                            type = "3"
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
                        binding.tvOutput.text = "No faces detected"
                        description = "No faces detected"
                        bitmapList.add(ImageWithText(selectedImages[kostil], "No faces detected"))
                        adapter.notifyDataSetChanged()
                        kostil++
                    } else {
                        binding.tvOutput.text = String.format("%d faces detected", faces.size)
                        description = String.format("%d faces detected", faces.size)
                        val boxes: MutableList<BoxWithText?> = ArrayList()
                        for (face in faces) {
                            boxes.add(BoxWithText(face.trackingId.toString() + "", face.boundingBox))
                        }
                        binding.ivImage.setImageBitmap(drawDetectionResult(finalBitmap, boxes))
                        bitmapList.add(ImageWithText((drawDetectionResult(selectedImages[kostil].copy(Bitmap.Config.ARGB_8888, true), boxes))!!, String.format("%d faces detected", faces.size)))
                        adapter.notifyDataSetChanged()
                        kostil++
                    }
                }
        }
        binding.tvGroupOutput.text = "Count of group images ${bitmap.size}"
        kostil = 0
    }

}