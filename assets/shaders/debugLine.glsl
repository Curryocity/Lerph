//type vertex
#version 410 core
layout (location = 0) in vec3 aPos;
layout (location = 1) in vec4 aColor;
layout (location = 2) in float aThickness;

out vec4 gColor;
out float gThickness;

uniform mat4 uProjection;
uniform mat4 uView;

void main() {
    gColor = aColor;
    gThickness = aThickness;
    gl_Position = uProjection * uView * vec4(aPos, 1.0);
}

//type geometry
#version 410 core

layout (lines) in;
layout (triangle_strip, max_vertices = 4) out;

in vec4 gColor[];
in float gThickness[];

out vec4 fColor;

uniform mat4 uProjection;

void main() {
    vec2 direction = normalize(gl_in[1].gl_Position.xy - gl_in[0].gl_Position.xy); // direction
    vec4 normal =  vec4(-direction.y * gThickness[0] * 0.5, direction.x * gThickness[0] * 0.5, 0.0, 0.0); // Perpendicular

    gl_Position = gl_in[0].gl_Position + uProjection * normal;
    fColor = gColor[0];
    EmitVertex();

    gl_Position = gl_in[0].gl_Position - uProjection * normal;
    fColor = gColor[0];
    EmitVertex();

    gl_Position = gl_in[1].gl_Position + uProjection * normal;
    fColor = gColor[1];
    EmitVertex();

    gl_Position = gl_in[1].gl_Position - uProjection * normal;
    fColor = gColor[1];
    EmitVertex();

    EndPrimitive();
}

//type fragment
#version 410 core
in vec4 fColor;

out vec4 color;

void main() {
    color = fColor;
}