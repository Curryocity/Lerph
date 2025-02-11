package entity;

import block.Block;
import block.BlockLoader;
import component.*;
import core.Application;
import data.Space;
import manager.*;
import manager.InputBind.InputType;
import renderer.RendererLibrary;

import java.util.ArrayList;
import java.util.List;

import static block.Block.DEFAULT_SLIP;
import static component.AdvPacket.GRADIENT_HORIZONTAL;
import static core.Application.secToFrame;
import static core.Game.*;
import static manager.KeyInput.*;

public class Player extends Entity{
    final String playerSheetPath = "assets/SpriteSheet/sigmoidSheetV5.png";

    final double acceleration = 0.32, dashBoost = 1, dashMomentum = 0.16;
    final double jumpVz = 0.5, jumpBoost = 0.09, superJumpBoost = 0.16, hyperJumpBoost = 0.25;
    final double boomExplosion = 0.36, boomRedirect = 0.2;
    final double airDrag = 0.97, airAco = 0.12, gravity = 0.12;
    final int maxStamina = 100, dashRecoveryRate = 4, dashCost = 60, dashDuration = 2, staminaRefund = 30;
    final int boomRecoveryRate = 4, boomCost = 50, coyoteDuration = 2;
    final int GROUND_DASH = 0, FLOATY_DASH = 1, SHARP_DASH = 2;

    public double[] normWasdVec = {0,0};
    double ax, ay, xBoost, yBoost, xForce, yForce, slip = DEFAULT_SLIP;
    int dashCountDown, dashState, dashStamina, boomStamina, coyotePhase;
    boolean inFluid, isJumpTick, wasGrounded;
    double spawnX = 0.5, spawnY = 0.5;
    Block directGround;

    BoomExecutor boomExe;
    CollisionBody collisionBody;
    StdPacket character;
    AdvPacket shadow;

    List<AdvPacket> dashTrails = new ArrayList<>();
    List<Float> trailTimes = new ArrayList<>();
    public Sprite[] runningAnimation, idleAnimation;
    int sprintCounter, blinkTimer;
    boolean rightQ, stopQ = true;

    public Player(){
        init();
    }
    private void init(){
        getPlayerImage();
        hitBox = new SimpleBox(0.3, 0.3);
        relativeYRep = -0.3f;
        collisionBody = new CollisionBody(this, null);
        boomExe = new BoomExecutor(this);
    }

    public void spawn(Space spaceLocation){
        spawn(spaceLocation, spaceLocation.getSpawnX(), spaceLocation.getSpawnY());
    }

    public void spawn(Space spaceLocation, double spawnX, double spawnY){
        this.spaceLocation = spaceLocation;
        this.spawnX = spawnX;
        this.spawnY = spawnY;
        collisionBody.setSpaceLocation(spaceLocation);
        respawn();
        setShown(true);
    }

    public void respawn(){
        reset(spawnX, spawnY);
        isAlive = true;
    }

    public void reset(double x, double y){
        this.x = x; this.y = y; this.z = 0;
        this.prev_x = (float) x; this.prev_y = (float) y; this.prev_z = (float) z;
        vx = 0.0;    vy = 0.0;      vz = 0;
        dashStamina = maxStamina;
        dashCountDown = 0;
        xBoost = 0;  yBoost = 0;
        blinkTimer = 0;
        sprintCounter = 0;
        runningAnimationID = 0;
        character.sprite = idleAnimation[0];
        character.scaleX = 1;
        hitBox.setPos(x, y);
    }

    public Sprite shadowSprite;
    public final static float characterAnchorY = 0.15f;
    public final static float shadowAnchorY = 0.8f;
    public void getPlayerImage(){
        SpriteSheet playerSheet = AssetPool.getSpriteSheet(playerSheetPath);
        RendererLibrary.addTexture(playerSheet.getTexture());
        playerSheet.gridCrop(16,16,7);
        playerSheet.crop(32,16, 13, 5);

        runningAnimation = playerSheet.getSprites(0, 4);
        Sprite.setOffset(runningAnimation, 0.5f, characterAnchorY);
        idleAnimation = playerSheet.getSprites(5, 6);
        Sprite.setOffset(idleAnimation, 0.5f, characterAnchorY);
        shadowSprite = playerSheet.getSprite(7);
        shadowSprite.anchorY = shadowAnchorY;

        character = new StdPacket(idleAnimation[0], baseZDepth);
        shadow = new AdvPacket(shadowSprite, baseZDepth);
    }

