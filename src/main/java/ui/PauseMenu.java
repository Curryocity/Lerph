package ui;

import core.Game;
import manager.InputBind;
import manager.KeyInput;
import org.joml.Vector4f;
import scene.InGameScene;
import scene.TitleScene;
import ui_basic.AdvText;
import ui_basic.Overlay;
import ui_basic.Text;

import static manager.MouseInput.LMB;
import static ui_basic.ColorPalette.limeGreen;
import static ui_basic.ColorPalette.silver;

public class PauseMenu extends Overlay {

    final InGameScene inGameScene;

    Text title = new Text("PAUSED", silver);
    AdvText resume = new AdvText("Resume");
    AdvText options = new AdvText("Options");
    AdvText saveAndQuit = new AdvText("Save and Quit");
    AdvText levelEditor = new AdvText("Level Editor <3");

    boolean inOptionMenuQ = false;
    OptionMenu optionMenu = new OptionMenu(this);

    public PauseMenu(InGameScene inGameScene) {
        super(true);
        this.inGameScene = inGameScene;

        setOverlayBG(0f, 0.55f, 50f);
        resume.setIdleAndHoverColor(silver, limeGreen);
        options.setIdleAndHoverColor(silver, limeGreen);
        saveAndQuit.setIdleAndHoverColor(silver, limeGreen);
        levelEditor.setIdleAndHoverColor(silver, limeGreen);

        this.addComponent(title, new Vector4f(0, 0.44f, 0f ,0.16f));
        this.addComponent(resume, new Vector4f(0, 0.0f, 0f ,0.06f));
        this.addComponent(options, new Vector4f(0, -0.22f, 0f ,0.06f));
        this.addComponent(saveAndQuit, new Vector4f(0, -0.44f, 0f ,0.06f));
        this.addComponent(levelEditor, new Vector4f(75, -0.93f, 0f ,0.045f));

        hide();
    }

    @Override
    public void update() {
        if(inOptionMenuQ){
            optionMenu.update();
            return;
        }

        super.update();

        if(resume.useQueue(LMB) || KeyInput.useQueue(InputBind.InputType.PAUSE) ){
            inGameScene.unPause();
        }

        if(options.useQueue(LMB)){
            inOptionMenuQ = true;
            this.hide();
            optionMenu.show();
        }

        if(saveAndQuit.useQueue(LMB)){
            Game.switchScene(new TitleScene());
        }

        if(levelEditor.useQueue(LMB)){
            inGameScene.enterLevelEditor();
        }
    }

    public void quitOptionMenu(){
        inOptionMenuQ = false;
        optionMenu.hide();
        this.show();
    }

    @Override
    public void handleResize(){
        super.handleResize();
        optionMenu.handleResize();
    }

}
