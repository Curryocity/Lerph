package scene;

import core.Application;
import core.Game;
import data.Chunk;
import data.Map;
import data.Space;
import manager.KeyInput;
import manager.MouseInput;
import manager.MouseMoveListener;
import ui.PauseMenu;
import ui_basic.Panel;

import java.util.ArrayList;
import java.util.List;

import static component.AdvPacket.GRADIENT_HORIZONTAL;
import static core.Game.*;
import static manager.InputBind.*;
import static core.Game.player;

public class InGameScene extends Scene implements MouseMoveListener {

    Panel bgPanel = new Panel(0.45f, 0.4f, 0.2f, 1.0f,
            0.45f, 0.2f, 0.3f, 1.0f,
            GRADIENT_HORIZONTAL, 0.1f);

    public static final int simulatedDistance = 2; // chunks away for base chunk
    public List<Chunk> simulatedChunks = new ArrayList<>();
    GameState gameState;
    boolean testMode = false;
    PauseMenu pauseMenu = new PauseMenu(this);

    enum GameState{
        INGAME, PAUSED
    }

    public InGameScene(Map world){
        this.map = world;
    }

    double testX, testY;
    public InGameScene(Space space, double testX, double testY){
        this.map = space.srcMap;
        this.testX = testX;
        this.testY = testY;
        testMode = true;
    }

    @Override
    public void init(){
        initialized = true;
        bgPanel.setRelativeToCamera(true);
        if(!testMode){
            map = Map.reload(map);
            map.switchDefaultSpace(false);
            player.spawn(currentSpace());
        }else {
            String spaceName = currentSpace().name;
            System.out.println("play-testing MAP: " + map.name + " " + spaceName);
            map = Map.reload(map);
            map.switchSpace(spaceName, false);
            player.spawn(currentSpace(), testX, testY);
        }
        refresh();
    }

    @Override
    public void refresh() {
        bgPanel.show();
        gameState = GameState.INGAME;
        renderSystem.chunkRenderer.syncScene(this);
        MouseInput.subscribeToMove(this);

        handleResize();
    }

    @Override
    public void update(float alpha) {

        if(gameState == GameState.INGAME && currentSpace() != null){
            player.frame(alpha);
            camera.setWorldPosition(player.frameX, player.frameY);
        }

        if(gameState == GameState.PAUSED){
            pauseMenu.update();
        }else {
            customCursorLogic();
        }

        renderSystem.render(alpha);

        if(KeyInput.useQueue(InputType.PAUSE)){
            if(gameState == GameState.INGAME) {
                pause();
            }
        }
    }

    float maxCursorDuration = 1.5f;
    float cursorTimer = 0;
    private void customCursorLogic() {
        cursorTimer = Math.max(0, cursorTimer - 1);
        if(cursorTimer <= fadeDuration){
            fadeAndHideCursor();
        }else {
            cursor.setTransparency(1);
        }
    }

    float fadeDuration = (float) Application.secToFrame(0.15);
    private void fadeAndHideCursor() {
        if(cursorTimer <= 0){
            cursor.hide();

        }else {
            float alpha = cursorTimer / fadeDuration;
            cursor.setTransparency(alpha);
        }
    }

    @Override
    public void onMouseMove(double xPos, double yPos) {
        if(gameState == GameState.PAUSED) return;

        cursorTimer = (float) Application.secToFrame(maxCursorDuration);
        cursor.show();
    }

    public void enterLevelEditor(){
        Game.switchScene(new EditorScene(currentSpace(), player.x, player.y), false);
    }

    public void pause(){
        cursor.show();
        cursor.setTransparency(1);
        gameState = GameState.PAUSED;
        pauseMenu.show();
    }

    public void unPause(){
        cursor.hide();
        gameState = GameState.INGAME;
        pauseMenu.hide();
    }

    @Override
    public void exit() {
        if(!testMode) saveProgress();
        MouseInput.unSubscribeToMove(this);
        cursor.setTransparency(1);
        renderSystem.reset();
    }

    @Override
    public void handleResize() {
        super.handleResize();
        camera.setWorldPosition(player.frameX, player.frameY);
        bgPanel.refresh();
        pauseMenu.handleResize();
    }

    boolean refreshSimulatedChunks = true;
    int prevPlayerChunkX = 0, prevPlayerChunkY = 0;
    int playerChunkX = 0, playerChunkY = 0;
    public void tick(){
        if(gameState == GameState.INGAME){
            player.tick();
        }

        if(testMode && KeyInput.useQueue(InputType.CHECKPOINT)){
            player.setCheckPointBeneath();
        }

        prevPlayerChunkX = playerChunkX;
        prevPlayerChunkY = playerChunkY;
        playerChunkX = Chunk.getChunkCoord(player.x);
        playerChunkY = Chunk.getChunkCoord(player.y);

        if (prevPlayerChunkX != playerChunkX || prevPlayerChunkY != playerChunkY) {
            refreshSimulatedChunks = true;
        }

        if (refreshSimulatedChunks) {

            for(Chunk simulatedChunk : simulatedChunks){
                simulatedChunk.setSimulated(false);
            }
            simulatedChunks.clear();

            for (int x = playerChunkX - simulatedDistance; x <= playerChunkX + simulatedDistance; x++) {
                for (int y = playerChunkY - simulatedDistance; y <= playerChunkY + simulatedDistance; y++) {
                    Chunk chunk = currentSpace().getChunk(x, y);
                    if (chunk != null) {
                        simulatedChunks.add(chunk);
                        chunk.setSimulated(true);
                    }
                }
            }
            refreshSimulatedChunks = false;

        }

        for(Chunk chunk : simulatedChunks){
            chunk.tick();
        }

    }

    public void saveProgress(){
        System.out.println("Saving progress...");
        map.saveProgress();
    }

}

