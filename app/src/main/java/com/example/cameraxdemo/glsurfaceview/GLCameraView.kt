package com.example.cameraxdemo.glsurfaceview

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder


//放到xml里面用来显示预览
class GLCameraView(context: Context?, attrs: AttributeSet?) :
    GLSurfaceView(context, attrs) {
    private var TAG: String = "CameraView"
    val renderer: CameraRender

    constructor(context: Context?) : this(context, null) {}

    override fun surfaceCreated(holder: SurfaceHolder) {
        super.surfaceCreated(holder)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        super.surfaceDestroyed(holder)
        renderer.onSurfaceDestroyed()
    }

    init {
        //使用OpenGL ES 2.0 context.
        setEGLContextClientVersion(2)
        renderer = CameraRender(this)
        setRenderer(renderer)
        //注意必须在setRenderer 后面。
        //这里我使用的是RENDERMODE_CONTINUOUSLY自动刷新，
        //还有一种模式 RENDERMODE_WHEN_DIRTY，如果使用这种的话
        //需要每一帧自己去要求渲染cameraView.requestRender();
        renderMode = RENDERMODE_WHEN_DIRTY
        Log.i(TAG,"CameraView init")

    }





}