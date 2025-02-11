package entity;

import component.AdvPacket;
import component.SimpleBox;
import component.Sprite;
import core.Application;
import core.Game;
import data.Space;

import static core.Game.renderSystem;
import static entity.EntityLoader.getEntitySheet;

public class CheckPoint extends Entity{
    final public static String name = "checkpoint";
    final private static float crystalBalancedHeight = 0.4f, crystalWaveMagnitude = 0.25f, crystalWavePeriod = 1.5f;
    private float crystalTimer;
    private AdvPacket base, crystal;


    protected CheckPoint() {}

    @Override
    public void init(Space spaceLocation, double x, double y, int state){
        super.init(spaceLocation, x, y, state);

        crystal = new AdvPacket(crystalSprite, baseZDepth);
        base = new AdvPacket(baseSprite, baseZDepth);

        hitBox = new SimpleBox(0.5, 0.5);

        relativeYRep = -0.25f;
    }

    @Override
    public void tick() {
        hitBox.setPos(x, y);
    }

    @Override
    public void frame(float alpha) {
        double crystalPeriodInFrame = Application.secToFrame(crystalWavePeriod);
        z = crystalBalancedHeight + crystalWaveMagnitude * Math.sin(crystalTimer / crystalPeriodInFrame * 2 * Math.PI);
        if(crystalTimer > crystalPeriodInFrame){
            crystalTimer = 0;
        }
        crystalTimer += 1;

        syncFramePosition();
        yRepStuff();

        base.setPos(frameX, frameY);
        crystal.setPos(frameX, frameY + frameZ * Game.cosineViewAngle);
    }

    @Override
    public void setShown(boolean shownQ) {
        if(shownQ){
            renderSystem.activate(base);
            renderSystem.activate(crystal);
        }else{
            base.alive = false;
            crystal.alive = false;
        }
    }

    @Override
    public int getEntityID() {
        return EntityLoader.CHECKPOINT_ID;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public float yRepStuff() {
        float yRep = super.yRepStuff();
        base.yRep = yRep + 0.5f;
        crystal.yRep = yRep;
        return yRep;
    }

    @Override
    public void respawn(){
        super.respawn();
        crystalTimer = 0;
    }

    public static Sprite crystalSprite;
    public static Sprite baseSprite;
    public static Sprite[] loadSprite(){

        crystalSprite = getEntitySheet().crop(0, 0, 16, 16);
        crystalSprite.anchorX = 7.5f/16.0f;
        crystalSprite.anchorY = 2.0f/16.0f;

        baseSprite = getEntitySheet().crop(16, 0, 16, 16);

        return new Sprite[]{crystalSprite, baseSprite};
    }

}
