package com.cabincrew.academy

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cabincrew.academy.data.settings.ThemeMode
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Drives the real app end-to-end (Room + DataStore + navigation) on a simulated
 * 1280x800dp tablet and records screenshots into app/build/screenshots.
 * The questions below are neutral test fixtures, not study content.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalRoborazziApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w1280dp-h800dp-land-mdpi")
class AppTourTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private val container get() = ApplicationProvider.getApplicationContext<CabinCrewApplication>().container

    private fun shot(name: String) {
        rule.waitForIdle()
        captureScreenRoboImage("build/screenshots/$name.png")
    }

    private fun waitText(text: String, timeout: Long = 10_000) =
        rule.waitUntilAtLeastOneExists(hasText(text, substring = true), timeout)

    private fun seed() = TestFixtures.seed(container)

    private fun dump(tag: String) = runBlocking {
        container.repository.loadQuestionsWithProgress().filter { it.progress != null }.forEach {
            println("DUMP $tag ${it.question.externalId} id=${it.question.id} p=${it.progress}")
        }
    }

    @Test
    fun fullTour() {
        waitText("Welcome aboard")
        shot("01_dashboard_empty")

        rule.onNodeWithText("Import files").performScrollTo().performClick()
        waitText("File format guide")
        shot("02_import")
        rule.onNodeWithContentDescription("Back").performClick()

        seed()
        waitText("Good")
        waitText("Alex")
        shot("03_dashboard")

        // Study mode
        rule.onAllNodesWithText("Study").onFirst().performClick()
        waitText("What would you like to study?")
        shot("04_study_setup")
        rule.onNodeWithText("Start studying").performScrollTo().performClick()
        waitText("Question 1 of 24")
        rule.onNodeWithText("Second option").performClick()
        rule.onNodeWithText("Check answer").performClick()
        waitText("Explanation")
        shot("05_study_feedback")
        rule.onNodeWithText("Next question").performClick()
        waitText("Question 2 of 24")
        rule.onNodeWithText("First option").performClick()
        rule.onNodeWithText("Check answer").performClick()
        rule.onNodeWithContentDescription("Bookmark for revision").performClick()
        rule.onNodeWithContentDescription("Mark as difficult").performClick()
        shot("06_study_second")
        dump("afterStudy")
        rule.onNodeWithContentDescription("Back").performClick()

        // Exam mode
        rule.onAllNodesWithText("Exam").onFirst().performClick()
        waitText("Begin examination")
        shot("07_exam_setup")
        rule.onNodeWithText("Begin examination").performScrollTo().performClick()
        waitText("Question overview")
        rule.onNodeWithText("Third option").performClick()
        rule.onNodeWithText("Mark for review").performClick()
        rule.onNodeWithText("Next").performClick()
        rule.onNodeWithText("First option").performClick()
        shot("08_exam_session")
        rule.onNodeWithText("Submit").performClick()
        waitText("Submit examination?")
        shot("09_exam_submit_dialog")
        rule.onNodeWithText("Submit now").performClick()
        waitText("Correction report", 15_000)
        shot("10_exam_result")
        dump("afterExam")
        rule.onNodeWithContentDescription("Back").performClick()

        rule.onAllNodesWithText("Questions").onFirst().performClick()
        waitText("Question bank")
        shot("11_question_bank")
        rule.onNodeWithText("Sample fixture question 2 for Category One?").performClick()
        waitText("Edit question")
        shot("12_question_editor")
        rule.onNodeWithContentDescription("Back").performClick()

        rule.onAllNodesWithText("Statistics").onFirst().performClick()
        waitText("Performance by category")
        shot("13_statistics")

        rule.onAllNodesWithText("Settings").onFirst().performClick()
        waitText("Examination")
        shot("14_settings")

        rule.onAllNodesWithText("Dashboard").onFirst().performClick()
        waitText("Continue studying")
        shot("15_dashboard_after_activity")

        runBlocking { container.settings.setThemeMode(ThemeMode.DARK) }
        rule.waitForIdle()
        Thread.sleep(300)
        shot("16_dashboard_dark")
    }
}
