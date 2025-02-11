package renderer;

import component.AdvPacket;
import manager.AssetPool;
import component.StdPacket;

import java.util.ArrayList;
import java.util.List;
import static renderer.RendererLibrary.MAX_QUADS;

public class RenderSystem {

    private final List<StdRenderer> stdBatches;
    private final List<StdPacket> stdPackets;
    private final AdvRenderer advRenderer;
    public final ChunkRenderer chunkRenderer;

    public Shader shader;

    public RenderSystem(){
        RendererLibrary.init();
        shader = AssetPool.getShader("assets/shaders/default.glsl");
        advRenderer = new AdvRenderer();
        chunkRenderer = new ChunkRenderer(null);
        stdBatches = new ArrayList<>(2);
        stdPackets = new ArrayList<>();
        LineRenderer.init();
    }

    public void activate(StdPacket packet){
        if(!packet.alive){
            stdPackets.add(packet);
            packet.alive = true;
        }
    }

    public void activate(AdvPacket advPacket){
        advRenderer.activatePacket(advPacket);
    }

    public void distribute(){
        stdPackets.removeIf(packet -> !packet.alive);
        int i = 0;

        for (StdRenderer stdRenderer : stdBatches) {
            stdRenderer.reset();
        }

        for (StdRenderer batch : stdBatches) {
            if (batch.hasRoom()) {
                while (batch.hasRoom() && i < stdPackets.size()) {
                    StdPacket packet = stdPackets.get(i);
                    batch.addRenderPack(packet);
                    i++;
                }
            }
            if (i >= stdPackets.size()) {
                break; // All packets have been processed
            }
        }

        // Calculate remaining packets to be batched
        int packetLeft = stdPackets.size() - i;
        int newBatchesAmount = (int) Math.ceil((double) packetLeft / MAX_QUADS);

        for (int j = 0; j < newBatchesAmount; j++) {
            StdRenderer newBatch = new StdRenderer(this);
            newBatch.init();
            stdBatches.add(newBatch);

            for (int k = 0; k < MAX_QUADS && i < stdPackets.size(); k++) {
                StdPacket packet = stdPackets.get(i);
                newBatch.addRenderPack(packet);
                i++;
            }
        }
    }


    public void render(float alpha){
        LineRenderer.render();
        chunkRenderer.render(alpha);
        distribute();
        for (StdRenderer batch : stdBatches) {
            batch.render();
        }
        advRenderer.render();
    }

    public void reset(){
        shader = AssetPool.getShader("assets/shaders/default.glsl");
        stdBatches.clear();
        for(StdPacket stdPacket : stdPackets){
            stdPacket.alive = false;
        }
        stdPackets.clear();
        advRenderer.clear();
        chunkRenderer.reset();
        LineRenderer.clear();
    }

    public void removeAllDeadPackets(){
        stdPackets.removeIf(packet -> !packet.alive);
        advRenderer.removeAllDeadPackets();
        LineRenderer.removeAllDeadPackets();

    }

}
