package com.example.cameraxdemo

import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.util.Log
import androidx.camera.core.ImageProxy
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer

class Utils {

    companion object {
        private var TAG= "Utils"


        fun compressToJpeg( data: ByteArray, fileName: String, image: ImageProxy) {
            val outStream: FileOutputStream
            try {
                outStream = FileOutputStream(fileName)
            } catch (ioe: IOException) {
                throw RuntimeException("Unable to create output file $fileName", ioe)
            }
            val rect: Rect = image.cropRect
            val yuvImage = YuvImage(
                data,
                ImageFormat.NV21,
                rect.width(),
                rect.height(),
                null
            )
            yuvImage.compressToJpeg(rect, 100, outStream)
        }


        private const val COLOR_FormatI420 = 1
        private const val COLOR_FormatNV21 = 2

        private fun isImageFormatSupported(image: ImageProxy): Boolean {
            when (image.format) {
                ImageFormat.YUV_420_888, ImageFormat.NV21, ImageFormat.YV12 -> return true
            }
            return false
        }

        private fun getDataFromImage(fileName: String,image: ImageProxy, colorFormat: Int): ByteArray? {
            require(!(colorFormat != COLOR_FormatI420 && colorFormat != COLOR_FormatNV21)) { "only support COLOR_FormatI420 " + "and COLOR_FormatNV21" }
            if (!isImageFormatSupported(image)) {
                throw java.lang.RuntimeException("can't convert Image to byte array, format " + image.format)
            }
            val crop = image.cropRect
            val format = image.format
            val width = crop.width()
            val height = crop.height()
            val planes = image.planes
            val data = ByteArray(width * height * ImageFormat.getBitsPerPixel(format) / 8)
            val rowData = ByteArray(planes[0].rowStride)
            Log.v(TAG, "image.format  $format ")
            Log.v(TAG, "get data from " + planes.size + " planes")
            var channelOffset = 0
            var outputStride = 1
            for (i in planes.indices) {
                when (i) {
                    0 -> {
                        channelOffset = 0
                        outputStride = 1
                    }
                    1 -> if (colorFormat == COLOR_FormatI420) {
                        channelOffset = width * height
                        outputStride = 1
                    } else if (colorFormat == COLOR_FormatNV21) {
                        channelOffset = width * height + 1
                        outputStride = 2
                    }
                    2 -> if (colorFormat == COLOR_FormatI420) {
                        channelOffset = (width * height * 1.25).toInt()
                        outputStride = 1
                    } else if (colorFormat == COLOR_FormatNV21) {
                        channelOffset = width * height
                        outputStride = 2
                    }
                }
                val buffer: ByteBuffer = planes[i].buffer
                val rowStride = planes[i].rowStride
                val pixelStride = planes[i].pixelStride
                Log.v(TAG, "pixelStride $pixelStride")
                Log.v(TAG, "rowStride $rowStride")
                Log.v(TAG, "width $width")
                Log.v(TAG, "height $height")
                Log.v(TAG, "buffer size " + buffer.remaining())
                val shift = if (i == 0) 0 else 1
                val w = width shr shift
                val h = height shr shift
                buffer.position(rowStride * (crop.top shr shift) + pixelStride * (crop.left shr shift))
                for (row in 0 until h) {
                    var length: Int
                    if (pixelStride == 1 && outputStride == 1) {
                        length = w
                        buffer.get(data, channelOffset, length)
                        channelOffset += length
                    } else {
                        length = (w - 1) * pixelStride + 1
                        buffer.get(rowData, 0, length)
                        for (col in 0 until w) {
                            data[channelOffset] = rowData[col * pixelStride]
                            channelOffset += outputStride
                        }
                    }
                    if (row < h - 1) {
                        buffer.position(buffer.position() + rowStride - length)
                    }
                }
                Log.v(TAG, "Finished reading data from plane $i")
            }
//            dumpFile("$fileName.yuv",data)
            return data
        }

        public fun dumpFile(fileName: String, data: ByteArray) {
            val outStream: FileOutputStream = try {
                FileOutputStream(fileName)
            } catch (ioe: IOException) {
                throw java.lang.RuntimeException("Unable to create output file $fileName", ioe)
            }
            try {
                outStream.write(data)
                outStream.close()
            } catch (ioe: IOException) {
                throw java.lang.RuntimeException("failed writing data to file $fileName", ioe)
            }
        }

        fun compressToJpeg(fileName: String, image: ImageProxy) {
            val outStream: FileOutputStream = try {
                FileOutputStream("$fileName.jpg")
            } catch (ioe: IOException) {
                throw java.lang.RuntimeException("Unable to create output file $fileName", ioe)
            }
            val rect = image.cropRect
            val yuvImage = YuvImage(
                getDataFromImage(fileName,image, COLOR_FormatNV21),
                ImageFormat.NV21,
                rect.width(),
                rect.height(),
                null
            )
            yuvImage.compressToJpeg(rect, 100, outStream)
        }



        fun LOGI( msg: String){
            Log.i("cxlogI",msg)
        }

        fun LOGE( msg: String){
            Log.e("cxlogE",msg)
        }


    }




}