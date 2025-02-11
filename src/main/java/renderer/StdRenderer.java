package renderer;

import component.StdPacket;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static renderer.RendererLibrary.generateQuadIndices;
import static renderer.RendererLibrary.MAX_QUADS;

public class StdRenderer {
    private final float[] vertices;
    private int quadAmount;
    private int vaoID, vboID;
    private final Shader shader;

    public StdRenderer(RenderSystem renderSystem){
        vertices = new float[MAX_QUADS * 4 * RendererLibrary.getVertexSize(false)];
        shader = renderSystem.shader;
    }

    public void init(){
        vaoID = glGenVertexArrays();
        glBindVertexArray(vaoID); // Binding means making the VAO the active object that will store any subsequent vertex attribute or buffer binding.
                                 // Analogy: You can think of a VAO as a blueprint that records how data in the buffers is structured and how the data should be drawn.
        vboID = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferData(GL_ARRAY_BUFFER, (long) vertices.length * Float.BYTES, GL_DYNAMIC_DRAW);

        int eboID = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboID);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, generateQuadIndices(), GL_STATIC_DRAW);

        RendererLibrary.setupAttrib(false);
    }

    public void render(){
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferSubData(GL_ARRAY_BUFFER, 0, vertices);

        RendererLibrary.drawQuad(shader, quadAmount, vaoID, 4);
    }

    public void addRenderPack(StdPacket renderPack){
        RendererLibrary.addPackToVertices(renderPack, quadAmount, vertices, renderPack.usedLocalCoords);
        quadAmount ++;
    }

    public boolean hasRoom(){
        return quadAmount < MAX_QUADS;
    }

    public void reset() {
        quadAmount = 0;
    }

}
