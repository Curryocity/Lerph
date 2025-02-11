package renderer;

import component.FrameBuffer;
import manager.AssetPool;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public class PostProcessor {
    final Shader shader;
    private int vaoID, vboID;

    public PostProcessor(){
        shader = AssetPool.getShader("assets/shaders/frameBuffer.glsl");
        init();
    }

    public void init(){
        float[] quadVertices = {
                // Positions   // TexCoords
                -1.0f, -1.0f,  0.0f, 0.0f,  // Bottom-left
                1.0f, -1.0f,  1.0f, 0.0f,  // Bottom-right
                1.0f,  1.0f,  1.0f, 1.0f,   // Top-right

                1.0f,  1.0f,  1.0f, 1.0f,   // Top-right
                -1.0f,  1.0f,  0.0f, 1.0f,  // Top-left
                -1.0f, -1.0f,  0.0f, 0.0f  // Bottom-left
        };

        vaoID = glGenVertexArrays();
        vboID = glGenBuffers();

        glBindVertexArray(vaoID);
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferData(GL_ARRAY_BUFFER, quadVertices, GL_STATIC_DRAW);

        // Position
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        // TexCoords
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    public void renderFrameBuffer(FrameBuffer frameBuffer){

        glDisable(GL_DEPTH_TEST);
        shader.use();
        glActiveTexture(GL_TEXTURE0);
        frameBuffer.getTexture().bind();

        shader.uploadInt("uTexture", 0);

        glBindVertexArray(vaoID);
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);

        glDrawArrays(GL_TRIANGLES, 0, 6);
        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);

        glBindVertexArray(0);

        frameBuffer.getTexture().unbind();
        shader.detach();
        glEnable(GL_DEPTH_TEST);

        if(glGetError() != GL_NO_ERROR) throw new RuntimeException("glError: " + glGetError());
    }
}
