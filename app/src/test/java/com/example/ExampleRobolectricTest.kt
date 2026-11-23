package com.example

import android.content.Context
import android.view.WindowManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.Lifecycle
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
    composeTestRule.onNodeWithTag("home_lazy_column")
      .performScrollToNode(hasTestTag("home_daily_challenge_card"))
    composeTestRule.onNodeWithTag("home_daily_challenge_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("home_lazy_column")
      .performScrollToNode(hasTestTag("home_signature_button"))
    composeTestRule.onNodeWithTag("home_signature_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("fillword_board").assertIsDisplayed()
    composeTestRule.onNodeWithText("Lv 35 • Fillwords Classic").assertIsDisplayed()
    composeTestRule.onNodeWithTag("extra_words_bucket_pill").assertIsDisplayed()
  }

  @Test
  fun `tapping top hint bulb three times opens visible pin setup, unlocks vault, supports 0000 3x recovery and instant minimize lock`() {
    composeTestRule.waitForIdle()
    val bulb = composeTestRule.onNodeWithTag("home_hint_bulb_pill")
    bulb.assertIsDisplayed()

    // 2 taps do NOT open PIN overlay
    bulb.performClick()
    bulb.performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("pin_gate_overlay").assertDoesNotExist()

    // 3rd consecutive tap opens Create Vault PIN visibly (FLAG_SECURE off by default)
    bulb.performClick()
    composeTestRule.waitUntil(timeoutMillis = 3_000L) {
      composeTestRule.onAllNodes(hasTestTag("pin_gate_overlay")).fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("pin_gate_overlay").assertIsDisplayed()
    composeTestRule.onNodeWithText("Create Vault PIN").assertIsDisplayed()
    val flags = composeTestRule.activity.window.attributes.flags
    assertEquals(0, flags and WindowManager.LayoutParams.FLAG_SECURE)

    // Setup 4-digit PIN: 4-8-2-9 -> Confirm 4-8-2-9
    listOf("4", "8", "2", "9").forEach { d ->
      composeTestRule.onNodeWithTag("pin_digit_$d").performClick()
    }
    composeTestRule.onNodeWithTag("pin_submit_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Confirm Vault PIN").assertIsDisplayed()

    listOf("4", "8", "2", "9").forEach { d ->
      composeTestRule.onNodeWithTag("pin_digit_$d").performClick()
    }
    composeTestRule.onNodeWithTag("pin_submit_button").performClick()
    composeTestRule.waitUntil(timeoutMillis = 5_000L) {
      composeTestRule.onAllNodes(hasTestTag("vault_add_media_fab")).fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Private Vault").assertIsDisplayed()

    // Landing page after unlock is Encrypted Folders (zero thumbnails/files shown)
    composeTestRule.onNodeWithTag("vault_back_to_game_button").assertIsDisplayed()
    composeTestRule.onNodeWithText("Logout").assertIsDisplayed()
    composeTestRule.onNodeWithText("Encrypted Folders").assertIsDisplayed()
    composeTestRule.onNodeWithTag("vault_folder_grid").assertIsDisplayed()
    composeTestRule.onNodeWithTag("vault_folder_photos").assertIsDisplayed()
    composeTestRule.onNodeWithTag("vault_folder_videos").assertIsDisplayed()
    composeTestRule.onNodeWithTag("vault_filter_row").assertDoesNotExist()
    composeTestRule.onNodeWithText("No Hidden Files Yet").assertDoesNotExist()

    // + FAB from Folder view opens Add Chooser
    composeTestRule.onNodeWithTag("vault_add_media_fab").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("vault_add_chooser_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("add_vault_photos_videos").assertIsDisplayed()
    composeTestRule.onNodeWithTag("add_vault_audio").assertIsDisplayed()
    composeTestRule.onNodeWithTag("add_vault_documents").assertIsDisplayed()
    composeTestRule.onNodeWithText("Cancel").performClick()
    composeTestRule.waitForIdle()

    // Drill into "All Files" folder to inspect Audio/Docs/Other filters, then navigate back to Folders
    composeTestRule.onNodeWithTag("vault_folder_grid")
      .performScrollToNode(hasTestTag("vault_folder_all"))
    composeTestRule.onNodeWithTag("vault_folder_all").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("No Hidden Files Yet").assertIsDisplayed()
    composeTestRule.onNodeWithTag("vault_filter_row")
      .performScrollToNode(hasTestTag("vault_filter_audio"))
    composeTestRule.onNodeWithTag("vault_filter_audio").assertIsDisplayed()
    composeTestRule.onNodeWithTag("vault_filter_row")
      .performScrollToNode(hasTestTag("vault_filter_document"))
    composeTestRule.onNodeWithTag("vault_filter_document").assertIsDisplayed()
    composeTestRule.onNodeWithTag("vault_filter_row")
      .performScrollToNode(hasTestTag("vault_filter_other"))
    composeTestRule.onNodeWithTag("vault_filter_other").assertIsDisplayed()
    composeTestRule.onNodeWithTag("vault_folder_back_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("vault_folder_grid").assertIsDisplayed()

    // Back-to-Game top icon returns cleanly to Fillwords game and locks vault
    composeTestRule.onNodeWithTag("vault_back_to_game_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("FILLWORDS").assertIsDisplayed()
    composeTestRule.onNodeWithTag("vault_add_media_fab").assertDoesNotExist()

    // Re-unlock with 4829, then verify minimizing / pausing immediately logs out back to Fillwords
    repeat(3) { composeTestRule.onNodeWithTag("home_hint_bulb_pill").performClick() }
    composeTestRule.waitUntil(timeoutMillis = 3_000L) {
      composeTestRule.onAllNodes(hasTestTag("pin_gate_overlay")).fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.waitForIdle()
    listOf("4", "8", "2", "9").forEach { d ->
      composeTestRule.onNodeWithTag("pin_digit_$d").performClick()
    }
    composeTestRule.onNodeWithTag("pin_submit_button").performClick()
    composeTestRule.waitUntil(timeoutMillis = 5_000L) {
      composeTestRule.onAllNodes(hasTestTag("vault_add_media_fab")).fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.waitForIdle()

    composeTestRule.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
    composeTestRule.waitForIdle()
    composeTestRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("FILLWORDS").assertIsDisplayed()
    composeTestRule.onNodeWithTag("vault_add_media_fab").assertDoesNotExist()

    // Reopen PIN gate via 3 bulb taps -> test forgot-PIN 0000 3 times in a row!
    repeat(3) { composeTestRule.onNodeWithTag("home_hint_bulb_pill").performClick() }
    composeTestRule.waitUntil(timeoutMillis = 3_000L) {
      composeTestRule.onAllNodes(hasTestTag("pin_gate_overlay")).fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Enter Vault PIN").assertIsDisplayed()

    // Attempt 1 & 2 with 0000 show Incorrect PIN
    repeat(2) {
      repeat(4) { composeTestRule.onNodeWithTag("pin_digit_0").performClick() }
      composeTestRule.onNodeWithTag("pin_submit_button").performClick()
      composeTestRule.waitUntil(timeoutMillis = 3_000L) {
        composeTestRule.onAllNodes(hasTestTag("pin_gate_overlay")).fetchSemanticsNodes().isNotEmpty() &&
          composeTestRule.onAllNodes(androidx.compose.ui.test.hasText("Incorrect PIN")).fetchSemanticsNodes().isNotEmpty()
      }
      composeTestRule.waitForIdle()
      composeTestRule.onNodeWithText("Incorrect PIN").assertIsDisplayed()
    }

    // Attempt 3 with 0000 unlocks into Recovery Mode!
    repeat(4) { composeTestRule.onNodeWithTag("pin_digit_0").performClick() }
    composeTestRule.onNodeWithTag("pin_submit_button").performClick()
    composeTestRule.waitUntil(timeoutMillis = 5_000L) {
      composeTestRule.onAllNodes(hasTestTag("vault_add_media_fab")).fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.waitForIdle()

    // Inside Security tab -> Set New Vault PIN (no old PIN required in recovery mode!)
    composeTestRule.onNodeWithTag("vault_tab_security").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("vault_security_list")
      .performScrollToNode(hasTestTag("vault_change_pin_button"))
    composeTestRule.onNodeWithTag("vault_change_pin_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("change_pin_old_input").assertDoesNotExist()
    composeTestRule.onNodeWithTag("change_pin_new_input").performTextInput("5678")
    composeTestRule.onNodeWithTag("change_pin_confirm_input").performTextInput("5678")
    composeTestRule.onNodeWithTag("change_pin_save_button").performClick()
    composeTestRule.waitForIdle()

    // Security tab also exposes "Logout & Back to Game"
    composeTestRule.onNodeWithTag("vault_security_list")
      .performScrollToNode(hasTestTag("vault_security_logout_button"))
    composeTestRule.onNodeWithTag("vault_security_logout_button").assertIsDisplayed()

    // Logout button locks and returns to Fillwords; verify new PIN 5678 works cleanly
    composeTestRule.onNodeWithTag("vault_lock_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("FILLWORDS").assertIsDisplayed()

    repeat(3) { composeTestRule.onNodeWithTag("home_hint_bulb_pill").performClick() }
    composeTestRule.waitUntil(timeoutMillis = 3_000L) {
      composeTestRule.onAllNodes(hasTestTag("pin_gate_overlay")).fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.waitForIdle()
    listOf("5", "6", "7", "8").forEach { d ->
      composeTestRule.onNodeWithTag("pin_digit_$d").performClick()
    }
    composeTestRule.onNodeWithTag("pin_submit_button").performClick()
    composeTestRule.waitUntil(timeoutMillis = 5_000L) {
      composeTestRule.onAllNodes(hasTestTag("vault_add_media_fab")).fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Private Vault").assertIsDisplayed()

    // Add a private encrypted message, then lock back to Fillwords
    composeTestRule.onNodeWithTag("vault_tab_messages").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("vault_add_message_fab").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("vault_message_contact_input").performTextInput("Safe Contact")
    composeTestRule.onNodeWithTag("vault_message_body_input").performTextInput("Top Secret 7788")
    composeTestRule.onNodeWithTag("vault_message_save_button").performClick()
    composeTestRule.waitUntil(timeoutMillis = 4_000L) {
      composeTestRule.onAllNodes(androidx.compose.ui.test.hasText("Top Secret 7788")).fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Top Secret 7788").assertIsDisplayed()

    // Check Security tab shows Emergency Self-Destruct PIN configuration card
    composeTestRule.onNodeWithTag("vault_tab_security").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("vault_security_list")
      .performScrollToNode(hasTestTag("vault_destruct_pin_card"))
    composeTestRule.onNodeWithTag("vault_destruct_pin_card").assertIsDisplayed()

    composeTestRule.onNodeWithTag("vault_lock_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("FILLWORDS").assertIsDisplayed()

    // Enter hard 6-digit Destructive Password (937184) 2 consecutive times on lock screen:
    repeat(3) { composeTestRule.onNodeWithTag("home_hint_bulb_pill").performClick() }
    composeTestRule.waitUntil(timeoutMillis = 3_000L) {
      composeTestRule.onAllNodes(hasTestTag("pin_gate_overlay")).fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.waitForIdle()

    // Attempt 1 shows "Incorrect PIN" quietly without wiping yet
    listOf("9", "3", "7", "1", "8", "4").forEach { d ->
      composeTestRule.onNodeWithTag("pin_digit_$d").performClick()
    }
    composeTestRule.onNodeWithTag("pin_submit_button").performClick()
    composeTestRule.waitUntil(timeoutMillis = 3_000L) {
      composeTestRule.onAllNodes(androidx.compose.ui.test.hasText("Incorrect PIN")).fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Incorrect PIN").assertIsDisplayed()

    // Attempt 2 wipes everything cleanly and opens an empty Vault (attacker finds nothing!)
    listOf("9", "3", "7", "1", "8", "4").forEach { d ->
      composeTestRule.onNodeWithTag("pin_digit_$d").performClick()
    }
    composeTestRule.onNodeWithTag("pin_submit_button").performClick()
    composeTestRule.waitUntil(timeoutMillis = 5_000L) {
      composeTestRule.onAllNodes(hasTestTag("vault_add_media_fab")).fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("vault_folder_grid").assertIsDisplayed()
    composeTestRule.onNodeWithTag("vault_folder_grid")
      .performScrollToNode(hasTestTag("vault_folder_all"))
    composeTestRule.onNodeWithTag("vault_folder_all").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("No Hidden Files Yet").assertIsDisplayed()

    composeTestRule.onNodeWithTag("vault_tab_messages").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("No Private Messages Yet").assertIsDisplayed()
    composeTestRule.onNodeWithText("Top Secret 7788").assertDoesNotExist()

    // Lock and reopen -> Vault config was wiped too, so Create Vault PIN starts completely fresh
    composeTestRule.onNodeWithTag("vault_lock_button").performClick()
    composeTestRule.waitForIdle()
    repeat(3) { composeTestRule.onNodeWithTag("home_hint_bulb_pill").performClick() }
    composeTestRule.waitUntil(timeoutMillis = 3_000L) {
      composeTestRule.onAllNodes(hasTestTag("pin_gate_overlay")).fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Create Vault PIN").assertIsDisplayed()
  }
}
