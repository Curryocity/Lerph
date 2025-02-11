//type vertex
#version 410 core
layout (location=0) in vec3 aPos;
layout (location=1) in vec2 aTexCoords;
layout (location=2) in float aTexSlot;
layout (location=3) in float yRep;
layout (location=4) in vec4 aColor;


uniform mat4 uProjection;
uniform mat4 uView;

out vec2 fTexCoords;
out float fTexSlot;
out vec4 fColor;

void main(){
    fTexCoords = aTexCoords;
    fTexSlot = aTexSlot;
    gl_Position = uProjection * uView * vec4(aPos, 1.0);
    gl_Position.z = gl_Position.z + yRep * 0.0001;
    fColor = aColor;
}

//type fragment
#version 410 core

uniform sampler2D uTextures[16];

in vec2 fTexCoords;
in float fTexSlot;
in vec4 fColor;

out vec4 color;

void main(){

    if(fTexSlot == 0){
        color = fColor;
    }else {
        if (fColor.a >= 0.0) {
            color = fColor * texture(uTextures[int(fTexSlot)], fTexCoords);
        }else{
            color = vec4(fColor.rgb, - fColor.a * step(0.01, texture(uTextures[int(fTexSlot)], fTexCoords).a ) );
        }

        if (abs(color.a) < 0.01) { discard; }
    }

}