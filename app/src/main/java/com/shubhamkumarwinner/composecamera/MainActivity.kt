package com.shubhamkumarwinner.composecamera

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner.current
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner.current
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
import androidx.compose.ui.text.intl.LocaleList.Companion.current
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.shubhamkumarwinner.composecamera.ui.theme.ComposeCameraTheme
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ThreadLocalRandom.current

class MainActivity : ComponentActivity() {
    private var imageBitmapState = mutableStateOf<Bitmap?>(null)

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
                        FeatureThatRequiresCameraPermission {
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
    private fun FeatureThatRequiresCameraPermission(
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
}



