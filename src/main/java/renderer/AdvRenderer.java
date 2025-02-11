package renderer;

import component.AdvPacket;
import manager.AssetPool;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static renderer.RendererLibrary.generateQuadIndices;
import static renderer.RendererLibrary.MAX_QUADS;
public class AdvRenderer {

    private int quadAmount;
    private int vaoID, vboID;
    private final Shader shader;
    private final float[] vertices;
    private final List<AdvPacket> advPackets = new ArrayList<>(32);
    private boolean needSorting = true;

    public AdvRenderer(){
        vertices = new float[MAX_QUADS * 4 * RendererLibrary.getVertexSize(true)];
        shader = AssetPool.getShader("assets/shaders/colorful.glsl");
        init();
    }

    public void init(){
        vaoID = glGenVertexArrays();
        glBindVertexArray(vaoID);

        vboID = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferData(GL_ARRAY_BUFFER, (long) vertices.length * Float.BYTES, GL_DYNAMIC_DRAW);

        int eboID = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboID);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, generateQuadIndices(), GL_STATIC_DRAW);

        RendererLibrary.setupAttrib(true);
    }

    public void render(){
        if(needSorting) {
            zSorting();
            needSorting = false;
        }
        fillVertices();
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferSubData(GL_ARRAY_BUFFER, 0, vertices);

        RendererLibrary.drawQuad(shader, quadAmount, vaoID, 5);


    }

    public void activatePacket(AdvPacket packet){
        if(!packet.alive){
            advPackets.add(packet);
            packet.alive = true;
            quadAmount ++;
            needSorting = true;
        }
    }

    public void fillVertices(){
        removeAllDeadPackets();
        quadAmount = advPackets.size();
        if(quadAmount >= MAX_QUADS) throw new RuntimeException("packets in advRenderer exceeds MAX_QUAD = 1024 limit, fix asap! " + quadAmount);
        for (int i = 0; i < quadAmount; i++) {
            AdvPacket packet = advPackets.get(i);
            RendererLibrary.addPackToVertices(packet, i, vertices, packet.usedLocalCoords);
        }
    }

    private static final Comparator<AdvPacket> Z_Y_COMPARATOR = (a, b) -> {
        int zCompare = Float.compare(a.zDepth, b.zDepth);
        if (zCompare != 0) return zCompare;
        return Float.compare(b.yRep, a.yRep);
    };
    public void zSorting(){
        advPackets.sort(Z_Y_COMPARATOR);
    }

    public void clear() {
        for(AdvPacket advPacket : advPackets){
            advPacket.alive = false;
        }
        advPackets.clear();
        quadAmount = 0;
        needSorting = true;
    }

    public void removeAllDeadPackets() {
        advPackets.removeIf(packet -> !packet.alive);
    }
}
