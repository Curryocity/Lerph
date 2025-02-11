package data;

import block.Block;
import core.Application;
import entity.Entity;

import java.io.*;
import java.util.List;

import static data.Chunk.getChunkCoord;

public class Space {
    public static final String fileExtension = ".lps";
    public static final String spaceDirectory = "spaces";
    public final Map srcMap;
    public int version;
    public int chunkLengthX, chunkLengthY;
    public Chunk[] chunks;
    public String name, filePath;
    private boolean initialized, modified;

    private int spawnX, spawnY;

    public Space(Map srcMap, String name, int chunkLengthX, int chunkLengthY){
        if(chunkLengthX < 1 || chunkLengthY < 1 || chunkLengthX > 16 || chunkLengthY > 16){
            throw new IllegalArgumentException("Chunk size parameters must be between 1 and 16");
        }
        this.srcMap = srcMap;
        this.name = name;
        this.chunkLengthX = chunkLengthX;
        this.chunkLengthY = chunkLengthY;
        chunks = new Chunk[this.chunkLengthX * this.chunkLengthY];
        for (int y = 0; y < this.chunkLengthY; y++) {
            for(int x = 0; x < this.chunkLengthX; x++){
                chunks[this.chunkLengthX * y + x] = new Chunk(this, x, y, null);
            }
        }
        filePath = srcMap.directoryPath + "/" + spaceDirectory + "/" + name + fileExtension;
        initialized = true;
        modified = true;
    }

    public Space(Map srcMap, String name) {
        this.srcMap = srcMap;
        this.name = name;
        filePath = srcMap.directoryPath + "/" + spaceDirectory + "/" + name + fileExtension;
    }

    public void load(){
        if(initialized){
            restart();
            return;
        }

        try (DataInputStream dis = new DataInputStream(new FileInputStream(filePath))) {
            // Read space file version
            int header = dis.readInt();
            version = header >>> 8;
            int chunkSize = header & 0xFF;
            this.chunkLengthX = ((chunkSize >>> 4) & 0x0F) + 1;
            this.chunkLengthY = (chunkSize & 0x0F) + 1;

            int spawnProperties = dis.readInt();
            this.spawnX = (spawnProperties >>> 9) & 0x1FF;
            this.spawnY = spawnProperties & 0x1FF;
            // Calculate the total remaining size in the file
            long fileLength = new File(filePath).length();
            int headerSize = 3 + 1 + 4; // 3 bytes for version + 1 byte for chunkSize + 4 bytes for spawnProperties
            int contentDataSize = (int) (fileLength - headerSize);

            if (contentDataSize < 0) {
                throw new RuntimeException("Invalid file size or corrupted file.");
            }

            System.out.println(Application.versionIntToString(version));
            System.out.println("Space size (in chunks): " + chunkLengthX + " " + chunkLengthY);
            System.out.println("Spawn: " + spawnX + " " + spawnY);
            System.out.println("Size of '" + name + fileExtension + "': " + fileLength + " bytes");

            byte[] buffer = new byte[contentDataSize];
            dis.readFully(buffer);

            int totalChunks = chunkLengthX * chunkLengthY;
            chunks = new Chunk[totalChunks];

            // Extract each chunk from the buffer
            int pointer = 0;
            for (int i = 0; i < totalChunks; i++) {
                int chunkDataSize = ((buffer[pointer] & 0xFF) << 8) | (buffer[pointer + 1] & 0xFF); //extracting chunkDataSize metadata as unsigned short

                pointer += 2;
                byte[] chunkData = new byte[chunkDataSize];
                System.arraycopy(buffer, pointer, chunkData, 0, chunkDataSize); //then extract the actual chunk data using metadata as index
                if(chunkDataSize != 0){
                    System.out.println("chunkDataSize: " + chunkDataSize);
                }
                chunks[i] = new Chunk(this, calculateChunkX(i) ,calculateChunkY(i), chunkData);
                pointer += chunkDataSize;
            }
            initialized = true;
            modified = false;

        } catch (IOException e) {e.printStackTrace();}

    }

