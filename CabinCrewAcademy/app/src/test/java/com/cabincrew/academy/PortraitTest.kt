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
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Same app on a tablet held in portrait (800x1280dp). */
@OptIn(ExperimentalTestApi::class, ExperimentalRoborazziApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w800dp-h1280dp-port-mdpi")
class PortraitTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val container get() = ApplicationProvider.getApplicationContext<CabinCrewApplication>().container

    private fun shot(name: String) { rule.waitForIdle(); captureScreenRoboImage("build/screenshots/portrait_$name.png") }
    private fun waitText(t: String) = rule.waitUntilAtLeastOneExists(hasText(t, substring = true), 10_000)

    @Test
    fun portraitTour() {
        TestFixtures.seed(container)
        waitText("Alex")
        shot("01_dashboard")
        rule.onAllNodesWithText("Study").onFirst().performClick()
        waitText("What would you like to study?")
        rule.onNodeWithText("Start studying").performScrollTo().performClick()
        waitText("Question 1 of 24")
        rule.onNodeWithText("Fourth option").performClick()
        rule.onNodeWithText("Check answer").performClick()
        waitText("Incorrect")
        shot("02_study")
        rule.onNodeWithContentDescription("Back").performClick()
        rule.onAllNodesWithText("Exam").onFirst().performClick()
        rule.onNodeWithText("Begin examination").performScrollTo().performClick()
        waitText("Mock examination")
        rule.onNodeWithText("Second option").performClick()
        shot("03_exam")
        rule.onNodeWithContentDescription("Question overview").performClick()
        waitText("Question overview")
        shot("04_exam_overview_sheet")
    }
}
