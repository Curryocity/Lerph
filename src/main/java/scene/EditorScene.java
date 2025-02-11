package scene;

import block.Block;
import block.BlockLoader;
import component.AdvPacket;
import core.Application;
import core.Game;
import data.Chunk;
import data.Map;
import data.Space;
import entity.Entity;
import entity.EntityLoader;
import manager.InputBind.InputType;
import manager.KeyInput;

import manager.MouseInput;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import component.Line;
import renderer.*;
import ui_basic.VisualBox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static core.Application.levelEditorImGui;
import static core.Game.*;
import static manager.MouseInput.LMB;
import static ui_basic.ColorPalette.white;

public class EditorScene extends Scene{

    final public static float defaultZoom = 0.4f, zoomSensitivity = 0.015f, cameraSpeed = 0.6f;
    final public static int cameraBound = 10;

    public ImGuiEditor imGui;
    Vector4f blockGridColor = new Vector4f(0.85f, 0.85f, 0.85f, 1);
    Vector4f chunkGridColor = new Vector4f(0.4f, 0.4f, 0.4f, 1);

    private int mouseX, mouseY;
    public boolean blockGridOn = true, chunkBorderOn = true;

    public EditorScene(){
        map = Map.load("Default");
        map.switchSpace(map.createSpace("default", 16, 16).name, false);
    }

    public EditorScene(Map map){
        this.map = map;
        if(map.getCurrentSpace() == null) {
            map.switchDefaultSpace(false);
        }else {
            map.refreshCurrentSpace(false);
        }
    }

    double setupX = -1, setupY = -1;
    public EditorScene(Space space, double x, double y){
        if(space == null) return;
        map = Map.load(space.srcMap.name);
        map.switchSpace(space.name, false);
        setupX = x;
        setupY = y;
    }


    @Override
    public void init(){
        initialized = true;
        zoom = defaultZoom;
        setupCharacters();
        if(setupX == -1 || setupY == -1) {
            resetCameraPosition();
        }else {
            camera.setWorldPosition(setupX, setupY);
        }
        refresh();
    }

    @Override
    public void refresh() {
        imGui = levelEditorImGui;
        imGui.bindLevelEditorScene(this);
        renderSystem.chunkRenderer.syncScene(this);
        spawnCharacter.show();
        spawnShadow.show();
    }

    double lastAlpha = 0;
    @Override
    public void update(float alpha) {

        if(playQueue){
            play();
        }

        if(KeyInput.useQueue(InputType.PAUSE)){
            switchScene(new TitleScene());
        }

        if(currentSpace() != null){

            mouseX = (int) Math.floor(MouseInput.getMouseWorldCoords().x);
            mouseY = (int) Math.floor(MouseInput.getMouseWorldCoords().y);

            cameraMovement(alpha);
            spawnPointStuffs();
            blockSelectAndPlacement();
            entityPlacement();
            entitySelection();

            if(chunkBorderOn){ generateGrid(32, chunkGridColor, 3,50); }
            if(blockGridOn)  { generateGrid(1, blockGridColor, 2,1); }
        }

        renderSystem.render(alpha);
        imGui.render();
    }

    boolean playQueue = false;
    private void play(){
        Game.switchScene(new InGameScene(map), true);
    }


    public void setupCharacters(){
        spawnCharacter = new AdvPacket(player.idleAnimation[0], ChunkRenderer.floorZDepth + 0.01f);
        spawnShadow = new AdvPacket(player.shadowSprite, ChunkRenderer.floorZDepth + 0.005f);
        spawnPreview = new AdvPacket(player.idleAnimation[0], ChunkRenderer.floorZDepth + 0.01f);
        spawnPreviewShadow = new AdvPacket(player.shadowSprite, ChunkRenderer.floorZDepth + 0.005f);
        spawnPreview.setColors(1, 1, 1, 0.6f);
        selectedBox.setUsedLocalCoords(true);
    }

