# CameraX+OpenGLES

### 实现

两种实现方式预览
1. GLSurfaceView
2. TextureView


依赖Surface实现

### 踩坑

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

shader语法错误，也会导致该问题

3. 退出之后再次进入崩溃

