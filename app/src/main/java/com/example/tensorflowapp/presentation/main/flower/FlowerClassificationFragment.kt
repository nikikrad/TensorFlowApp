package com.example.tensorflowapp.presentation.main.flower

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.PermissionChecker
import androidx.core.content.PermissionChecker.checkSelfPermission
import androidx.fragment.app.Fragment
import com.example.tensorflowapp.MainActivity
import com.example.tensorflowapp.databinding.FragmentFlowerClassificationBinding
import com.example.tensorflowapp.ml.Model
import com.google.mlkit.vision.label.ImageLabeler
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*


class FlowerClassificationFragment : Fragment() {

    private lateinit var binding: FragmentFlowerClassificationBinding
//    private lateinit var photoFile: File
//    private lateinit var imageLabeler: ImageLabeler
//
//    private val REQUEST_PICK_IMAGE = 1000
//    private val REQUEST_CAPTURE_IMAGE = 1001
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

//        val localModel = LocalModel.Builder().setAssetFilePath("model_flowers.tflite").build()
//        val options = CustomImageLabelerOptions.Builder(localModel)
//            .setConfidenceThreshold(0.7f)
//            .setMaxResultCount(5)
//            .build()
//        imageLabeler = ImageLabeling.getClient(options)
//

        binding.btnGalleryImage.setOnClickListener {
//            onPickImage()
            val cameraIntent =
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(cameraIntent, 1)
        }
        binding.btnCameraImage.setOnClickListener {
//            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
//                == PackageManager.PERMISSION_DENIED
//            ) {
//                ActivityCompat.requestPermissions(
//                    activity as MainActivity,
//                    arrayOf(Manifest.permission.CAMERA),
//                    PackageManager.PERMISSION_DENIED
//                )
//            }
//            onStartCamera()
            if (checkSelfPermission(requireContext(), Manifest.permission.CAMERA) === PermissionChecker.PERMISSION_GRANTED) {
                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(cameraIntent, 3)
            } else {
                requestPermissions(arrayOf<String>(Manifest.permission.CAMERA), 100)
            }
        }

    }

    fun classifyImage(image: Bitmap?) {
        try {
            val model: Model = Model.newInstance(requireContext())

            // Creates inputs for reference.
            val inputFeature0: TensorBuffer =
                TensorBuffer.createFixedSize(intArrayOf(1, 32, 32, 3), DataType.FLOAT32)
            val byteBuffer: ByteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3)
            byteBuffer.order(ByteOrder.nativeOrder())
            val intValues = IntArray(imageSize * imageSize)
            image!!.getPixels(intValues, 0, image.width, 0, 0, image.width, image.height)
            var pixel = 0
            //iterate over each pixel and extract R, G, and B values. Add those values individually to the byte buffer.
            for (i in 0 until imageSize) {
                for (j in 0 until imageSize) {
                    val `val` = intValues[pixel++] // RGB
                    byteBuffer.putFloat((`val` shr 16 and 0xFF) * (1f / 1))
                    byteBuffer.putFloat((`val` shr 8 and 0xFF) * (1f / 1))
                    byteBuffer.putFloat((`val` and 0xFF) * (1f / 1))
                }
            }
            inputFeature0.loadBuffer(byteBuffer)

            // Runs model inference and gets result.
            val outputs: Model.Outputs = model.process(inputFeature0)
            val outputFeature0: TensorBuffer = outputs.outputFeature0AsTensorBuffer
            val confidences: FloatArray = outputFeature0.floatArray
            // find the index of the class with the biggest confidence.
            var maxPos = 0
            var maxConfidence = 0f
            for (i in confidences.indices) {
                if (confidences[i] > maxConfidence) {
                    maxConfidence = confidences[i]
                    maxPos = i
                }
            }
            val classes = arrayOf("Apple", "Banana", "Orange")
            binding.tvOutput.text = "${classes[maxPos]}: ${confidences[0] + confidences[1] + confidences[2]}"
//            result.setText(classes[maxPos])

            // Releases model resources if no longer used.
            model.close()
        } catch (e: Exception) {
            // TODO Handle the exception
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            if (requestCode == 3) {
                var image = data?.extras!!["data"] as Bitmap?
                val dimension = Math.min(image!!.width, image.height)
                image = ThumbnailUtils.extractThumbnail(image, dimension, dimension)
                binding.ivImage.setImageBitmap(image)
                image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false)
                classifyImage(image)
            } else {
                val dat = data?.data
                var image: Bitmap? = null
                try {
                    image = MediaStore.Images.Media.getBitmap((activity as MainActivity).contentResolver, dat)
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
                binding.ivImage.setImageBitmap(image)
                image = Bitmap.createScaledBitmap(image!!, imageSize, imageSize, false)
                classifyImage(image)
            }
        }
    }

//
//    private fun runDetection(bitmap: Bitmap) {
//        val inputImage = InputImage.fromBitmap(bitmap, 0)
//        imageLabeler.process(inputImage).addOnSuccessListener { imageLabels: List<ImageLabel> ->
//            val sb = StringBuilder()
//            for (label in imageLabels) {
//                sb.append(label.text).append(": ").append(label.confidence).append("\n")
//            }
//            if (imageLabels.isEmpty()) {
//                binding.tvOutput.text = "Could not identify!!"
//            } else {
//                binding.tvOutput.text = sb.toString()
//            }
//        }.addOnFailureListener { e: Exception -> e.printStackTrace() }
//    }
//
//
//    private fun onPickImage() {
//        val intent = Intent(Intent.ACTION_GET_CONTENT)
//        intent.type = "image/*"
//
//        startActivityForResult(intent, REQUEST_PICK_IMAGE)
//    }
//
//    private fun onStartCamera() {
//        photoFile = createPhotoFile()
//        val fileUri =
//            context?.let { FileProvider.getUriForFile(it, "com.iago.fileprovider", photoFile) }
//
//        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri)
//
//        startActivityForResult(intent, REQUEST_CAPTURE_IMAGE)
//    }
//
//    private fun createPhotoFile(): File {
//        val photoFileDir = File(
//            (activity as MainActivity).getExternalFilesDir(Environment.DIRECTORY_PICTURES),
//            "SIEGA"
//        )
//
//        if (!photoFileDir.exists()) {
//            photoFileDir.mkdirs()
//        }
//
//        val name = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
//        val file = File(photoFileDir.path + File.separator + name)
//        return file
//    }
//
//    private fun loadFromUri(uri: Uri): Bitmap? {
//        var bitmap: Bitmap? = null
//        try {
//            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1) {
//                val source =
//                    ImageDecoder.createSource((activity as MainActivity).contentResolver, uri)
//                bitmap = ImageDecoder.decodeBitmap(source)
//            } else {
//                bitmap = MediaStore.Images.Media.getBitmap(
//                    (activity as MainActivity).contentResolver,
//                    uri
//                )
//            }
//        } catch (e: java.lang.Exception) {
//            e.printStackTrace()
//        }
//        return bitmap
//    }
//
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        if (resultCode == Activity.RESULT_OK) {
//            if (requestCode == REQUEST_PICK_IMAGE) {
//                val uri = data?.data
//                val bitmap = loadFromUri(uri!!)
//                binding.ivImage.setImageBitmap(bitmap)
//                if (bitmap != null) {
//                    runDetection(bitmap)
//                }
//            } else if (requestCode == REQUEST_CAPTURE_IMAGE) {
//                val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
//                binding.ivImage.setImageBitmap(bitmap)
//                runDetection(bitmap)
//            }
//        }
//
//    }
}