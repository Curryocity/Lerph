package data;

import block.Block;
import block.BlockLoader;
import component.Sprite;
import core.Game;
import entity.Entity;
import org.joml.Vector4f;
import util.Util;
import renderer.ChunkRenderer;
import renderer.RendererLibrary;

import java.util.*;

import static block.BlockLoader.*;
import static core.Game.BLOCK_SCALE;
import static core.Game.GAME_SCALE;

public class Chunk {

    public final static int blocksPerAxis = 32;
    public final Space spaceLocation;
    public final int chunkX, chunkY;

    private byte[] rawData;
    final byte[] blockMap = new byte[blocksPerAxis * blocksPerAxis]; //store actual block id
    final int[] runTimeBlockMap = new int[blocksPerAxis * blocksPerAxis]; //store block sprite id (include appearance state)

    //each entity data took 4 bytes: entityType, PositionXY, State & Properties (2 bytes)
    // 4 quadrant ( 16 x 16 ), bottomLeft, bottomRight, topLeft, topRight
    @SuppressWarnings("unchecked")
    private final List<Integer>[] entityInitData = new List[]{
            new ArrayList<>(),
            new ArrayList<>(),
            new ArrayList<>(),
            new ArrayList<>()
    };

    private boolean shouldSave = true, isMapEmpty = true, isLoaded = false, shouldRegenMesh = true;

    private final List<Entity> entities = new ArrayList<>(); // actual entities could move around, cross chunk borders
    private final List<Entity> deadEntities = new ArrayList<>();

    private final float[] mesh = new float[ChunkRenderer.VERTICES_PER_CHUNK * RendererLibrary.getVertexSize(false)];

    public Chunk(Space spaceLocation, int chunkX, int chunkY, byte[] rawData){
        this.spaceLocation = spaceLocation;
        this.chunkX = chunkX ;
        this.chunkY = chunkY;
        this.rawData = rawData;
    }

    private void deserialize(){

        // Check for empty or null data
        if (rawData == null || rawData.length == 0) {
            isMapEmpty = true;
            Arrays.fill(blockMap, (byte) 0);
            for (List<Integer> entityInitDatum : entityInitData) {
                entityInitDatum.clear();
            }
            return;
        }

        int pointer = 0;
        int paletteSize = rawData[pointer++] & 0xFF;
        byte[] palettes = new byte[paletteSize];
        System.arraycopy(rawData, pointer, palettes, 0, paletteSize);
        pointer += paletteSize;

        // Calculate palette ID bit size = ceil(log2(paletteSize))
        int paletteID_BitSize = Integer.SIZE - Integer.numberOfLeadingZeros(paletteSize - 1);

        System.out.println("paletteBitSize: " + paletteID_BitSize);
        System.out.println("blockIDtoPaletteID: "+ Arrays.toString(palettes));
        System.out.println(Arrays.toString(rawData));

        if(pointer == rawData.length){
            if(palettes[0] == 0){
                isMapEmpty = true;
            }else {
                Arrays.fill(blockMap, palettes[0]);
            }

            return;
        }

        if(paletteSize > 1) {

            // Read rowUniformInfo (4 bytes)
            int rowUniformInfo = ((rawData[pointer++] & 0xFF) << 24)
                    | ((rawData[pointer++] & 0xFF) << 16)
                    | ((rawData[pointer++] & 0xFF) << 8)
                    | (rawData[pointer++] & 0xFF);

            // Deserialize blockMap
            byte carry;
            int bitPointer = 0;
            for (int y = 0; y < blocksPerAxis; y++) {
                boolean isRowUniform = (rowUniformInfo & (1 << (31 - y))) != 0;


                int iterations = isRowUniform ? 1 : blocksPerAxis;

                // Unpack bit-packed data
                for (int x = 0; x < iterations; x++) {
                    carry = (byte) ( ((rawData[pointer] << bitPointer) & 0xFF) >>> (8 - paletteID_BitSize));
                    bitPointer += paletteID_BitSize;
                    if (bitPointer >= 8) {
                        bitPointer -= 8;
                        pointer++;
                        if (bitPointer > 0) {
                            carry |= (byte) ((rawData[pointer] & 0xFF) >>> (8 - bitPointer));
                        }
                    }
                    try {
                        blockMap[y * blocksPerAxis + x] = palettes[carry & 0xFF];
                    } catch (ArrayIndexOutOfBoundsException e) {
                        throw new RuntimeException("Carry: " + carry + ", x: " + x + ", y: " + y + "Pointer: " + pointer);
                    }

                }

                if (isRowUniform) {
                    Arrays.fill(blockMap, y * blocksPerAxis + 1, (y + 1) * blocksPerAxis, blockMap[y * blocksPerAxis]);
                }
            }

            if(bitPointer > 0){
                pointer++;
            }

        }else {
            Arrays.fill(blockMap, palettes[0]);
        }

        // no entity
        if(pointer == rawData.length) return;

        int[] entityCount = new int[4];

        for (int quadrant = 0; quadrant < 4; quadrant++) {
            entityCount[quadrant] = rawData[pointer++] & 0xFF;
        }

        // Deserialize entity data
        for (int quadrant = 0; quadrant < 4; quadrant++) {

            entityInitData[quadrant].clear();

            for (int i = 0; i < entityCount[quadrant]; i++) {

                int entityType = rawData[pointer++] & 0xFF;
                int x = (rawData[pointer] >>> 4) & 0x0F;
                int y = rawData[pointer++] & 0x0F;
                int state = rawData[pointer++] & 0xFF | rawData[pointer++] & 0xFF;

                System.out.println("deserialized entity: " + entityType + " " + x + " " + y + " " + state + ". Quadrant: " + quadrant);

                int entityData = (entityType << 24)
                        | (x << 20)
                        | (y << 16)
                        | (state & 0xFFFF);
                entityInitData[quadrant].add(entityData);
            }

        }

        loadEntities();

        System.out.println("blockMap deserialized: "+ Arrays.toString(blockMap));

        shouldSave = false;

    }

