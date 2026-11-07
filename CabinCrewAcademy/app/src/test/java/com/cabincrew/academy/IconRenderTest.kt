package com.cabincrew.academy

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Renders the adaptive launcher icon to build/screenshots/launcher_icon.png for visual review. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class IconRenderTest {
    @Test
    fun renderIcon() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val drawable = ctx.getDrawable(R.mipmap.ic_launcher)
        assertNotNull(drawable)
        val bmp = Bitmap.createBitmap(432, 432, Bitmap.Config.ARGB_8888)
        drawable!!.setBounds(0, 0, 432, 432)
        drawable.draw(Canvas(bmp))
        File("build/screenshots").mkdirs()
        File("build/screenshots/launcher_icon.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
