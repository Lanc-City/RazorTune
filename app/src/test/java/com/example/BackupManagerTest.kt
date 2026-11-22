package com.example

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.BackupManager
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class BackupManagerTest {

    private lateinit var context: Context
    private val dbName = "test_razortune_db"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val dbFile = context.getDatabasePath(dbName)
        if (dbFile.exists()) {
            dbFile.delete()
        }
        val walFile = File(dbFile.parentFile, "$dbName-wal")
        if (walFile.exists()) walFile.delete()
        val shmFile = File(dbFile.parentFile, "$dbName-shm")
        if (shmFile.exists()) shmFile.delete()
    }

    @Test
    fun `formatBytes returns expected formatted string`() {
        assertEquals("0 B", BackupManager.formatBytes(0))
        assertEquals("500 B", BackupManager.formatBytes(500))
        assertEquals("1.0 KB", BackupManager.formatBytes(1024))
        assertEquals("1.5 MB", BackupManager.formatBytes(1572864))
    }

    @Test
    fun `exportDatabase succeeds when database file exists`() = runTest {
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()
        val db = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        db.execSQL("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT);")
        db.execSQL("INSERT INTO test VALUES (1, 'RazorTune');")
        db.close()

        val expectedSize = dbFile.length()
        val destFile = File(context.cacheDir, "exported_backup.zip")
        destFile.parentFile?.mkdirs()
        val destUri = Uri.fromFile(destFile)

        val result = BackupManager.exportDatabase(context, destUri, dbName)

        assertTrue(result.isSuccess)
        val opResult = result.getOrNull()
        assertEquals(expectedSize, opResult?.bytesCopied)

        var extractedDbContent: ByteArray? = null
        ZipInputStream(destFile.inputStream()).use { zipIn ->
            var entry = zipIn.nextEntry
            while (entry != null) {
                if (entry.name == "database/$dbName") {
                    extractedDbContent = zipIn.readBytes()
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }
        val contentBytes = extractedDbContent
        assertTrue(contentBytes != null && contentBytes.isNotEmpty())
    }

    @Test
    fun `exportDatabase fails when database file does not exist`() = runTest {
        val destFile = File(context.cacheDir, "non_existent_export.zip")
        val destUri = Uri.fromFile(destFile)

        val result = BackupManager.exportDatabase(context, destUri, "missing_db")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun `importDatabase restores database and cleans sidecar WAL and SHM files`() = runTest {
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()
        dbFile.writeText("Old DB Content")

        val walFile = File(dbFile.parentFile, "$dbName-wal")
        val shmFile = File(dbFile.parentFile, "$dbName-shm")
        walFile.writeText("Old WAL data")
        shmFile.writeText("Old SHM data")

        assertTrue(walFile.exists())
        assertTrue(shmFile.exists())

        val sourceBackupZip = File(context.cacheDir, "source_backup.zip")
        val newContent = "Restored SQLite DB Content"

        ZipOutputStream(sourceBackupZip.outputStream()).use { zipOut ->
            zipOut.putNextEntry(ZipEntry("database/$dbName"))
            zipOut.write(newContent.toByteArray())
            zipOut.closeEntry()
            zipOut.finish()
        }
        val sourceUri = Uri.fromFile(sourceBackupZip)

        val result = BackupManager.importDatabase(context, sourceUri, dbName)

        assertTrue(result.isSuccess)
        assertEquals(newContent, dbFile.readText())
        assertFalse("WAL file should be cleaned up after restore", walFile.exists())
        assertFalse("SHM file should be cleaned up after restore", shmFile.exists())
    }

    @Test
    fun `databaseExists and getDatabaseSizeBytes behave correctly`() {
        assertFalse(BackupManager.databaseExists(context, dbName))
        assertEquals(0L, BackupManager.getDatabaseSizeBytes(context, dbName))

        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()
        dbFile.writeText("Hello Database")

        assertTrue(BackupManager.databaseExists(context, dbName))
        assertEquals("Hello Database".length.toLong(), BackupManager.getDatabaseSizeBytes(context, dbName))
    }
}
