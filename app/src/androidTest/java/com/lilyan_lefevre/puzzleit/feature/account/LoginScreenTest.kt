package com.lilyan_lefevre.puzzleit.feature.account

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.lilyan_lefevre.puzzleit.core.ui.theme.PuzzleTheme
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Renders [LoginScreen] on its own (no account, nobody signed out): unreachable server, retry, loading. */
class LoginScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun unreachableServerOffersRetryThenShowsLoading() {
        var unreachable by mutableStateOf(true)
        var busy by mutableStateOf(false)
        var retries = 0
        compose.setContent {
            PuzzleTheme {
                LoginScreen(
                    null, busy = busy, serverUnreachable = unreachable, error = null,
                    onProvider = {}, onRetry = { retries++; busy = true },
                )
            }
        }
        compose.onNodeWithTag("textUnreachable").assertIsDisplayed()
        compose.onNodeWithTag("buttonRetry").assertIsDisplayed()
        shot("login_unreachable")

        compose.onNodeWithTag("buttonRetry").performClick()
        compose.waitForIdle()
        assertEquals(1, retries)
        compose.onNodeWithTag("buttonRetry").assertDoesNotExist()
        shot("login_loading")

        busy = false; unreachable = false
        compose.waitForIdle()
        compose.onNodeWithTag("textUnreachable").assertDoesNotExist()
    }

    /** PixelCopy of the window into the app's shots folder (does not disturb focus). */
    private fun shot(label: String) {
        compose.waitForIdle()
        val w = compose.activity.window
        val bmp = Bitmap.createBitmap(w.decorView.width, w.decorView.height, Bitmap.Config.ARGB_8888)
        val latch = CountDownLatch(1)
        PixelCopy.request(w, bmp, { latch.countDown() }, Handler(Looper.getMainLooper()))
        latch.await(5, TimeUnit.SECONDS)
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val dir = File(ctx.getExternalFilesDir(null), "shots").also { it.mkdirs() }
        File(dir, "$label.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
