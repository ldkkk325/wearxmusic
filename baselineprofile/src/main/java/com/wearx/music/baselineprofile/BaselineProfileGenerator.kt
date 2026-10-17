package com.wearx.music.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        rule.collect(packageName = "com.wearx.music") {
            pressHome()
            startActivityAndWait()

            // First launch may land on the onboarding agreement; a fresh install of a release
            // build always shows SOMETHING the profile should still warm up.
            device.wait(Until.hasObject(By.pkg("com.wearx.music").depth(0)), 5_000)

            // Fling whatever vertical list is on screen — the library's TransformingLazyColumn
            // (and its surface-scaling transformation) is the main scroll cost this profile
            // exists to pre-compile. On the onboarding page this flings the agreement text,
            // which is harmless.
            device.findObject(By.scrollable(true))?.let { list ->
                list.setGestureMargin(device.displayWidth / 5)
                repeat(3) {
                    list.fling(Direction.DOWN)
                    device.waitForIdle()
                    list.fling(Direction.UP)
                    device.waitForIdle()
                }
            }
        }
    }
}
