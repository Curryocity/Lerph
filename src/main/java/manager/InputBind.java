package manager;

import java.util.HashMap;

import static org.lwjgl.glfw.GLFW.*;

public class InputBind {

    public enum InputType {
        PAUSE, FORWARD, BACKWARD, LEFT,
        RIGHT, DASH, JUMP, BOOM, PICK,
        TOGGLE_HITBOX, INFO_MENU, SELF_DESTROY
        , CHECKPOINT
    }
    public static HashMap<InputType,Integer> bindMap = new HashMap<>(32);
    private InputBind(){}
    public static void reset(){
        bindMap.put(InputType.PAUSE, GLFW_KEY_ESCAPE);
        bindMap.put(InputType.FORWARD, GLFW_KEY_W);
        bindMap.put(InputType.BACKWARD, GLFW_KEY_S);
        bindMap.put(InputType.LEFT, GLFW_KEY_A);
        bindMap.put(InputType.RIGHT, GLFW_KEY_D);
        bindMap.put(InputType.DASH, GLFW_KEY_K);
        bindMap.put(InputType.JUMP, GLFW_KEY_SPACE);
        bindMap.put(InputType.BOOM, GLFW_KEY_J);
        bindMap.put(InputType.PICK, GLFW_KEY_E);
        bindMap.put(InputType.TOGGLE_HITBOX, GLFW_KEY_B);
        bindMap.put(InputType.INFO_MENU, GLFW_KEY_I);
        bindMap.put(InputType.SELF_DESTROY, GLFW_KEY_R);
        bindMap.put(InputType.CHECKPOINT, GLFW_KEY_C);
    }
}
