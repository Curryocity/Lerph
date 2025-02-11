package ui;

import core.Application;
import manager.InputBind;
import manager.KeyInput;
import manager.MouseInput;
import manager.MouseScrollListener;
import org.joml.Vector4f;
import ui_basic.AdvText;
import ui_basic.Overlay;
import ui_basic.Text;

import static ui_basic.ColorPalette.silver;

public class OptionMenu extends Overlay implements MouseScrollListener {

    PauseMenu pauseMenu;
    Text title = new Text("Options");
    Text language = new Text("Language: Only English");
    AdvText controls = new AdvText("Controls");
    Text video = new Text("Video");
    Text aspectRatio = new Text("Aspect Ratio");    AdvText aspectRatioVal = new AdvText("16:9");
    Text fps = new Text("FPS");     AdvText fpsVal = new AdvText("120");
    Text audio = new Text("Audio");
    Text music = new Text("Music");     AdvText musicVal = new AdvText("  10");
    Text sound = new Text("Sound");     AdvText soundVal = new AdvText("  10");
    Text gamePlay = new Text("Gameplay");
    Text cursorVisibility = new Text("Cursor Visibility");     AdvText cursorVisibilityVal = new AdvText("Always Shown");
    Text cameraTracking = new Text("Camera Tracking");         AdvText cameraTrackingVal = new AdvText("Fixed on Player");
    Text motionBlur = new Text("Motion Blur Level");         AdvText motionBlurVal = new AdvText("   0");
    Text screenShake = new Text("Screen Shake");             AdvText screenShakeVal = new AdvText("OFF");
    Text deathsCounter = new Text("Deaths Counter");         AdvText deathsCounterVal = new AdvText("OFF");
    Text speedrunTimer = new Text("Speedrun Timer");         AdvText speedrunTimerVal = new AdvText("OFF");
    AdvText advanced = new AdvText("Advanced Options");


    public OptionMenu(){
        super(false);

        setup();
    }

    public OptionMenu(PauseMenu pauseMenu){
        super(true);
        this.pauseMenu = pauseMenu;
        this.setZDepth(60f);
        setOverlayBG(0f, 0.55f, 50f);

        this.setAllTextsColor(silver);

        setup();
    }

    public void setup(){
        setRelativeSize(0.5f, 1f);
        setRelativeToCamera(true);

        setBounds(1f, 5f);


        this.addComponent(title, new Vector4f(0, 0.72f, 0f ,0.1f));
        this.addComponent(language, new Vector4f(0, 0.4f, 0f ,0.05f));
        this.addComponent(controls, new Vector4f(0, 0.2f, 0f ,0.05f));

        this.addComponent(video, new Vector4f(-2f, -0.14f, 0f ,0.03f));
        this.addComponent(aspectRatio, new Vector4f(-2f, -0.25f, 0f ,0.04f));
        this.addComponent(fps, new Vector4f(-2f, -0.4f, 0f ,0.04f));

        this.addComponent(aspectRatioVal, new Vector4f(2f, -0.25f, 0f ,0.04f));
        this.addComponent(fpsVal, new Vector4f(2f, -0.4f, 0f ,0.04f));

        this.addComponent(audio, new Vector4f(-2f, -0.64f, 0f ,0.03f));
        this.addComponent(music, new Vector4f(-2f, -0.75f, 0f ,0.04f));
        this.addComponent(sound, new Vector4f(-2f, -0.9f, 0f ,0.04f));

        this.addComponent(musicVal, new Vector4f(2f, -0.75f, 0f ,0.04f));
        this.addComponent(soundVal, new Vector4f(2f, -0.9f, 0f ,0.04f));

        this.addComponent(gamePlay, new Vector4f(-2f, -1.14f, 0f ,0.03f));
        this.addComponent(cursorVisibility, new Vector4f(-2f, -1.25f, 0f ,0.04f));
        this.addComponent(cameraTracking, new Vector4f(-2f, -1.4f, 0f ,0.04f));
        this.addComponent(motionBlur, new Vector4f(-2f, -1.55f, 0f ,0.04f));
        this.addComponent(screenShake, new Vector4f(-2f, -1.7f, 0f ,0.04f));
        this.addComponent(deathsCounter, new Vector4f(-2f, -1.85f, 0f ,0.04f));
        this.addComponent(speedrunTimer, new Vector4f(-2f, -2.0f, 0f ,0.04f));

        this.addComponent(cursorVisibilityVal, new Vector4f(2f, -1.25f, 0f ,0.04f));
        this.addComponent(cameraTrackingVal, new Vector4f(2f, -1.4f, 0f ,0.04f));
        this.addComponent(motionBlurVal, new Vector4f(2f, -1.55f, 0f ,0.04f));
        this.addComponent(screenShakeVal, new Vector4f(2f, -1.7f, 0f ,0.04f));
        this.addComponent(deathsCounterVal, new Vector4f(2f, -1.85f, 0f ,0.04f));
        this.addComponent(speedrunTimerVal, new Vector4f(2f, -2.0f, 0f ,0.04f));


        this.addComponent(advanced, new Vector4f(0, -2.4f, 0f ,0.05f));

        Vector4f gray = new Vector4f(0.7f, 0.7f, 0.75f, 1f);

        video.setColor(gray);
        audio.setColor(gray);
        gamePlay.setColor(gray);
    }

    @Override
    public void update() {
        super.update();

        if(pauseMenu != null && KeyInput.useQueue(InputBind.InputType.PAUSE)){
            pauseMenu.quitOptionMenu();
        }

    }

    @Override
    public void show() {
        super.show();
        setPos(0, 0);
        MouseInput.subscribeToScroll(this);
    }

    @Override
    public void hide() {
        super.hide();
        MouseInput.unSubscribeToScroll(this);
    }

    final float scrollSensitivity = 15f;
    final float maxY = 1f;
    @Override
    public void onScroll(double scrollX, double scrollY) {
        float nextY = (float) (y - scrollSensitivity * scrollY);
        if(nextY < 0) nextY = 0;
        else if (nextY > maxY * Application.viewPortHeight) nextY = maxY * Application.viewPortHeight;
        setAbsolutePos(x, nextY);
    }
}
