package ui_basic;

import component.AdvPacket;
import core.Application;
import core.Game;
import org.joml.Vector4f;

import java.util.HashMap;
import java.util.Map;

public class Panel implements AlignAble, Updatable{
    final Map<AlignAble, Vector4f> contentsLayout; // layout data: xPos, yPos, xSize, ySize
    Map<AlignAble, String> idComponentLookup;
    float relX, relY;
    protected float x, y;
    float relHalfDx, relHalfDy;
    float halfDx, halfDy;
    boolean shown = true;
    private boolean relativeToCamera;

    private float xBound = 1f, yBound = 1f;

    VisualBox bg;

    // covers the whole window
    public Panel(){
        this.contentsLayout = new HashMap<>();
        setRelativePos(0,0);
        setRelativeSize(1f, 1f);
    }

    public Panel(float r1, float g1, float b1, float a1, float r2, float g2, float b2, float a2, int gradientType, float bgDepth){
        this();
        setBackGround(r1, g1, b1, a1, r2, g2, b2, a2, gradientType, bgDepth);
    }

    public Panel(float r, float g, float b, float a, float bgDepth){
        this();
        setBackGround(r, g, b, a, bgDepth);
    }

    public Panel(float xParam, float yParam, float dxParam, float dyParam, boolean relQ){
        this.contentsLayout = new HashMap<>();
        if(relQ){
            setRelativePos(xParam, relY);
            setRelativeSize(relHalfDx, relHalfDy);
        }else{
            setAbsolutePos(xParam, yParam);
            setSize(dxParam, dyParam);
        }
    }

    /**
     * Adds a component to this panel and applies the given layout data.
     * The layout data is a Vector4f where the x and y coordinates represent the relative position of the component in the panel,
     * If the input value is within the range of [-1,1], it is interpreted as a coordinate relative to the window with origin at the center of the window, and the end points of the window being -1 and 1.
     * If the input value is outside of this range, it is interpreted as a padding size in pixels relative to the corresponding edge of the window.
     * and the z and w coordinates represent the relative size of the component in the panel.
     * if layout data is null, it defaults to Vec4(0, 0, 1, 1), which usually means covering the entire panel
     * @param component the component to add
     * @param layoutData the layout data to apply to the component
     */
    public void addComponent(AlignAble component, Vector4f layoutData, boolean shown){
        if(layoutData == null) layoutData = new Vector4f(0, 0, 1, 1);

        if(shown) component.show();
        else component.hide();

        this.contentsLayout.put(component, layoutData);
        alignComponent(component);
    }

    public void addComponent(AlignAble component, Vector4f layoutData){
        addComponent(component, layoutData, this.shown);
    }

    public void addComponent(AlignAble component, Vector4f layoutData, String id, boolean shown){
        if(layoutData == null) layoutData = new Vector4f(0, 0, 1, 1);

        if(shown) component.show();
        else component.hide();

        if(this.idComponentLookup == null){
            this.idComponentLookup = new HashMap<>();
        }

        this.idComponentLookup.put(component, id);
        this.contentsLayout.put(component, layoutData);
        alignComponent(component);
    }

    public void addComponent(AlignAble component, Vector4f layoutData, String id){
        addComponent(component, layoutData, id, this.shown);
    }

    public void changeID(AlignAble component, String id){
        if(this.idComponentLookup == null){
            this.idComponentLookup = new HashMap<>();
        }
        if(this.contentsLayout.get(component) == null) return;
        this.idComponentLookup.put(component, id);
    }

    public void changeLayout(AlignAble component, float x, float y, float dx, float dy){
        if(this.contentsLayout.get(component) == null) return;
        this.contentsLayout.get(component).set(x, y, dx, dy);
        alignComponent(component);
    }

    public AlignAble getComponent(String id){
        if(this.idComponentLookup == null) return null;

        for (AlignAble component : idComponentLookup.keySet()){
            if(this.idComponentLookup.get(component).equals(id)){
                return component;
            }
        }
        return null;
    }

