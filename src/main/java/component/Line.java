package component;

import org.joml.Vector4f;
import renderer.LineRenderer;
import ui_basic.AlignAble;

public class Line implements AlignAble {

    final public Vector4f midPointAndHalfDimensions ;
    public Vector4f color1, color2;
    public boolean colorWereIndependent = false;
    public float thickness, zDepth;
    public boolean usedLocalCoords, relativeToCamera;
    public boolean alive, immediateMode;

    public Line(Vector4f color, float zDepth){
        midPointAndHalfDimensions = new Vector4f();
        color1 = color;
        color2 = color;
        this.zDepth = zDepth;
    }

    public Line(float startX, float startY, float endX, float endY, Vector4f color, float zDepth, float thickness) {
        this(startX, startY, endX, endY, color, color, zDepth, thickness);
    }

    public Line(float startX, float startY, float endX, float endY, Vector4f color1, Vector4f color2, float zDepth, float thickness) {
        this.color1 = color1;
        this.color2 = color2;
        this.zDepth = zDepth;
        this.thickness = thickness;
        this.midPointAndHalfDimensions = new Vector4f((startX + endX) * 0.5f, (startY + endY) * 0.5f,Math.abs(endX - startX) * 0.5f, Math.abs(endY - startY) * 0.5f);
    }

    public Line(Vector4f midPointAndHalfDimensions, Vector4f color, float zDepth, float thickness) {
        this(midPointAndHalfDimensions, color, color, zDepth, thickness);
    }

    public Line(Vector4f midPointAndHalfDimensions, Vector4f color1, Vector4f color2, float zDepth, float thickness) {
        this.midPointAndHalfDimensions = midPointAndHalfDimensions;
        this.color1 = color1;
        this.color2 = color2;
        this.zDepth = zDepth;
        this.thickness = thickness;
    }

    public void setStartAndEnd(float startX, float startY, float endX, float endY){
        midPointAndHalfDimensions.x = (startX + endX) * 0.5f;
        midPointAndHalfDimensions.y = (startY + endY) * 0.5f;
        midPointAndHalfDimensions.z = Math.abs(endX - startX) * 0.5f;
        midPointAndHalfDimensions.w = Math.abs(endY - startY) * 0.5f;
    }

    public void setColorReference(Vector4f color){
        color1 = color;
        color2 = color;
        colorWereIndependent = false;
    }

    public void setColorReference(Vector4f color1, Vector4f color2){
        this.color1 = color1;
        this.color2 = color2;
        colorWereIndependent = false;
    }

    public void setColor(Vector4f color){
        setColor(color, color);
    }

    public void setColor(Vector4f color1, Vector4f color2){
        if(colorWereIndependent) {
            this.color1.set(color1);
            this.color2.set(color2);
        }else {
            this.color1 = new Vector4f(color1);
            this.color2 = new Vector4f(color2);
            colorWereIndependent = true;
        }
    }

    @Override
    public void setPos(double x, double y) {
        midPointAndHalfDimensions.x = (float) x;
        midPointAndHalfDimensions.y = (float) y;
    }
    @Override
    public void setSize(double sizeX, double sizeY) {
        midPointAndHalfDimensions.z = (float) sizeX * 0.5f;
        midPointAndHalfDimensions.w = (float) sizeY * 0.5f;
    }
    @Override
    public void show() {
        if(alive) return;
        LineRenderer.addLine(this);
    }
    @Override
    public void hide() {
        alive = false;
    }
    @Override
    public Vector4f getDimensionAndPivot() {return new Vector4f(2 * Math.abs(midPointAndHalfDimensions.z), 2 * Math.abs(midPointAndHalfDimensions.w), 0.5f, 0.5f);}

    @Override
    public void setRelativeToCamera(boolean isRelativeToCamera) {
        this.relativeToCamera = isRelativeToCamera;
    }

    @Override
    public boolean isCoordsLocal() {return usedLocalCoords;}

    @Override
    public void setZDepth(float zDepth) {
        this.zDepth = zDepth;
    }
}
