package renderer;

import component.AdvPacket;
import component.Sprite;
import component.StdPacket;
import component.Texture;
import core.Game;
import manager.AssetPool;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

import static core.Game.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;

public class RendererLibrary {
    // hopefully reduce boilerplate code, plz
    public static final int MAX_QUADS = 1024;
    private static final int POS_SIZE = 3, COLOR_SIZE = 4, TEX_COORDS_SIZE = 2, TEX_ID_SIZE = 1, Y_REP_SIZE = 1;
    public static final int[] texture_slots = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
    // ChatGPT said modern low end pc has at least 16 texture slots
    public static List<Texture> textures = new ArrayList<>();

    public static void init(){
        textures.add(AssetPool.getTexture("assets/SpriteSheet/null.png"));
    }

    public static void setupAttrib(boolean hasColor){
        int VERTEX_SIZE = getVertexSize(hasColor);

        int POS_OFFSET = 0;
        int TEX_COORDS_OFFSET = POS_OFFSET + POS_SIZE * Float.BYTES;
        int TEX_ID_OFFSET = TEX_COORDS_OFFSET + TEX_COORDS_SIZE * Float.BYTES;
        int Y_REP_OFFSET = TEX_ID_OFFSET + TEX_ID_SIZE * Float.BYTES;
        int COLOR_OFFSET = Y_REP_OFFSET + Y_REP_SIZE * Float.BYTES;

        int VERTEX_SIZE_BYTES = VERTEX_SIZE * Float.BYTES;

        glVertexAttribPointer(0, POS_SIZE, GL_FLOAT, false, VERTEX_SIZE_BYTES, POS_OFFSET);
        glEnableVertexAttribArray(0);

        glVertexAttribPointer(1, TEX_COORDS_SIZE, GL_FLOAT, false, VERTEX_SIZE_BYTES, TEX_COORDS_OFFSET);
        glEnableVertexAttribArray(1);

        glVertexAttribPointer(2, TEX_ID_SIZE, GL_FLOAT, false, VERTEX_SIZE_BYTES, TEX_ID_OFFSET);
        glEnableVertexAttribArray(2);

        glVertexAttribPointer(3, Y_REP_SIZE, GL_FLOAT, false, VERTEX_SIZE_BYTES, Y_REP_OFFSET);
        glEnableVertexAttribArray(3);

        if(hasColor){
            glVertexAttribPointer(4, COLOR_SIZE, GL_FLOAT, false, VERTEX_SIZE_BYTES, COLOR_OFFSET);
            glEnableVertexAttribArray(4);
        }
    }

    public static void drawQuad(Shader shader, int quadAmount, int vaoID, int attribAmount){
        shader.use();
        shader.uploadMat4f("uProjection", Game.getCurrentScene().camera().getProjectionMatrix());
        shader.uploadMat4f("uView", Game.getCurrentScene().camera().getViewMatrix());

        //upload texture
        for (int i = 0; i < textures.size(); i++) {
            glActiveTexture(GL_TEXTURE0 + i);
            textures.get(i).bind();
        }
        shader.uploadIntArray("uTextures", texture_slots);

        // Bind the VAO that we're using
        glBindVertexArray(vaoID);
        // Enable the vertex attribute pointers
        for (int i = 0; i < attribAmount - 1; i++) {
            glEnableVertexAttribArray(i);
        }
        glDrawElements(GL_TRIANGLES, quadAmount * 6, GL_UNSIGNED_INT, 0);

        // Unbind everything
        for (int i = 0; i < attribAmount - 1; i++) {
            glDisableVertexAttribArray(i);
        }

        glBindVertexArray(0);
        for (Texture texture : textures) {
            texture.unbind();
        }
        shader.detach();

        if(glGetError() != GL_NO_ERROR) throw new RuntimeException("glError: " + glGetError());

    }

    public static int getVertexSize(boolean includeColor){
        return POS_SIZE + TEX_COORDS_SIZE + TEX_ID_SIZE + Y_REP_SIZE + (includeColor ?  COLOR_SIZE : 0);
    }

