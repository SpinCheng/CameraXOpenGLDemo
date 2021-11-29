package com.example.cameraxdemo.glsurfaceview

import android.content.Context
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class ScreenFilter(context: Context) {
    private var vPosition = 0
    private var vCoord = 0
    private var vTexture = 0
    private var vMatrix = 0
    private var program = 0
    var vertexBuffer //顶点坐标缓存区
            : FloatBuffer? = null
    var textureBuffer // 纹理坐标
            : FloatBuffer? = null
    private var mWidth = 0
    private var mHeight = 0
    private lateinit var mtx: FloatArray

    val vertexSharder = "//顶点坐标 \n" +
            "attribute vec4 vPosition;\n" +
            "//传给片元进行采样的纹理坐标 \n" +
            "attribute vec4 vCoord; \n" +
            "//易变变量 和片元写的一模一样 会传给片元 \n" +
            "varying vec2 aCoord; \n" +
            "uniform mat4 vMatrix; \n"+
            "void main(){ \n" +
            "//内置变量 顶点给它就行 \n" +
            "gl_Position = vPosition; \n" +
            "aCoord = (vMatrix * vCoord).xy; \n" +
//            "aCoord = vCoord; \n" +
            "}"

    val fragSharder = "#extension GL_OES_EGL_image_external : require \n" +
            "precision mediump float; //数据精度 \n" +
            "varying vec2 aCoord; \n" +
            "uniform samplerExternalOES vTexture; \n" +
            "void main(){ \n" +
            "gl_FragColor = texture2D(vTexture,aCoord); \n" +
            "}"


    init {
        //准备坐标数据
        /*** 顶点坐标 * ================================================================ */
        // 4个点 x，y = 4*2 float 4字节 所以 4*2*4

        //准备坐标数据
        /*** 顶点坐标 * ================================================================  */
        // 4个点 x，y = 4*2 float 4字节 所以 4*2*4
        vertexBuffer = ByteBuffer.allocateDirect(4 * 4 * 2).order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        val VERTEX = floatArrayOf(
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f, 1.0f,
            1.0f, 1.0f
        )
        vertexBuffer!!.clear()
        vertexBuffer!!.put(VERTEX)

        /*** 纹理坐标 * ================================================================  */
        textureBuffer = ByteBuffer.allocateDirect(4 * 4 * 4).order(ByteOrder.nativeOrder())
            .asFloatBuffer()

        val TEXTURE = floatArrayOf(
            //矩阵
            0.0f,0.0f,0.0f,1.0f,
            1.0f,0.0f,0.0f,1.0f,
            0.0f,1.0f,0.0f,1.0f,
            1.0f,1.0f,0.0f,1.0f

            //后置镜像
//            1.0f,0.0f,
//            1.0f,1.0f,
//            0.0f,0.0f,
//            0.0f,1.0f
            //后置
//            1.0f,1.0f,
//            1.0f,0.0f,
//            0.0f,1.0f,
//            0.0f,0.0f


            //镜像
            //                0.0f, 0.0f,
            //                0.0f, 1.0f,
            //                1.0f, 0.0f,
            //                1.0f, 1.0f,
            //正常
//            0.0f, 1.0f,
//            0.0f, 0.0f,
//            1.0f, 1.0f,
//            1.0f, 0.0f,
        )

        textureBuffer!!.clear()
        textureBuffer!!.put(TEXTURE);
        //着色器程序准备好 通过着色器代码准备好着色器程序，用int 表示这个程序的 id
        program = loadProgram(vertexSharder, fragSharder);


        //获取程序中的变量 索引
        //获得顶点着色器中的 attribute 变量的索引值
        vPosition = GLES20.glGetAttribLocation(program, "vPosition");
        vCoord = GLES20.glGetAttribLocation(program, "vCoord");
        //获得片元着色器中的 Uniform vTexture变量的索引值
        vTexture = GLES20.glGetUniformLocation(program, "vTexture");
        vMatrix = GLES20.glGetUniformLocation(program, "vMatrix");

    }

    private fun loadProgram(vertexSharder: String, fragSharder: String): Int {

        val vshader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER)
        //加载shader代码 glShaderSource 和 glCompileShader
        //加载shader代码 glShaderSource 和 glCompileShader
        GLES20.glShaderSource(vshader, vertexSharder)
        GLES20.glCompileShader(vshader)

        val fshader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER)
        //加载shader代码 glShaderSource 和 glCompileShader
        //加载shader代码 glShaderSource 和 glCompileShader
        GLES20.glShaderSource(fshader, fragSharder)
        GLES20.glCompileShader(fshader)

        val mProgram = GLES20.glCreateProgram()
        GLES20.glAttachShader(mProgram, vshader)
        GLES20.glAttachShader(mProgram, fshader)

        GLES20.glLinkProgram(mProgram)
        return mProgram;
    }


    fun setSize(width: Int, height: Int) {
        Log.i("cx---","setSize--width= $width,heigit= $height")
        mWidth = width
        mHeight = height

    }

    fun setTransformMatrix(mtx: FloatArray) {
        this.mtx = mtx
    }

    fun onDraw(texture: Int) {

        //设置绘制区域
        GLES20.glViewport(0, 0, mWidth, mHeight);
        GLES20.glUseProgram(program);

        vertexBuffer?.position(0);
        //  ormalized  [-1,1] . 把[2,2]转换为[-1,1]
        GLES20.glVertexAttribPointer(vPosition, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        //CPU传数据到GPU，默认情况下着色器无法读取到这个数据。 需要我们启用一下才可以读取
        GLES20.glEnableVertexAttribArray(vPosition);

        textureBuffer?.position(0);
        //normalized  [-1,1] . 把[2,2]转换为[-1,1]
        GLES20.glVertexAttribPointer(vCoord, 4, GLES20.GL_FLOAT, false, 0, textureBuffer);
        //CPU传数据到GPU，默认情况下着色器无法读取到这个数据。 需要我们启用一下才可以读取
        GLES20.glEnableVertexAttribArray(vCoord);

        //相当于激活一个用来显示图片的画框
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture);

        // 0: 图层ID  GL_TEXTURE0
        // GL_TEXTURE1 ， 1
        GLES20.glUniform1i(vTexture, 0);
        GLES20.glUniformMatrix4fv(vMatrix, 1, false, mtx, 0);

        //通知画画，
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);

    }


}
