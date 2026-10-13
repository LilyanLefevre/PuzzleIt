package com.lilyan_lefevre.puzzleit.feature.puzzle.capture

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.google.android.material.transition.MaterialSharedAxis
import com.lilyan_lefevre.puzzleit.R
import com.lilyan_lefevre.puzzleit.core.ui.screen
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** Takes the photo of one loose piece and hands the file back to the working screen. The camera is CameraX, the rest is [PieceCaptureScreen]. */
class PieceCaptureFragment : Fragment() {

    private var previewView: PreviewView? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private lateinit var executor: ExecutorService

    // Read by the screen: Compose redraws it when they change, even when they are set from the camera's thread.
    private var torch by mutableStateOf(false)
    private var shutterEnabled by mutableStateOf(true)
    /** The photo just taken: the flash plays, then it is handed back. */
    private var taken by mutableStateOf<File?>(null)

    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startCamera() else {
            Toast.makeText(requireContext(), R.string.camera_permission_denied, Toast.LENGTH_LONG).show()
            findNavController().navigateUp()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Y, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Y, false)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View = screen {
        val flash = remember { Animatable(0f) }
        // Shutter flash, then back to the table where the analysis starts.
        LaunchedEffect(taken) {
            val file = taken ?: return@LaunchedEffect
            flash.snapTo(0.9f)
            flash.animateTo(0f, tween(260))
            setFragmentResult(RESULT_KEY, bundleOf(PHOTO_PATH to file.absolutePath))
            findNavController().navigateUp()
        }
        PieceCaptureScreen(
            onPreviewReady = { previewView = it; openCamera() },
            torchOn = torch,
            shutterEnabled = shutterEnabled,
            flash = flash.value,
            onClose = { findNavController().navigateUp() },
            onTorch = { torch = !torch; camera?.cameraControl?.enableTorch(torch) },
            onShutter = ::takePhoto,
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        executor = Executors.newSingleThreadExecutor()
    }

    /** Called once the preview exists: start the camera, asking for the permission first if needed. */
    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) startCamera()
        else permission.launch(Manifest.permission.CAMERA)
    }

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(requireContext())
        future.addListener({
            // The provider can come back after the user already left this screen.
            val view = previewView ?: return@addListener
            val provider = future.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(view.surfaceProvider) }
            imageCapture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).build()
            try {
                provider.unbindAll()
                camera = provider.bindToLifecycle(viewLifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
            } catch (e: Exception) {
                Log.e(TAG, "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        val capture = imageCapture ?: return
        shutterEnabled = false
        val file = File(requireContext().cacheDir, "piece_${System.currentTimeMillis()}.jpg")
        capture.takePicture(ImageCapture.OutputFileOptions.Builder(file).build(), executor, object : ImageCapture.OnImageSavedCallback {
            override fun onError(e: ImageCaptureException) {
                Log.e(TAG, "Capture failed", e)
                shutterEnabled = true
            }

            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                taken = file
            }
        })
    }

    override fun onDestroyView() {
        camera?.cameraControl?.enableTorch(false)
        executor.shutdown()
        previewView = null
        super.onDestroyView()
    }

    companion object {
        private const val TAG = "PieceCapture"
        const val RESULT_KEY = "pieceCaptureResult"
        const val PHOTO_PATH = "photoPath"
    }
}
