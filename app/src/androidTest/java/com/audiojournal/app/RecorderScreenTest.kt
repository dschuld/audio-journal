package com.audiojournal.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI smoke test. Runs on an emulator or device:
 * ./gradlew connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class RecorderScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun recordButtonIsShownWhenIdle() {
        composeRule.onNodeWithTag("record_button").assertIsDisplayed()
        composeRule.onNodeWithText("Tap to record").assertIsDisplayed()
        composeRule.onNodeWithText("00:00").assertIsDisplayed()
    }
}
