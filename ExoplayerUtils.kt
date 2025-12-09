package com.sadip.player.exo_utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.DefaultRenderersFactory.ExtensionRendererMode
import com.google.android.exoplayer2.ExoPlayerLibraryInfo
import com.google.android.exoplayer2.RenderersFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.upstream.HttpDataSource

class ExoplayerUtils {
    companion object{

        /** Returns a [HttpDataSource.Factory].  */
        fun buildHttpDataSourceFactory(context: Context): HttpDataSource.Factory? {
            return DefaultHttpDataSourceFactory(getUserAgent(context, "MyApplication"))
        }

        fun buildRenderersFactory(context: Context, preferExtensionRenderer: Boolean): RenderersFactory? {
            @ExtensionRendererMode val extensionRendererMode =
                DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
            return DefaultRenderersFactory( /* context= */context)
                .setExtensionRendererMode(extensionRendererMode)
        }


        fun getUserAgent(
            context: Context,
            applicationName: String
        ): String? {
            val versionName: String
            versionName = try {
                val packageName = context.packageName
                val info =
                    context.packageManager.getPackageInfo(packageName, 0)
                info.versionName
            } catch (e: PackageManager.NameNotFoundException) {
                "?"
            }
            return (applicationName + "/" + versionName + " (Linux;Android " + Build.VERSION.RELEASE
                    + ") " + ExoPlayerLibraryInfo.VERSION_SLASHY)
        }
    }
}
