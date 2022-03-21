package com.kieronquinn.app.pixellaunchermods.utils.glide

import android.graphics.Bitmap
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import java.security.MessageDigest

class ScaleTransformation(private val scale: Float): BitmapTransformation() {

    override fun transform(
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {
        val cropEdgeFraction = (scale - 1f) / 2f
        val cropWidth = (cropEdgeFraction * outWidth).toInt()
        val cropHeight = (cropEdgeFraction * outHeight).toInt()
        val croppedBitmap = Bitmap.createBitmap(
            toTransform,
            cropWidth,
            cropHeight,
            toTransform.width - (cropWidth * 2),
            toTransform.height - (cropHeight * 2)
        )
        return Bitmap.createScaledBitmap(croppedBitmap, outWidth, outHeight, true).also {
            croppedBitmap.recycle()
        }
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        //No-op
    }

}