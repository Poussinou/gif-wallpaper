/* Copyright 2020 Redwarp
 * Copyright 2020 GifWallpaper Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.redwarp.gifwallpaper.data

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.redwarp.gifwallpaper.TAG
import net.redwarp.gifwallpaper.util.FileUtils

internal class WallpaperLiveData(private val context: Context) :
    LiveData<WallpaperStatus>() {
    private var currentUri: Uri? = null
    private var localUri: Uri? = null

    init {
        postValue(WallpaperStatus.Loading)
        loadInitialValue()
    }

    fun loadNewGif(uri: Uri) {
        if (uri == currentUri) return

        CoroutineScope(Dispatchers.Main).launch {
            postValue(WallpaperStatus.Loading)
            val copiedUri =
                FileUtils.copyFileLocally(context, uri)
            if (copiedUri == null) {
                postValue(WallpaperStatus.NotSet)
            } else {
                postValue(
                    WallpaperStatus.Wallpaper(
                        copiedUri
                    )
                )
            }
            cleanupOldUri(localUri)
            currentUri = uri
            localUri = copiedUri
            storeCurrentWallpaperUri(context, copiedUri)
        }
    }

    fun clearGif() {
        postValue(WallpaperStatus.NotSet)
        cleanupOldUri(localUri)
        storeCurrentWallpaperUri(context, null)
        currentUri = null
        localUri = null
    }

    private fun loadInitialValue() {
        val uri: Uri? = loadCurrentWallpaperUri(context)
        localUri = uri
        if (uri == null) {
            postValue(WallpaperStatus.NotSet)
        } else {
            postValue(
                WallpaperStatus.Wallpaper(
                    uri
                )
            )
        }
    }

    private fun loadCurrentWallpaperUri(context: Context): Uri? {
        val sharedPreferences =
            context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
        val urlString = sharedPreferences.getString(KEY_WALLPAPER_URI, null)
        return urlString?.let { Uri.parse(it) }
    }

    private fun storeCurrentWallpaperUri(context: Context, uri: Uri?) {
        val sharedPreferences =
            context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
        if (uri != null) {
            sharedPreferences.edit().putString(KEY_WALLPAPER_URI, uri.toString()).apply()
        } else {
            sharedPreferences.edit().remove(KEY_WALLPAPER_URI).apply()
        }
    }

    private fun cleanupOldUri(uri: Uri?) {
        val path = uri?.path ?: return
        val file = File(path)
        if (file.exists()) {
            file.delete().also { Log.d(TAG, "File deleted: $it") }
        }
    }
}

sealed class WallpaperStatus {
    object NotSet : WallpaperStatus()
    object Loading : WallpaperStatus()
    data class Wallpaper(val uri: Uri) : WallpaperStatus()
}
