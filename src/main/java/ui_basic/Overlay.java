package ui_basic;

import component.AdvPacket;
import core.Application;

public abstract class Overlay extends Panel{
    protected AdvPacket overlayBG = new AdvPacket(0f, 0f, 0f, 0.5f , 50f, false);
    private boolean hasBG;

    protected Overlay(boolean hasBG){
        super();
        this.hasBG = hasBG;
        overlayBG.setPos(0,0);
        overlayBG.setSize(Application.getAbsoluteX(2), Application.getAbsoluteY(2));
        overlayBG.setRelativeToCamera(true);
        setRelativeToCamera(true);
    }

    @Override
    public void update(){
        super.update();
    }

    @Override
    public void show(){
        super.show();
        if(hasBG) overlayBG.show();
    }

    @Override
    public void hide(){
        super.hide();
        overlayBG.hide();
    }

    public void setHasBG(boolean hasBG){
        this.hasBG = hasBG;
    }

    public void handleResize(){
        this.refresh();
        overlayBG.setPos(0,0);
        overlayBG.setSize(Application.getAbsoluteX(2), Application.getAbsoluteY(2));
    }

    public void setOverlayBG(float brightness, float alpha, float zDepth){
        overlayBG.setColors(brightness, brightness, brightness, alpha);
        overlayBG.zDepth = zDepth;
    }

    public void setOverlayBG(float r, float g, float b, float alpha, float zDepth){
        overlayBG.setColors(r, g, b, alpha);
        overlayBG.zDepth = zDepth;
    }
}
