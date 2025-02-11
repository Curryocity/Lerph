package component;

import core.Application;
import core.Game;
import org.joml.Vector4f;
import ui_basic.AlignAble;

public class StdPacket implements AlignAble {
    public Sprite sprite;
    public float x, y, scaleX, scaleY, zDepth = 0.1f;
    public float yRep = 0;
    // yRep (y Representation): we will use yRep to contribute in zDepth, for y-sorting or depth-test
    // always use in-game y-coord
    public boolean alive, immediate, relativeToCamera, usedLocalCoords = true;
    protected StdPacket(){}

    public StdPacket(Sprite sprite, float zDepth){
        this(sprite, zDepth, 1, 1);
    }

    public StdPacket(Sprite sprite, float zDepth, float scaleX, float scaleY) {
        this.sprite = sprite;
        this.zDepth = zDepth;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
    }

    public void setPos(float x, float y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public void setPos(double x, double y) {
        setPos((float) x, (float) y);
    }

    @Override
    public void setSize(double sizeX, double sizeY) {
        if(sprite != null){
            scaleX = (float) sizeX / sprite.txcDimensions.z;
            scaleY = (float) sizeY / sprite.txcDimensions.w;
        }else {
            scaleX = (float) sizeX;
            scaleY = (float) sizeY;
        }
    }

    public void setProperties(boolean usedLocalCoords, boolean relativeToCamera){
        this.usedLocalCoords = usedLocalCoords;
        this.relativeToCamera = relativeToCamera;
    }

    public void setRelYRep(float yRep) {
        this.yRep = yRep;
    }


    @Override
    public void show() {
        if(alive) return;
        immediate = false;
        Game.renderSystem.activate(this);
    }

    @Override
    public void hide() {alive = false;}

    @Override
    public Vector4f getDimensionAndPivot() {
        float dx = scaleX, dy = scaleX, pivotX = 0.5f, pivotY = 0.5f;
        if(sprite != null){
            dx = sprite.txcDimensions.z;
            dy = sprite.txcDimensions.w;
            pivotX = sprite.anchorX;
            pivotY = sprite.anchorY;
        }
        return new Vector4f(dx, dy, pivotX, pivotY);
    }

    @Override
    public boolean isCoordsLocal() {return usedLocalCoords;}

    @Override
    public void setRelativeToCamera(boolean isRelativeToCamera) {
        this.relativeToCamera = isRelativeToCamera;
    }

    @Override
    public void setZDepth(float zDepth) {
        this.zDepth = zDepth;
    }
}
