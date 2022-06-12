package com.example.bitmap2yuv

import android.graphics.*
import android.media.ImageWriter
import android.os.Bundle
import android.view.Surface
import android.view.TextureView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.scale
import androidx.lifecycle.lifecycleScope
import io.github.crow_misia.libyuv.AbgrBuffer
import io.github.crow_misia.libyuv.I420Buffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), TextureView.SurfaceTextureListener {
    private lateinit var textureView: TextureView
    private var bitmap: Bitmap? = null
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
        yuvBuffer = I420Buffer.allocate(width, height)
        //bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
        //    eraseColor(Color.BLUE)
        //}
        bitmap = BitmapFactory.decodeStream(assets.open("selfie.jpeg")).scale(width, height)
        val argbBuffer = AbgrBuffer.allocate(width, height)
        bitmap?.copyPixelsToBuffer(argbBuffer.asBuffer())
        argbBuffer.convertTo(yuvBuffer!!)
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
                        image.planes[0].buffer.put(yuvBuffer.planeY.buffer)
                        image.planes[1].buffer.put(yuvBuffer.planeU.buffer)
                        image.planes[2].buffer.put(yuvBuffer.planeV.buffer)
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