    @Override
    public void setShown(boolean shownQ) {
        if(shownQ){
            renderSystem.activate(character);
            renderSystem.activate(shadow);
        }else{
            character.alive = false;
            shadow.alive = false;
        }
    }

    public void tick() {
        if (spaceLocation == null) return;

        //Getting "WASD" input and transform it into movementVec(include normalization)
        normWasdVec = KeyInput.normWasdVec();
        xForce = 0;       yForce = 0;

        //Getting slipperiness of the block directly below player's center position
        directGround = BlockLoader.blocks[spaceLocation.getBlockID(x, y)];
        if(onGround() && directGround.blockType == Block.GROUND || collisionBody.isInFluid()){
            slip = directGround.slip;
        }else if (onGround()){
            double pendingSlip = collisionBody.getMaximumNonAirSlip();
            if(pendingSlip != 0 ) slip = pendingSlip;
        }

        dashStamina = Math.min(dashStamina + dashRecoveryRate, maxStamina);
        boomStamina = Math.min(boomStamina + boomRecoveryRate, maxStamina);

        dash();
        jump();
        boom();

        updateVelocity();
        prev_x = (float) x;  prev_y = (float) y;  prev_z = (float) z;
        updatePositionAndCollision();

        for(Entity entity : collisionBody.updateContactEntities()){
            switch (entity.getEntityID()){
                case (EntityLoader.CHECKPOINT_ID):
                    spawnX = entity.x;
                    spawnY = entity.y;
                    break;

                case (EntityLoader.PORTAL_ID):
                    System.out.println("portal");
                    break;
            }
        }

        deathHandling();
    }

    public boolean onGround(){
        return coyotePhase >= 0;
    }

    double recoveryVx = 0, recoveryVy = 0;
    public void dash(){
        // Dash input handling
        boolean hasMovement = normWasdVec[0] != 0 || normWasdVec[1] != 0;

        if (dashCountDown >= 0) dashCountDown--;
        if(hasMovement && (dashStamina >= dashCost) && dashCountDown == -1){
            if (useQueue(InputType.DASH)) {
                dashCountDown = dashDuration;
                dashStamina -= dashCost;
                if(onGround()){
                    dashState = GROUND_DASH;
                }else if(isKeyPressed(InputType.JUMP)){
                    dashState = FLOATY_DASH;
                    dashStamina -= staminaRefund;
                    recoveryVx = vx; recoveryVy = vy;
                    vx *= 0.25;  vy *= 0.25;  vz = 0.56;
                }else {
                    dashState = SHARP_DASH;
                }
            }
        }

        // as boost is designed not to be considered in
        // next tick vel calculation -> remove prev_boost
        vx -= xBoost;
        vy -= yBoost;

        // Dash movement
        if (dashCountDown > 0) {
            if (hasMovement) { // constant boost during the dash
                xBoost = normWasdVec[0] * dashBoost * ((dashState != FLOATY_DASH) ? 1 : 0.75);
                yBoost = normWasdVec[1] * dashBoost * ((dashState != FLOATY_DASH) ? 1 : 0.75);
            } else { // partially refund if dash forces to end
                dashStamina += staminaRefund;
                dashCountDown = 0;
                vx += xBoost / dashBoost * acceleration;
                vy += yBoost / dashBoost * acceleration;
            }
        } else {
            xBoost = 0;
            yBoost = 0;
        }

        if (dashCountDown == 0) { // gain momentum at the end of the dash
            if(dashState != FLOATY_DASH) {
                xForce = normWasdVec[0] * dashMomentum;
                yForce = normWasdVec[1] * dashMomentum;
            }else {
                xForce = normWasdVec[0] * dashMomentum * 0.4;
                yForce = normWasdVec[1] * dashMomentum * 0.4;
                vz = 0.2;
            }

            if (vx * normWasdVec[0] < 0) vx = 0;
            if (vy * normWasdVec[1] < 0) vy = 0;
        }
    }

    public void jump(){
        isJumpTick = false;
        if (isKeyPressed(InputType.JUMP) && onGround()) {
            isJumpTick = true;

            double theJumpBoost = jumpBoost;

            if(vz > 0) {
                dashStamina += dashCost;
                theJumpBoost = superJumpBoost;
            }

            vz = (inFluid ? 0.6 : 1) * jumpVz;

            if(dashCountDown >= 0){
                if(dashState == GROUND_DASH){
                    theJumpBoost = superJumpBoost;
                    dashStamina -= staminaRefund;
                    vz += 0.08;
                }else if(dashState == SHARP_DASH){
                    theJumpBoost = hyperJumpBoost;
                }
            }

            xForce = theJumpBoost * normWasdVec[0];
            yForce = theJumpBoost * normWasdVec[1];

            dashCountDown = -1;
        }
    }

