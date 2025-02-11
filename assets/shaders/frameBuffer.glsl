//type vertex
#version 410 core
layout (location=0) in vec2 aPos;
layout (location=1) in vec2 aTexCoords;

out vec2 fTexCoords;

void main(){
    fTexCoords = aTexCoords;
    gl_Position = vec4(aPos, 0.5, 1.0);
}


//type fragment
#version 410 core

uniform sampler2D uTexture;

in vec2 fTexCoords;

out vec4 color;

void main(){

    color = texture(uTexture, fTexCoords);
    if (color.a < 0.01) { discard; }

}