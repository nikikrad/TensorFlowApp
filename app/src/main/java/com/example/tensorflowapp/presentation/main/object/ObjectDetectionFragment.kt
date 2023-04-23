package com.example.tensorflowapp.presentation.main.`object`

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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.example.tensorflowapp.MainActivity
import com.example.tensorflowapp.databinding.FragmentObjectDetectionBinding
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

    private val REQUEST_PICK_IMAGE = 1000
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

    }

    private fun runDetection(bitmap: Bitmap?) {
        val inputImage = InputImage.fromBitmap(bitmap!!, 0)
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
                        }
                    }
                    binding.tvOutput.text = builder.toString()
                    if (binding.checkBox.isChecked) {
                        binding.ivImage.setImageBitmap(drawDetectionResult(bitmap, boxes))
                    }
                } else {
                    binding.tvOutput.text = "Could not detect"
                }
            }.addOnFailureListener {
                it.printStackTrace()
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
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_PICK_IMAGE) {
                val uri = data?.data
                val bitmap = loadFromUri(uri!!)
                binding.ivImage.setImageBitmap(bitmap)
                if (bitmap != null) {
                    runDetection(bitmap)
                }
            } else if (requestCode == REQUEST_CAPTURE_IMAGE) {
                val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                binding.ivImage.setImageBitmap(bitmap)
                runDetection(bitmap)
            }
        }

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
}