    private void serialize() {

        Set<Byte> palettesSet = new HashSet<>();
        for (byte b : blockMap) {
            palettesSet.add(b);
        }

        int paletteSize = palettesSet.size();

        serializeEntities();

        int entityAmounts = 0;
        for (List<Integer> entityQuadrantData : entityInitData){
            entityAmounts += entityQuadrantData.size();
        }

        // empty chunk
        if (paletteSize <= 1 && blockMap[0] == 0 && entityAmounts == 0) {
            rawData = new byte[0];
            return;
        }

        // Calculate palette ID bit size = ceil(log2(paletteSize))
        int paletteID_BitSize = Integer.SIZE - Integer.numberOfLeadingZeros(paletteSize - 1);

        // upper bound estimate, would truncate later
        rawData = new byte[1 + paletteSize + 4 + blockMap.length * paletteID_BitSize / 8 + 4 + entityAmounts * 4];
        // The format is: paletteSize + palettesData + rowEmptyInfo + blockMap + entityQuadrantSize + entityData

        int pointer = 0;
        rawData[pointer++] = (byte) paletteSize;
        for (byte palette : palettesSet) {
            rawData[pointer++] = palette;
        }

        // otherwise a single blockID could represent the whole blockMap, which is the only palette
        if (paletteSize > 1) {

            int rowUniformInfoPointer = pointer;
            pointer += 4; // 32 bits to store whether a y level of chunk is consist of a single block type

            byte[] blockIDtoPaletteID = new byte[256];
            byte paletteID = 0;
            for (byte palette : palettesSet) {
                blockIDtoPaletteID[palette & 0xFF] = paletteID;
                paletteID++;
            }
            System.out.println("blockIDtoPaletteID: " + Arrays.toString(blockIDtoPaletteID));
            System.out.println(Arrays.toString(blockMap));

            // store blockMap with bit packing
            byte carry = 0;
            int bitPointer = 0;
            for (int y = 0; y < blocksPerAxis; y++) {
                boolean isRowUniform = true;
                byte firstBlockID = blockMap[y * blocksPerAxis];
                for (int x = 1; x < blocksPerAxis; x++) {
                    byte thisBlockID = blockMap[y * blocksPerAxis + x];
                    if (thisBlockID != firstBlockID) {
                        isRowUniform = false;
                        break;
                    }
                }

                if (isRowUniform) {
                    int whichByte = y / 8;
                    int whichBit = y % 8;
                    rawData[rowUniformInfoPointer + whichByte] |= (byte) (1 << (7 - whichBit));
                }

                int iterations = isRowUniform ? 1 : blocksPerAxis;

                for (int x = 0; x < iterations; x++) {
                    byte blockID = blockMap[y * blocksPerAxis + x];
                    byte p = blockIDtoPaletteID[blockID & 0xFF];
                    carry |= (byte) ((p << (8 - paletteID_BitSize)) >>> bitPointer);
                    bitPointer += paletteID_BitSize;
                    if (bitPointer >= 8) {
                        bitPointer -= 8;
                        rawData[pointer++] = carry;
                        carry = (byte) (p << (8 - bitPointer));
                    }
                }

            }

            if(bitPointer > 0){
                pointer++;
            }

        }

        if(entityAmounts > 0) {

            // divide entity data into 4 quadrants, so that position XY can be fit into a byte
            int entityQuadrantSizePointer = pointer;
            pointer += 4;

            int temp = 0;

            // store entity data
            for (List<Integer> entityQuadrantData : entityInitData) {

                System.out.println("serializing in quadrant: " + (temp++) + " " + entityQuadrantData.size() + " entities");
                rawData[entityQuadrantSizePointer++] = (byte) entityQuadrantData.size();

                for (int entityData : entityQuadrantData) {
                    rawData[pointer++] = (byte) (entityData >>> 24);
                    rawData[pointer++] = (byte) (entityData >>> 16);
                    rawData[pointer++] = (byte) (entityData >>> 8);
                    rawData[pointer++] = (byte) entityData;
                }
            }
        }

        // truncate
        rawData = Arrays.copyOf(rawData, pointer);

        System.out.println("Chunk serialized: " + spaceLocation.name + " " + chunkX + " " + chunkY + "size: " + rawData.length);
        System.out.println(Arrays.toString(rawData));
    }

