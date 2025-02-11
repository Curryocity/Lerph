package manager;

import core.Application;
import core.Game;
import imgui.ImGui;
import org.joml.Vector2f;
import scene.EditorScene;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

public class MouseInput{
    private static MouseInput instance = null;

    private double xPos, yPos, lastX, lastY, scrollX, scrollY;
    private final boolean[] pressed    = new boolean[GLFW_MOUSE_BUTTON_LAST + 1];
    private final boolean[] queued     = new boolean[GLFW_MOUSE_BUTTON_LAST + 1];
    private final boolean[] wasPressed = new boolean[GLFW_MOUSE_BUTTON_LAST + 1];
    private final float[] lastPressedX = new float[GLFW_MOUSE_BUTTON_LAST + 1];
    private final float[] lastPressedY = new float[GLFW_MOUSE_BUTTON_LAST + 1];
    private boolean isDragging = false;

    List<MouseScrollListener> scrollListener = new ArrayList<>();
    List<MouseMoveListener> mouseMoveListener = new ArrayList<>();

    private MouseInput(){
        xPos = 0;
        yPos = 0;
    }

    public static MouseInput get(){
        if(MouseInput.instance == null){
            MouseInput.instance = new MouseInput();
        }
        return instance;
    }


    public static void mousePosCallBack(long window, double xPos, double yPos){
        get().lastX = get().xPos - Application.viewPortX * 0.5;
        get().lastY = get().yPos - Application.viewPortY * 0.5;
        get().xPos = xPos - Application.viewPortX * 0.5;
        get().yPos = yPos - Application.viewPortY * 0.5;
        for(boolean q : get().pressed) {
            if(q) {
                get().isDragging = true;
            }
        }
        for (MouseMoveListener listener : get().mouseMoveListener) {
            listener.onMouseMove(xPos, yPos);
        }
    }

    public static void mouseButtonCallBack(long window, int button, int action, int mods){
        if (ImGui.getIO().getWantCaptureMouse()) return;
        if(action == GLFW_PRESS) {
            get().pressed[button] = true;
            if (!get().wasPressed[button]) { //for trigger-once keybind
                get().queued[button] = true;
                get().wasPressed[button] = true;
                get().lastPressedX[button] = getX();
                get().lastPressedY[button] = getY();
            }
        }else if(action == GLFW_RELEASE){
            get().pressed[button] = false;
            get().queued[button] = false;
            get().wasPressed[button] = false;
            get().isDragging = false;
        }
    }

    public static void mouseScrollCallback(long window, double xOffset, double yOffset) {
        get().scrollX = xOffset;
        get().scrollY = yOffset;
        if(Game.getCurrentScene() instanceof EditorScene editorScene){
            editorScene.scrolled((float) yOffset);
        }
        for (MouseScrollListener listener : get().scrollListener) {
            listener.onScroll(xOffset, yOffset);
        }
    }

    public static Vector2f getMouseWorldCoords(){
        Vector2f translations = Game.cameraPos(true);
        float scale = 1.0f / Game.getScale(Game.BLOCK_SCALE);
        return new Vector2f(getX() * scale + translations.x, getY() * scale + translations.y);
    }

    public static void subscribeToScroll(MouseScrollListener listener) {get().scrollListener.add(listener);}
    public static void unSubscribeToScroll(MouseScrollListener listener) {get().scrollListener.remove(listener);}
    public static void subscribeToMove(MouseMoveListener listener) {get().mouseMoveListener.add(listener);}
    public static void unSubscribeToMove(MouseMoveListener listener) {get().mouseMoveListener.remove(listener);}

    public static float getX() {return (float)get().xPos * 2f - Application.viewPortWidth * 0.5f;}
    public static float getY() {return Application.viewPortHeight * 0.5f - (float)get().yPos * 2f ;}
    public static float getScrollX() {
        return (float) get().scrollX;
    }
    public static float getScrollY() {
        return (float) get().scrollY;
    }
    public static float getLastPressedX(int button) {
        return get().lastPressedX[button];
    }
    public static float getLastPressedY(int button) {
        return get().lastPressedY[button];
    }

    public static boolean isPressed(int button){
        return get().pressed[button];
    }

    public static boolean isQueued(int button){
        return get().queued[button];
    }

    public static void removeQueue(int button){
        get().queued[button] = false;
    }

    public static boolean useQueue(int button){
        if(get().queued[button]){
            get().queued[button] = false;
            return true;
        }
        return false;
    }

    public static final int LMB = GLFW_MOUSE_BUTTON_LEFT, RMB = GLFW_MOUSE_BUTTON_RIGHT;

    
}
