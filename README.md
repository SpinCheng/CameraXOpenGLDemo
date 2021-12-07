# CameraX+OpenGLES

## 实现
 CameraX+OpenGL ES2.0 \
 预览使用两种实现方式，通过rendererMode参数控制切换
1. GLSurfaceView
2. TextureView (Consumer API>=24 可使用回调代替)

都是通过surface绑定SurfaceTexture实现渲染


## 重点
---------------------
###  CameraX

#### ImageAnalysis.Analyzer
ImageAnalysis.Analyzer接口中，analyze(image: ImageProxy) 回调的到的image数据可能存在两种yuv420格式数据：I420以及NV12/NV21
> 可通过plane[1]是否为plane[0]的1/4长度来判断

1. I420

`plane[1].buffer.remaining = plane[0].buffer.remaining/4`
```
//-------I420转NV21 
for (i in 0..(vSize+uSize) ){
    if (i%2 == 0){
        vBuffer.get(nv21,ySize+i,1)
    }else{
        uBuffer.get(nv21,ySize+i,1)
    }
}
```

2. NV12/NV21

`plane[1].buffer.remaining <= plane[0].buffer.remaining/2`

plane[1]和plane[2]中，UV数据交替出现，且长度均为`plane[0].buffer.remaining/2-1`（少一字节，bug？）
```
plane[1] : UVUVUVUVUVUVUVUV...
plane[2] : VUVUVUVUVUVUVUVU...
```
plane[1]和plane[2]中，忽略少的一字节数据，分别均已包含所有图像UV数据信息。所以：
`plane[0] + plane[1]`可得NV12 \
`plane[0] + plane[2]`可得NV21 \
或者 \
先从plane[0]中提取出Y数据，然后在plane[1]中提取U数据，最后在plane[2]中提取V数据,提取的数据最完整

[参考](https://blog.csdn.net/lbknxy/article/details/54633008)



## 踩坑

1. **预览模糊 使用surface之后，Preview预览参数设置无效**

```
//设置之后解决预览模糊问题//
mSurfaceTexture.setDefaultBufferSize(1440,1080)
```

2. **OpenGL bindTextureImage: clearing GL error 0x502错误**

```
11-29 23:46:18.973 19500-195 :18.973 19500-19538/com.example.38/com.example.cameraxdemo W/Adreno-ES20: <core_glUseProgram:1577>: GL_INVALID_OPERATION
11-29 23:46:19.011 19500-19538/com.example.cameraxdemo W/GLConsumer: [SurfaceTexture-1-19500-0] bindTextureImage: clearing GL error: 0x502
```
- shader语法错误，会导致该问题
- 硬解码要先生成一个GLES11Ext.GL_TEXTURE_EXTERNAL_OES的纹理，再由这个纹理生成SurfaceTexture ,
GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,textureid)


3. **退出之后再次进入崩溃** \
surface为空，逻辑会先进入cameraProviderFuture.addListener


4. **预览画面变形** \
画面与view比例不一致导致变形问题，开启矩阵转换适配竖屏模式，宽高需要置换




