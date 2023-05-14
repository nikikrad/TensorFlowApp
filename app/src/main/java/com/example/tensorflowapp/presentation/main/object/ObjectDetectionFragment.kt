package com.example.tensorflowapp.presentation.main.`object`

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentResolver
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
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tensorflowapp.MainActivity
import com.example.tensorflowapp.R
import com.example.tensorflowapp.databinding.FragmentObjectDetectionBinding
import com.example.tensorflowapp.presentation.main.images.ImageWithText
import com.example.tensorflowapp.presentation.main.images.adapter.ImagesAdapter
import com.example.tensorflowapp.presentation.main.`object`.adapter.ClassificationAdapter
import com.example.tensorflowapp.presentation.main.`object`.model.ModelFirebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class ObjectDetectionFragment : Fragment() {

    private lateinit var binding: FragmentObjectDetectionBinding
    private lateinit var objectDetector: ObjectDetector
    private lateinit var photoFile: File
    private lateinit var imageLabeler: ImageLabeler
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentObjectDetectionBinding.inflate(inflater, container, false)
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

        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
        objectDetector = ObjectDetection.getClient(options)



        binding.btnGalleryImage.setOnClickListener {
            binding.apply {
                ivImage.visibility = View.VISIBLE
                tvOutput.visibility = View.VISIBLE
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
                rvImages.visibility = View.INVISIBLE
            }
            onStartCamera()

        }
        binding.btnImages.setOnClickListener {
            val bundle = Bundle()
            bundle.putInt("TYPE", 2)
            findNavController().navigate(R.id.imagesFragment, bundle)
        }

        binding.btnGroupImages.setOnClickListener {
            binding.apply {
                ivImage.visibility = View.INVISIBLE
                tvOutput.visibility = View.INVISIBLE
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


    private fun runDetection(bitmap: Bitmap?) {
        var kostil = 0
        val inputImage = InputImage.fromBitmap(bitmap!!, 0)
        objectDetector.process(inputImage)
            .addOnSuccessListener { detectorObjects ->
                if (!detectorObjects.isEmpty()) {
                    val builder = StringBuilder()
                    val boxes = mutableListOf<BoxWithText>()
                    val text = ""
                    detectorObjects.forEach {
                        if (!it.labels.isEmpty()) {
                            val label = it.labels[0].text
                            builder.append(label)
                                .append(": ")
                                .append(it.labels[0].confidence)
                                .append("\n")
                            boxes.add(BoxWithText(label, it.boundingBox))
                        } else {
                            binding.tvOutput.text = "Unknown"
                        }
                    }
                    binding.tvOutput.text = builder.toString()


                    description = builder.toString()
                    if (binding.checkBox.isChecked) {
                        binding.ivImage.setImageBitmap(drawDetectionResult(bitmap, boxes))
//                        lifecycleScope.launch(Dispatchers.IO) {
//                            addImageToDatabase(
//                                drawDetectionResult(bitmap, boxes)!!,
//                                builder.toString()
//                            )
//                        }
                    } else {
//                        lifecycleScope.launch(Dispatchers.IO) {
//                            addImageToDatabase(bitmap, builder.toString())
//                        }
                    }
                } else {
                    binding.tvOutput.text = "Could not detect"
                }
            }.addOnFailureListener {
                it.printStackTrace()
            }
        kostil = 0
    }

//    private suspend fun addImageToRealtimeDatabase(bitmap: Bitmap, text: String) {
//        val stream = ByteArrayOutputStream()
//        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
//        var database = Firebase.database.reference
//        lifecycleScope.launch(Dispatchers.IO) {
//            database.child(auth.currentUser?.email.toString().substringBefore("@")).get()
//                .addOnSuccessListener {
//
//                    if (it.children.toList() !== null) {
//                        database.child(auth.currentUser?.email.toString().substringBefore("@"))
//                            .child("2")
//                            .child("0")
//                            .setValue(
//                                ImageResponse(
//                                    text = text,
//                                    byteImage = bitmap.toString(),
//                                    type = 2
//                                )
//                            )
//                    } else {
//                        database.child(auth.currentUser?.email.toString().substringBefore("@"))
//                            .child("${it.children.toList().last().value.toString().toInt() + 1}")
//                            .setValue(
//                                ImageResponse(
//                                    text = text,
//                                    byteImage = bitmap.toString(),
//                                    type = 2
//                                )
//                            )
//                    }
//                }
//        }
//    }
//
//    private suspend fun addImageToDatabase(bitmap: Bitmap, text: String) {
//        val stream = ByteArrayOutputStream()
//        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
//        val dao = TensorDatabase.getDatabase(requireContext()).TensorDao()
//        dao.addImage(TensorEntity(text = text, byteImage = stream.toByteArray(), type = 2))
//    }

    private fun onPickImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_PICK_IMAGE)
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

    private fun runGroupDetection(bitmap: MutableList<Bitmap>) {
        var kostil = 0
        bitmapList.clear()
        bitmap.forEach { mBitmap ->
            val inputImage = InputImage.fromBitmap(mBitmap!!, 0)
            objectDetector.process(inputImage)
                .addOnSuccessListener { detectorObjects ->
                    if (!detectorObjects.isEmpty()) {
                        val builder = StringBuilder()
                        val boxes = mutableListOf<BoxWithText>()
                        detectorObjects.forEach {
                            if (!it.labels.isEmpty()) {
                                val label = it.labels[0].text
                                builder.append(label)
                                    .append(": ")
                                    .append(it.labels[0].confidence)
                                    .append("\n")
                                boxes.add(BoxWithText(label, it.boundingBox))
                            } else {
                                binding.tvOutput.text = "Unknown"

//                                bitmapList.add(
//                                    ImageWithText(
//                                        image = mBitmap,
//                                        text = builder.toString()
//                                    )
//                                )
//                                adapter.notifyDataSetChanged()
                            }
                        }
                        binding.tvOutput.text = builder.toString()
                        bitmapList.add(ImageWithText(selectedImages[kostil], builder.toString()))
                        adapter.notifyDataSetChanged()
                        kostil++
                        description = builder.toString()
                        if (binding.checkBox.isChecked) {
//                            bitmapList.clear()
//                            bitmapList.add(
//                                ImageWithText(
//                                    image = drawDetectionResult(mBitmap, boxes)!!,
//                                    text = builder.toString()
//                                )
//                            )
//                            adapter.notifyDataSetChanged()
                        }
                    } else {
                        bitmapList.add(ImageWithText(selectedImages[kostil], "Could not detect"))
                        adapter.notifyDataSetChanged()
                        kostil++
//                        bitmapList.add(
//                            ImageWithText(
//                                image = mBitmap,
//                                text = "Could not detect"
//                            )
//                        )
//
                    }
                    adapter.notifyDataSetChanged()
                }.addOnFailureListener {
                    it.printStackTrace()
                }
        }

        kostil = 0
    }

    private fun drawDetectionResult(
        bitmap: Bitmap,
        detectionResults: List<BoxWithText?>
    ): Bitmap? {
        val outputBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(outputBitmap)
        val pen = Paint()
        pen.setTextAlign(Paint.Align.LEFT)
        for (box in detectionResults) {
            // draw bounding box
            pen.setColor(Color.RED)
            pen.setStrokeWidth(8f)
            pen.setStyle(Paint.Style.STROKE)
            if (box != null) {
                canvas.drawRect(box.rect, pen)
            }
            val tagSize = Rect(0, 0, 0, 0)

            // calculate the right font size
            pen.setStyle(Paint.Style.FILL_AND_STROKE)
            pen.setColor(Color.YELLOW)
            pen.setStrokeWidth(2f)
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
            canvas.drawText(
                box.text, box.rect.left + margin,
                (box.rect.top + tagSize.height()).toFloat(), pen
            )
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
                                            type = "2"
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