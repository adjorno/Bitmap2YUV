package com.example.bitmap2yuv

import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.media.ImageWriter
import android.os.Bundle
import android.view.Surface
import android.view.TextureView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.scale
import androidx.lifecycle.lifecycleScope
import io.github.crow_misia.libyuv.AbgrBuffer
import io.github.crow_misia.libyuv.I420Buffer
import io.github.crow_misia.libyuv.ext.ImageExt.toI420Buffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), TextureView.SurfaceTextureListener {
    private lateinit var textureView: TextureView
    private var imageWriter: ImageWriter? = null
    private var yuvBuffer: I420Buffer? = null
    private var running = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        textureView = TextureView(this)
        textureView.surfaceTextureListener = this
        setContentView(textureView)
    }

    private fun createImageWriter(surface: Surface, width: Int, height: Int) {
        yuvBuffer = I420Buffer.allocate(width, height).apply {
            val abgrBuffer = AbgrBuffer.allocate(width, height).apply {
                BitmapFactory.decodeStream(assets.open("selfie.jpeg")).scale(width, height).copyPixelsToBuffer(asBuffer())
            }
            abgrBuffer.convertTo(this)
        }
        imageWriter = ImageWriter.newInstance(surface, 1, ImageFormat.YUV_420_888)
        startRendering()
    }

    private fun startRendering() {
        imageWriter?.let { imageWriter ->
            yuvBuffer?.let { yuvBuffer ->
                running = true
                lifecycleScope.launch(Dispatchers.IO) {
                    while (running) {
                        delay(1000)
                        val image = imageWriter.dequeueInputImage()
                        yuvBuffer.convertTo(image.toI420Buffer())
                        imageWriter.queueInputImage(image)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        startRendering()
    }

    override fun onStop() {
        super.onStop()
        running = false
    }

    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
        createImageWriter(Surface(surfaceTexture), width, height)
    }

    override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {
    }

    override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
        return true
    }

    override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {
    }
}