    public void cameraMovement(float alpha){
        float timeDelta = (float) (alpha - lastAlpha);
        if(timeDelta < 0.0f) timeDelta += 1;
        lastAlpha = alpha;

        double[] movementVec = KeyInput.normWasdVec();
        camera.position.x += (float) movementVec[0] * cameraSpeed * timeDelta * block_px / (float) Math.pow(zoom, 0.25);
        camera.position.y +=  (float) movementVec[1] * cameraSpeed * timeDelta * block_px / (float) Math.pow(zoom, 0.25);

        float chunkPixelSize = Chunk.blocksPerAxis * block_px * zoom;

        camera.position.x = Math.min(Math.max(-cameraBound * chunkPixelSize, camera.position.x),
                (map.getCurrentSpace().chunkLengthX + cameraBound) * chunkPixelSize);
        camera.position.y = Math.min(Math.max(-cameraBound * chunkPixelSize, camera.position.y),
                (map.getCurrentSpace().chunkLengthY + cameraBound) * chunkPixelSize);
    }

    private int lastPlaceX = -1, lastPlaceY = -1;
    private boolean hasPlaced = false;
    AdvPacket blockPreview = new AdvPacket(0.8f, 0.8f, 0.8f, 0.7f, 1, 1, 12f, true);
    public void blockSelectAndPlacement(){
        int selectedBlockID = imGui.selectedBlock;

        if(selectedBlockID < 0){
            blockPreview.hide();
            return;
        }else{
            settingMode = NONE;
        }

        if(!mouseOnValidChunks()) {
            blockPreview.hide();
            return;
        }

        if(selectedBlockID == 0){
            blockPreview.sprite = null;
            blockPreview.setColors(0.8f, 0.8f, 0.8f, 0.7f);
        }else {
            blockPreview.sprite = BlockLoader.blockSprites[BlockLoader.blockID_SpriteID_MAP[selectedBlockID]];
            blockPreview.setColors(1, 1, 1, 0.7f);
        }

        blockPreview.setPos(mouseX + 0.5f, mouseY + 0.5f);
        blockPreview.show();

        if (MouseInput.isPressed(LMB)) {
            if (!(mouseX == lastPlaceX && mouseY == lastPlaceY)) {
                addSelected(mouseX, mouseY);
                hasPlaced = true;
            }
        } else if (hasPlaced) {
            sendPlacingPacket();
            lastPlaceX = -1;
            lastPlaceY = -1;
        }


    }

    AdvPacket entityPreview = new AdvPacket(1f, 1f, 1f, 0.7f, 1, 1, 12f, true);
    private void entityPlacement() {
        int selectedEntityID = imGui.selectedEntity;

        if(selectedEntityID < 0){
            entityPreview.hide();
            return;
        }else{
            settingMode = NONE;
        }

        if (!mouseOnValidChunks()){
            entityPreview.hide();
            return;
        }

        entityPreview.sprite = EntityLoader.entitySprites.get(EntityLoader.entityID_SpriteID_MAP[selectedEntityID]);
        entityPreview.setPos(mouseX + 0.5f, mouseY + 0.5f);
        entityPreview.show();

        if (MouseInput.useQueue(LMB)){
            currentSpace().placeEntity(selectedEntityID, mouseX, mouseY);
        }

    }

