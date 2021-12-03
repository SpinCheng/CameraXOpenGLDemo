# CameraX+OpenGLES

## 实现

 两种实现方式预览，通过rendererMode参数控制切换
1. GLSurfaceView
2. TextureView (Consumer API>=24 可使用回调代替)

都是通过surface绑定SurfaceTexture实现渲染

-----------------
## 踩坑

1. 预览模糊 使用surface之后，预览设置无效

```
//设置之后解决预览模糊问题//
mSurfaceTexture.setDefaultBufferSize(1440,1080)
```

2. OpenGL bindTextureImage: clearing GL error 0x502错误

```
11-29 23:46:18.973 19500-19538/com.example.cameraxdemo W/Adreno-ES20: <core_glUseProgram:1577>: GL_INVALID_OPERATION
11-29 23:46:19.011 19500-19538/com.example.cameraxdemo W/GLConsumer: [SurfaceTexture-1-19500-0] bindTextureImage: clearing GL error: 0x502
```
- shader语法错误，会导致该问题
- 硬解码要先生成一个GLES11Ext.GL_TEXTURE_EXTERNAL_OES的纹理，再由这个纹理生成SurfaceTexture ,
GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,textureid)



3. 退出之后再次进入崩溃 \
surface为空，逻辑会先进入cameraProviderFuture.addListener

4. 预览画面变形 \
面与view比例不一致导致。
形问题，开启矩阵转换适配竖屏模式，宽高需要置换



