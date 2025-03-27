package scene;

import component.Line;
import core.Game;
import data.Map;
import manager.InputBind;
import manager.KeyInput;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import ui.MapCreationMenu;
import ui_basic.AdvText;
import ui_basic.Panel;
import ui_basic.Text;
import ui_basic.VisualBox;

import java.util.ArrayList;
import java.util.List;

import static component.AdvPacket.GRADIENT_VERTICAL;
import static core.Game.*;
import static manager.MouseInput.LMB;
import static ui_basic.AdvText.UPPER;
import static ui_basic.ColorPalette.white;

public class MapSelectionScene extends Scene{

    Panel bgPanel = new Panel(0.4f, 0.4f, 0.25f, 1.0f,
            0.6f, 0.42f, 0.42f, 1.0f,
            GRADIENT_VERTICAL, 0.1f);

    Text title = new Text("Map Selection");
    Line underLine = new Line(new Vector4f(0,0,1,0), new Vector4f(1,1,1,1), 0.1f, 3f);
    AdvText goBack = new AdvText("GoBack");
    Text currentPage = new Text("Page 1 of 1");
    AdvText prevPage = new AdvText("<") , nextPage = new AdvText(">");
    AdvText createNewMap = new AdvText("+ Map");

    MapCreationMenu mapCreationMenu = new MapCreationMenu(this);

    public List<String> existMapNames = new ArrayList<>();
    private int page = 1, totalPages = 1;

    Panel[] mapPreviews = new Panel[4];

    @Override
    public void init() {
        initialized = true;

        prevPage.setHitBoxInfo(3, UPPER);
        nextPage.setHitBoxInfo(3, UPPER);
        goBack.setFrameInfo(4, 1f, UPPER,true, white, null);
        createNewMap.setFrameInfo(4, 1f, UPPER,true, white, null);


        bgPanel.addComponent(title, new Vector4f(0, 0.84f, 0.06f ,0.06f));
        bgPanel.addComponent(underLine, new Vector4f(0, 0.82f, 0.4f ,0f));
        bgPanel.addComponent(currentPage, new Vector4f(0, -0.86f, 0.045f ,0.045f));
        bgPanel.addComponent(prevPage, new Vector4f(-0.15f, -0.86f, 0.04f, 0.04f));
        bgPanel.addComponent(nextPage, new Vector4f(0.15f, -0.86f, 0.04f, 0.04f));
        bgPanel.addComponent(goBack, new Vector4f(-0.83f, 0.82f, 0.038f, 0.038f));
        bgPanel.addComponent(createNewMap, new Vector4f(0.85f, 0.82f, 0.04f ,0.04f));

        bgPanel.setZDepth(20f);

        bgPanel.addComponent(mapCreationMenu, new Vector4f(0, 0, 0.47f, 0.47f));

        refresh();
    }



    @Override
    public void refresh() {
        super.refresh();
        bgPanel.show();
        if(!isCreatingMap){
            mapCreationMenu.hide();
        }

        refreshPage();
        handleResize();
    }

    final Vector4f gold = new Vector4f(1f, 0.85f, 0.55f, 1f);
    public boolean isCreatingMap = false;
    @Override
    public void update(float alpha) {

        if(isCreatingMap){
            mapCreationMenu.update();
            renderSystem.render(alpha);
            return;
        }

        bgPanel.update();

        if (goBack.useQueue(GLFW.GLFW_MOUSE_BUTTON_LEFT) || KeyInput.useQueue(InputBind.InputType.PAUSE)) {
            switchScene(previousScene);
        }

        for (int i = 0; i < mapPreviews.length; i++) {
            if (mapPreviews[i] == null) break;

            VisualBox frame = (VisualBox) mapPreviews[i].getComponent("frame");

            if (frame.isHovered(true)) {
                frame.setEdgeColor(gold);
                frame.setThickness(8);
            } else {
                frame.setEdgeColor(white);
                frame.setThickness(4);
            }

            if (frame.useQueue(GLFW.GLFW_MOUSE_BUTTON_LEFT, true)) {
                Map loadedMap = Map.load(existMapNames.get((page - 1) * 4 + i));
                Game.switchScene(new InGameScene(loadedMap), true);
            }

        }

        if (page > 1) {
            if (prevPage.useQueue(LMB)) {
                page--;
                refreshPage();
            }
        }

        if (page < totalPages) {
            if (nextPage.useQueue(LMB)) {
                page++;
                refreshPage();
            }
        }

        if (createNewMap.useQueue(LMB)) {
            isCreatingMap = true;
            mapCreationMenu.show();
        }


        renderSystem.render(alpha);
    }


    public Panel generatePreviewPanel(String mapName) {
        Panel panel = new Panel();
        VisualBox frame = new VisualBox(1,1,5,1, white, null);
        Text name = new Text(mapName);
        panel.addComponent(name, new Vector4f(-35f, 35f, 0.35f, 0.33f));
        panel.addComponent(frame, new Vector4f(0, 0, 1f, 1f), "frame");
        return panel;
    }

    public void addWorldPreview(String mapName, int slot) {
        Panel panel = generatePreviewPanel(mapName);
        panel.setZDepth(20f);
        mapPreviews[slot] = panel;
        bgPanel.addComponent(panel, new Vector4f(0f, 0.55f - 0.35f * slot, 0.5f, 0.14f));
    }

    public void refreshPage() {
        camera.setPosition(0, 0);
        for (int i = 0; i < mapPreviews.length; i++) {
            bgPanel.removeComponent(mapPreviews[i]);
            mapPreviews[i] = null;
        }

        Map.reloadSavesFolder();
        existMapNames.clear();

        existMapNames.addAll(Map.existMaps);
        this.totalPages = (int) Math.max(1, Math.ceil(existMapNames.size() / 4.0));
        currentPage.setText("Page " + this.page + " of " + totalPages);
        int startIndex = (this.page - 1) * 4;
        for (int i = 0; i < 4; i++) {
            if (startIndex + i < existMapNames.size()) {
                addWorldPreview(existMapNames.get(startIndex + i), i);
            }
        }
        bgPanel.refresh();
    }

    @Override
    public void exit() {
        bgPanel.hide();
        renderSystem.reset();
    }

    @Override
    public void handleResize() {
        super.handleResize();
        bgPanel.refresh();
        mapCreationMenu.handleResize();
    }

}
