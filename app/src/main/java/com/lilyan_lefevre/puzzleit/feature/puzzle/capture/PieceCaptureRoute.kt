package com.lilyan_lefevre.puzzleit.feature.puzzle.capture

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.lilyan_lefevre.puzzleit.R
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** The camera objects, which Compose does not need to watch: only [PieceCaptureRoute] touches them. */
private class CameraSession {
    var previewView: PreviewView? = null
    var imageCapture: ImageCapture? = null
    var camera: Camera? = null
    val executor: ExecutorService = Executors.newSingleThreadExecutor()
}

/** Takes the photo of one loose piece and hands the file back through [onPhoto]. The camera is CameraX, the rest is [PieceCaptureScreen]. */
@Composable
fun PieceCaptureRoute(onPhoto: (path: String) -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val session = remember { CameraSession() }

    // Read by the screen: Compose redraws it when they change, even when they are set from the camera's thread.
    var torch by remember { mutableStateOf(false) }
    var shutterEnabled by remember { mutableStateOf(true) }
    /** The photo just taken: the flash plays, then it is handed back. */
    var taken by remember { mutableStateOf<File?>(null) }
    val flash = remember { Animatable(0f) }

    fun startCamera() {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            // The provider can come back after the user already left this screen.
            val view = session.previewView ?: return@addListener
            val provider = future.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(view.surfaceProvider) }
            session.imageCapture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).build()
            try {
                provider.unbindAll()
                session.camera = provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, session.imageCapture)
            } catch (e: Exception) {
                Log.e(TAG, "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startCamera() else {
            Toast.makeText(context, R.string.camera_permission_denied, Toast.LENGTH_LONG).show()
            onClose()
        }
    }

    /** Called once the preview exists: start the camera, asking for the permission first if needed. */
    fun openCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) startCamera()
        else permission.launch(Manifest.permission.CAMERA)
    }

    fun takePhoto() {
        val capture = session.imageCapture ?: return
        shutterEnabled = false
        val file = File(context.cacheDir, "piece_${System.currentTimeMillis()}.jpg")
        capture.takePicture(ImageCapture.OutputFileOptions.Builder(file).build(), session.executor, object : ImageCapture.OnImageSavedCallback {
            override fun onError(e: ImageCaptureException) {
                Log.e(TAG, "Capture failed", e)
                shutterEnabled = true
            }

            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                taken = file
            }
        })
    }

    // Shutter flash, then back to the table where the analysis starts.
    LaunchedEffect(taken) {
        val file = taken ?: return@LaunchedEffect
        flash.snapTo(0.9f)
        flash.animateTo(0f, tween(260))
        onPhoto(file.absolutePath)
    }

    DisposableEffect(Unit) {
        onDispose {
            session.camera?.cameraControl?.enableTorch(false)
            session.executor.shutdown()
            session.previewView = null
        }
    }

    PieceCaptureScreen(
        onPreviewReady = { session.previewView = it; openCamera() },
        torchOn = torch,
        shutterEnabled = shutterEnabled,
        flash = flash.value,
        onClose = onClose,
        onTorch = { torch = !torch; session.camera?.cameraControl?.enableTorch(torch) },
        onShutter = ::takePhoto,
    )
}

private const val TAG = "PieceCapture"
