package ui;

import component.Line;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import scene.MapSelectionScene;
import ui_basic.AdvText;
import ui_basic.Overlay;
import ui_basic.Text;
import ui_basic.VisualBox;

import static ui_basic.AdvText.UPPER;
import static ui_basic.ColorPalette.white;

public class MapCreationMenu extends Overlay {
    MapSelectionScene mapSelectionScene;

    VisualBox popupFrame = new VisualBox(1,1,5,1, white, new Vector4f(0.5f,0.4f,0.6f,1f));
    Text popupTitle = new Text("Create New Map");
    AdvText popupCancel = new AdvText("Cancel");
    AdvText popupCreate = new AdvText("Create");
    Line inputTextUnderLine = new Line(new Vector4f(0,0,1,0), white, 0.1f, 3f);

    public MapCreationMenu(MapSelectionScene mapSelectionScene) {
        super(true);
        this.mapSelectionScene = mapSelectionScene;

        setOverlayBG(0.5f, 0.6f , 30f);

        popupCancel.setFrameInfo(3, 1f, UPPER, true, white, null);
        popupCreate.setFrameInfo(3, 1f, UPPER, true, white, null);

        addComponent(popupFrame, new Vector4f(0, 0, 1, 1));
        addComponent(popupTitle, new Vector4f(0f, 0.7f, 0.1f ,0.1f));
        addComponent(popupCancel, new Vector4f(-50, -0.85f, 0.08f ,0.08f));
        addComponent(popupCreate, new Vector4f(50, -0.85f, 0.08f ,0.08f));
        addComponent(inputTextUnderLine, new Vector4f(0, -0.1f, 0.45f ,0f));

        setZDepth(40f);

        popupFrame.setZDepth(39f);
    }

    @Override
    public void update() {
        super.update();
        if(popupCancel.useQueue(GLFW.GLFW_MOUSE_BUTTON_LEFT)){
            exit();
        }
    }

    public void exit(){
        mapSelectionScene.isCreatingMap = false;
        hide();
    }
}
