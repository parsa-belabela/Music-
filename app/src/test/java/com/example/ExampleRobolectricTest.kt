package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Aura Music", appName)
  }

  @Test
  fun `lrc parser parses lines correctly`() {
    val lrc = "[00:10.50]Hello Aura Music\n[00:20.00]Second Line"
    val lines = com.example.lyrics.LrcParser.parse(lrc)
    assertEquals(2, lines.size)
    assertEquals(10500L, lines[0].timestampMs)
    assertEquals("Hello Aura Music", lines[0].text)
  }
}
