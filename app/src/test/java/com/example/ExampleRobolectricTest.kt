package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Fillwords", appName)
  }

  @Test
  fun `home screen renders and launches signature 8x11 board`() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("FILLWORDS").assertIsDisplayed()
    composeTestRule.onNodeWithTag("home_play_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("home_signature_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("fillword_board").assertIsDisplayed()
    composeTestRule.onNodeWithText("Lv 35 • Fillwords Classic").assertIsDisplayed()
    composeTestRule.onNodeWithTag("extra_words_bucket_pill").assertIsDisplayed()
  }
}
