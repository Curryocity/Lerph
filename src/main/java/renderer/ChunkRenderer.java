package renderer;

import block.BlockLoader;
import core.Application;
import core.Game;
import data.Chunk;
import data.Space;
import manager.AssetPool;
import org.joml.Vector2f;
import scene.Scene;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static data.Chunk.blocksPerAxis;

public class ChunkRenderer {

    public final static float floorZDepth = 10f;
    public final static int INDICES_PER_CHUNK = 6 * blocksPerAxis * blocksPerAxis,
                            VERTICES_PER_CHUNK = 4 * blocksPerAxis * blocksPerAxis;
    private final List<Chunk> renderedChunks;
    private final Shader shader;
    private Space currentSpace;


    float[] vertices = new float[0];
    int[] indices = new int[0];
    private int vaoID, vboID, eboID;
    private boolean resizeEvent, capacityChange;
    private int chunkCapacity_Vertices, hasMapChunks;

    public ChunkRenderer(Space space){
        setCurrentSpace(space);
        renderedChunks = new ArrayList<>(6);
        RendererLibrary.addTexture(BlockLoader.getBlockSheetTexture());
        shader = AssetPool.getShader("assets/shaders/chunk.glsl");
        init();
    }

    public void setCurrentSpace(Space space){
        this.currentSpace = space;
    }

    public void syncScene(Scene scene){
        currentSpace = scene.currentSpace();
    }

    public void init(){
        vaoID = glGenVertexArrays();
        glBindVertexArray(vaoID);

        vboID = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferData(GL_ARRAY_BUFFER, (long) vertices.length * Float.BYTES, GL_DYNAMIC_DRAW);

        eboID = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboID);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_DYNAMIC_DRAW);

        RendererLibrary.setupAttrib(false);
    }

    public void scanChunks(){

        Vector2f cameraPos = Game.cameraPos(true);
        float chunkSizeInverse = 1.0f /(blocksPerAxis * Game.getScale(Game.BLOCK_SCALE));

        final float renderDistanceX = Application.viewPortWidth * chunkSizeInverse * 0.5f;
        final float renderDistanceY = Application.viewPortHeight * chunkSizeInverse * 0.5f;

        final int cameraChunkX = (int) (cameraPos.x / blocksPerAxis);
        final int cameraChunkY = (int) (cameraPos.y / blocksPerAxis);

        final int startChunkX = Math.max(0, (int) (cameraChunkX - renderDistanceX));
        final int startChunkY = Math.max(0, (int) (cameraChunkY - renderDistanceY));
        final int endChunkX = Math.min(currentSpace.chunkLengthX - 1, (int) (cameraChunkX + renderDistanceX) + 1);
        final int endChunkY = Math.min(currentSpace.chunkLengthY - 1, (int) (cameraChunkY + renderDistanceY) + 1);

        Set<Chunk> oldChunks = new HashSet<>(renderedChunks);

        for (int i = startChunkX; i <= endChunkX ; i++) {
            for (int j = startChunkY; j <= endChunkY ; j++) {
                Chunk chunk = currentSpace.getChunk(i,j);
                // chunk doesn't exist, no contents, or already rendered
                if (chunk == null || (!chunk.hasEntities() && chunk.isMapEmpty()) || oldChunks.remove(chunk)) continue;

                chunk.setRendered(true);
                renderedChunks.add(chunk);

                if (!chunk.isMapEmpty()) hasMapChunks++;
            }
        }

        renderedChunks.removeAll(oldChunks);

        for (Chunk chunk : oldChunks) {
            chunk.setRendered(false);
            if(!chunk.isMapEmpty())  hasMapChunks--;
        }

        int prevCapacity = chunkCapacity_Vertices;
        chunkCapacity_Vertices = (int) (4 * ( renderDistanceX + 1.5) * ( renderDistanceY + 1.5));

        capacityChange = (chunkCapacity_Vertices != prevCapacity);

        generateIndices();
        generateVertices();
    }

    public void render(float alpha) {
        if(currentSpace == null) return;

        scanChunks();

        if(capacityChange) {
            glBindBuffer(GL_ARRAY_BUFFER, vboID);
            glBufferData(GL_ARRAY_BUFFER, vertices, GL_DYNAMIC_DRAW);

            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboID);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_DYNAMIC_DRAW);

            capacityChange = false;
        }else{
            glBindBuffer(GL_ARRAY_BUFFER, vboID);
            glBufferSubData(GL_ARRAY_BUFFER, 0, vertices);

        }

        RendererLibrary.drawQuad(shader, hasMapChunks * blocksPerAxis * blocksPerAxis, vaoID, 4);

        renderEntities(alpha);
    }

    private void renderEntities(float alpha) {
        for (Chunk chunk : renderedChunks) {
            chunk.renderEntities(alpha);
        }
    }

    public void generateIndices() {
        if (indices.length == chunkCapacity_Vertices * INDICES_PER_CHUNK) return;
        indices = new int[chunkCapacity_Vertices * INDICES_PER_CHUNK];
        indices = getIndices(chunkCapacity_Vertices);
    }

    public int[] getIndices(int amountsOfChunks){
        int[] theIndices = new int[amountsOfChunks * INDICES_PER_CHUNK];

        for (int i = 0; i < amountsOfChunks; i++) {

            int vertexBaseOffset = VERTICES_PER_CHUNK * i;
            int indicesBaseOffset = INDICES_PER_CHUNK * i;

            for (int j = 0; j < blocksPerAxis * blocksPerAxis; j++) {

                int indexOffset = 6 * j;
                int vertexOffset = 4 * j;

                theIndices[indicesBaseOffset + indexOffset + 0] = vertexBaseOffset + vertexOffset + 0;
                theIndices[indicesBaseOffset + indexOffset + 1] = vertexBaseOffset + vertexOffset + 1;
                theIndices[indicesBaseOffset + indexOffset + 2] = vertexBaseOffset + vertexOffset + 2;
                theIndices[indicesBaseOffset + indexOffset + 3] = vertexBaseOffset + vertexOffset + 2;
                theIndices[indicesBaseOffset + indexOffset + 4] = vertexBaseOffset + vertexOffset + 3;
                theIndices[indicesBaseOffset + indexOffset + 5] = vertexBaseOffset + vertexOffset + 0;
            }

        }
        return theIndices;
    }

    public void generateVertices(){
        int pendingSize = chunkCapacity_Vertices * VERTICES_PER_CHUNK * RendererLibrary.getVertexSize(false);
        if(vertices.length != pendingSize){
            vertices = new float[pendingSize];
        }

        if(resizeEvent) currentSpace.regenAllChunkMeshes();

        int pointer = 0;
        for(Chunk chunk : renderedChunks){
            if(resizeEvent) chunk.queueRegenMesh();
            float[] mesh = chunk.generateMesh();
            System.arraycopy(mesh, 0, vertices, pointer, mesh.length);
            pointer += VERTICES_PER_CHUNK * RendererLibrary.getVertexSize(false);
        }
        resizeEvent = false;
    }

    public void reset(){
        setCurrentSpace(null);
        renderedChunks.clear();
        vertices = new float[0];
        resizeEvent = true;
    }

    public void queueResizeEvent(){
        resizeEvent = true;
    }

}