    public byte[] getChunkData(){
        if(shouldSave){
            serialize();
            shouldSave = false;
        }
        return rawData;
    }

    public void forceLoadAndSave(){
        deserialize();
        serialize();
    }

    public void load(){
        if(isLoaded) return;
        deserialize();
        isLoaded = true;
        shouldRegenMesh = true;

    }

    public boolean isMapEmpty(){
        if(shouldRegenMesh){
            generateMesh();
        }
        return isMapEmpty;
    }

    public boolean hasEntities(){
        return !entities.isEmpty();
    }

    public void queueRegenMesh(){
        shouldRegenMesh = true;
    }

    private void generateRunTimeBlockMap(){
        if(!shouldRegenMesh) return;

        Chunk theChunkBelow = null;
        boolean lowest = (chunkY == 0);
        if(!lowest){
            theChunkBelow = spaceLocation.getChunk(chunkX, chunkY - 1);
            if(theChunkBelow.isMapEmpty) lowest = true;
        }

        //deal with the bottom, cross chunk section
        if(lowest){
            for (int i = 0; i < blocksPerAxis; i++) {
                int blockID = blockMap[i] & 0xFF;
                if(Block.isObstacle(blockID)) {
                    runTimeBlockMap[i] = blockID_SpriteID_MAP[blockID] + 1;
                }else {
                    runTimeBlockMap[i] = blockID_SpriteID_MAP[blockID];
                }
            }
        }else {
            for (int i = 0; i < blocksPerAxis; i++) {
                int blockID = blockMap[i] & 0xFF;
                if(Block.isObstacle(theChunkBelow.blockMap[blocksPerAxis * (blocksPerAxis - 1) + i]) || !Block.isObstacle(blockID)){
                    runTimeBlockMap[i] = blockID_SpriteID_MAP[blockID];
                }else{
                    runTimeBlockMap[i] = blockID_SpriteID_MAP[blockID] + 1;
                }
            }
        }

        //deal with the non-bottom part of the chunk
        for (int j = 1; j < blocksPerAxis; j++) {
            for (int i = 0; i < blocksPerAxis; i++) {

                int blockID = blockMap[i + blocksPerAxis * j] & 0xFF;

                int theBlockIDBelow = blockMap[i + blocksPerAxis * (j-1)] & 0xFF;
                if(Block.isObstacle(theBlockIDBelow) || !Block.isObstacle(blockID)){
                    runTimeBlockMap[i + blocksPerAxis * j] = blockID_SpriteID_MAP[blockID];
                }else{
                    runTimeBlockMap[i + blocksPerAxis * j] = blockID_SpriteID_MAP[blockID] + 1;
                }
            }
        }

    }

