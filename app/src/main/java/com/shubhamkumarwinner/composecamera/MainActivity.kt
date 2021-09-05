package com.shubhamkumarwinner.composecamera

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import java.io.File
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.common.util.concurrent.ListenableFuture
import com.shubhamkumarwinner.composecamera.ui.theme.ComposeCameraTheme
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

const val REQUEST_VIDEO_CAPTURE = 1
//step 3 capture image with cameraX api starts
private const val FILENAME_FORMAT = "yyyyMMdd_HHmm_ssSSS"
private const val TAG = "CameraXBasic"
class MainActivity : ComponentActivity() {
    private var outputDirectory: File? = null

    private var imageCapture: ImageCapture? = null
    private var cameraExecutor: ExecutorService? = null
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    //step 3 capture image with cameraX api ends

    private var imageBitmapState = mutableStateOf<Bitmap?>(null)
    private var uriForVideo = mutableStateOf<Uri?>(null)

    private val takePicture = registerForActivityResult(
        ActivityResultContracts
            .TakePicturePreview()) {
        saveToFile(it)
        imageBitmapState.value = it
    }
    @ExperimentalPermissionsApi
    override fun onCreate(savedInstanceState: Bundle?) {
        window?.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN)
        super.onCreate(savedInstanceState)
        setContent {
            ComposeCameraTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        /*VideoRecorder {
                            startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                    .apply {
                                        data = Uri.parse("package:$packageName")
                                    })
                        }*/
                        CameraX()
                    }
                }
            }

        }
        outputDirectory = File("/storage/emulated/0/${getString(R.string.app_name)}Photo")
            .apply {
            mkdir()
        }
        //step 3 capture image with cameraX api ends
    }

    //step 1 Taking photo starts
    @Composable
    fun LaunchCamera() {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (imageBitmapState.value !=  null){
                ShowImage(bitmap = imageBitmapState.value!!)
            }
            Button(onClick = { takePicture.launch(null) }) {
                Text(text = "Open Camera")
            }
        }
    }

    @ExperimentalPermissionsApi
    @Composable
    private fun CaptureImage(
        navigateToSettingsScreen: () -> Unit
    ) {
        // Track if the user doesn't want to see the rationale any more.
        var doNotShowRationale by rememberSaveable { mutableStateOf(false) }

        // Camera permission state
        val storagePermissionState = rememberMultiplePermissionsState(
            permissions = remember{mutableStateListOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)}
        )

        when {
            storagePermissionState.allPermissionsGranted -> {
                LaunchCamera()
            }
            storagePermissionState.shouldShowRationale ||
                    !storagePermissionState.permissionRequested -> {
                if (doNotShowRationale) {
                    Text("Feature not available")
                }
                else {
                    Column {
                        Text("The storage permission is important for this app. Please grant the permission.")
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            Button(onClick = { storagePermissionState.launchMultiplePermissionRequest() }) {
                                Text("Request permission")
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(onClick = { doNotShowRationale = true }) {
                                Text("Don't show rationale again")
                            }
                        }
                    }
                }
            }
            else -> {
                Column {
                    Text(
                        "Storage permission denied. See this FAQ with information about why we " +
                                "need this permission. Please, grant us access on the Settings screen."
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = navigateToSettingsScreen) {
                        Text("Open Settings")
                    }
                }
            }
        }
    }
    @Composable
    fun ShowImage(bitmap: Bitmap) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "image",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            )
        }
    }

    private fun saveToFile(bitmap: Bitmap): File? {
        var file: File? = null
        return try {
            val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            file = File("/storage/emulated/0/Pictures/${"JPEG_${timeStamp}_.jpg"}")
            file.createNewFile()
            Log.d("test", "name: $file")
            //Convert bitmap to byte array
            val bos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos) // YOU can also save it in JPEG
            val bitmapData = bos.toByteArray()
            //write the bytes in file
            val fos = FileOutputStream(file)
            fos.write(bitmapData)
            fos.flush()
            fos.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            file // it will return null
        }
    }
    //step1 Taking photo ends


    //step 2 Recording video starts
    @ExperimentalPermissionsApi
    @Composable
    private fun VideoRecorder(
        navigateToSettingsScreen: () -> Unit
    ) {
        // Track if the user doesn't want to see the rationale any more.
        var doNotShowRationale by rememberSaveable { mutableStateOf(false) }

        // Camera permission state
        val storagePermissionState = rememberMultiplePermissionsState(
            permissions = remember{mutableStateListOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)}
        )

        when {
            storagePermissionState.allPermissionsGranted -> {
                LaunchVideoCamera()
            }
            storagePermissionState.shouldShowRationale ||
                    !storagePermissionState.permissionRequested -> {
                if (doNotShowRationale) {
                    Text("Feature not available")
                } else {
                    Column {
                        Text("The storage permission is important for this app. Please grant the permission.")
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            Button(onClick = { storagePermissionState.launchMultiplePermissionRequest() }) {
                                Text("Request permission")
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(onClick = { doNotShowRationale = true }) {
                                Text("Don't show rationale again")
                            }
                        }
                    }
                }
            }
            else -> {
                Column {
                    Text(
                        "Storage permission denied. See this FAQ with information about why we " +
                                "need this permission. Please, grant us access on the Settings screen."
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = navigateToSettingsScreen) {
                        Text("Open Settings")
                    }
                }
            }
        }
    }

    @Composable
    fun LaunchVideoCamera() {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (uriForVideo.value !=  null){
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(500.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Player(uri = uriForVideo.value!!)
                }

            }
            Button(onClick = { dispatchTakeVideoIntent() }) {
                Text(text = "Open Video Camera")
            }
        }
    }

    @Composable
    fun Player(uri: Uri) {
        val context = LocalContext.current
        val lifecycle = LocalLifecycleOwner.current.lifecycle

        var autoPlay by rememberSaveable{ mutableStateOf(true) }
        var window by rememberSaveable{ mutableStateOf(0) }
        var position by rememberSaveable{ mutableStateOf(0L) }

        val player = remember {
            val player = SimpleExoPlayer.Builder(context)
                .build()

            val defaultDataSourceFactory = DefaultDataSourceFactory(context, "me")
            player.prepare(
                ProgressiveMediaSource.Factory(defaultDataSourceFactory)
                    .createMediaSource(uri)
            )
            player.playWhenReady = autoPlay
            player.seekTo(window, position)
            player
        }

        fun updateState() {
            autoPlay = player.playWhenReady
            window = player.currentWindowIndex
            position = player.contentPosition
        }

        val playerView = remember {
            val playerView = PlayerView(context)
            lifecycle.addObserver(object : LifecycleObserver {
                @OnLifecycleEvent(Lifecycle.Event.ON_START)
                fun onStart() {
                    playerView.onResume()
                    player.playWhenReady = autoPlay
                }

                @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
                fun onStop() {
                    updateState()
                    playerView.onPause()
                    player.playWhenReady = false
                }
            })
            playerView
        }

        DisposableEffect(Unit) {
            onDispose {
                updateState()
                player.release()
            }
        }


        AndroidView(
            modifier = Modifier
                .fillMaxWidth(),
            factory = {playerView},
            update = {
                playerView.player = player
            },
        )
    }

    private fun dispatchTakeVideoIntent() {
        Intent(MediaStore.ACTION_VIDEO_CAPTURE).also { takeVideoIntent ->
            takeVideoIntent.resolveActivity(packageManager)?.also {
                startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_VIDEO_CAPTURE && resultCode == RESULT_OK) {
            val videoUri: Uri = data?.data!!
            uriForVideo.value = videoUri
        }
    }

    //step 2 Recording video ends

    //step 3 capture image with cameraX api starts
    @ExperimentalPermissionsApi
    @Composable
    fun CameraX(){
        var doNotShowRationale by rememberSaveable { mutableStateOf(false) }

        // Contact permission state
        val requiredPermissionsState = rememberMultiplePermissionsState(
            permissions = remember{
                mutableStateListOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            }
        )

        when {
            requiredPermissionsState.allPermissionsGranted -> {
                CaptureImageCameraX()
            }
            requiredPermissionsState.shouldShowRationale ||
                    !requiredPermissionsState.permissionRequested -> {
                if (doNotShowRationale) {
                    Text("Feature not available")
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "The contact is important for this app. Please grant the permission.",
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { requiredPermissionsState.launchMultiplePermissionRequest() }) {
                            Text("Request permission")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { doNotShowRationale = true }) {
                            Text("Don't show rationale again")
                        }
                    }
                }
            }
            else -> {
                AlertDialog(
                    onDismissRequest = {

                    },
                    title = {
                        Text(text = "Permission Denied")
                    },
                    text = {
                        Text("Some permissions has been denied. Please, grant us access on the Settings screen.")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                startActivity(
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                        .apply {
                                            data = Uri.parse("package:$packageName")
                                        })
                            }) {
                            Text("Settings")
                        }
                    },
                    dismissButton = {
                        Button(
                            onClick = {
                                onBackPressed()
                            }) {
                            Text("Not now")
                        }
                    }
                )
            }
        }
    }
    @SuppressLint("RememberReturnType")
    @Composable
    fun CaptureImageCameraX() {
        val context = LocalContext.current

        val cameraProviderFuture = remember{
            ProcessCameraProvider.getInstance(context)
        }

        cameraExecutor = remember{
            Executors.newSingleThreadExecutor()
        }

        val previewView = remember{
            PreviewView(context).apply{
                id = R.id.preview_view
            }
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize()){
                startCameraX(previewView, cameraProviderFuture)
            }
            IconButton(
                onClick = {
                    if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                        cameraExecutor?.shutdown()
                        cameraSelector =CameraSelector.DEFAULT_FRONT_CAMERA
                        startCameraX(previewView, cameraProviderFuture)
                    }else{
                        cameraExecutor?.shutdown()
                        cameraSelector =CameraSelector.DEFAULT_BACK_CAMERA
                        startCameraX(previewView, cameraProviderFuture)
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .height(90.dp)
                    .width(80.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_flip_camera),
                    contentDescription = null,
                    tint = Color.White
                )
            }

            IconButton(onClick = { takePhoto() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .height(80.dp)) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_camera),
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }

    }

    private fun startCameraX(
        previewView: PreviewView,
        cameraProviderFuture: ListenableFuture<ProcessCameraProvider>,
    ){

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build()
                .also{
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .build()

            val faceAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build().also{
                    it.setAnalyzer( cameraExecutor!!, FaceAnalyzer())
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, faceAnalysis)
            }catch (e: Exception){
                Log.e(TAG, "CaptureImageCameraX: ${e.localizedMessage}")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time-stamped output file to hold the image
        val photoFile = File(
            outputDirectory,
            "IMG_"+SimpleDateFormat(FILENAME_FORMAT, Locale.US
            ).format(System.currentTimeMillis()) + ".jpg")

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Photo capture succeeded: $savedUri"
                    Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor?.shutdown()
    }
}

private class FaceAnalyzer : ImageAnalysis.Analyzer {
    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val image = imageProxy.image
        if (image != null) {
            Log.d(TAG, "analyze: ${image.height}")
        }
        image?.close()
    }
}
//step 3 capture image with cameraX api ends