    public void removeComponent(AlignAble component){
        if(component == null) return;
        component.hide();
        this.contentsLayout.remove(component);
        if (this.idComponentLookup != null) {
            this.idComponentLookup.remove(component);
        }
    }

    @Override
    public void update(){
        for (AlignAble c : contentsLayout.keySet()){
            if(c instanceof Updatable u) u.update();
        }
    }

    public void setAbsolutePos(float x, float y){
        this.x = x;
        this.y = y;
        this.relX = Application.getRelativeX(x);
        this.relY = Application.getRelativeY(y);
        rePosAllComponents();
    }

    public void setRelativePos(float xParam, float yParam){
        this.relX = xParam;
        this.relY = yParam;
        if(Math.abs(xParam) <= 1){
            this.x = Application.getAbsoluteX(xParam);
        }else{
            this.x = Application.getAbsoluteX(0) + Math.signum(xParam) * (Application.viewPortWidth * 0.5f - Math.abs(xParam));
        }
        if (Math.abs(yParam) <= 1){
            this.y = Application.getAbsoluteY(yParam);
        }else{
            this.y = Application.getAbsoluteY(0) + Math.signum(yParam) * (Application.viewPortHeight * 0.5f - Math.abs(yParam));
        }
        rePosAllComponents();
    }

    public void setSize(float dx, float dy){
        this.halfDx = dx * 0.5f;
        this.halfDy = dy * 0.5f;
        this.relHalfDx = Application.getRelativeX(dx) * 0.25f;
        this.relHalfDy = Application.getRelativeY(dy) * 0.25f;
        reSizeAllComponents();
    }

    public void setRelativeSize(float relDx, float relDy){
        this.relHalfDx = relDx * 0.5f;
        this.relHalfDy = relDy * 0.5f;
        this.halfDx = Application.getAbsoluteX(relDx);
        this.halfDy = Application.getAbsoluteY(relDy);
        reSizeAllComponents();
    }

    public void alignComponent(AlignAble component){
        rePosComponent(component);
        reSizeComponent(component);
    }

    public void rePosComponent(AlignAble component){
        Vector4f layoutData = contentsLayout.get(component);
        if(layoutData == null) return;
        float x, y;

        Vector4f dimensionAndPivot = component.getDimensionAndPivot();
        if(Math.abs(layoutData.x) <= xBound){
            x = this.x + this.halfDx * layoutData.x;
        }else{
            x = this.x + Math.signum(layoutData.x) * (this.halfDx - 0.5f * dimensionAndPivot.x) - layoutData.x + dimensionAndPivot.x * (dimensionAndPivot.z - 0.5f);
            // panelCenterX +- PanelHalfDx (to reach the edge of the panel)
            // padding + ComponentHalfDx away from the edge
            // component's pivot offCenter correction(does not depend on the direction of alignment)
        }
        if(Math.abs(layoutData.y) <= yBound){
            y = this.y + this.halfDy * layoutData.y;
        }else {
            y = this.y + Math.signum(layoutData.y) * (this.halfDy - 0.5f * dimensionAndPivot.y) - layoutData.y + dimensionAndPivot.y * (dimensionAndPivot.w - 0.5f);
        }

        if(component.isCoordsLocal()){
            component.setPos(x / Game.block_px, y / Game.block_px);
        }else{
            component.setPos(x, y);
        }

        component.setRelativeToCamera(this.relativeToCamera);
    }

    public void reSizeComponent(AlignAble component){
        Vector4f layoutData = contentsLayout.get(component);
        if(layoutData == null) return;
        float xSize = 2 * this.halfDx * layoutData.z;
        float ySize = 2 * this.halfDy * layoutData.w;
        if(component.isCoordsLocal()){
            component.setSize(xSize / Game.block_px, ySize / Game.block_px);
        }else{
            component.setSize(xSize, ySize);
        }
    }

