package com.example.cameraxdemo

//import kotlinx.android.synthetic.main.activity_main.*

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.Camera
import androidx.camera.core.impl.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.cameraxdemo.databinding.ActivityMainBinding
import com.example.cameraxdemo.glsurfaceview.OnPreviewSurfaceView
import com.example.cameraxdemo.textureview.VideoTextureRenderer
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


typealias LumaListener = (data: ByteArray) -> Unit

class MainActivity : AppCompatActivity()
    , TextureView.SurfaceTextureListener  //textureview
, OnPreviewSurfaceView
{
    private var imageCapture: ImageCapture? = null
    private lateinit var binding: ActivityMainBinding
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    var isBack = true
    private lateinit var camera: Camera
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var preview: Preview



    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root;
        setContentView(view)

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        // Set up the listener for take photo button
        binding.cameraCaptureButton.setOnClickListener { takePhoto() }

        outputDirectory = getOutputDirectory()

        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.openOrCloseCamera.setOnClickListener { openOrCloseCamera() }

        binding.switchCamera.setOnClickListener { switchCamera() }

        binding.viewFinder.renderer.onPreviewSurfaceView = this


        //------------textureview
//        binding.viewFinder.surfaceTextureListener = this

    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time-stamped output file to hold the image
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(
                FILENAME_FORMAT, Locale.US
            ).format(System.currentTimeMillis()) + ".jpg"
        )

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile)
            .build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
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

    class PreviewSurfaceProvider (private val surface: Surface, private val executor: Executor): Preview.SurfaceProvider {

        override fun onSurfaceRequested(request: SurfaceRequest) {

            Log.i("cx---", "onSurfaceRequested---request.resolution.height="+request.resolution.height+",,request.resolution.width="+request.resolution.width)


            request.provideSurface(surface, executor, {

                Log.i("cx---", "provideSurface in")


            })
        }


    }



    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            cameraProvider = cameraProviderFuture.get()

            // Preview
            preview = Preview.Builder()
//                .setTargetResolution(Size(320,240))
//                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .setTargetRotation(Surface.ROTATION_90)
                .build()
                .also {
//                    it.setSurfaceProvider(PreviewSurfaceProvider(surface,executor))  //textureview
//                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)

                    //必须判断是否初始化，否则退出app之后再次进入会崩溃
                    if(!binding.viewFinder.renderer.isSurfaceInit()) return@also
                    Utils.LOGI("startCamera---preview->thread:${Thread.currentThread().name}")
                    it.setSurfaceProvider(PreviewSurfaceProvider(binding.viewFinder.renderer.surface,executor = Executors.newSingleThreadExecutor()))
                }
//            preview = binding.viewFinder.renderer.preview


            imageCapture = ImageCapture.Builder()
//                .setTargetResolution(Size(1920,1080))
                .setTargetResolution(Size(1080,1920))
//                .setTargetRotation(Surface.ROTATION_90)
                .build()

            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetResolution(Size(1080,1920))
//                .setTargetRotation(Surface.ROTATION_90)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, LuminosityAnalyzer { luma ->
                        Log.d(TAG, "Average luminosity: $luma")

                    })
                }


            // Select back camera as a default
//            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                camera = cameraProvider.bindToLifecycle(
//                    this, cameraSelector, preview, imageCapture, imageAnalyzer
                    this, cameraSelector, preview, imageCapture,imageAnalyzer
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun switchCamera() {

//        Log.i("cx---","preview-heigit=${preview.resolutionInfo?.resolution?.height},width=${preview.resolutionInfo?.resolution?.width}")

        if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            isBack = false
            startCamera()
        } else {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            isBack = true
            startCamera()
        }
    }

    private fun openOrCloseCamera() {
//        camera?.cameraInfo.
        if (cameraProvider.isBound(imageCapture!!)) {
//            cameraProvider.unbind(imageCapture)
            cameraProvider.unbindAll()
        } else {
            cameraProvider.bindToLifecycle(
                this, cameraSelector, imageCapture,preview
            )
        }

    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    override fun onResume() {
        Utils.LOGI("onResume")
        super.onResume()

    }

    override fun onStart() {
        Utils.LOGI("onStart")
        super.onStart()
    }

    override fun onPause() {
        Utils.LOGI("onPause")
        super.onPause()
    }

    override fun onStop() {
        Utils.LOGI("onStop")
        super.onStop()
    }

    override fun onDestroy() {
        Utils.LOGI("onDestroy")
        cameraExecutor.shutdown()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)


        private lateinit var outputDirectory: File
    }


    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
