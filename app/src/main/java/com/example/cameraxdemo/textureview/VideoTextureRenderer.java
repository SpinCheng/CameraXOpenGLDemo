package com.example.cameraxdemo.textureview;


import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.os.Build;
import android.util.Log;

import com.example.cameraxdemo.Utils;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.function.Consumer;

import androidx.annotation.RequiresApi;

public class VideoTextureRenderer extends TextureSurfaceRenderer implements SurfaceTexture.OnFrameAvailableListener, Serializable
{
    private static final String VERTEX_SHADER_CODE =
                    "attribute vec4 vPosition;" +
                    "attribute vec4 vTexCoordinate;" +
                    "uniform mat4 textureTransform;" +
                    "varying vec2 v_TexCoordinate;" +
                    "void main() {" +
                    "   v_TexCoordinate = (textureTransform * vTexCoordinate).xy;" +
//                    "   v_TexCoordinate = (vTexCoordinate).xy;" +
                    "   gl_Position = vPosition;" +
                    "}";

    private static final String FRAGMENT_SHADER_CODE =
                    "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;" +
                    "uniform samplerExternalOES texture;" +
                    "varying vec2 v_TexCoordinate;" +
                    "void main () {" +
                    "    vec4 color = texture2D(texture, v_TexCoordinate);" +
                    "    gl_FragColor = color;" +
                    "}";


    private static final float SQUARE_SIZE = 1.0f;
    private static final float[] SQUARE_COORDINATES = {-SQUARE_SIZE, SQUARE_SIZE, 0.0f,   // top left
                                                       -SQUARE_SIZE, -SQUARE_SIZE, 0.0f,   // bottom left
                                                        SQUARE_SIZE, -SQUARE_SIZE, 0.0f,   // bottom right
                                                        SQUARE_SIZE, SQUARE_SIZE, 0.0f}; // top right

    private static final short[] DRAW_ORDER = {0, 1, 2, 0, 2, 3};

    private static final float[] TEXTURE_COORDINATES = {
            0.0f, 1.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 0.0f, 1.0f,
            1.0f, 0.0f, 0.0f, 1.0f,
            1.0f, 1.0f, 0.0f, 1.0f
    };

    private final Consumer<SurfaceTexture> initCompleteRunnable;

    // Texture to be shown in backgrund
    private FloatBuffer textureBuffer;

    private int[] textures = new int[1];

    private int shaderProgram;
    private FloatBuffer vertexBuffer;
    private ShortBuffer drawListBuffer;

    private SurfaceTexture videoTexture;
    private float[] videoTextureTransform;
    private boolean frameAvailable = false;

    private int videoWidth;
    private int videoHeight;

    private int SCALE_RESET = -1;
    private final int SCALE_MODE_FIT = 0;
    private final int SCALE_MODE_CENTER_INSIDE = 1;
    private final int SCALE_MODE_CENTER_CROP = 2;
    private int adjustViewport = SCALE_MODE_CENTER_CROP;

    public void setAdjustViewport(int adjustViewport) {
        this.adjustViewport = adjustViewport;
    }

    public VideoTextureRenderer(SurfaceTexture texture, int width, int height, Consumer<SurfaceTexture> initComplete)
    {
        super(texture, width, height);
        videoTextureTransform = new float[16];
        initCompleteRunnable = initComplete;
    }

    private void loadShaders()
    {
        int vertexShaderHandle = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        GLES20.glShaderSource(vertexShaderHandle, VERTEX_SHADER_CODE);
        GLES20.glCompileShader(vertexShaderHandle);
        checkGlError("Vertex shader compile");

        int fragmentShaderHandle = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        GLES20.glShaderSource(fragmentShaderHandle, FRAGMENT_SHADER_CODE);
        GLES20.glCompileShader(fragmentShaderHandle);
        checkGlError("Pixel shader compile");

        shaderProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(shaderProgram, vertexShaderHandle);
        GLES20.glAttachShader(shaderProgram, fragmentShaderHandle);
        GLES20.glLinkProgram(shaderProgram);
        checkGlError("Shader program compile");

        int[] status = new int[1];
        GLES20.glGetProgramiv(shaderProgram, GLES20.GL_LINK_STATUS, status, 0);
        if (status[0] != GLES20.GL_TRUE) {
            String error = GLES20.glGetProgramInfoLog(shaderProgram);
            Log.e("SurfaceTest", "Error while linking program:\n" + error);
        }

    }


    private void setupVertexBuffer()
    {
        // Draw list buffer
        ByteBuffer dlb = ByteBuffer.allocateDirect(DRAW_ORDER. length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(DRAW_ORDER);
        drawListBuffer.position(0);

        // Initialize the texture holder
        ByteBuffer bb = ByteBuffer.allocateDirect(SQUARE_COORDINATES.length * 4);
        bb.order(ByteOrder.nativeOrder());

        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(SQUARE_COORDINATES);
        vertexBuffer.position(0);
    }


    private void setupTexture()
    {
        ByteBuffer texturebb = ByteBuffer.allocateDirect(TEXTURE_COORDINATES.length * 4);
        texturebb.order(ByteOrder.nativeOrder());

        textureBuffer = texturebb.asFloatBuffer();
        textureBuffer.put(TEXTURE_COORDINATES);
        textureBuffer.position(0);

        // Generate the actual texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glGenTextures(1, textures, 0);
        checkGlError("Texture generate");

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0]);
        checkGlError("Texture bind");