    VisualBox selectedBox = new VisualBox(0.5, 0.5, 3, 90, white, null);
    List<Entity> selectedEntities = new ArrayList<>();
    int selectedEntityIndex = 0;
    Entity theSelectedEntity = null;
    private void entitySelection() {
        if(imGui.selectedBlock >= 0 || imGui.selectedEntity >= 0){
            theSelectedEntity = null;
            selectedBox.hide();
            return;
        }

        if(MouseInput.useQueue(LMB)){
            selectedEntities = currentSpace().getEntitiesAt(MouseInput.getMouseWorldCoords().x , MouseInput.getMouseWorldCoords().y);
            System.out.println("clicked");
            if(selectedEntities != null && !selectedEntities.isEmpty()) {
                System.out.println("select");
                selectedEntityIndex = 0;
                Entity pendingEntity = selectedEntities.get(selectedEntityIndex);
                if(pendingEntity != theSelectedEntity){
                    theSelectedEntity = pendingEntity;
                    selectedBox.show();
                }else {
                    theSelectedEntity = null;
                    selectedBox.hide();
                }
            }else {
                theSelectedEntity = null;
                selectedBox.hide();
            }

        }

        if(theSelectedEntity == null) return;

        selectedBox.setPos(theSelectedEntity.x, theSelectedEntity.y);

        if(KeyInput.useQueue(GLFW.GLFW_KEY_BACKSPACE)){
            currentSpace().removeEntity(theSelectedEntity);
            selectedBox.hide();
        }


    }


    public final static int NONE = 0, SPAWNPOINT = 1, PLAYTEST = 2;
    public int settingMode = NONE;
    private AdvPacket spawnCharacter, spawnShadow;
    private AdvPacket spawnPreview, spawnPreviewShadow;

    public void spawnPointStuffs(){
        spawnCharacter.setPos(map.getCurrentSpace().getSpawnX(), map.getCurrentSpace().getSpawnY());
        spawnShadow.setPos(map.getCurrentSpace().getSpawnX(), map.getCurrentSpace().getSpawnY());

        if(settingMode == NONE){
            spawnPreview.hide();
            spawnPreviewShadow.hide();
            return;
        }

        if(mouseOnValidChunks()) {
            spawnPreview.setPos(mouseX + 0.5f, mouseY + 0.5f);
            spawnPreviewShadow.setPos(mouseX + 0.5f, mouseY + 0.5f);
            spawnPreview.show();
            spawnPreviewShadow.show();

            if (MouseInput.useQueue(LMB)) {
                if(settingMode == SPAWNPOINT) {
                    if (currentSpace().setSpawnPoint(mouseX, mouseY)) {
                        settingMode = NONE;
                    } else {
                        System.out.println("Invalid spawn point");
                    }
                }else if(settingMode == PLAYTEST){
                    if(Block.isGround(currentSpace().getBlockID(mouseX, mouseY))){
                        settingMode = NONE;
                        Game.switchScene(new InGameScene(currentSpace(), mouseX + 0.5, mouseY + 0.5), false);
                        System.out.println("Valid PlayTest Point");
                    }else{
                        System.out.println("Invalid PlayTest Point");
                    }
                }
            }
        }else {
            if (MouseInput.useQueue(LMB)) {
                settingMode = NONE;
            }
            spawnPreview.hide();
            spawnPreviewShadow.hide();
        }

    }

    public void generateGrid(int blockSpacing, Vector4f color, float width, float zDepth){
        Vector2f center = camera.worldCoords();
        float blockSpacingInv = 1.0f / (zoom * block_px);

        float startX = (float)  Math.floor(center.x - Application.viewPortWidth * blockSpacingInv);
        startX -= startX % blockSpacing + blockSpacing;
        if(startX < 0) startX = 0;

        float endX = (float) Math.ceil(center.x + Application.viewPortWidth * blockSpacingInv);
        endX += endX % blockSpacing + blockSpacing;
        float maxX = map.getCurrentSpace().chunkLengthX * Chunk.blocksPerAxis;
        if(endX > maxX) endX = maxX;

        float startY = (float)  Math.floor(center.y - Application.viewPortHeight * blockSpacingInv);
        startY -= startY % blockSpacing + blockSpacing;
        if(startY < 0) startY = 0;

        float endY = (float) Math.ceil(center.y + Application.viewPortHeight * blockSpacingInv);
        endY += endY % blockSpacing + blockSpacing;
        float maxY = map.getCurrentSpace().chunkLengthY * Chunk.blocksPerAxis;
        if(endY > maxY) endY = maxY;

        for (float x = startX; x <= endX; x += blockSpacing) {
            Line gridLine = new Line(x, startY, x, endY, color, zDepth, width);
            gridLine.usedLocalCoords = true;
            LineRenderer.addImmediateLine(gridLine);
        }

        for (float y = startY; y <= endY; y += blockSpacing) {
            Line gridLine = new Line(startX, y, endX, y, color, zDepth, width);
            gridLine.usedLocalCoords = true;
            LineRenderer.addImmediateLine(gridLine);
        }
    }

