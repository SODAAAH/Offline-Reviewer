package com.studentreviewer.offline.ui.main

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import com.studentreviewer.offline.MainActivity

class MainScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun testActivityLaunches() {
    composeTestRule.waitForIdle()
  }
}
