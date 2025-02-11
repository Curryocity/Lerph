package scene;

import ui_basic.Panel;
import ui_basic.Text;
import ui_basic.AdvText;
import core.Application;
import core.Game;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;

import static component.AdvPacket.GRADIENT_VERTICAL;
import static core.Game.cursor;
import static core.Game.renderSystem;
import static manager.MouseInput.LMB;

public class TitleScene extends Scene{

    Panel bgPanel = new Panel(0.4f, 0.4f, 0.25f, 1.0f,
            0.6f, 0.42f, 0.42f, 1.0f,
            GRADIENT_VERTICAL, 0.1f);
    Text title = new Text("Lerph");
    AdvText startButton = new AdvText("Start");
    AdvText optionButton = new AdvText("Options");
    AdvText exitButton = new AdvText("Exit");
    Text version = new Text(Application.getVersionText(), new Vector4f(0.9f, 0.8f, 0.5f, 0.7f));

    @Override
    public void init() {
        initialized = true;


        bgPanel.addComponent(title, new Vector4f(0, 0.42f, 0.23f ,0.23f));
        bgPanel.addComponent(startButton, new Vector4f(0, 0f, 0.1f, 0.1f));
        bgPanel.addComponent(optionButton, new Vector4f(0, -0.34f, 0.1f, 0.1f));
        bgPanel.addComponent(exitButton, new Vector4f(0, -0.68f, 0.1f, 0.1f));
        bgPanel.addComponent(version, new Vector4f(-69, -0.95f, 0.05f, 0.05f));

        refresh();
    }

    @Override
    public void refresh() {
        super.refresh();
        camera.setPosition(0,0);
        bgPanel.show();
        handleResize();
    }

    @Override
    public void update(float alpha) {
        bgPanel.update();
        if(startButton.useQueue(LMB)){
            Game.switchScene(new MapSelectionScene(), true);
        }

        if(optionButton.useQueue(LMB)){
            Game.switchScene(new OptionScene(), true);
        }

        if(exitButton.useQueue(LMB)){
            Application.standardExit();
        }

        renderSystem.render(alpha);
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
    }

}