//                    startCamera()
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



    private class LuminosityAnalyzer(private val listener: LumaListener) : ImageAnalysis.Analyzer {

        private fun ImageProxy.toByteArray(): ByteArray {
//            rewind()    // Rewind the buffer to zero
//            val data = ByteArray(remaining())
//            get(data)   // Copy the buffer into a byte array

            val yBuffer = planes[0].buffer // Y
            val uBuffer = planes[1].buffer // U
            val vBuffer = planes[2].buffer // V

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

//            val nv21 = ByteArray(ySize + vSize + uSize)
            val nv21 = ByteArray(ySize + vSize)

            //U and V are swapped
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)   //VUVU
//            uBuffer.get(nv21, ySize + vSize, uSize) //
            //-------I420转NV21
//            for (i in 0..(vSize+uSize) ){
//                if (i%2 == 0){
//                    vBuffer.get(nv21,ySize+i,1)
//                }else{
//                    uBuffer.get(nv21,ySize+i,1)
//                }
//            }

            return nv21 // Return the byte array
        }

        override fun analyze(image: ImageProxy) {

//            val buffer = image.planes[0].buffer
//            val data = buffer.toByteArray()
//            val pixels = data.map { it.toInt() and 0xFF }
//            val luma = pixels.average()
//
//

            //-------------------------
//            Log.i(TAG, "image format: " + image.format)
////            // 从image里获取三个plane
////            // 从image里获取三个plane
//            val planes = image.planes
//            for (i in planes.indices) {
//                val iBuffer = planes[i].buffer
//                val iSize = iBuffer.remaining()
//                Log.i(TAG, "pixelStride  " + planes[i].pixelStride)
//                Log.i(TAG, "rowStride   " + planes[i].rowStride)
//                Log.i(TAG, "width  " + image.width)
//                Log.i(TAG, "height  " + image.height)
//                Log.v(TAG, "buffer size $iSize")
//                Log.i(TAG, "Finished reading data from plane  $i")
//            }
//            listener(image.toByteArray())
//            image.toBitmap()

            //-------------------------------
//            val path = outputDirectory.absolutePath+"/bitmap-${System.currentTimeMillis()}"
//            Utils.compressToJpeg(path,image)


            image.close()
        }


        fun ImageProxy.toBitmap(nv21: ByteArray): Bitmap? {
            val path = outputDirectory.absolutePath + "/preview-${System.currentTimeMillis()}"

            //保存yuv
//            Utils.dumpFile("$path.yuv", nv21)
            //保存jpg
            Utils.compressToJpeg(nv21, "$path.jpg", this)

            return null;
        }


    }



    //--------------textureview


    lateinit var surface: Surface
    lateinit var renderer: VideoTextureRenderer

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        Utils.LOGI("onSurfaceTextureAvailable--w*h=${width}*${height}")
        Utils.LOGI("onSurfaceTextureAvailable--surface=${surface}")


        renderer = VideoTextureRenderer(surface,width,height,  java.util.function.Consumer {
            Utils.LOGI("onSurfaceTextureAvailable-accept-w*h=${width}*${height}")
            renderer.setVideoSize(width,height)
            this.surface = Surface(it)
            it.setDefaultBufferSize(width,height)
            it.setDefaultBufferSize(width,height)
            startCamera()
        })

    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {

    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {

    }


    override fun onSurfaceCreated() {
        Utils.LOGI("OnPreviewSurfaceView-onSurfaceCreated")
//        runOnUiThread {
            startCamera()
//        }

    }


}