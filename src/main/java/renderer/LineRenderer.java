package renderer;

import core.Game;
import manager.AssetPool;
import component.Line;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public class LineRenderer {
    private static final int MAX_LINES = 1024, POS_SIZE = 3, COLOR_SIZE = 4, THICKNESS_SIZE = 1;
    private static final int VERTEX_SIZE = POS_SIZE + COLOR_SIZE + THICKNESS_SIZE;
    private static final List<Line> lines = new ArrayList<>();
    private static final float[] vertexArray = new float[MAX_LINES * VERTEX_SIZE * 2];     // 8 floats per vertex, 2 vertices per line
    private static final Shader shader = AssetPool.getShader("assets/shaders/debugLine.glsl");

    private static int vaoID, vboID;
    private static boolean started = false;

    public static void init() {
        if (!started) {
            vaoID = glGenVertexArrays();
            glBindVertexArray(vaoID);

            int POS_OFFSET = 0;
            int COLOR_SIZE_OFFSET = POS_OFFSET + POS_SIZE * Float.BYTES;
            int THICKNESS_SIZE_OFFSET = COLOR_SIZE_OFFSET + COLOR_SIZE * Float.BYTES;

            int VERTEX_SIZE_BYTES = VERTEX_SIZE * Float.BYTES;

            vboID = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, vboID);
            glBufferData(GL_ARRAY_BUFFER, (long) vertexArray.length * Float.BYTES, GL_DYNAMIC_DRAW);

            glVertexAttribPointer(0, POS_SIZE, GL_FLOAT, false, VERTEX_SIZE_BYTES, POS_OFFSET);
            glEnableVertexAttribArray(0);

            glVertexAttribPointer(1, COLOR_SIZE, GL_FLOAT, false, VERTEX_SIZE_BYTES, COLOR_SIZE_OFFSET);
            glEnableVertexAttribArray(1);

            glVertexAttribPointer(2, THICKNESS_SIZE, GL_FLOAT, false, VERTEX_SIZE_BYTES, THICKNESS_SIZE_OFFSET);
            glEnableVertexAttribArray(2);

            started = true;
        }

    }

    public static void render() {
        if (lines.isEmpty()) return;

        removeAllDeadPackets();

        int index = 0;
        for (Line line : lines) {

            float scale = line.usedLocalCoords ? Game.getScale(Game.BLOCK_SCALE) : Game.getScale(Game.REGULAR_SCALE);

            float offsetX = 0;
            float offsetY = 0;
            if(line.relativeToCamera){
                offsetX += Game.cameraPos(false).x;
                offsetY += Game.cameraPos(false).y;
            }
            Vector4f posInfo = line.midPointAndHalfDimensions;
            for (int sig = -1; sig <= 1; sig += 2) {
                Vector4f color = (sig == -1) ? line.color1 : line.color2;

                vertexArray[index] = (posInfo.x + sig*posInfo.z + offsetX) * scale;
                vertexArray[index + 1] = (posInfo.y + sig*posInfo.w + offsetY) * scale;
                vertexArray[index + 2] = line.zDepth;

                vertexArray[index + 3] = color.x;
                vertexArray[index + 4] = color.y;
                vertexArray[index + 5] = color.z;
                vertexArray[index + 6] = color.w;

                vertexArray[index + 7] = line.thickness;
                index += VERTEX_SIZE;
            }

            if(line.immediateMode){
                line.alive = false;
            }
        }

        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferSubData(GL_ARRAY_BUFFER, 0, vertexArray);

        shader.use();
        shader.uploadMat4f("uProjection", Game.getCurrentScene().camera().getProjectionMatrix());
        shader.uploadMat4f("uView", Game.getCurrentScene().camera().getViewMatrix());

        glBindVertexArray(vaoID);
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        glEnableVertexAttribArray(2);

        glDrawArrays(GL_LINES, 0, lines.size() * 2);

        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glDisableVertexAttribArray(2);
        glBindVertexArray(0);

        shader.detach();
    }
    public static void addLine(Line line){
        if (lines.size() >= MAX_LINES) return;
        line.alive = true;
        LineRenderer.lines.add(line);
    }

    public static void addImmediateLine(Line line){
        if (lines.size() >= MAX_LINES) return;
        line.immediateMode = true;
        line.alive = true;
        LineRenderer.lines.add(line);
    }

    public static void clear(){
        lines.clear();
    }

    public static void removeAllDeadPackets() {
        lines.removeIf(line -> !line.alive);
    }
}
