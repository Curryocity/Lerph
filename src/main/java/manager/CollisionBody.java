package manager;

import block.Block;
import block.BlockLoader;
import data.Chunk;
import data.Space;
import entity.Entity;

import java.util.ArrayList;
import java.util.List;

import static block.Block.*;

public class CollisionBody {
    public final static double LOWEST_LAVA_STEP_ABLE = -0.1;
    public final static double LOWEST_FLOOR_STEP_ABLE = -0.6;
    public Entity owner;
    private Space loc;
    private boolean aboveFloorLevel = true;
    public double collisionX, collisionY;
    public boolean collideq;
    final List<Entity> entitiesInContact = new ArrayList<>();

    public CollisionBody(Entity entity, Space spaceLocation) {
        this.owner = entity;
        this.loc = spaceLocation;
    }

    public void setSpaceLocation(Space spaceLocation) {
        this.loc = spaceLocation;
    }

    public void updateCollisionState(boolean trueXfalseY){
        aboveFloorLevel = owner.z >= LOWEST_FLOOR_STEP_ABLE;
        double newX = owner.x, newY = owner.y;
        int layers;
        double direction = trueXfalseY? Math.signum(owner.vx) : Math.signum(owner.vy);

        if(trueXfalseY){
            newX += owner.vx;
            layers = (int) Math.abs(owner.vx);
            for(int i = layers; i >= 0; i--){
                calculateCollisionPos(newX - i * direction , newY, true);
                if(collideq) break;
            }
        }else{
            newY += owner.vy;
            layers = (int) Math.abs(owner.vy);
            for(int i = layers; i >= 0; i--){
                calculateCollisionPos(newX, newY - i * direction, false);
                if(collideq) break;
            }
        }
    }


    private void calculateCollisionPos(double newX, double newY, boolean trueXfalseY){
        collideq = isColliding(newX, newY);
        if (!collideq) return;

        double halfDx = owner.hitBox.halfDx;
        double halfDy = owner.hitBox.halfDy;
        if (trueXfalseY) {
            collisionX = (owner.vx > 0)
                    ? Math.floor(newX + halfDx) - halfDx - 1e-7
                    : Math.ceil(newX - halfDx) + halfDx + 1e-7;
        } else {
            collisionY = (owner.vy > 0)
                    ? Math.floor(newY + halfDy) - halfDy - 1e-7
                    : Math.ceil(newY - halfDy) + halfDy + 1e-7;
        }
    }

    public boolean isColliding(double x, double y){
        return isColliding(x, y, 0, 0);
    }

    public boolean isColliding(double x, double y, double expandedX, double expandedY){
        aboveFloorLevel = owner.z >= LOWEST_FLOOR_STEP_ABLE;
        double halfDx = owner.hitBox.halfDx, halfDy = owner.hitBox.halfDy;

        if (aboveFloorLevel) {
            return isObstacle(loc.getBlockID(x - halfDx - expandedX, y - halfDy - expandedY)) ||
                    isObstacle(loc.getBlockID(x + halfDx + expandedX, y - halfDy - expandedY)) ||
                    isObstacle(loc.getBlockID(x + halfDx + expandedX, y + halfDy + expandedY)) ||
                    isObstacle(loc.getBlockID(x - halfDx - expandedX, y + halfDy + expandedY));
        }else {
            return !( isVoid(loc.getBlockID(x - halfDx - expandedX, y - halfDy - expandedY)) &&
                    isVoid(loc.getBlockID(x + halfDx + expandedX, y - halfDy - expandedY)) &&
                    isVoid(loc.getBlockID(x + halfDx + expandedX, y + halfDy + expandedY)) &&
                    isVoid(loc.getBlockID(x - halfDx - expandedX, y + halfDy + expandedY)) );
        }
    }

    static final double[] xOffset = {-1, 1, 1, -1};
    static final double[] yOffset = {-1, -1, 1, 1};
    public double getMaximumNonAirSlip(){
        double maxSlip = 0;

        for(int i = 0; i < 4; i++){
            double checkX = owner.x + xOffset[i] * owner.hitBox.halfDx;
            double checkY = owner.y + yOffset[i] * owner.hitBox.halfDy;

            Block checkBlock = BlockLoader.blocks[loc.getBlockID(checkX, checkY)];
            if(checkBlock.blockType == GROUND && checkBlock.slip > maxSlip){
                maxSlip = checkBlock.slip;
            }
        }

        return maxSlip;
    }

    public boolean isBottomSupported(){
        double minX = owner.hitBox.posX - owner.hitBox.halfDx;
        double maxX = owner.hitBox.posX + owner.hitBox.halfDx;
        double minY = owner.hitBox.posY - owner.hitBox.halfDy;
        double maxY = owner.hitBox.posY + owner.hitBox.halfDy;
        return isGround(loc.getBlockID(minX, minY)) || isGround(loc.getBlockID(minX, maxY)) || isGround(loc.getBlockID(maxX, minY)) || isGround(loc.getBlockID(maxX, maxY));
    }

    public boolean isInFluid(){
        double minX = owner.hitBox.posX - owner.hitBox.halfDx;
        double maxX = owner.hitBox.posX + owner.hitBox.halfDx;
        double minY = owner.hitBox.posY - owner.hitBox.halfDy;
        double maxY = owner.hitBox.posY + owner.hitBox.halfDy;
        return isFluid(loc.getBlockID(minX, minY)) && isFluid(loc.getBlockID(minX, maxY)) && isFluid(loc.getBlockID(maxX, minY)) && isFluid(loc.getBlockID(maxX, maxY));
    }

    public List<Entity> updateContactEntities(){
        entitiesInContact.clear();
        Chunk[] checkChunks = loc.getCloseByChunks(owner.x, owner.y, 1,1);
        List<Entity> nearEntities = new ArrayList<>();

        for(Chunk chunks : checkChunks){
            nearEntities.addAll(chunks.getAllEntities());
        }
        for (Entity entity : nearEntities){
            if(entity.hitBox == null) continue;
            if( (Math.abs(entity.x - owner.x) <= entity.hitBox.halfDx + owner.hitBox.halfDx + 1e-7) &&
                    (Math.abs(entity.y - owner.y) <= entity.hitBox.halfDy + owner.hitBox.halfDy + 1e-7) ){
                entitiesInContact.add(entity);
            }
        }
        return entitiesInContact;
    }

    public List<Entity> getNearEntities(double distanceX, double distanceY){
        boolean contactQ = false;
        Chunk[] checkChunks = loc.getCloseByChunks(owner.x, owner.y, distanceX, distanceY);
        List<Entity> nearEntities = new ArrayList<>();

        for(Chunk chunks : checkChunks){
            nearEntities.addAll(chunks.getAllEntities());
        }

        return nearEntities;
    }

}
