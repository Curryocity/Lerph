package scene;

import component.Camera;
import data.Map;
import data.Space;

import static core.Game.cursor;

public abstract class Scene {

    public Scene previousScene = null;
    protected boolean initialized = false;
    protected Camera camera = new Camera();
    public float zoom = 1.0f;

    // optional for a scene
    public Map map = null;

    public Scene(){}

    public abstract void init();
    public void refresh(){
        cursor.show();
    }
    public abstract void update(float alpha);
    public abstract void exit();

    public void handleResize(){
        camera.adjustProjection();
    }

    public Camera camera(){
        return camera;
    }

    public Space currentSpace(){
        if(map == null) return null;
        return map.getCurrentSpace();
    }

    public boolean isInitialized(){
        return initialized;
    }

}
