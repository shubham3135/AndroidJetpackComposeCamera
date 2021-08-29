package com.shubhamkumarwinner.composecamera

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.shubhamkumarwinner.composecamera.ui.theme.ComposeCameraTheme
import kotlinx.coroutines.channels.Channel
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

const val REQUEST_VIDEO_CAPTURE = 1
class MainActivity : ComponentActivity() {
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
        super.onCreate(savedInstanceState)
        setContent {
            ComposeCameraTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        /*CaptureImage {
                            startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                    .apply {
                                        data = Uri.parse("package:$packageName")
                                    })
                        }*/

                        VideoRecorder {
                            startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                .apply {
                                    data = Uri.parse("package:$packageName")
                                })
                        }
                    }
                }
            }
        }
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
            permissions = remember{mutableStateListOf(android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)}
        )

        when {
            storagePermissionState.allPermissionsGranted -> {
                LaunchCamera()
            }
            storagePermissionState.shouldShowRationale ||
                    !storagePermissionState.permissionRequested -> {
                if (doNotShowRationale) {
                    Text("Feature not available")
                } else {
                    Column {
                        Text("The camera is important for this app. Please grant the permission.")
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
                        "Camera permission denied. See this FAQ with information about why we " +
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
            permissions = remember{mutableStateListOf(android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)}
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
                        Text("The camera is important for this app. Please grant the permission.")
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
                        "Camera permission denied. See this FAQ with information about why we " +
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
                    modifier = Modifier.fillMaxWidth().height(500.dp),
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
}