    public void boom(){
        if( !(z > -2 && boomStamina >= boomCost)) return;

        if (useQueue(InputType.BOOM) ) {
            if(boomExe.boom()) {
                if (!onGround()) {
                    vz = Math.min(Math.max(jumpVz * 0.5, vz + jumpVz * 0.25), jumpVz);
                } else if (isJumpTick) {
                    vz = jumpVz * (1 + 0.5) / 2;
                    // idk, boomJump for distance is unbalanced, the jump height would be the middleGround ><
                } else {
                    vz = jumpVz * 0.5;
                }

                if(boomExe.forceX != 0){
                    xForce += boomExe.forceX * boomExplosion + normWasdVec[0] * boomRedirect;
                }
                if(boomExe.forceY != 0){
                    yForce += boomExe.forceY * boomExplosion + normWasdVec[1] * boomRedirect;
                }

                boomStamina -= boomCost;

                isJumpTick = true;
            }
        }
    }

    public void updateVelocity(){

        if (coyotePhase != coyoteDuration && !isJumpTick) {
            double gCo = 1;
            if(inFluid) gCo = 0.5;

            vz = Math.max(vz - gravity * gCo, -0.8);

            if(dashCountDown > 0 && dashState == SHARP_DASH){
                vz = Math.min(-0.5, vz);
            }
        }

        // inertia threshold: 0.01
        if (Math.abs(vx) < 0.01) vx = 0;
        if (Math.abs(vy) < 0.01) vy = 0;
        if (Math.abs(vz) < 0.01) vz = 0;

        ax = normWasdVec[0] * acceleration;
        ay = normWasdVec[1] * acceleration;

        double aCo = airAco, theSlip = 1;
        if (onGround() || inFluid ) { // TODO: fluid physics
            theSlip = slip;
            aCo = 0.0625 / (slip * slip * slip * slip);
            // secret sauce of better bunny hop
            if(!wasGrounded){
                aCo *= 1.25 + vx * vx + vy * vy;
            }
        }

        if(dashCountDown <= 0 || onGround()) {
            vx = (vx * airDrag + ax * aCo) * theSlip;
            vy = (vy * airDrag + ay * aCo) * theSlip;
        }

        vx += xForce + xBoost;
        vy += yForce + yBoost;
    }

    public void updatePositionAndCollision(){
        // TODO: if there is no ground below the player, it can fall through z = 0

        z += vz;

        wasGrounded = onGround();

        if (z <= 1e-7) {
            if (collisionBody.isBottomSupported()){
                z = 0;
                coyotePhase = coyoteDuration;
                if(vz <= 0) {
                    vz = 0;
                }else if(dashState == FLOATY_DASH){
                    // upward weird corner boost, cause by floaty_dash
                    // (but I see a loop hole here hehe, more glitches nice)
                    vx += recoveryVx;
                    vy += recoveryVy;
                }
                recoveryVx = 0; recoveryVy = 0;
            }else if (onGround()){
                coyotePhase--;
            }
        }else {
            coyotePhase = -1;
        }

        if(collisionBody.isInFluid() && z < -0.1){
            inFluid = true;
            if(z <= -0.5){
                z = -0.5;
                coyotePhase = coyoteDuration;
            }else{
                coyotePhase = -1;
            }
        }else {
            inFluid = false;
        }

        // made movement more lenient and introduce glitch yay
        if (!(collisionBody.isColliding(x + vx / 2, y + vy / 2) || collisionBody.isColliding(x + vx, y + vy))) {
            x += vx;
            y += vy;
        } else {
            // check and move x for collision
            collisionBody.updateCollisionState(true);
            if (!collisionBody.collideq) {
                x += vx;
            } else {
                vx = 0;
                xBoost = 0;
                x = collisionBody.collisionX;
            }
            // then check, move y for collision
            collisionBody.updateCollisionState(false);
            if (!collisionBody.collideq) {
                y += vy;
            } else {
                vy = 0;
                yBoost = 0;
                y = collisionBody.collisionY;
            }
        }

        hitBox.setPos(x, y);
    }

    public void deathHandling(){
        if (z < -5) {
            isAlive = false;
        }

        if(!isAlive) respawn(); // TODO: checkpoint system
    }

