package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object AvatarStorage {
    private const val PREFS_NAME = "shaddaha_avatar_prefs"
    private const val KEY_PREFIX = "avatar_mode_"
    const val MODE_CARTOON = "cartoon"
    const val MODE_GOOGLE = "google"
    const val PRESET_PREFIX = "preset:"

    val presetOptions: List<Pair<String, Int>> = listOf(
        "${PRESET_PREFIX}1" to R.drawable.avatar_preset_1,
        "${PRESET_PREFIX}2" to R.drawable.avatar_preset_2,
        "${PRESET_PREFIX}3" to R.drawable.avatar_preset_3,
        "${PRESET_PREFIX}4" to R.drawable.avatar_preset_4,
        "${PRESET_PREFIX}5" to R.drawable.avatar_preset_5,
        "${PRESET_PREFIX}6" to R.drawable.avatar_preset_6,
    )

    fun presetDrawableRes(avatarUrl: String?): Int? {
        if (avatarUrl == null || !avatarUrl.startsWith(PRESET_PREFIX)) return null
        return presetOptions.firstOrNull { it.first == avatarUrl }?.second
    }

    fun resolveAvatarUrl(
        context: Context,
        userId: String,
        googlePhotoUrl: String?,
        remoteAvatarUrl: String?,
    ): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return when (val saved = prefs.getString(KEY_PREFIX + userId, null)) {
            null, MODE_CARTOON -> null
            MODE_GOOGLE -> googlePhotoUrl ?: remoteAvatarUrl
            else -> when {
                saved.startsWith(PRESET_PREFIX) -> if (presetDrawableRes(saved) != null) saved else null
                else -> {
                    val cleanPath = saved.substringBefore("?").removePrefix("file://")
                    if (File(cleanPath).exists()) saved else null
                }
            }
        }
    }

    fun setCartoonAvatar(context: Context, userId: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PREFIX + userId, MODE_CARTOON)
            .apply()
    }

    fun setGoogleAvatar(context: Context, userId: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PREFIX + userId, MODE_GOOGLE)
            .apply()
    }

    fun setPresetAvatar(context: Context, userId: String, presetKey: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PREFIX + userId, presetKey)
            .apply()
    }

    suspend fun savePickedImage(
        context: Context,
        userId: String,
        sourceUri: Uri,
    ): String? = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.filesDir, "avatars").apply { mkdirs() }
            val destFile = File(dir, "avatar_${userId.hashCode()}.jpg")

            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOpts)
            }

            val maxDim = maxOf(boundsOpts.outWidth, boundsOpts.outHeight).coerceAtLeast(1)
            var sampleSize = 1
            while (maxDim / sampleSize > 512) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val bitmap = context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: return@withContext null

            val scaled = if (maxOf(bitmap.width, bitmap.height) > 512) {
                val ratio = 512f / maxOf(bitmap.width, bitmap.height).toFloat()
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * ratio).toInt().coerceAtLeast(1),
                    (bitmap.height * ratio).toInt().coerceAtLeast(1),
                    true,
                ).also {
                    if (it !== bitmap) bitmap.recycle()
                }
            } else {
                bitmap
            }

            FileOutputStream(destFile).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            scaled.recycle()

            val fileUri = "file://${destFile.absolutePath}?t=${System.currentTimeMillis()}"
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_PREFIX + userId, fileUri)
                .apply()
            fileUri
        }.getOrNull()
    }
}
