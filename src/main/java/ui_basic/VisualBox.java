package ui_basic;

import component.AdvPacket;
import component.Line;
import component.SimpleBox;
import core.Game;
import manager.MouseInput;
import org.joml.Vector2f;
import org.joml.Vector4f;

import static ui_basic.ColorPalette.limeGreen;
import static ui_basic.ColorPalette.white;

public class VisualBox extends SimpleBox implements AlignAble {
    private boolean usedLocalCoords, relativeToCamera;
    private boolean showEdge, showFill;
    private boolean shown;
    private final Line[] edges;
    public final AdvPacket fillQuad;

    public VisualBox(double halfDx, double halfDy, float thickness, float zDepth, Vector4f edgeColor, Vector4f fillColor) {
        super(halfDx, halfDy);

        if(edgeColor != null) showEdge = true;
        else edgeColor = new Vector4f(white);

        if(fillColor != null) showFill = true;
        else fillColor = new Vector4f(limeGreen);

        edges = new Line[]{new Line(edgeColor, zDepth),
                new Line(edgeColor, zDepth),
                new Line(edgeColor, zDepth),
                new Line(edgeColor, zDepth)};
        fillQuad = new AdvPacket(fillColor, (float) (2 * halfDx), (float) (2 * halfDy), zDepth, usedLocalCoords);
        regenerateEdges(thickness);

        setEdgeShown(showEdge);
        setFillShown(showFill);
    }

    public VisualBox(SimpleBox simpleBox, float thickness, float zDepth, Vector4f edgeColor, Vector4f fillColor) {
        this(simpleBox.halfDx, simpleBox.halfDy, thickness, zDepth, edgeColor, fillColor);
        setPos(simpleBox.posX, simpleBox.posY);
    }

    public VisualBox(AdvPacket fillQuad, float thickness, Vector4f edgeColor) {
        super(fillQuad.scaleX/2, fillQuad.scaleY/2);

        showFill = true;
        float zDepth = fillQuad.zDepth;

        if(edgeColor != null) showEdge = true;
        else edgeColor = new Vector4f(white);



        edges = new Line[]{new Line(edgeColor, zDepth),
                new Line(edgeColor, zDepth),
                new Line(edgeColor, zDepth),
                new Line(edgeColor, zDepth)};
        this.fillQuad = fillQuad;
        regenerateEdges(thickness);

        setEdgeShown(showEdge);
        setFillShown(showFill);
    }

    public void regenerateEdges(float thickness){
        Vector2f[] corners = this.getCorners();

        if(thickness <= 0){
            thickness = edges[0].thickness;
        }

        for (int i = 0; i < 4; i++) {
            edges[i].setStartAndEnd(corners[i].x, corners[i].y, corners[(i + 1) % 4].x, corners[(i + 1) % 4].y);
            edges[i].thickness = thickness;

            float thicknessScaled = thickness * (this.usedLocalCoords ? 1f/Game.getScale(Game.BLOCK_SCALE) : 1);
            if(i == 0 || i == 2){
                edges[i].midPointAndHalfDimensions.z += thicknessScaled * 0.5f;
            }else{
                edges[i].midPointAndHalfDimensions.w += thicknessScaled * 0.5f;
            }

            edges[i].relativeToCamera = this.relativeToCamera;
            edges[i].usedLocalCoords = this.usedLocalCoords;

        }
    }

    public void setEdgeColor(Vector4f edgeColor){
        boolean prevEdgeShown = showEdge;
        if(edgeColor != null){
            for (Line edge : edges) { edge.setColorReference(edgeColor);}
            showEdge = true;
        }else{
            showEdge = false;
        }

        if(prevEdgeShown != showEdge){
            setEdgeShown(showEdge);
        }
    }

    public void setFillColor(Vector4f fillColor){
        boolean prevFillShown = showFill;
        if(fillColor != null){
            fillQuad.setColors(fillColor);
            showFill = true;
        }else{
            showFill = false;
        }

        if(prevFillShown != showFill){
            setFillShown(showFill);
        }
    }