    public float[] generateMesh(){
        if(!isLoaded) load();
        if(!shouldRegenMesh) return mesh;
        isMapEmpty = Util.isArrayFullOfZeros(blockMap);

        generateRunTimeBlockMap();
        //generate mesh
        int offset = 0;
        final int spriteSheetID = RendererLibrary.getTextureID(BlockLoader.getBlockSheetTexture());

        final float blockSize = Game.getScale(BLOCK_SCALE);
        float blockWidth, blockHeight;
        final float[] xOffsets = {0f, 1f, 1f, 0f};
        final float[] yOffsets = {0f, 0f, 1f, 1f};

        for(int y = 0; y < blocksPerAxis; y++){
            for(int x = 0; x < blocksPerAxis; x++) {

                int spriteID = runTimeBlockMap[y * blocksPerAxis + x];

                Sprite blocksprite = blockSprites[spriteID];
                Vector4f dimension = blocksprite != null ? blocksprite.txcDimensions : null;

                blockWidth = blockSize;
                blockHeight = blockSize;


                final float startX = (x + chunkX * blocksPerAxis) * blockSize;
                float startY = (y + chunkY * blocksPerAxis) * blockSize;

                float yRep = y + chunkY * blocksPerAxis + 1f;
                if(isSpriteObstacle[spriteID]){
                    startY += 6f/16 * blockSize;
                }else if(spriteID > 0 && isSpriteObstacle[spriteID - 1]){
                    blockHeight += 6f/16 * blockSize;
                }

                for (int i = 0; i < 4; i++) {
                    float xAdd = xOffsets[i], yAdd = yOffsets[i];

                    mesh[offset + 0] = startX + xAdd * blockWidth;
                    mesh[offset + 1] = startY + yAdd * blockHeight;
                    mesh[offset + 2] = ChunkRenderer.floorZDepth;

                    if(blocksprite != null) {
                        mesh[offset + 3] = dimension.x + xAdd * dimension.z;
                        mesh[offset + 4] =  dimension.y + yAdd * dimension.w;
                        mesh[offset + 5] = spriteSheetID;
                    }else{
                        mesh[offset + 3] = 0;
                        mesh[offset + 4] = 0;
                        mesh[offset + 5] = 0;
                    }

                    mesh[offset + 6] = yRep;

                    offset += 7;
                }
            }
        }
        shouldRegenMesh = false;

        return mesh;
    }

    public int getBlockID(int x, int y){
        if(x >= blocksPerAxis || y >= blocksPerAxis || x < 0 || y < 0) return 0;
        return blockMap[y * blocksPerAxis + x] & 0xFF;
    }

    public static int getChunkCoord(double axis){
        return (int) Math.floor(axis / blocksPerAxis);
    }

    //single block update, could be used in all cases
    void edit(int blockID, int xPos, int yPos){
        if(xPos >= blocksPerAxis || yPos >= blocksPerAxis || xPos < 0 || yPos < 0) throw new IllegalArgumentException("sub-chunk coordinate out of bound");
        blockMap[yPos * blocksPerAxis + xPos] = (byte) blockID;
        shouldSave = true;
        shouldRegenMesh = true;
    }

    // TODO: rectangle, faster I think
    void edit(int blockID, int x1, int x2, int y1, int y2){
        if(x1 >= blocksPerAxis || x2 >= blocksPerAxis || y1 >= blocksPerAxis || y2 >= blocksPerAxis || x1 < 0 || x2 < 0 || y1 < 0 || y2 < 0){
            throw new IllegalArgumentException("sub-chunk coordinate out of bound");
        }
        for(int y = y1 ; y <= y2; y++){
            for(int x = x1; x <= x2; x++){
                blockMap[y * blocksPerAxis + x] = (byte) blockID;
            }
        }
        shouldSave = true;
        shouldRegenMesh = true;
    }

    // level editor placing that would be saved
    void placeEntity(int entityID, int xPos, int yPos){
        if(xPos >= blocksPerAxis || yPos >= blocksPerAxis || xPos < 0 || yPos < 0) throw new IllegalArgumentException("sub-chunk coordinate out of bound");

        System.out.println("Entity placed: " + entityID + " at " + xPos + " " + yPos + " in " + chunkX + " " + chunkY);
        Entity entity = Entity.newEntity(entityID);
        entity.init(spaceLocation, chunkX * blocksPerAxis + xPos + 0.5, chunkY * blocksPerAxis + yPos + 0.5, 0);
        entity.respawn();
        summon(entity);

        shouldSave = true;
    }

