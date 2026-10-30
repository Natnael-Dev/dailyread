package io.github.natnael_dev.dailyread

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.natnael_dev.dailyread.data.db.DailyReadDatabase
import io.github.natnael_dev.dailyread.data.db.entity.LibrarySourceEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SafRebootPersistenceTest {

    @Test
    fun testPersistedUriPermissionsQueryable() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val persisted = context.contentResolver.persistedUriPermissions
        assertNotNull(persisted)
    }

    @Test
    fun testRegisterAndVerifyLibrarySource() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = DailyReadDatabase.getInstance(context)
        val sourceDao = db.librarySourceDao()

        val testUri = "content://com.android.externalstorage.documents/tree/primary%3ATestLibrary"
        val existing = sourceDao.getSourceByTreeUri(testUri)
        if (existing == null) {
            sourceDao.insertSource(
                LibrarySourceEntity(
                    treeUri = testUri,
                    displayName = "TestLibrary",
                    addedAt = System.currentTimeMillis(),
                    state = "OK"
                )
            )
        }

        val registered = sourceDao.getSourceByTreeUri(testUri)
        assertNotNull(registered)
        assertTrue(registered?.state == "OK")
    }
}