    @Override
    public void setPos(double x, double y) {setAbsolutePos((float) x, (float) y);}
    @Override
    public void setSize(double sizeX, double sizeY) {setSize((float) sizeX, (float) sizeY);}
    @Override
    public void show(){
        shown = true;
        for (AlignAble component : contentsLayout.keySet()){ component.show();}
    }
    @Override
    public void hide(){
        shown = false;
        for (AlignAble component : contentsLayout.keySet()){ component.hide();}
    }

    Vector4f dimensionAndPivot = new Vector4f();
    @Override
    public Vector4f getDimensionAndPivot() {
        dimensionAndPivot.set(2 * halfDx, 2 * halfDy, 0.5f, 0.5f);
        return dimensionAndPivot;
    }

    @Override
    public boolean isCoordsLocal() {return false;}

    @Override
    public void setRelativeToCamera(boolean isRelativeToCamera) {
        this.relativeToCamera = isRelativeToCamera;
        for(AlignAble component : contentsLayout.keySet()) component.setRelativeToCamera(isRelativeToCamera);
    }

    @Override
    public void setZDepth(float zDepth) {
        float bgZdepth = (bg != null) ? bg.getZDepth() : -1;

        for (AlignAble component : contentsLayout.keySet()){
            component.setZDepth(zDepth);
        }

        if(bg != null) bg.setZDepth(bgZdepth);
    }

    public void rePosition(){ setRelativePos(relX, relY);}
    public void resize(){ setRelativeSize(2 * relHalfDx, 2 * relHalfDy);}
    public void rePosAllComponents(){ for (AlignAble component : contentsLayout.keySet()){ rePosComponent(component);} }
    public void reSizeAllComponents(){ for (AlignAble component : contentsLayout.keySet()){ reSizeComponent(component);} }
    public void refresh(){
        resize();
        rePosition();
    }

    public void setBackGround(float r1, float g1, float b1, float a1, float r2, float g2, float b2, float a2, int gradientType, float bgDepth){
        if(bg == null) {
            AdvPacket fillQuad = new AdvPacket(r1, g1, b1, a1, r2, g2, b2, a2, gradientType, bgDepth, false);
            bg = new VisualBox(fillQuad, 0, null);
        } else {
            bg.fillQuad.setColors(r1, g1, b1, a1, r2, g2, b2, a2, gradientType);
            bg.setZDepth(bgDepth);
        }

        addComponent(bg, null);
    }

    public void setBackGround(float r, float g, float b, float a, float bgDepth){
        if(bg == null) {
            AdvPacket fillQuad = new AdvPacket(r, g, b, a, bgDepth, false);
            bg = new VisualBox(fillQuad, 0, null);
        } else {
            bg.fillQuad.setColors(r, g, b, a);
            bg.setZDepth(bgDepth);
        }

        addComponent(bg, null);
    }

    public void setFrame(Vector4f frameColor, float bgDepth, float thickness){
        if(bg == null)
            bg = new VisualBox(0,0, thickness, bgDepth, frameColor, null);
        else {
            bg.setEdgeColor(frameColor);
            bg.setZDepth(bgDepth);
            bg.setThickness(thickness);
        }

        addComponent(bg, null);
    }

    public void setAllTextsColor(Vector4f color){
        for (AlignAble component : contentsLayout.keySet()){
            if(component instanceof Text) ((Text) component).setColor(color);
            if(component instanceof AdvText) ((AdvText) component).setIdleColor(color);
        }
    }

    public void setBounds(float xBounds, float yBounds){
        this.xBound = xBounds;
        this.yBound = yBounds;
    }

    public VisualBox getBackGround(){
        if(bg == null){
            bg = new VisualBox(0, 0 ,0, 0, null, null);
        }
        return bg;
    }

}
