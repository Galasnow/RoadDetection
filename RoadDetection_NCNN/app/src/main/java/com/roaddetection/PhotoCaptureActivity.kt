package com.roaddetection

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.roaddetection.databinding.ActivityPhotoBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class PhotoCaptureActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPhotoBinding

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    private var isBack = true
    private lateinit var mCameraProvider: ProcessCameraProvider

    // CameraX's use case
    private lateinit var mPreview: Preview
    private var mImageCapture: ImageCapture? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 请求相机权限
        if (allPermissionsGranted()) {
            startCamera(binding.viewFinder)
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        // 设置拍照的监听按钮
        binding.cameraCaptureButton.setOnClickListener { takePhoto() }
        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.imageButton.setOnClickListener {
            isBack = !isBack
            startCamera(binding.viewFinder)
        }

    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    //申请权限的方法
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera(binding.viewFinder)
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    private fun takePhoto() {
        // 获得可修改的图像捕获用例
        val mimageCapture = mImageCapture ?: return
        // 创建带时间戳的输出文件用于保存文件

        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(
                FILENAME_FORMAT, Locale.CHINA
            ).format(System.currentTimeMillis()) + ".jpg"
        )

//        val photoFile = File(
//            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).path + SimpleDateFormat(
//                FILENAME_FORMAT, Locale.CHINA
//            ).format(System.currentTimeMillis()) + ".jpg"
//        )

        // 创建输出选项对象
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // 设置图像捕获监听器，拍照后触发
        mimageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Photo capture succeeded: $savedUri"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }
            })
    }

    private fun startCamera(previewView: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                mCameraProvider = cameraProviderFuture.get()
                bindPreview(mCameraProvider, previewView)

            } catch (e: ExecutionException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindPreview(
        cameraProvider: ProcessCameraProvider,
        previewView: PreviewView
    ) {
        // Selector that determine which camera will be used.
        val cameraSelector =
            if (isBack) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA

        // Image preview use case
        val previewBuilder = Preview.Builder()

        // Image capture use case
        val captureBuilder = ImageCapture.Builder()
            .setTargetRotation(previewView.display.rotation)

        // Create preview and capture use case's instance
        mPreview = previewBuilder.build()

        mImageCapture = captureBuilder.build()

        // Must unbind all owner to the camera before binding.
        cameraProvider.unbindAll()

        try {
            cameraProvider.bindToLifecycle(
                this, cameraSelector,
                mPreview, mImageCapture
            )
            // Bind the view's surface to preview use case.
            mPreview.setSurfaceProvider(previewView.surfaceProvider)
        } catch (e: java.lang.Exception) {
            Log.e("Camera", "camera provider bind error:", e)
        }

    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }

        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        // Used to load the 'objdetection' library on application startup.
        // init {
        //     System.loadLibrary("objdetection")
        //  }
        private const val TAG = "RoadDetection"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}



