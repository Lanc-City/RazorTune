package com.example.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Utility object for backing up and restoring Room / SQLite databases
 * alongside offline media files using Android's Storage Access Framework (SAF).
 */
object BackupManager {

    const val DEFAULT_DB_NAME = "razortune_db"

    data class BackupOperationResult(
        val bytesCopied: Long,
        val archiveName: String,
        val uri: Uri
    )

    private fun getFileFromUri(uri: Uri): File? {
        if (uri.scheme == "file") {
            val rawPath = uri.path ?: uri.schemeSpecificPart ?: return null
            val cleanPath = if (rawPath.startsWith("/") && rawPath.length > 2 && rawPath[2] == ':') {
                rawPath.substring(1)
            } else {
                rawPath
            }
            return File(cleanPath)
        }
        return null
    }

    private fun isSQLiteDatabase(file: File): Boolean {
        if (!file.exists() || file.length() < 16L) return false
        return runCatching {
            file.inputStream().use { input ->
                val header = ByteArray(16)
                if (input.read(header) == 16) {
                    String(header, Charsets.US_ASCII).startsWith("SQLite format 3")
                } else false
            }
        }.getOrDefault(false)
    }

    /**
     * Exports a full backup ZIP containing the SQLite database and the `offline_audio` folder.
     * Ensures SQLite WAL checkpointing is performed before zipping to guarantee database integrity.
     */
    suspend fun exportDatabase(
        context: Context,
        destinationUri: Uri,
        dbName: String = DEFAULT_DB_NAME
    ): Result<BackupOperationResult> = withContext(Dispatchers.IO) {
        runCatching {
            val dbFile = context.getDatabasePath(dbName)
            if (!dbFile.exists()) {
                throw IllegalStateException("Local database '$dbName' does not exist.")
            }

            // Force SQLite WAL checkpoint to flush pending transactions into the main .db file
            if (isSQLiteDatabase(dbFile)) {
                runCatching {
                    SQLiteDatabase.openDatabase(
                        dbFile.absolutePath,
                        null,
                        SQLiteDatabase.OPEN_READWRITE
                    ).use { db ->
                        db.execSQL("PRAGMA wal_checkpoint(FULL);")
                    }
                }
            }

            val offlineDir = File(context.filesDir, "offline_audio")
            val contentResolver = context.contentResolver

            val fileFromUri = getFileFromUri(destinationUri)
            fileFromUri?.parentFile?.mkdirs()

            val outputStream: OutputStream = if (fileFromUri != null) {
                fileFromUri.outputStream()
            } else {
                contentResolver.openOutputStream(destinationUri, "w")
            } ?: throw IllegalStateException("Failed to open output stream for URI: $destinationUri")

            var totalBytes = 0L

            ZipOutputStream(outputStream).use { zipOut ->
                // 1. Add main database file
                val dbEntry = ZipEntry("database/$dbName")
                zipOut.putNextEntry(dbEntry)
                dbFile.inputStream().use { input ->
                    totalBytes += input.copyTo(zipOut)
                }
                zipOut.closeEntry()

                // 2. Add offline audio files (.m4a, .json, .art, etc.)
                if (offlineDir.exists() && offlineDir.isDirectory) {
                    offlineDir.listFiles()?.forEach { file ->
                        if (file.isFile && file.length() > 0L) {
                            val audioEntry = ZipEntry("offline_audio/${file.name}")
                            zipOut.putNextEntry(audioEntry)
                            file.inputStream().use { input ->
                                totalBytes += input.copyTo(zipOut)
                            }
                            zipOut.closeEntry()
                        }
                    }
                }
                zipOut.finish()
            }

            BackupOperationResult(
                bytesCopied = totalBytes,
                archiveName = destinationUri.lastPathSegment ?: "backup.zip",
                uri = destinationUri
            )
        }
    }

    /**
     * Restores the SQLite database and extracts offline audio files from a backup ZIP.
     */
    suspend fun importDatabase(
        context: Context,
        sourceUri: Uri,
        dbName: String = DEFAULT_DB_NAME
    ): Result<BackupOperationResult> = withContext(Dispatchers.IO) {
        runCatching {
            val dbFile = context.getDatabasePath(dbName)
            dbFile.parentFile?.let { if (!it.exists()) it.mkdirs() }

            // Release active database connections before replacement
            if (dbFile.exists() && isSQLiteDatabase(dbFile)) {
                runCatching {
                    SQLiteDatabase.openDatabase(
                        dbFile.absolutePath,
                        null,
                        SQLiteDatabase.OPEN_READWRITE
                    ).use { it.close() }
                }
            }
            cleanSidecarFiles(dbFile)

            val offlineDir = File(context.filesDir, "offline_audio").apply { mkdirs() }
            val contentResolver = context.contentResolver

            val fileFromUri = getFileFromUri(sourceUri)
            val inputStream: InputStream = if (fileFromUri != null) {
                fileFromUri.inputStream()
            } else {
                contentResolver.openInputStream(sourceUri)
            } ?: throw IllegalStateException("Failed to open input stream for URI: $sourceUri")

            var bytesCopied = 0L

            ZipInputStream(inputStream).use { zipIn ->
                var entry = zipIn.nextEntry
                while (entry != null) {
                    val name = entry.name
                    if (name == "database/$dbName" || name.startsWith("database/")) {
                        FileOutputStream(dbFile).use { out ->
                            bytesCopied += zipIn.copyTo(out)
                        }
                    } else if (name.startsWith("offline_audio/")) {
                        val fileName = name.substringAfter("offline_audio/")
                        if (fileName.isNotBlank()) {
                            val targetFile = File(offlineDir, fileName)
                            FileOutputStream(targetFile).use { out ->
                                bytesCopied += zipIn.copyTo(out)
                            }
                        }
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }

            if (bytesCopied <= 0L) {
                // Fallback for raw legacy database files if archive was not zipped
                val rawInputStream = if (fileFromUri != null) {
                    fileFromUri.inputStream()
                } else {
                    contentResolver.openInputStream(sourceUri)
                }
                if (rawInputStream != null) {
                    rawInputStream.use { input ->
                        FileOutputStream(dbFile).use { out ->
                            bytesCopied = input.copyTo(out)
                        }
                    }
                }
            }

            if (bytesCopied <= 0L) {
                throw IllegalStateException("Restored backup archive was empty or invalid.")
            }

            BackupOperationResult(
                bytesCopied = bytesCopied,
                archiveName = sourceUri.lastPathSegment ?: "backup.zip",
                uri = sourceUri
            )
        }
    }

    fun databaseExists(context: Context, dbName: String = DEFAULT_DB_NAME): Boolean {
        return context.getDatabasePath(dbName).exists()
    }

    fun getDatabaseSizeBytes(context: Context, dbName: String = DEFAULT_DB_NAME): Long {
        val file = context.getDatabasePath(dbName)
        return if (file.exists()) file.length() else 0L
    }

    private fun cleanSidecarFiles(dbFile: File) {
        val parent = dbFile.parentFile ?: return
        val dbName = dbFile.name
        listOf("$dbName-wal", "$dbName-shm", "$dbName-journal").forEach { sidecar ->
            val file = File(parent, sidecar)
            if (file.exists()) file.delete()
        }
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val index = digitGroups.coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, index.toDouble())
        return if (index == 0) "$bytes B" else String.format(java.util.Locale.US, "%.1f %s", value, units[index])
    }
}