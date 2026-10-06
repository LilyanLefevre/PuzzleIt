package com.lilyan_lefevre.puzzleit

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.lilyan_lefevre.puzzleit.core.ui.theme.PuzzleTheme
import com.lilyan_lefevre.puzzleit.feature.account.data.AccountStore
import com.lilyan_lefevre.puzzleit.feature.account.data.SyncRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.opencv.android.OpenCVLoader

/** The only activity: it hosts the theme and the [PuzzleNavHost] where every screen lives. */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var accountStore: AccountStore
    @Inject lateinit var syncRepository: SyncRepository

    /** The e2e tests hand the piece photo to the table through it, the way the camera screen does. */
    @VisibleForTesting
    lateinit var navController: NavHostController

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        syncRepository.startAutoSync()
        setContent {
            PuzzleTheme {
                // testTagsAsResourceId: the tests tag nodes with names (like the old view ids), and the screen recorder / UiAutomator read them as ids.
                Surface(Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }, color = MaterialTheme.colorScheme.background) {
                    navController = rememberNavController()
                    PuzzleNavHost(navController, accountStore)
                }
            }
        }

        // Initialize OpenCV
        initializeOpenCV()
    }

    // Leaving the app is the moment to push what changed: there is no sync button.
    override fun onStop() {
        super.onStop()
        syncRepository.syncInBackground()
    }

    /**
     * Initialize OpenCV library
     * This must be called before using any OpenCV functions
     */
    private fun initializeOpenCV() {
        if (OpenCVLoader.initLocal()) {
            Log.i(TAG, "OpenCV loaded successfully")
        } else {
            Log.e(TAG, "OpenCV initialization failed!")
            Toast.makeText(this, "OpenCV initialization failed!", Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
