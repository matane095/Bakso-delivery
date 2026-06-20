package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.BaksoMenu
import com.example.ui.FoodCardItem
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val sampleMenu = BaksoMenu(
      id = 1,
      name = "Bakso Urat Granat",
      description = "Bakso sapi urat ukuran jumbo yang kenyal dan gurih, diisi potongan sambal cabai rawit pedas meledak.",
      price = 28000.0,
      rating = 4.8f,
      category = "Spesial Pedas",
      emoji = "🌶️"
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        FoodCardItem(
          menu = sampleMenu,
          onAddToCart = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
