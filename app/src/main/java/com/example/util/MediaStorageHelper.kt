package com.example.util

import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

data class ImportedAudioMeta(
    val title: String,
    val artist: String,
    val localFilePath: String,
    val mimeType: String,
    val durationSeconds: Int
)

object MediaStorageHelper {

    fun importProfilePicture(
        context: Context,
        uri: Uri,
        previousPath: String = ""
    ): String? {
        val savedPath = importScaledImage(
            context = context,
            uri = uri,
            subfolder = "profile_media",
            filePrefix = "avatar",
            maxDimension = 800,
            jpegQuality = 88
        )
        if (savedPath != null && previousPath.isNotBlank() && previousPath != savedPath) {
            deleteLocalMediaFile(context, previousPath)
        }
        return savedPath
    }

    fun importTimetablePicture(
        context: Context,
        uri: Uri,
        previousPath: String = ""
    ): String? {
        val savedPath = importScaledImage(
            context = context,
            uri = uri,
            subfolder = "timetable_media",
            filePrefix = "timetable",
            maxDimension = 2200,
            jpegQuality = 92
        )
        if (savedPath != null && previousPath.isNotBlank() && previousPath != savedPath) {
            deleteLocalMediaFile(context, previousPath)
        }
        return savedPath
    }

    private fun importScaledImage(
        context: Context,
        uri: Uri,
        subfolder: String,
        filePrefix: String,
        maxDimension: Int,
        jpegQuality: Int
    ): String? {
        return try {
            val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return null
            if (rawBytes.isEmpty()) return null

            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, boundsOptions)

            var sampleSize = 1
            while (
                (boundsOptions.outWidth / sampleSize) > maxDimension * 2 ||
                (boundsOptions.outHeight / sampleSize) > maxDimension * 2
            ) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOptions)
                ?: return null

            val finalBitmap = scaleDownIfNeeded(decoded, maxDimension)
            val dir = File(context.filesDir, subfolder).apply { mkdirs() }
            val outFile = File(dir, "${filePrefix}_${System.currentTimeMillis()}.jpg")

            FileOutputStream(outFile).use { fos ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, fos)
                fos.flush()
            }
            if (finalBitmap !== decoded) {
                decoded.recycle()
            }
            outFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap

        val ratio = width.toFloat() / height.toFloat()
        val targetW: Int
        val targetH: Int
        if (width >= height) {
            targetW = maxDimension
            targetH = (maxDimension / ratio).toInt().coerceAtLeast(1)
        } else {
            targetH = maxDimension
            targetW = (maxDimension * ratio).toInt().coerceAtLeast(1)
        }
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }

    fun importAudioFile(
        context: Context,
        uri: Uri,
        subfolder: String = "focus_music",
        fallbackPrefix: String = "Study Track"
    ): ImportedAudioMeta? {
        return try {
            val resolver = context.contentResolver
            val mimeType = resolver.getType(uri) ?: "audio/mpeg"
            val displayName = resolveDisplayName(context, uri)
            val extension = displayName
                ?.substringAfterLast('.', "")
                ?.lowercase()
                ?.takeIf { it in setOf("mp3", "m4a", "wav", "ogg", "aac", "flac", "opus") }
                ?: when {
                    mimeType.contains("wav") -> "wav"
                    mimeType.contains("ogg") -> "ogg"
                    mimeType.contains("mp4") || mimeType.contains("m4a") -> "m4a"
                    else -> "mp3"
                }

            val dir = File(context.filesDir, subfolder).apply { mkdirs() }
            val outFile = File(dir, "audio_${System.currentTimeMillis()}.$extension")

            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            if (!outFile.exists() || outFile.length() == 0L) {
                outFile.delete()
                return null
            }

            var extractedTitle: String? = null
            var extractedArtist: String? = null
            var durationSec = 0

            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(outFile.absolutePath)
                extractedTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                extractedArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                val durMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                durationSec = (durMs / 1000L).toInt().coerceAtLeast(0)
            } catch (_: Exception) {
            } finally {
                try {
                    retriever.release()
                } catch (_: Exception) {
                }
            }

            val cleanFallbackTitle = displayName
                ?.substringBeforeLast('.')
                ?.replace('_', ' ')
                ?.replace('-', ' ')
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: fallbackPrefix

            ImportedAudioMeta(
                title = extractedTitle?.trim()?.takeIf { it.isNotBlank() } ?: cleanFallbackTitle,
                artist = extractedArtist?.trim()?.takeIf { it.isNotBlank() } ?: "Imported Audio",
                localFilePath = outFile.absolutePath,
                mimeType = mimeType,
                durationSeconds = durationSec
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveDisplayName(context: Context, uri: Uri): String? {
        return try {
            var name: String? = null
            val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        name = it.getString(idx)
                    }
                }
            }
            name ?: uri.lastPathSegment
        } catch (_: Exception) {
            uri.lastPathSegment
        }
    }

    fun loadBitmapSafe(path: String, maxDimension: Int = 1600): Bitmap? {
        if (path.isBlank()) return null
        return try {
            val file = File(path)
            if (!file.exists() || file.length() == 0L) return null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            var sampleSize = 1
            while (
                (bounds.outWidth / sampleSize) > maxDimension * 2 ||
                (bounds.outHeight / sampleSize) > maxDimension * 2
            ) {
                sampleSize *= 2
            }
            val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            BitmapFactory.decodeFile(file.absolutePath, opts)
        } catch (_: Exception) {
            null
        }
    }

    fun deleteLocalMediaFile(context: Context, path: String): Boolean {
        if (path.isBlank()) return false
        return try {
            val file = File(path)
            val rootDir = context.filesDir.canonicalPath
            if (file.exists() && file.canonicalPath.startsWith(rootDir)) {
                file.delete()
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun createSyntheticSampleBitmapBytes(width: Int = 64, height: Int = 64, colorArgb: Int = 0xFF0A84FF.toInt()): ByteArray {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(colorArgb)
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
        bmp.recycle()
        return out.toByteArray()
    }
}