        videoTexture = new SurfaceTexture(textures[0]);
        videoTexture.setOnFrameAvailableListener(this);
    }

    @Override
    protected boolean draw()
    {
        synchronized (this)
        {
            if (frameAvailable)
            {
                videoTexture.updateTexImage();
                videoTexture.getTransformMatrix(videoTextureTransform);
                frameAvailable = false;
            }
            else
            {
                return false;
            }

        }


        switch (adjustViewport){
            case SCALE_MODE_CENTER_CROP:
                setScaleModeCenterCrop();
                break;
            case SCALE_MODE_CENTER_INSIDE:
                setScaleModeCenterInside();
                break;
            default:
                break;
        }

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        // Draw texture
        GLES20.glUseProgram(shaderProgram);
        int textureParamHandle = GLES20.glGetUniformLocation(shaderProgram, "texture");
        int textureCoordinateHandle = GLES20.glGetAttribLocation(shaderProgram, "vTexCoordinate");
        int positionHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition");
        int textureTranformHandle = GLES20.glGetUniformLocation(shaderProgram, "textureTransform");

        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 4 * 3, vertexBuffer);

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0]);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glUniform1i(textureParamHandle, 0);

        GLES20.glEnableVertexAttribArray(textureCoordinateHandle);
        GLES20.glVertexAttribPointer(textureCoordinateHandle, 4, GLES20.GL_FLOAT, false, 0, textureBuffer);

        GLES20.glUniformMatrix4fv(textureTranformHandle, 1, false, videoTextureTransform, 0);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, DRAW_ORDER.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);
        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(textureCoordinateHandle);

        return true;
    }

    private void setScaleModeCenterCrop()
    {
        Utils.Companion.LOGI("setScaleModeCenterCrop--w*h="+width+"*"+height);
        Utils.Companion.LOGI("setScaleModeCenterCrop--vw*vh="+videoWidth+"*"+videoHeight);
        float surfaceAspect = height / (float)width;
        float videoAspect = videoHeight / (float)videoWidth;

        if (surfaceAspect > videoAspect)
        {
            float heightRatio = height / (float)videoHeight;
            int newWidth = (int)(videoWidth * heightRatio);
            int xOffset = (newWidth - width) / 2;
            GLES20.glViewport(-xOffset, 0, newWidth, height);
        }
        else
        {
            float widthRatio = width / (float)videoWidth;
            int newHeight = (int)(videoHeight * widthRatio);
            int yOffset = (newHeight - height) / 2;
            GLES20.glViewport(0, -yOffset, width, newHeight);
        }

        adjustViewport = SCALE_RESET;
    }

    private void setScaleModeCenterInside()
    {
        Utils.Companion.LOGI("setScaleModeCenterInside--w*h="+width+"*"+height);
        Utils.Companion.LOGI("setScaleModeCenterInside--vw*vh="+videoWidth+"*"+videoHeight);
        float surfaceAspect = height / (float)width;
        float videoAspect = videoHeight / (float)videoWidth;
        Utils.Companion.LOGI("setScaleModeCenterInside--surfaceAspect="+surfaceAspect+"，videoAspect="+videoAspect);

        if (surfaceAspect < videoAspect)
        {
            float heightRatio = height / (float)videoHeight;
            int newWidth = (int)(videoWidth * heightRatio);
            int xOffset = (newWidth - width) / 2;
            GLES20.glViewport(-xOffset, 0, newWidth, height);
        }
        else
        {
            float widthRatio = width / (float)videoWidth;
            int newHeight = (int)(videoHeight * widthRatio);
            int yOffset = (newHeight - height) / 2;
            GLES20.glViewport(0, -yOffset, width, newHeight);
        }

        adjustViewport = SCALE_RESET;
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void initGLComponents()
    {
        synchronized (this) {
            setupVertexBuffer();
            setupTexture();
            loadShaders();
        }

        initCompleteRunnable.accept(videoTexture);
    }

    @Override
    protected void deinitGLComponents()
    {
        GLES20.glDeleteTextures(1, textures, 0);
        GLES20.glDeleteProgram(shaderProgram);
        videoTexture.release();
        videoTexture.setOnFrameAvailableListener(null);
    }

    public void setVideoSize(int width, int height)
    {
        this.videoWidth = width;
        this.videoHeight = height;
        adjustViewport = SCALE_MODE_CENTER_CROP;
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture)
    {
        synchronized (this)
        {
            frameAvailable = true;
        }
    }

    private void checkGlError(String op)
    {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e("SurfaceTest", op + ": glError " + GLUtils.getEGLErrorString(error));
        }
    }
}