    public static void addPackToVertices(StdPacket packet, int quadIndex, float[] verticesArray, boolean usedLocalCoords) {
        if (quadIndex >= MAX_QUADS) return;
        if (packet.immediate) packet.alive = false;

        Sprite sprite = packet.sprite;
        Vector4f dimensions = (sprite != null) ? sprite.txcDimensions : null;
        int textureID = (sprite != null) ? getTextureID(sprite.textureSrc) : 0;

        float sizeX = packet.scaleX, sizeY = packet.scaleY;
        float anchorX = 0.5f, anchorY = 0.5f;
        if (sprite != null) {
            anchorX = sprite.anchorX;
            anchorY = sprite.anchorY;
            sizeX *= sprite.sizeX;
            sizeY *= sprite.sizeY;
        } else if (usedLocalCoords) {
            sizeX *= original_block_px;
            sizeY *= original_block_px;
        }

        float posX = packet.x + (packet.relativeToCamera ? Game.cameraPos(false).x : 0);
        float posY = packet.y + (packet.relativeToCamera ? Game.cameraPos(false).y : 0);

        if (usedLocalCoords) {
            posX *= original_block_px;
            posY *= original_block_px;
        }

        final float scaleFactor = usedLocalCoords ? Game.getScale(GAME_SCALE) : 1;

        final float startX = (posX - anchorX * sizeX) * scaleFactor;
        final float startY = (posY - anchorY * sizeY) * scaleFactor;
        final float deltaX = sizeX * scaleFactor;
        final float deltaY = sizeY * scaleFactor;

        final float[] xOffsets = {0f, 1f, 1f, 0f};
        final float[] yOffsets = {0f, 0f, 1f, 1f};

        final float zDepth = packet.zDepth;
        final float[] txcDims = (sprite != null) ? new float[]{dimensions.x, dimensions.y, dimensions.z, dimensions.w} : new float[]{0, 0, 0, 0};

        final float yRep = packet.yRep; // yRep is for comparison, I don't think it needs to be scaled, and only ingame it matters

        final boolean isAdvPacket = packet instanceof AdvPacket;
        final AdvPacket advPacket = isAdvPacket ? (AdvPacket) packet : null;

        final int vertexSize = getVertexSize(isAdvPacket);
        int offset = quadIndex * 4 * vertexSize;

        int colorMode = 1;
        int[] gradientCorners = new int[]{0, 0, 0, 0};
        if(isAdvPacket){
            colorMode = (advPacket.isColorsMultiplier()) ? 1 : -1;
            switch (advPacket.getGradientType()){
                case AdvPacket.GRADIENT_HORIZONTAL -> gradientCorners = new int[]{0, 1, 1, 0};
                case AdvPacket.GRADIENT_VERTICAL -> gradientCorners = new int[]{0, 0, 1, 1};
            }
        }

        for (int i = 0; i < 4; i++) {
            float xAdd = xOffsets[i], yAdd = yOffsets[i];

            verticesArray[offset] = startX + xAdd * deltaX;
            verticesArray[offset + 1] = startY + yAdd * deltaY;
            verticesArray[offset + 2] = zDepth;

            verticesArray[offset + 3] = txcDims[0] + xAdd * txcDims[2];
            verticesArray[offset + 4] = txcDims[1] + yAdd * txcDims[3];

            verticesArray[offset + 5] = textureID;

            verticesArray[offset + 6] = yRep;

            if (isAdvPacket) {
                verticesArray[offset + 7] = advPacket.colors[4 * gradientCorners[i]];
                verticesArray[offset + 8] = advPacket.colors[4 * gradientCorners[i] + 1];
                verticesArray[offset + 9] = advPacket.colors[4 * gradientCorners[i] + 2];
                verticesArray[offset + 10] = advPacket.colors[4 * gradientCorners[i] + 3]
                                            * colorMode;
            }

            offset += vertexSize;
        }
    }

    public static int[] generateQuadIndices(){
        // 6 indices per quad (3 per triangle)
        int[] indices = new int[6 * MAX_QUADS];
        for (int i = 0; i < MAX_QUADS; i++) {
            int offsetArrayIndex = 6 * i;
            int offset = 4 * i;

            // 0, 1, 2, 2, 3, 0
            // Triangle 1
            indices[offsetArrayIndex + 0] = offset + 0;
            indices[offsetArrayIndex + 1] = offset + 1;
            indices[offsetArrayIndex + 2] = offset + 2;
            // Triangle 2
            indices[offsetArrayIndex + 3] = offset + 2;
            indices[offsetArrayIndex + 4] = offset + 3;
            indices[offsetArrayIndex + 5] = offset + 0;
        }

        return indices;
    }

    public static void addTexture(Texture texture){
        if(!textures.contains(texture)){
            textures.add(texture);
        }
    }

    public static int getTextureID(Texture texture){
        if (texture == null) return 0;
        int spriteSheetID = 0;
        for (Texture value : textures) {
            if (value.equals(texture)) {
                spriteSheetID = texture.texID - 1;
                break;
            }
        }
        return spriteSheetID;
    }

}
