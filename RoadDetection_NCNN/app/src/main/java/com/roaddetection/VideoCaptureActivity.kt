package com.roaddetection

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.VideoCapture.OnVideoSavedCallback
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.roaddetection.databinding.ActivityVideoBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors

class VideoCaptureActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVideoBinding
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    private var camera: Camera? = null//相机对象

    private var mVideoCapture: VideoCapture? = null//录像用例
    private lateinit var mCameraProvider: ProcessCameraProvider//相机信息
    private lateinit var mPreview: Preview//预览对象
    private var mImageCapture: ImageCapture? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (allPermissionsGranted()) {
            startCamera(binding.viewFinder)
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        binding.captureButton.setOnClickListener {
            if (allPermissionsGranted()) {
                takeVideo()
                binding.stopButton.visibility = View.VISIBLE
                binding.captureButton.visibility = View.INVISIBLE
            } else {
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

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

    @SuppressLint("RestrictedApi")
    private fun bindPreview(
        cameraProvider: ProcessCameraProvider,
        previewView: PreviewView
    ) {

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        // Image preview use case
        val previewBuilder = Preview.Builder()

        // Image capture use case
        val captureBuilder = ImageCapture.Builder()
            .setTargetRotation(previewView.display.rotation)

        mPreview = previewBuilder.build()

        mImageCapture = captureBuilder.build()

        mVideoCapture = VideoCapture.Builder()//录像用例配置
            .setTargetAspectRatio(AspectRatio.RATIO_4_3) //设置高宽比
            .setTargetRotation(binding.viewFinder.display.rotation)//设置旋转角度
            .setVideoFrameRate(30)
            .build()

        try {
            cameraProvider.unbindAll()//先解绑所有用例
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, mPreview, mImageCapture, mVideoCapture)//绑定用例
            mPreview.setSurfaceProvider(previewView.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    @SuppressLint("RestrictedApi")
    private fun takeVideo() {

        //视频保存路径
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).path + "/Obj" + SimpleDateFormat(
            FILENAME_FORMAT, Locale.CHINA
        ).format(System.currentTimeMillis()) + ".mp4")

        val options = VideoCapture.OutputFileOptions.Builder(file).build()

//        VideoCapture.OutputFileOptions()
//
//        ContentValues contentValues = new ContentValues();
//        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "NEW_VIDEO");
//        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
//
//        OutputFileOptions options = OutputFileOptions.builder(
//                getContentResolver(),
//        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
//        contentValues).build();

        //开始录像
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        //videoCapture?.startRecording(file, Executors.newSingleThreadExecutor(), object : OnVideoSavedCallback {
        mVideoCapture?.startRecording(options, Executors.newSingleThreadExecutor(), object : OnVideoSavedCallback {
            //override fun onVideoSaved(@NonNull file: File) {
            override fun onVideoSaved(outputFileResults: VideoCapture.OutputFileResults) {
                //保存视频成功回调，会在停止录制时被调用
//                Looper.prepare();
//                Toast.makeText(this@VideoCaptureActivity, file.absolutePath, Toast.LENGTH_SHORT).show()
//                Looper.loop();
            }

            override fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
                //保存失败的回调，可能在开始或结束录制时被调用
                Log.e("", "onError: $message")
            }
        })

        binding.stopButton.setOnClickListener {
            mVideoCapture?.stopRecording()//停止录制
            binding.stopButton.visibility = View.INVISIBLE
            binding.captureButton.visibility = View.VISIBLE

            Toast.makeText(this, file.path, Toast.LENGTH_SHORT).show()
            Log.d("path", file.path)
        }
    }

    override fun onDestroy() {
        mCameraProvider.unbindAll()
        cameraExecutor.shutdown()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO)
    }
}
