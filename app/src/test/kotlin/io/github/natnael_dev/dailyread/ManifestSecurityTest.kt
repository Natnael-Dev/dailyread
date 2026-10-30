package io.github.natnael_dev.dailyread

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ManifestSecurityTest {

    @Test
    fun `verify manifest strictly forbids INTERNET permission`() {
        val manifestFile = File("src/main/AndroidManifest.xml")
        assertTrue("AndroidManifest.xml must exist", manifestFile.exists())

        val content = manifestFile.readText()

        assertFalse(
            "MVP strictly forbids INTERNET permission in AndroidManifest.xml (DEC-029 / AGENTS §6)",
            content.contains("android.permission.INTERNET")
        )
    }

    @Test
    fun `verify allowBackup is false`() {
        val manifestFile = File("src/main/AndroidManifest.xml")
        assertTrue("AndroidManifest.xml must exist", manifestFile.exists())

        val content = manifestFile.readText()

        assertTrue(
            "allowBackup must be false for offline-only security (DEC-029)",
            content.contains("android:allowBackup=\"false\"")
        )
    }
}