    public void setEdgeShown(boolean showEdge){
        this.showEdge = showEdge;
        if(showEdge && shown){
            for (Line edge : edges) { edge.show();}
        }else {
            for (Line edge : edges) { edge.hide();}
        }
    }

    public void setFillShown(boolean showFill){
        this.showFill = showFill;
        if(showFill && shown){
            fillQuad.show();
        }else {
            fillQuad.hide();
        }
    }

    @Override
    public void setPos(double x,double y){
        double deltaX = x - posX;
        double deltaY = y - posY;
        for (Line edge : edges) {
            edge.setPos(edge.midPointAndHalfDimensions.x + deltaX, edge.midPointAndHalfDimensions.y + deltaY);
        }
        fillQuad.setPos(x, y);

        posX = x;
        posY = y;
    }

    public void movePos(double dx, double dy){
        setPos(posX + dx, posY + dy);
    }

    public void setUsedLocalCoords(boolean usedLocalCoords){
        this.usedLocalCoords = usedLocalCoords;
        fillQuad.usedLocalCoords = usedLocalCoords;
        for (Line edge : edges) {
            edge.usedLocalCoords = usedLocalCoords;
        }
        regenerateEdges(-1);
    }

    @Override
    public void setRelativeToCamera(boolean isRelativeToCamera) {
        this.relativeToCamera = isRelativeToCamera;
        fillQuad.relativeToCamera = relativeToCamera;
        for (Line edge : edges) {
            edge.relativeToCamera = relativeToCamera;
        }
    }

    public void setThickness(float thickness){
        regenerateEdges(thickness);
    }

    @Override
    public void setSize(double sizeX, double sizeY) {
        halfDx = sizeX/2;
        halfDy = sizeY/2;
        if(showEdge) regenerateEdges(-1);
        if(fillQuad != null) fillQuad.setSize(sizeX, sizeY);
    }

    @Override
    public void show() {
        shown = true;
        if(showEdge){ for (Line edge : edges) { edge.show();}}
        if(showFill){ fillQuad.show();}
    }
    @Override
    public void hide() {
        shown = false;
        for (Line edge : edges) { edge.hide();}
        fillQuad.hide();
    }

    Vector4f dimensionAndPivot = new Vector4f();
    @Override
    public Vector4f getDimensionAndPivot() {
        dimensionAndPivot.x = (float) (2 * halfDx);
        dimensionAndPivot.y = (float) (2 * halfDy);
        dimensionAndPivot.z = 0.5f;
        dimensionAndPivot.w = 0.5f;
        return dimensionAndPivot;
    }

    @Override
    public boolean isCoordsLocal() {
        return usedLocalCoords;
    }

    @Override
    public void setZDepth(float zDepth) {
        fillQuad.zDepth = zDepth;
        for (Line edge : edges) {
            edge.zDepth = zDepth;
        }
    }

    public float getZDepth(){
        return fillQuad.zDepth;
    }

    public boolean isHovered(boolean detectIfShown){
        return (shown || !detectIfShown) && include(MouseInput.getX(), MouseInput.getY());
    }

    public boolean isPressed(int button, boolean detectIfShown){
        return (shown || !detectIfShown) && include(MouseInput.getX(), MouseInput.getY()) && include(MouseInput.getLastPressedX(button), MouseInput.getLastPressedY(button)) && MouseInput.isPressed(button);
    }

    public boolean hasQueue(int button, boolean detectIfShown){
        return (shown || !detectIfShown) && include(MouseInput.getX(), MouseInput.getY()) && include(MouseInput.getLastPressedX(button), MouseInput.getLastPressedY(button)) && MouseInput.isQueued(button);
    }

    public boolean useQueue(int button, boolean detectIfShown){
        if( (shown || !detectIfShown) && include(MouseInput.getX(), MouseInput.getY()) && include(MouseInput.getLastPressedX(button), MouseInput.getLastPressedY(button)) ){
            return MouseInput.useQueue(button);
        }
        else return false;
    }


}