    private int calculateChunkX(int index) {
        return index - chunkLengthX * (index / chunkLengthX);
    }
    private int calculateChunkY(int index) {
        return index / chunkLengthX;
    }

    public void save(){
        if(!initialized) return;
        int versionInt = Application.getVersionAsInt();
        boolean versionChanged = versionInt != this.version;

        if(!modified && !versionChanged) return;
        try (DataOutputStream dos = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(filePath)))) {

            int chunkSize = (chunkLengthX - 1)  << 4 | (chunkLengthY - 1);
            dos.writeInt((versionInt << 8) | chunkSize);
            dos.writeInt( (spawnX << 9) | spawnY);
            if(chunks == null) throw new RuntimeException("Error: chunks array is null");
            for (Chunk chunk : chunks) {
                if(versionChanged){
                    chunk.forceLoadAndSave();
                }
                byte[] chunkData = chunk.getChunkData();
                if(chunkData != null){
                    // Write the value as an unsigned short
                    dos.writeByte((chunkData.length >> 8) & 0xFF); // Write the high byte
                    dos.writeByte(chunkData.length & 0xFF);       // Write the low byte
                    dos.write(chunkData);           // Write the chunk data itself
                }else {
                    dos.writeByte(0);
                    dos.writeByte(0);
                }
            }

        }catch (IOException e) {e.printStackTrace();}
    }

    public Chunk getChunk(int chunkX, int chunkY){
        if(chunkX < 0 || chunkY < 0 || chunkX >= chunkLengthX || chunkY >= chunkLengthY) return null;
        int index = chunkLengthX * chunkY + chunkX;
        return chunks[index];
    }

    public Chunk getChunkInclude(double x, double y){
        return getChunk(getChunkCoord(x), getChunkCoord(y));
    }

    public Chunk[] getCloseByChunks(double x, double y, double distanceX, double distanceY){
        int minX = Math.max(0, getChunkCoord(x - distanceX));
        int maxX = Math.min(chunkLengthX - 1, getChunkCoord(x + distanceX));
        int minY = Math.max(0, getChunkCoord(y - distanceY));
        int maxY = Math.min(chunkLengthY - 1, getChunkCoord(y + distanceY));

        if(minX >= chunkLengthX || maxX < 0 || minY >= chunkLengthY || maxY < 0) return new Chunk[0];

        Chunk[] neighborChunks = new Chunk[(maxX - minX + 1) * (maxY - minY + 1)];
        int index = 0;
        for(int i = minX; i <= maxX; i++) {
            for(int j = minY; j <= maxY; j++){
                neighborChunks[index ++] = getChunk(i,j);
            }
        }
        return neighborChunks;
    }

    public int getBlockID(double x, double y){  //get the ID for the block that includes the in-game coordinate given

        int xPos = (int) Math.floor(x);
        int yPos = (int) Math.floor(y);

        int chunkX = xPos / Chunk.blocksPerAxis;
        int subChunkX = xPos - chunkX * Chunk.blocksPerAxis;
        int chunkY = yPos / Chunk.blocksPerAxis;
        int subChunkY = yPos - chunkY * Chunk.blocksPerAxis;

        if(chunkX >= chunkLengthX || chunkY >= chunkLengthY || chunkX < 0 || chunkY < 0){
            return 0;
        }

        return getChunk(chunkX, chunkY).getBlockID(subChunkX, subChunkY);
    }

    public void setModified(){
        modified = true;
    }

    public boolean setSpawnPoint(int x, int y){
        if(x < 0 || x > chunkLengthX * Chunk.blocksPerAxis || y < 0 || y > chunkLengthY * Chunk.blocksPerAxis) return false;
        if( !Block.isGround(getBlockID(x, y)) ) return false;
        this.spawnX = x;
        this.spawnY = y;
        setModified();
        return true;
    }

    public double getSpawnX(){ return spawnX + 0.5;}
    public double getSpawnY(){ return spawnY + 0.5;}

    //all cases
    public void edit(int blockID, int[] xPos, int[] yPos){
        if(xPos.length != yPos.length) throw new IllegalArgumentException("Edit packets: x positions sent in does not match y positions in length");
        if(blockID < 0 || blockID > 255) throw new IllegalArgumentException("Edit packets: blockID outside of bound");
        for (int i = 0; i < xPos.length; i++) {
            int chunkX = xPos[i] / Chunk.blocksPerAxis;
            int subChunkX = xPos[i] - chunkX * Chunk.blocksPerAxis;
            int chunkY = yPos[i] / Chunk.blocksPerAxis;
            int subChunkY = yPos[i] - chunkY * Chunk.blocksPerAxis;
            getChunk(chunkX, chunkY).edit(blockID, subChunkX, subChunkY);
        }
        setModified();
    }

    // TODO: rectangle edit
    public void edit(int blockID, int x1, int x2, int y1, int y2){
        if(blockID < 0 || blockID > 255) throw new IllegalArgumentException("Edit packets: blockID outside of bound");

        int chunkX1 = x1 / Chunk.blocksPerAxis;
        int subChunkX1 = x1 - chunkX1 * Chunk.blocksPerAxis;
        int chunkY1 = y1 / Chunk.blocksPerAxis;
        int subChunkY1 = y1 - chunkY1 * Chunk.blocksPerAxis;
        int chunkX2 = x2 / Chunk.blocksPerAxis;
        int subChunkX2 = x2 - chunkX2 * Chunk.blocksPerAxis;
        int chunkY2 = y2 / Chunk.blocksPerAxis;
        int subChunkY2 = y2 - chunkY2 * Chunk.blocksPerAxis;

        for (int y = chunkY1; y <= chunkY2; y++) {
            for(int x = chunkX1; x <= chunkX2; x++){
                if(x == chunkX1 || y == chunkY1){

                } else if (x == chunkX2 || y == chunkY2) {
                    // i'll do this later bruh
                } else{
                    getChunk(x,y).edit(blockID,0,Chunk.blocksPerAxis - 1, 0 , Chunk.blocksPerAxis - 1);
                }
            }
        }
    }

    public void placeEntity(int entityID, int xPos, int yPos){
        if(entityID < 0 || entityID > 255) throw new IllegalArgumentException("Edit packets: entityID outside of bound");
        // TODO: add entity
        int chunkX = xPos / Chunk.blocksPerAxis;
        int subChunkX = xPos - chunkX * Chunk.blocksPerAxis;
        int chunkY = yPos / Chunk.blocksPerAxis;
        int subChunkY = yPos - chunkY * Chunk.blocksPerAxis;
        getChunk(chunkX, chunkY).placeEntity(entityID, subChunkX, subChunkY);

        setModified();
    }

    public List<Entity> getEntitiesAt(double x, double y){
        int chunkX = getChunkCoord(x);
        int chunkY = getChunkCoord(y);
        if(chunkX >= chunkLengthX || chunkY >= chunkLengthY || chunkX < 0 || chunkY < 0) return null;
        double localX = x - chunkX * Chunk.blocksPerAxis;
        double localY = y - chunkY * Chunk.blocksPerAxis;
        return getChunk(chunkX, chunkY).getEntitiesAt(localX, localY);
    }

    public void summon(Entity entity, double x, double y){
        getChunkInclude(x,y).summon(entity);
    }

    public void restart(){
        for (Chunk chunk : chunks) {
            chunk.restart();
        }
    }

    public void exit(){
        for (Chunk chunk : chunks) {
            chunk.setRendered(false);
            chunk.setSimulated(false);
        }
    }

    public void removeEntity(Entity theSelectedEntity) {
        if(getChunkInclude(theSelectedEntity.x, theSelectedEntity.y).removeEntity(theSelectedEntity)){
            setModified();
        }
    }

    public void regenAllChunkMeshes() {
        for (Chunk chunk : chunks) {
            chunk.queueRegenMesh();
        }
    }
}