    public void frame(float alpha) {
        lerpFramePosition(alpha);

        generateDashTrail();

        stopQ = (normWasdVec[0] == 0 && normWasdVec[1] == 0);
        if (normWasdVec[0] != 0) {
            rightQ = (normWasdVec[0] > 0);
        }

        if (!stopQ) handleRunningAnimation();
        else handleIdleAnimation();

        updateDashTrail();
        glowAnimation();

        updateCharacterVisualPos();
    }

    private void glowAnimation() {
        if(dashStamina < dashCost && dashCountDown < 0){
            shadow.setColors(1,1,1,1);
        }else{
            shadow.setColors(0.9f,0.25f,1.0f,0.4f);
        }
    }

    public static final float dashTrailDuration = 0.4f;
    public static final float trailBaseAlpha = 0.9f;
    double dashAnimationTimer = 0;
    private void generateDashTrail() {
        if(dashCountDown > 0) {
            if (dashAnimationTimer <= 0){
                AdvPacket afterImage = new AdvPacket(character.sprite, baseZDepth + 0.01f);
                afterImage.setColorsMode(false);

                afterImage.setColors(0.25f, 0.55f, 0.45f, trailBaseAlpha,
                                     0.45f, 0.10f, 0.25f, trailBaseAlpha, GRADIENT_HORIZONTAL);
                afterImage.setPos(frameX - vx * 0.25, frameY - vy * 0.25 + cosineViewAngle * (frameZ - vz * 0.25));
                dashTrails.add(afterImage);
                trailTimes.add(dashTrailDuration);
                afterImage.show();

                dashAnimationTimer = Application.secToFrame( 1.0 / 24);
            }

            dashAnimationTimer--;
        }else {
            dashAnimationTimer = 0;
        }
    }

    private void updateDashTrail() {
        for (int i = 0; i < dashTrails.size(); i++) {
            AdvPacket trail = dashTrails.get(i);
            float newTime = (float) (trailTimes.get(i) - 1.0 / Application.realTimeFPS());
            if(newTime > 0){
                trailTimes.set(i, newTime);
                trail.setTransparency(trailBaseAlpha * newTime / dashTrailDuration + 0.2f);
            }else{
                trail.alive = false;
                dashTrails.set(i, dashTrails.getLast());
                dashTrails.set(dashTrails.size() - 1, trail);
                trailTimes.set(i, trailTimes.getLast());
                trailTimes.set(trailTimes.size() - 1, newTime);
            }
        }
    }

    int runningAnimationID;
    private void handleRunningAnimation() {
        sprintCounter++;

        if (sprintCounter > secToFrame(1.0 / 15)) {
            runningAnimationID = (runningAnimationID + 1) % 5;
            sprintCounter = 0;
        }

        character.sprite = runningAnimation[runningAnimationID];
        character.scaleX = Math.abs(character.scaleX) * (rightQ ? 1 : -1);
        blinkTimer = 0; // reset blinking
    }

    private void handleIdleAnimation() {
        blinkTimer++;
        if (blinkTimer > secToFrame(2)) blinkTimer = 0;

        character.sprite = (blinkTimer < secToFrame(1.8))
                ? idleAnimation[0]
                : idleAnimation[1];
    }

    private void updateCharacterVisualPos() {
        character.setPos(frameX,  frameY + cosineViewAngle * frameZ);
        shadow.setPos(frameX, frameY);

        if(collisionBody.isBottomSupported()){
            shadow.setTransparency(1f);
            shadow.zDepth = baseZDepth;
        }else {
            if(frameZ >= 0 && !collisionBody.isInFluid()) {
                shadow.zDepth = baseZDepth;
                shadow.setTransparency(0.5f);
            }else {
                shadow.zDepth = -1;
            }
        }

        yRepStuff();
    }

    public void setCheckPointBeneath(){
        if ((collisionBody.isBottomSupported()) && z <= 0) {
            z = 0;
            coyotePhase = coyoteDuration;
            spawnX = x;
            spawnY = y;
        }
    }

    public Space getSpaceLocation() {
        return spaceLocation;
    }

    @Override
    public int getEntityID() {
        return EntityLoader.PLAYER_ID;
    }

    // TODO: actual player name
    @Override
    public String getName() {
        return "Sigmoid";
    }

    @Override
    public float yRepStuff() {
        float yRep = super.yRepStuff();
        character.yRep = yRep;
        shadow.yRep = yRep;
        return yRep;
    }

}