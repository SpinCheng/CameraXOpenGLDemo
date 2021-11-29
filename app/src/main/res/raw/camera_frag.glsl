#extension GL_OES_EGL_image_external : require
precision mediump float; //数据精度
varying vec2 aCoord;
uniform samplerExternalOES vTexture;
void main(){
    gl_FragColor = texture2D(vTexture,aCoord);
}

