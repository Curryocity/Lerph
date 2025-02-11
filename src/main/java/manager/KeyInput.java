package manager;

import static org.lwjgl.glfw.GLFW.*;

import imgui.ImGui;
import manager.InputBind.InputType;

public class KeyInput{

    private static KeyInput instance;
    private final boolean[] keyPressed = new boolean[GLFW_KEY_LAST + 1];
    private final boolean[] keyActionQueued = new boolean[GLFW_KEY_LAST + 1];
    private final boolean[] keyWasPressed = new boolean[GLFW_KEY_LAST + 1];

    private KeyInput(){
        InputBind.reset();
    }

    public static KeyInput get(){
        if(KeyInput.instance == null){
            KeyInput.instance = new KeyInput();
        }
        return instance;
    }

    public static void keyCallback(long window, int key, int scancode, int action, int mods) {
        if (ImGui.getIO().getWantCaptureKeyboard()) return;
        try {
            if (action == GLFW_PRESS) {
                get().keyPressed[key] = true;
                if (!get().keyWasPressed[key]) { //for trigger-once keybind
                    get().keyActionQueued[key] = true;
                    get().keyWasPressed[key] = true;
                }
            } else if (action == GLFW_RELEASE) {
                get().keyPressed[key] = false;
                get().keyActionQueued[key] = false;
                get().keyWasPressed[key] = false;
            }
        }catch (ArrayIndexOutOfBoundsException e){ e.printStackTrace();}
    }
    public static boolean isKeyPressed(int keyCode) {
        return get().keyPressed[keyCode];
    }

    public static boolean isKeyPressed(InputType inputType) {
        return get().keyPressed[InputBind.bindMap.get(inputType)];
    }


    public static boolean isKeyQueued(int keyCode){
        return get().keyActionQueued[keyCode];
    }
    public static boolean isKeyQueued(InputType inputType){
        return get().keyActionQueued[InputBind.bindMap.get(inputType)];
    }

    public static void removeKeyQueue(int keyCode){
        get().keyActionQueued[keyCode] = false;
    }
    public static void removeKeyQueue(InputType inputType){
        get().keyActionQueued[InputBind.bindMap.get(inputType)] = false;
    }

    public static boolean useQueue(int keyCode){
        if (get().keyActionQueued[keyCode]){
            get().keyActionQueued[keyCode] = false;
            return true;
        }
        return false;
    }

    public static boolean useQueue(InputType inputType){
        if (get().keyActionQueued[InputBind.bindMap.get(inputType)]){
            get().keyActionQueued[InputBind.bindMap.get(inputType)] = false;
            return true;
        }
        return false;
    }

    public static double[] normWasdVec(){
        double[] movementInputVec = {0,0};
        boolean forward = KeyInput.isKeyPressed(InputType.FORWARD);
        boolean backward = KeyInput.isKeyPressed(InputType.BACKWARD);
        boolean left = KeyInput.isKeyPressed(InputType.LEFT);
        boolean right = KeyInput.isKeyPressed(InputType.RIGHT);

        if(left ^ right){
            movementInputVec[0] = right ? 1.0: -1.0;
        }

        if(forward ^ backward){
            movementInputVec[1] = forward ? 1.0: -1.0;
        }

        if (Math.abs(movementInputVec[0]) + Math.abs(movementInputVec[1]) > 1){
            movementInputVec[0] /= Math.sqrt(2);
            movementInputVec[1] /= Math.sqrt(2);
        }

        return movementInputVec;
    }
    

}
