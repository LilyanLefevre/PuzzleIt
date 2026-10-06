package com.lilyan_lefevre.puzzleit.e2e

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.lilyan_lefevre.puzzleit.Dest
import com.lilyan_lefevre.puzzleit.MainActivity
import com.lilyan_lefevre.puzzleit.PIECE_PHOTO
import com.lilyan_lefevre.puzzleit.R
import com.lilyan_lefevre.puzzleit.TestImages
import com.lilyan_lefevre.puzzleit.feature.project.data.ProjectRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.runBlocking

/**
 * Whole journey on the phone: list -> puzzle table -> (injected) piece photo -> result sheet -> leads -> dismiss.
 * The photo is injected through the same saved-state channel the camera screen uses, so everything
 * downstream of the shutter is the real code. The screens are Compose: nodes are found by their `testTag`
 * (the names the old view ids had). Run with animations off (see CI script / README).
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ScanFlowTest {

    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val camera: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA)
    @get:Rule(order = 2) val compose = createEmptyComposeRule()

    @Inject lateinit var repository: ProjectRepository

    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    private val id = "e2e-scan-flow"
    private val name = "E2E Cascade"
    private val cols = 20
    private val rows = 15
    private val art = TestImages.boxArt()

    @Before
    fun seed() {
        hilt.inject()
        // Delete a leftover project first: deleting it also deletes its image file.
        runBlocking { repository.deleteProject(id) }
        val ref = TestImages.save(art, ctx.filesDir, "e2e_ref.jpg").absolutePath
        runBlocking {
            repository.createProject(id, ref, ref, ref, name, cols * rows, rows, cols, "medium", null)
        }
    }

    @After
    fun cleanUp() {
        runBlocking { repository.deleteProject(id) }
        File(ctx.filesDir, "e2e_ref.jpg").delete()
    }

    private fun waitFor(timeoutMs: Long = 60_000, check: () -> Unit) {
        val end = System.currentTimeMillis() + timeoutMs
        while (true) {
            try { check(); return } catch (e: Throwable) {
                if (System.currentTimeMillis() > end) throw e
                Thread.sleep(250)
            }
        }
    }

    private fun shown(tag: String) = compose.onNodeWithTag(tag).assertIsDisplayed()

    /** Screenshots land in the app's external files dir: adb pull /sdcard/Android/data/<pkg>/files/shots */
    private fun shot(scenario: ActivityScenario<MainActivity>, label: String) {
        // PixelCopy of the activity window: unlike UiAutomation it doesn't disturb the window focus.
        var window: android.view.Window? = null
        scenario.onActivity { window = it.window }
        val w = window ?: return
        val bmp = android.graphics.Bitmap.createBitmap(w.decorView.width, w.decorView.height, android.graphics.Bitmap.Config.ARGB_8888)
        val latch = java.util.concurrent.CountDownLatch(1)
        android.view.PixelCopy.request(w, bmp, { latch.countDown() }, android.os.Handler(android.os.Looper.getMainLooper()))
        latch.await(5, java.util.concurrent.TimeUnit.SECONDS)
        val dir = File(ctx.getExternalFilesDir(null), "shots").also { it.mkdirs() }
        java.io.FileOutputStream(File(dir, "$label.png")).use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun idle(): Boolean = compose.onAllNodesWithTag("buttonCapturePiece").fetchSemanticsNodes().isNotEmpty()

    /**
     * The photo goes through the channel the camera screen uses. The table ignores it until its matcher is prepared, so it is
     * sent again until the screen leaves the idle state (a person needs seconds to take a photo; the test needs milliseconds).
     */
    private fun inject(scenario: ActivityScenario<MainActivity>, photo: File) {
        waitFor(30_000) {
            if (idle()) {
                scenario.onActivity { a ->
                    a.navController.getBackStackEntry<Dest.PuzzleWorking>().savedStateHandle[PIECE_PHOTO] = photo.absolutePath
                }
                Thread.sleep(300)
            }
            check(!idle()) { "the table did not start analysing" }
        }
    }

    private fun openTable() = ActivityScenario.launch(MainActivity::class.java).also {
        waitFor { compose.onNodeWithText(name).assertIsDisplayed() }
        compose.onNodeWithText(name).performClick()
        waitFor { shown("buttonCapturePiece") }
    }

    /** The chip of lead [n] in the row of chips (the cards below also say "Lead n"). */
    private fun chip(n: Int) = compose.onNodeWithTag("chipLead$n")

    @Test
    fun listShowsProjectAndTableShowsGrid() {
        openTable().use { scenario ->
            // The project loads asynchronously: wait for it instead of asserting on the first frame.
            waitFor { compose.onNodeWithTag("textViewProjectName").assertTextEquals(name) }
            waitFor { compose.onNodeWithTag("textViewProjectInfo").assertTextContains("${cols * rows}", substring = true) }
            Thread.sleep(800); shot(scenario, "1_table")
        }
    }

    @Test
    fun scanShowsResultWithLeadsThenDismisses() {
        openTable().use { scenario ->
            val photo = TestImages.save(TestImages.piecePhoto(art, cols, rows, 9, 6, 90f), ctx.cacheDir, "e2e_piece.jpg")
            inject(scenario, photo)
            // The analysing state is brief by design: catch it if we can, never fail on it.
            runCatching { waitFor(3_000) { shown("groupAnalyzing") }; shot(scenario, "2_analyzing") }
            waitFor { shown("groupResult") }
            Thread.sleep(1800)   // let the camera move and the piece spin settle
            shot(scenario, "3_result")
            shown("textSpot")
            shown("chipLeads")
            chip(2).performClick()
            waitFor { compose.onNodeWithTag("textLead").assertTextContains(ctx.getString(R.string.alt_lead, 2).uppercase(), substring = true) }
            Thread.sleep(1200); shot(scenario, "4_second_lead")
            compose.onNodeWithTag("buttonDismiss").performScrollTo().performClick()
            waitFor { shown("buttonCapturePiece") }
        }
    }

    /** A real swipe from [fromY] to [toY] (screen pixels) through the input system, like a finger on the sheet. */
    private fun drag(scenario: ActivityScenario<MainActivity>, fromY: Float, toY: Float) {
        var x = 0
        scenario.onActivity { x = it.window.decorView.width / 2 }
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("input swipe $x ${fromY.toInt()} $x ${toY.toInt()} 300").close()
    }

    private fun screenHeight(scenario: ActivityScenario<MainActivity>): Float {
        var h = 0f
        scenario.onActivity { h = it.window.decorView.height.toFloat() }
        return h
    }

    /** A little under the top edge of the result sheet, on the grabber: where a finger takes hold of it. */
    private fun sheetTop(): Float = compose.onNodeWithTag("sheet").fetchSemanticsNode().boundsInWindow.top + 24 * ctx.resources.displayMetrics.density

    @Test
    fun resultSheetSlidesFromPeekToFullAndAwayAndBack() {
        openTable().use { scenario ->
            inject(scenario, TestImages.save(TestImages.piecePhoto(art, cols, rows, 9, 6, 90f), ctx.cacheDir, "e2e_piece2.jpg"))
            waitFor { shown("groupResult") }
            Thread.sleep(1800)
            shown("textSpot")               // half level: the comparison is visible
            // Up to the full level: the page that explains the leads.
            shot(scenario, "5_half")
            drag(scenario, sheetTop(), 0.04f * screenHeight(scenario)); Thread.sleep(700)
            shot(scenario, "6_after_drag_up")
            // On a small screen the explanation page starts below the fold: scroll down to it.
            compose.onNodeWithTag("groupExplain").performScrollTo()
            Thread.sleep(400)
            shown("groupExplain"); shot(scenario, "6_explain")
            // Freeze the first card's blinking and bring it into view: the piece must sit on the box where the lead says.
            compose.onNodeWithTag("leadCompare0").performScrollTo().performClick()
            Thread.sleep(500); shot(scenario, "6b_explain_overlay")
            // All the way down: the last card must be whole above the bottom edge of the screen.
            compose.onNodeWithTag("explainCard2").performScrollTo()
            Thread.sleep(500); shot(scenario, "6c_explain_bottom")
            compose.onNodeWithTag("chipLeads").performScrollTo()
            Thread.sleep(300)
            // Away: a long swipe down takes the sheet off whatever the screen's size, the map is free and the button brings the leads back.
            // (The peek level is checked by the card test; on a small screen the half and full levels nearly coincide.)
            // The swipe is a shell command: on a slow CI emulator it can start while the sheet still moves, so it is repeated.
            waitFor {
                if (runCatching { shown("pillLeads") }.isFailure) {
                    drag(scenario, sheetTop(), 0.98f * screenHeight(scenario)); Thread.sleep(1500)
                }
                shown("pillLeads")
            }
            shot(scenario, "8_hidden")
            compose.onNodeWithTag("pillLeads").performClick(); Thread.sleep(700)
            shown("textSpot")
        }
    }

    @Test
    fun aLeadCardLeadsToThePuzzleWithThePieceOverIt() {
        openTable().use { scenario ->
            inject(scenario, TestImages.save(TestImages.piecePhoto(art, cols, rows, 9, 6, 90f), ctx.cacheDir, "e2e_piece3.jpg"))
            waitFor { shown("groupResult") }
            Thread.sleep(1800)
            drag(scenario, sheetTop(), 0.04f * screenHeight(scenario)); Thread.sleep(700)             // the page that explains the leads
            // Tapping the second lead's card selects it and drops the sheet to its peek level, over the map.
            // Low on the card, on "See on the puzzle": the middle is the blinking comparison, whose tap only freezes it.
            compose.onNodeWithTag("explainCard1").performScrollTo().performTouchInput { click(percentOffset(0.5f, 0.95f)) }
            Thread.sleep(1500)
            chip(2).assertIsSelected()
            shown("buttonOverlay")
            shot(scenario, "9_card_to_map"); Thread.sleep(900); shot(scenario, "9b_card_to_map")
        }
    }

    @Test
    fun blurryPhotoAsksToRetake() {
        openTable().use { scenario ->
            val photo = TestImages.save(TestImages.blurred(TestImages.piecePhoto(art, cols, rows, 5, 5, 0f)), ctx.cacheDir, "e2e_blur.jpg")
            inject(scenario, photo)
            waitFor { shown("groupError") }
            compose.onNodeWithTag("textErrorTitle").assertTextEquals(ctx.getString(R.string.blurry_title))
            shown("buttonRetake")
            shot(scenario, "5_blurry")
        }
    }

    @Test
    fun emptyTableReportsNoPiece() {
        openTable().use { scenario ->
            val photo = TestImages.save(TestImages.emptyTable(), ctx.cacheDir, "e2e_empty.jpg")
            inject(scenario, photo)
            waitFor { shown("groupError") }
        }
    }

    @Test
    fun aScanIsRatedAndKeptInTheHistory() {
        openTable().use { scenario ->
            inject(scenario, TestImages.save(TestImages.piecePhoto(art, cols, rows, 9, 6, 90f), ctx.cacheDir, "e2e_piece4.jpg"))
            waitFor { shown("groupResult") }
            Thread.sleep(1800)
            compose.onNodeWithTag("buttonRight").performScrollTo().performClick()
            compose.onNodeWithTag("textEvaluate").assertTextEquals(ctx.getString(R.string.evaluate_thanks))
            Thread.sleep(500)                                                                  // the verdict is written in the background
            compose.onNodeWithTag("buttonMore").performClick()
            compose.onNodeWithText(ctx.getString(R.string.menu_history)).performClick()
            waitFor { compose.onNodeWithText(ctx.getString(R.string.verdict_correct)).assertIsDisplayed() }
            shot(scenario, "10_history")
        }
    }

    @Test
    fun signingOutBringsTheLoginScreenBackAndItChecksTheForm() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitFor { shown("buttonAccount") }
            compose.onNodeWithTag("buttonAccount").performClick()
            waitFor { shown("buttonSignOut") }
            shot(scenario, "12_account")
            compose.onNodeWithTag("buttonSignOut").performClick()
            waitFor { shown("editEmail") }
            compose.onNodeWithTag("editPassword").performTextInput("short")
            compose.onNodeWithTag("buttonSignIn").performScrollTo().performClick()               // below the fold on a small screen
            compose.onNodeWithTag("textNotice").assertTextEquals(ctx.getString(R.string.credentials_invalid))
            shot(scenario, "13_login")
        }
    }

    @Test
    fun progressScreenOpensFromTheMenu() {
        openTable().use { scenario ->
            compose.onNodeWithTag("buttonMore").performClick()
            compose.onNodeWithText(ctx.getString(R.string.menu_progress)).performClick()
            waitFor { shown("buttonAddPhoto") }
            shown("textEmpty")
            shot(scenario, "11_progress")
        }
    }

    @Test
    fun scanButtonOpensViewfinderAndBackReturns() {
        openTable().use {
            compose.onNodeWithTag("buttonCapturePiece").performClick()
            waitFor { shown("shutter") }
            shown("previewView")
            compose.onNodeWithTag("buttonClose").performClick()
            waitFor { shown("buttonCapturePiece") }
        }
    }

    @Test
    fun editFromTheTableOpensThePrefilledForm() {
        openTable().use {
            compose.onNodeWithTag("buttonEdit").performClick()
            waitFor { compose.onNodeWithTag("editTextName").assertTextContains(name) }
            compose.onNodeWithTag("editTextPieces").assertTextContains("${cols * rows}")
        }
    }

    @Test
    fun deleteFromTableAsksThenRemovesTheProject() {
        openTable().use {
            compose.onNodeWithTag("buttonDelete").performClick()
            compose.onNodeWithText(ctx.getString(R.string.delete)).performClick()
            waitFor { shown("fabAddProject") }
            waitFor { compose.onNodeWithText(name).assertDoesNotExist() }
            assertNull(runBlocking { repository.getProjectById(id) })
        }
    }
}
