package com.example.cameraxdemo.glsurfaceview

import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.camera.core.Preview
import java.io.Serializable
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


class CameraRender(cameraView: GLCameraView) : GLSurfaceView.Renderer,
    Serializable,
    SurfaceTexture.OnFrameAvailableListener{
    private var mCameraView: GLCameraView = cameraView

    private lateinit var mSurfaceTexture: SurfaceTexture
    private lateinit var textures: IntArray
    private lateinit var screenFilter: ScreenFilter
    private var mtx = FloatArray(16)
    lateinit var surface:Surface
    var preview: Preview = Preview.Builder().build()

    lateinit var onPreviewSurfaceView: OnPreviewSurfaceView
    lateinit var previewSize: Size

    //判断surface是否已初始化
    fun isSurfaceInit() = ::surface.isInitialized
    fun isPreviewSizeInit() = this::previewSize.isInitialized


    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.i("cx---","onSurfaceCreated")
        textures = IntArray(1)
//        GLES20.glGenTextures(1, textures, 0)
//        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,textures[0])
        val textureid = getExternalOESTextureID(textures)

        mSurfaceTexture= SurfaceTexture(textureid)
        mSurfaceTexture.setOnFrameAvailableListener(this)
        //设置之后解决预览模糊问题
        mSurfaceTexture.setDefaultBufferSize(1440,1080)

        surface = Surface(mSurfaceTexture)
        //通知surfaceview创建完成，可以进行获取surface操作
        onPreviewSurfaceView.onSurfaceCreated()

        screenFilter = ScreenFilter(mCameraView.context)


    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.i("cx---","onSurfaceChanged")
        screenFilter.setSize(width, height)
    }

    override fun onDrawFrame(gl: GL10?) {

        mSurfaceTexture.updateTexImage()
        mSurfaceTexture.getTransformMatrix(mtx)
        screenFilter.setTransformMatrix(mtx);
        if(isPreviewSizeInit()){
            screenFilter.onDraw(textures[0],previewSize.width,previewSize.height);
        }
    }

    fun onSurfaceDestroyed() {
        Log.i("cx---","onSurfaceDestroyed")

    }


    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
//        if (surfaceTexture != null) {
//            mSurfaceTexture = surfaceTexture
//        };
        mCameraView.requestRender();
    }


    fun getExternalOESTextureID(texture:IntArray): Int {
        GLES20.glGenTextures(1, texture, 0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0])
        GLES20.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR.toFloat()
        )
        GLES20.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR.toFloat()
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE
        )
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
        return texture[0]
    }



}