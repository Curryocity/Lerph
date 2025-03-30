package ui_basic;

import component.SimpleBox;
import manager.MouseInput;
import org.joml.Vector4f;

import static ui_basic.ColorPalette.*;
import static ui_basic.Text.defaultColor;

public class AdvText implements AlignAble, Updatable{
    public static final int FULL = 0, UPPER = 1, LOWER = 2, MID = 3;
    private int boundingBoxType = FULL;
    private float padding;
    private boolean shown = false;
    private Vector4f idleColor, hoverColor;
    private final Text text;

    private final VisualBox frame;
    private boolean frameShown, frameHoverSync;
    private float frameThickness = 3;
    private Vector4f edgeColor, fillColor;

    public AdvText(String content){
        this(content, defaultColor, limeGreen);
    }
    public AdvText(String content, float zDepth){ this(content, defaultColor, limeGreen, zDepth); }
    public AdvText(String content, Vector4f idleColor, Vector4f hoverColor){this(content, idleColor, hoverColor, 99f);}

    public AdvText(String content, Vector4f idleColor, Vector4f hoverColor, float zDepth){
        this.text = new Text(content, zDepth);
        this.frame = new VisualBox(text.boundingBox(padding, boundingBoxType), frameThickness, text.getZDepth(), edgeColor, fillColor);
        setIdleAndHoverColor(idleColor, hoverColor);
        updateFrame();
    }


    @Override
    public void update(){
        if(!shown || hoverColor == null) return;

        if (isHovered()) {
            text.setColor(hoverColor);
        } else {
            text.setColor(idleColor);
        }

        if(!frameShown || hoverColor == null) return;

        if(isHovered() && frameHoverSync) {
            frame.setEdgeColor(hoverColor);
        } else {
            frame.setEdgeColor((edgeColor != null) ? edgeColor : idleColor);
        }

    }

    public void setContent(String content){
        text.setText(content);
        resizeFrame();
    }

    public void resizeFrame(){
        SimpleBox textBoundingBox = text.boundingBox(padding, boundingBoxType);
        this.frame.setSize(2 * textBoundingBox.halfDx, 2 * textBoundingBox.halfDy);
        this.frame.setPos(textBoundingBox.posX, textBoundingBox.posY);
    }

    public void updateFrame(){
        frame.setThickness(frameThickness);
        frame.setUsedLocalCoords(false);
        frame.setRelativeToCamera(text.relativeToCamera);

        if(edgeColor != null){
            frame.setEdgeColor(edgeColor);
            frame.setEdgeShown(true);
        }else{
            frame.setEdgeShown(false);
        }

        if(fillColor != null) {
            frame.setFillColor(fillColor);
            frame.setFillShown(true);
        }else {
            frame.setFillShown(false);
        }

        if(frameShown && shown){
            frame.show();
        } else{
            frame.hide();
        }
    }

    public void setPos(float x, float y){
        frame.movePos(x - text.getX(), y - text.getY());
        text.setPos(x,y);
    }

    public void setIdleColor(Vector4f idleColor){
        this.idleColor = (idleColor == null) ? white : idleColor;
    }

    public void setHoverColor(Vector4f hoverColor){
        this.hoverColor = (hoverColor == null || this.idleColor.equals(hoverColor)) ? null : hoverColor;
    }

    public void setIdleAndHoverColor(Vector4f idleColor, Vector4f hoverColor){
        this.idleColor = (idleColor == null) ? white : idleColor;
        this.hoverColor = (hoverColor == null || this.idleColor.equals(hoverColor)) ? null : hoverColor;
    }

    public void setHitBoxInfo(float padding, int boundingBoxType){
        this.padding = padding;
        this.boundingBoxType = boundingBoxType;
        resizeFrame();
    }

    public void setFrameInfo(float thickness, float padding, int boundingBoxType, boolean hoverSync, Vector4f edgeColor, Vector4f fillColor){
        this.frameThickness = thickness;
        this.padding = padding;
        this.frameHoverSync = hoverSync;
        this.edgeColor = edgeColor;
        this.fillColor = fillColor;
        this.boundingBoxType = boundingBoxType;
        this.frameShown = true;

        updateFrame();
        resizeFrame();
    }

    public void setShown(boolean shown){
        this.shown = shown;
        if(shown){
            text.show();
            if(frameShown) frame.show();
            else frame.hide();
        } else {
            text.hide();
            frame.hide();
        }
    }

    public boolean getShown(){return shown;}

    public void setFontSize(float fontSize){
        text.setFontSize(fontSize);
        resizeFrame();
    }

    public void setZDepth(float zDepth){
        text.setZDepth(zDepth);
        frame.setZDepth(zDepth);
    }

    public boolean isHovered(){
        return shown && frame.include(MouseInput.getX(), MouseInput.getY());
    }

    public boolean isPressed(int button){
        return shown && frame.include(MouseInput.getX(), MouseInput.getY()) && frame.include(MouseInput.getLastPressedX(button), MouseInput.getLastPressedY(button)) && MouseInput.isPressed(button);
    }

    public boolean hasQueue(int button){
        return shown && frame.include(MouseInput.getX(), MouseInput.getY()) && frame.include(MouseInput.getLastPressedX(button), MouseInput.getLastPressedY(button)) && MouseInput.isQueued(button);
    }

    public boolean useQueue(int button){
        if( shown && frame.include(MouseInput.getX(), MouseInput.getY()) && frame.include(MouseInput.getLastPressedX(button), MouseInput.getLastPressedY(button)) ){
            return MouseInput.useQueue(button);
        } else return false;
    }

    @Override
    public void setPos(double x, double y) {
        setPos((float) x, (float) y);
    }
    @Override
    public void setSize(double sizeX, double sizeY) {
        setFontSize((float) sizeY);
    }
    @Override
    public void show() {setShown(true);}
    @Override
    public void hide() {setShown(false);}
    @Override
    public Vector4f getDimensionAndPivot() { return frame.getDimensionAndPivot();}
    @Override
    public boolean isCoordsLocal() { return false;}

    @Override
    public void setRelativeToCamera(boolean isRelativeToCamera) {
        text.relativeToCamera = isRelativeToCamera;
        frame.setRelativeToCamera(isRelativeToCamera);
    }
}