    @Override
    public void exit() {
        settingMode = NONE;
        imGui.selectedBlock = -1;
        imGui.selectedEntity = -1;
        imGui.closeAllPopups();
        renderSystem.reset();
        saveCurrentMap();
    }

    public void scrolled(float amount){
        setZoom(zoom/defaultZoom * (1 + amount * zoomSensitivity) , MouseInput.getMouseWorldCoords().x , MouseInput.getMouseWorldCoords().y);
    }
    final public float minZoom = 0.6f, maxZoom = 5f;
    public void setZoom(float rawZoom, float anchorX, float anchorY) {
        float currentCameraX = camera.worldCoords().x;
        float currentCameraY = camera.worldCoords().y;
        float prevZoom = this.zoom;
        this.zoom = Math.min(Math.max(rawZoom, minZoom), maxZoom) * defaultZoom;
        float invZoomDelta = prevZoom / this.zoom ;
        camera.setWorldPosition(invZoomDelta * (currentCameraX - anchorX) + anchorX,invZoomDelta * (currentCameraY - anchorY) + anchorY);
        renderSystem.chunkRenderer.queueResizeEvent();
    }

    private final int[] selectedX = new int[256];
    private final int[] selectedY = new int[256];
    int pointer = 0;
    private final List<AdvPacket> highlight = new ArrayList<>();
    public void addSelected(int x, int y){
        if(x < 0 || y < 0 || x >= map.getCurrentSpace().chunkLengthX * Chunk.blocksPerAxis || y >= map.getCurrentSpace().chunkLengthY * Chunk.blocksPerAxis) return;

        lastPlaceX = x;
        lastPlaceY = y;

        for (int i = 0; i < pointer; i++) {
            if(selectedX[i] == x && selectedY[i] == y) return;
        }

        selectedX[pointer] = x;
        selectedY[pointer] = y;
        AdvPacket highlightBlock = new AdvPacket(1.0f, 0.65f, 0.0f, 0.5f, 1,1, 11, true);
        highlightBlock.setPos(x + 0.5f, y + 0.5f);
        highlight.add(highlightBlock);
        renderSystem.activate((highlightBlock));
        pointer ++;

        if(pointer > 255){
            sendPlacingPacket();
        }
    }

    public void sendPlacingPacket(){
        currentSpace().edit(levelEditorImGui.selectedBlock, Arrays.copyOfRange(selectedX, 0, pointer),  Arrays.copyOfRange(selectedY, 0, pointer));
        for(AdvPacket item : highlight){
            item.alive = false;
        }
        highlight.clear();
        pointer = 0;
        hasPlaced = false;
    }

    public void resetCameraPosition(){
        if(currentSpace() == null) camera.setWorldPosition(16,16);
        else camera.setWorldPosition(currentSpace().getSpawnX(), currentSpace().getSpawnY());
    }

    public boolean mouseOnValidChunks(){
        if(currentSpace() == null) return false;
        return !(mouseX < 0 || mouseY < 0 || mouseX >= map.getCurrentSpace().chunkLengthX * Chunk.blocksPerAxis || mouseY >= map.getCurrentSpace().chunkLengthY * Chunk.blocksPerAxis);
    }

    public void saveCurrentMap(){
        if(map != null) map.save();
    }
}