    private final int[] quadrantAddX = {0, blocksPerAxis/2, 0, blocksPerAxis/2};
    private final int[] quadrantAddY = {0, 0, blocksPerAxis/2, blocksPerAxis/2};
    void loadEntities(){
        entities.clear();

        for (int i = 0; i < 4; i++) {
            List<Integer> entityQuadrantData = entityInitData[i];
            int addX = quadrantAddX[i];
            int addY = quadrantAddY[i];

            for (int entityData : entityQuadrantData) {
                int entityID = (entityData >>> 24) & 0xFF;
                int x = (entityData >>> 20) & 0x0F;
                int y = (entityData >>> 16) & 0x0F;
                int state = entityData & 0xFFFF;

                System.out.println("Load entity: " + entityID + " at " + x + " " + y + " in " + chunkX + " " + chunkY);

                Entity entity = Entity.newEntity(entityID);
                entity.init(spaceLocation, chunkX * blocksPerAxis + x + addX + 0.5, chunkY * blocksPerAxis + y + addY + 0.5, state);

                entity.respawn();

                summon(entity);
            }
        }
    }

    void serializeEntities(){
        // Clear existing entityInitData
        for (int i = 0; i < 4; i++) {
            entityInitData[i].clear();
        }

        if(!entities.isEmpty()) {
            System.out.println("serializing " + entities.size() + " entities");
        }

        for (Entity entity : entities) {
            int entityID = entity.getEntityID();
            int chunkLocalX = (int) entity.initX - chunkX * blocksPerAxis;
            int chunkLocalY = (int) entity.initY - chunkY * blocksPerAxis;
            int state = entity.getState();

            int quadrantIndex = (chunkLocalX >= blocksPerAxis / 2 ? 1 : 0) +
                    (chunkLocalY >= blocksPerAxis / 2 ? 2 : 0);

            int addX = quadrantAddX[quadrantIndex];
            int addY = quadrantAddY[quadrantIndex];

            int localX = chunkLocalX - addX;
            int localY = chunkLocalY - addY;

            System.out.println("serialize entity: " + entityID + " at " + localX + " " + localY + " in " + chunkX + " " + chunkY + "quadrant: " + quadrantIndex + "state " + state);

            int entityData = (entityID << 24) | (localX << 20) | (localY << 16) | state;
            entityInitData[quadrantIndex].add(entityData);

        }
    }

    public List<Entity> getEntitiesAt(double x, double y){
        if(x >= blocksPerAxis || y >= blocksPerAxis || x < 0 || y < 0) throw new IllegalArgumentException("sub-chunk coordinate out of bound");
        double globalX = x + chunkX * blocksPerAxis;
        double globalY = y + chunkY * blocksPerAxis;
        List<Entity> entitiesAt = new ArrayList<>();
        for (Entity entity : entities) {
            if(Math.abs(entity.x - globalX) < 0.5 && Math.abs(entity.y - globalY) < 0.5){
                entitiesAt.add(entity);
            }
        }
        return entitiesAt;
    }

    // no killing entity in level editor
    public boolean removeEntity(Entity entity){
        boolean successRemove = entities.remove(entity);
        if(successRemove){
            entity.isAlive = false;
            entity.setShown(false);
            shouldSave = true;
        }
        return successRemove;
    }

    /**
     InGame section
     **/


    void restart(){
        entities.addAll(deadEntities);
        deadEntities.clear();
        for(Entity entity : entities){
            entity.respawn();
        }
    }

    private boolean inSimulation = false;
    private boolean isRendering = false;
    public void setRendered(boolean renderQ){
        if(isRendering == renderQ) return;

        isRendering = renderQ;
        for(Entity entity : entities){
            entity.setShown(renderQ);
        }
    }

    public void setSimulated(boolean simulateQ){
        inSimulation = simulateQ;
    }

    public void summon(Entity entity){
        entities.add(entity);
        entity.setShown(isRendering);
    }

    public void tick(){
        if(!inSimulation) return;

        for(int i = 0; i < entities.size(); i++){
            Entity entity = entities.get(i);
            if(entity.isAlive){
                entity.tick();
            }else{
                entity.setShown(false);
                entities.set(i, entities.get(entities.size() - 1));
                entities.removeLast();
                deadEntities.add(entity);
            }

        }
    }

    public void renderEntities(float alpha){
        for(Entity entity : entities){
            entity.frame(alpha);
        }
    }

    public static void entityCrossChunk(Entity entity, Chunk from, Chunk to){
        if(from == null || to == null) throw new IllegalArgumentException("from or to chunk is null");
        if(from == to) {
            System.out.println("Warning, crossed chunk with same chunk");
            return;
        }
        if(!from.entities.remove(entity)){
            System.out.println("Warning: Trying to move an non-existing entity");
            return;
        }
        to.entities.add(entity);
    }

    public List<Entity> getAllEntities(){
        return entities;
    }

}
