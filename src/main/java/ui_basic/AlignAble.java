package ui_basic;

import org.joml.Vector4f;

public interface AlignAble {
    public void setPos(double x, double y);
    public void setSize(double sizeX, double sizeY);
    public void show();
    public void hide();
    public Vector4f getDimensionAndPivot();
    public void setRelativeToCamera(boolean isRelativeToCamera);
    public boolean isCoordsLocal();
    void setZDepth(float zDepth);
}
