package entity;

import component.SimpleBox;
import component.Sprite;
import data.Space;

import static entity.EntityLoader.*;
import static renderer.ChunkRenderer.floorZDepth;

public abstract class Entity {
    public static final float baseZDepth = floorZDepth;
    Space spaceLocation;
    public double initX, initY;
    private int initState;

    public boolean isAlive;
    public SimpleBox hitBox;

    public double x, y, z;
    public double vx, vy, vz;
    float prev_x, prev_y, prev_z;
    public float frameX, frameY, frameZ;

    public float relativeYRep = 0;
    // subtract this from y to get yRep

    protected Entity(){}

    public void init(Space spaceLocation, double x, double y, int state){
        this.spaceLocation = spaceLocation;
        this.initX = x;
        this.initY = y;
        changeState(state);
    }

    public void respawn(){
        isAlive = true;
        x = initX; y = initY; z = 0;
        vx = 0; vy = 0; vz = 0;
        changeState(initState);
    }


    public void spawn(Space spaceLocation, double spawnX, double spawnY){
        this.spaceLocation = spaceLocation;
        x = spawnX;
        y = spawnY;
        isAlive = true;
        spaceLocation.summon(this, spawnX, spawnY);
    }

    abstract public void tick();

    abstract public void frame(float alpha);

    public void die(){
        isAlive = false;
    }

    public void syncFramePosition(){
        frameX = (float) x;
        frameY = (float) y;
        frameZ = (float) z;
    }

    public void lerpFramePosition(float alpha){
        frameX =  prev_x * ( 1 - alpha ) + (float) x * alpha;
        frameY =  prev_y * ( 1 - alpha ) + (float) y * alpha;
        frameZ =  prev_z * ( 1 - alpha ) + (float) z * alpha;
    }

    abstract public void setShown(boolean shownQ);

    abstract public int getEntityID();

    abstract public String getName();

    public float yRepStuff(){
        return frameY + relativeYRep;
    }

    public int getState(){
        return initState;
    }

    public void setInitPos(double x, double y){
        initX = x;
        initY = y;
    }

    public void changeState(int state){
        initState = state;
    }

    public static Entity newEntity(int entityID){
        return switch (entityID) {
            case CHECKPOINT_ID -> new CheckPoint();
            case PORTAL_ID -> new Portal();
            default -> null;
        };

    }

}
