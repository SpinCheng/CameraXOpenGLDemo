//顶点坐标
attribute vec4 vPosition;
//传给片元进行采样的纹理坐标
attribute vec4 vCoord;
//易变变量 和片元写的一模一样 会传给片元
varying vec2 aCoord;
uniform mat4 vMatrix;

void main(){
    //内置变量 顶点给它就行
    gl_Position = vPosition;
    aCoord = (vMatrix * vCoord).xy;

//    if(vPosition.x <=0.5 && vPosition.x >=-0.5){
//        gl_Position = vPosition;
//        aCoord = (vMatrix * vCoord).xy;
//    }else{
//        gl_Position = vec4(0,0,0,0);
//        aCoord = vec2(0,0);
//    }
}
