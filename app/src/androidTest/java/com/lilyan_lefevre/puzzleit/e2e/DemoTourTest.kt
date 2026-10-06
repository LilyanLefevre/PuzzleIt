package com.lilyan_lefevre.puzzleit.e2e

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.lilyan_lefevre.puzzleit.Dest
import com.lilyan_lefevre.puzzleit.MainActivity
import com.lilyan_lefevre.puzzleit.PIECE_PHOTO
import com.lilyan_lefevre.puzzleit.R
import com.lilyan_lefevre.puzzleit.TestImages
import com.lilyan_lefevre.puzzleit.feature.project.data.Project
import com.lilyan_lefevre.puzzleit.feature.project.data.ProjectRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Plays the tour that the README video shows, with slow real swipes, on the puzzle already on the device whose name is given as
 * `-e demoPuzzle "<name>"` (read only; the phone also holds private puzzles, so it is never picked by itself).
 * The screen records itself into /sdcard/demo.mp4: `adb pull /sdcard/demo.mp4`. Skipped without that argument or when no puzzle has that name.
 *
 * No Compose test rule here: it would freeze the infinite animations (the blinking piece, the loaders) that the video must show. The nodes are
 * found through the accessibility tree instead, by their test tag (the screens expose tags as resource ids), and touched with real `input` events.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DemoTourTest {

    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val camera: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA)

    @Inject lateinit var repository: ProjectRepository

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val ctx = instrumentation.targetContext

    private fun shell(cmd: String): String =
        instrumentation.uiAutomation.executeShellCommand(cmd).use { ParcelFileDescriptor.AutoCloseInputStream(it).readBytes().toString(Charsets.UTF_8) }

    private fun waitFor(timeoutMs: Long = 60_000, check: () -> Unit) {
        val end = System.currentTimeMillis() + timeoutMs
        while (true) {
            try { check(); return } catch (e: Throwable) {
                if (System.currentTimeMillis() > end) throw e
                Thread.sleep(250)
            }
        }
    }

    /** A real swipe, as long as [ms] says: the sheet and the lists follow the finger frame by frame. */
    private fun swipe(x: Float, fromY: Float, toY: Float, ms: Int) {
        shell("input swipe ${x.toInt()} ${fromY.toInt()} ${x.toInt()} ${toY.toInt()} $ms")
    }

    /** Where the node tagged [tag] is on the screen, or null when it is not shown. */
    private fun bounds(tag: String): Rect? =
        instrumentation.uiAutomation.rootInActiveWindow?.findAccessibilityNodeInfosByViewId(tag)?.firstOrNull()
            ?.let { node -> Rect().also(node::getBoundsInScreen) }

    private fun shown(tag: String): Rect = checkNotNull(bounds(tag)) { "$tag is not on the screen" }

    /** A real tap at the centre of the node (or on its lower edge, [low], to land beside what is drawn in its middle): Espresso refuses to run with animations on, and the video needs them. */
    private fun tap(tag: String, low: Boolean = false) {
        val r = shown(tag)
        shell("input tap ${r.centerX()} ${if (low) r.bottom - (24 * ctx.resources.displayMetrics.density).toInt() else r.centerY()}")
    }

    /** True while the table waits for a piece photo. */
    private fun idle() = bounds("buttonCapturePiece") != null

    /** The photo goes through the channel the camera screen uses; sent again until the matcher is ready. */
    private fun send(scenario: ActivityScenario<MainActivity>, file: File) = waitFor(60_000) {
        if (idle()) {
            scenario.onActivity { a ->
                a.navController.getBackStackEntry<Dest.PuzzleWorking>().savedStateHandle[PIECE_PHOTO] = file.absolutePath
            }
            Thread.sleep(300)
        }
        check(!idle()) { "the table did not start analysing" }
    }

    /** A little under the top edge of the result sheet, on the grabber. */
    private fun sheetTop(): Float = shown("sheet").top + 24 * ctx.resources.displayMetrics.density

    /** The puzzle to film, its box image and the grid a piece photo is cut on. */
    private class Demo(val project: Project, val art: Bitmap, val cols: Int, val rows: Int)

    /** Never "the first puzzle": the phone holds the person's private puzzles. The one to film is named on the command line. */
    private fun chosenPuzzle(): Demo {
        hilt.inject()
        val wanted = InstrumentationRegistry.getArguments().getString("demoPuzzle")
        assumeTrue("needs -e demoPuzzle \"<name of the puzzle to film>\"", wanted != null)
        val project = runBlocking { repository.getAllProjects().first() }.firstOrNull { it.name == wanted && it.warpedPath.isNotEmpty() }
        assumeTrue("no puzzle named \"$wanted\" on the device", project != null)
        project!!
        val art = BitmapFactory.decodeFile(project.warpedPath, BitmapFactory.Options().apply { inSampleSize = 2 })
        val cols = sqrt(project.puzzleSize * art.width.toFloat() / art.height).roundToInt()
        return Demo(project, art, cols, (project.puzzleSize / cols.toFloat()).roundToInt())
    }

    private fun pieceOf(d: Demo, name: String) =
        TestImages.save(TestImages.piecePhoto(d.art, d.cols, d.rows, (d.cols * 0.4f).toInt(), (d.rows * 0.5f).toInt(), 90f), ctx.cacheDir, name)

    /** PixelCopy of the app window (the status bar is not in it): `adb pull /sdcard/Android/data/<pkg>/files/shots`. */
    private fun shot(scenario: ActivityScenario<MainActivity>, label: String) {
        var window: android.view.Window? = null
        scenario.onActivity { window = it.window }
        val w = window ?: return
        val bmp = Bitmap.createBitmap(w.decorView.width, w.decorView.height, Bitmap.Config.ARGB_8888)
        val latch = java.util.concurrent.CountDownLatch(1)
        android.view.PixelCopy.request(w, bmp, { latch.countDown() }, android.os.Handler(android.os.Looper.getMainLooper()))
        latch.await(5, java.util.concurrent.TimeUnit.SECONDS)
        val dir = File(ctx.getExternalFilesDir(null), "shots").also { it.mkdirs() }
        java.io.FileOutputStream(File(dir, "readme_$label.png")).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun tour() {
        val demo = chosenPuzzle()
        val project = demo.project
        val photo = pieceOf(demo, "demo_piece.jpg")

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitFor { shown("project_${project.id}") }
            // The list is not filmed: it shows every puzzle of the phone. The recording starts once inside the chosen puzzle.
            tap("project_${project.id}")
            waitFor { shown("buttonCapturePiece") }
            val recorder = instrumentation.uiAutomation.executeShellCommand("screenrecord --size 720x1544 --bit-rate 14000000 /sdcard/demo.mp4")
            Thread.sleep(2500)

            send(scenario, photo)
            waitFor { shown("groupResult") }
            Thread.sleep(3500)

            var width = 0f; var height = 0f
            scenario.onActivity { width = it.window.decorView.width.toFloat(); height = it.window.decorView.height.toFloat() }

            tap("chipLead2"); Thread.sleep(2500)
            tap("chipLead1"); Thread.sleep(2000)
            swipe(width / 2, sheetTop(), 0.04f * height, 900); Thread.sleep(2000)    // full level: why these leads
            repeat(3) { swipe(width / 2, 0.8f * height, 0.35f * height, 900); Thread.sleep(1500) }
            repeat(3) { swipe(width / 2, 0.35f * height, 0.8f * height, 700); Thread.sleep(600) }
            Thread.sleep(800)
            tap("explainCard1", low = true)   // a card leads to the map
            Thread.sleep(3000)
            swipe(width / 2, sheetTop(), 0.995f * height, 200); Thread.sleep(2500)    // a flick hides the sheet: the map is free
            tap("pillLeads"); Thread.sleep(3000)

            shell("kill -2 ${shell("pidof screenrecord").trim()}")   // SIGINT: the recorder finishes the file
            Thread.sleep(2000)
            recorder.close()
        }
    }

    /** The README screenshots (table, blurry photo, result, "why these leads", free map), on the same puzzle; the list is not captured, it shows every puzzle of the phone. */
    @Test
    fun readmeShots() {
        val demo = chosenPuzzle()
        val project = demo.project
        val photo = pieceOf(demo, "demo_piece.jpg")

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitFor { shown("project_${project.id}") }
            tap("project_${project.id}")
            waitFor { shown("buttonCapturePiece") }
            Thread.sleep(2500)
            shot(scenario, "table")

            send(scenario, photo)
            waitFor { shown("groupResult") }
            Thread.sleep(3500); shot(scenario, "result")

            var width = 0f; var height = 0f
            scenario.onActivity { width = it.window.decorView.width.toFloat(); height = it.window.decorView.height.toFloat() }
            swipe(width / 2, sheetTop(), 0.04f * height, 900); Thread.sleep(2500); shot(scenario, "why")
            swipe(width / 2, sheetTop(), 0.995f * height, 200); Thread.sleep(2500); shot(scenario, "map")
        }
    }

    /**
     * The README screenshot of the list. It needs puzzles that are not the person's: on an emulator only (never on a real phone), it creates three from the
     * public-domain images wave.jpg, starry.jpg and lilies.jpg of the directory given as `-e demoBoxDir <dir>`, and removes them afterwards.
     */
    @Test
    fun readmeList() {
        hilt.inject()
        assumeTrue("emulator only: it would add puzzles to a real phone", android.os.Build.HARDWARE == "ranchu" || android.os.Build.HARDWARE == "goldfish")
        val dir = InstrumentationRegistry.getArguments().getString("demoBoxDir")
        assumeTrue("needs -e demoBoxDir <dir with wave.jpg, starry.jpg, lilies.jpg>", dir != null)
        val demos = listOf(Triple("wave", "La vague", 1000), Triple("starry", "La nuit étoilée", 500), Triple("lilies", "Les nymphéas", 1000))
        val ids = demos.map { "demo-list-${it.first}" }
        val files = demos.map { File(ctx.filesDir, "demo_list_${it.first}.jpg") }

        try {
            runBlocking {
                demos.forEachIndexed { i, (key, name, pieces) ->
                    val art = BitmapFactory.decodeFile(File(dir, "$key.jpg").path)
                    val cols = sqrt(pieces * art.width.toFloat() / art.height).roundToInt()
                    val ref = TestImages.save(art, ctx.filesDir, files[i].name).absolutePath
                    repository.deleteProject(ids[i])
                    repository.createProject(ids[i], ref, ref, ref, name, pieces, (pieces / cols.toFloat()).roundToInt(), cols, "medium", null)
                }
            }
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                waitFor { shown("project_${ids.last()}") }
                Thread.sleep(2000); shot(scenario, "list")
            }
        } finally {
            runBlocking { ids.forEach { repository.deleteProject(it) } }
            files.forEach { it.delete() }
        }
    }

    /** The README screenshot of a blurry photo being refused, on the same puzzle. */
    @Test
    fun readmeBlurry() {
        val demo = chosenPuzzle()
        val project = demo.project
        val blurry = TestImages.save(TestImages.blurred(TestImages.piecePhoto(demo.art, demo.cols, demo.rows, 5, 5, 0f)), ctx.cacheDir, "demo_blur.jpg")

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitFor { shown("project_${project.id}") }
            tap("project_${project.id}")
            waitFor { shown("buttonCapturePiece") }
            send(scenario, blurry)
            waitFor { shown("groupError") }
            Thread.sleep(1200); shot(scenario, "blurry")
        }
    }
}
