package scene;

import core.Game;
import manager.InputBind;
import manager.KeyInput;
import ui.OptionMenu;
import ui_basic.Panel;

import static component.AdvPacket.GRADIENT_VERTICAL;
import static core.Game.renderSystem;

public class OptionScene extends Scene{

    Panel bgPanel = new Panel(0.4f, 0.4f, 0.25f, 1.0f,
            0.6f, 0.42f, 0.42f, 1.0f,
            GRADIENT_VERTICAL, 0.1f);

    OptionMenu optionMenu;

    public OptionScene(){}

    @Override
    public void init() {
        initialized = true;
        optionMenu = new OptionMenu();
        optionMenu.show();
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
        renderSystem.render(alpha);

        if(KeyInput.useQueue(InputBind.InputType.PAUSE)){
            Game.switchScene(previousScene);
        }

        optionMenu.update();
    }

    @Override
    public void exit() {
        bgPanel.hide();
        optionMenu.hide();
        renderSystem.reset();
    }

    @Override
    public void handleResize() {
        super.handleResize();
        bgPanel.refresh();
        optionMenu.handleResize();
    }
}
