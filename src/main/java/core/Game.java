package core;

import block.BlockLoader;
import component.AdvPacket;
import component.Sprite;
import data.Map;
import entity.EntityLoader;
import entity.Player;
import font.FontLoader;
import manager.MouseInput;
import org.joml.Vector2f;
import renderer.RenderSystem;
import scene.*;
import ui.UILoader;

public class Game {

    final public static int original_block_px = 16;
    final public static float screenWidth_BLOCK = 25; // 25 x 14.0625 block
    public static final double viewAngle = Math.toRadians(45);
    public static final float cosineViewAngle = (float) Math.cos(viewAngle), sineViewAngle = (float) Math.sin(viewAngle);
    public static float scaleFactor = 6;
    public static float block_px = original_block_px * scaleFactor;

    public static RenderSystem renderSystem;
    private static Scene currentScene;
    public static Player player;
    public static AdvPacket cursor;

    public Game(){
        renderSystem = new RenderSystem();
        loadGameObjects();
        switchScene(new TitleScene());
    }

    public static void switchScene(Scene scene){
        switchScene(scene, false);
    }

    public static void switchScene(Scene scene, boolean savePrevSceneQ){
        Scene prevScene = currentScene;

        if(prevScene != null) prevScene.exit();
        else savePrevSceneQ = false;

        currentScene = scene;
        if(currentScene.isInitialized()) currentScene.refresh();
        else currentScene.init();

        if(savePrevSceneQ) currentScene.previousScene = prevScene;
    }

    public static Scene getCurrentScene(){
        return currentScene;
    }

    public static void loadGameObjects(){
        Map.reloadSavesFolder();
        FontLoader.LoadFont();
        BlockLoader.load();
        EntityLoader.load();
        UILoader.load();
        player = new Player();
    }

    public void frame(float alpha){
        currentScene.update(alpha);
        cursor.setPos(MouseInput.getX(), MouseInput.getY());
    }

    public void tick(){
        if (currentScene instanceof InGameScene InGameScene) {
            InGameScene.tick();
        }
    }

    public void handleResize(float scale){
        scaleFactor = scale;
        block_px = original_block_px * scale;
        currentScene.handleResize();
        renderSystem.chunkRenderer.queueResizeEvent();
        cursor.setSize(0.2f * scale, 0.2f * scale);
    }

    public void exit(){
        currentScene.exit();
    }

    public static final int BLOCK_SCALE = 0, GAME_SCALE = 1, REGULAR_SCALE = 2;
    public static float getScale(int type){
        return switch (type) {
            case BLOCK_SCALE -> block_px * getCurrentScene().zoom;
            case GAME_SCALE -> scaleFactor * getCurrentScene().zoom;
            case REGULAR_SCALE -> getCurrentScene().zoom;
            default -> throw new IllegalArgumentException("Unknown scale type");
        };
    }
    public static Vector2f cameraPos(boolean localCoords){
        return localCoords ? getCurrentScene().camera().worldCoords() : getCurrentScene().camera().position;
    }

    public static void showCursor(){
        cursor.show();
    }

    public static void hideCursor(){
        cursor.hide();
    }

}
