package entity;

import component.AdvPacket;
import component.SimpleBox;
import component.Sprite;
import core.Game;
import data.Space;

import static core.Game.renderSystem;
import static entity.EntityLoader.getEntitySheet;

public class Portal extends Entity implements Linkable{

    public static final String name = "portal";
    private int linkedID = -1;
    Portal linkedPortal = null;
    AdvPacket portalPack, shadowPack;

    protected Portal(){}

    public void init(Space spaceLocation, double x, double y, int state){
        super.init(spaceLocation, x, y, state);
        portalPack = new AdvPacket(portalSprite, baseZDepth);
        shadowPack = new AdvPacket(shadowSprite, baseZDepth);
        hitBox = new SimpleBox(0.5, 0.5);

        relativeYRep = -0.125f;
    }

    @Override
    public void tick() {
        hitBox.setPos(x, y);
    }

    @Override
    public void frame(float alpha) {
        syncFramePosition();
        yRepStuff();
        portalPack.setPos(frameX, frameY + frameZ * Game.cosineViewAngle);
        shadowPack.setPos(frameX, frameY);
    }

    @Override
    public void setShown(boolean shownQ) {
        if(shownQ) {
            renderSystem.activate(portalPack);
            renderSystem.activate(shadowPack);
        }else {
            portalPack.alive = false;
            shadowPack.alive = false;
        }
    }

    @Override
    public int getEntityID() {
        return EntityLoader.PORTAL_ID;
    }
    @Override
    public String getName() {
        return name;
    }

    @Override
    public float yRepStuff() {
        float yRep = super.yRepStuff();
        shadowPack.yRep = yRep + 0.25f;
        portalPack.yRep = yRep;
        return yRep;
    }

    @Override
    public void respawn() {
        super.respawn();
        z = 0.4;
    }

    @Override
    public void changeState(int state) {
        super.changeState(state);
        System.out.println("override change state");
    }

    public static Sprite portalSprite;
    public static Sprite shadowSprite;
    public static Sprite[] loadSprite(){
        portalSprite = getEntitySheet().crop(32, 0, 16, 25);
        portalSprite.anchorY = 0f;
        shadowSprite = getEntitySheet().crop(32, 28, 9, 4);

        return new Sprite[]{portalSprite, shadowSprite};
    }

    @Override
    public void setLinkedObject(Linkable object, int id) {
        linkedID = id;
        linkedPortal = (Portal) object;
    }

    @Override
    public Linkable getLinkedObject() {
        return linkedPortal;
    }

    @Override
    public int getLinkedID() {
        return linkedID;
    }
}
