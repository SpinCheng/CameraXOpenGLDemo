#extension GL_OES_EGL_image_external : require
precision mediump float; //数据精度
varying vec2 aCoord;
uniform samplerExternalOES vTexture;
void main(){
//    gl_FragColor = texture2D(vTexture,aCoord);

    if(aCoord.x<=0.75 && aCoord.x >0.25){
        gl_FragColor = texture2D(vTexture,aCoord);
    }else{
        gl_FragColor = vec4(0.0,0.0,0.0,0.0);
    }

}

