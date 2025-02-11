package manager;

import data.Space;
import entity.Player;

import static block.Block.isGround;
import static block.Block.isObstacle;

public class BoomExecutor {
    private static final int[] X_OFFSETS = {-1, 0, 1, 0, -1, 1, 1, -1}, Y_OFFSETS = {0, -1, 0, 1, -1, -1, 1, 1};
    private static final int[] edgeVectors = {0, -1, -1, 0, 1, -1, -1, 1}, cornerVectors = {0, 0, 1, 0, 1, 1, 0, 1};
    private final boolean[] explosionSpots = new boolean[8], edgesOccupied = new boolean[4];
    // explosionSpots: close enough (<= 0.5) to potentially cause explosion
    // edgesOccupied: <= 1 block edges from player, used to identify the obstacle terrain (U shape, and L corner)
    // 0 = x, 1 = y, 2 = X, 3 = Y, 4 = xy, 5 = Xy, 6 = XY, 7 = xY  (edges then corners, counterclockwise)
    // x,X mean left or right, y,Y mean top or bottom
    private Space loc;
    private final Player player;
    public double forceX, forceY;

    public BoomExecutor(Player player) {
        this.player = player;
    }

    public boolean boom(){ // return "verticalBoost?", return true when boost is non-zero and non-tunnel-effect
        loc = player.getSpaceLocation();
        double relX = player.x - Math.floor(player.x);
        double relY = player.y - Math.floor(player.y);
        forceX = 0; forceY = 0;

        // Get neighbor obstacle's existence and validity
        for(int i = 0; i < 4; i++){ // edges
            edgesOccupied[i] = boomAble(player.x + X_OFFSETS[i], player.y + Y_OFFSETS[i]);
            explosionSpots[i] = boomAble(player.x + X_OFFSETS[i] * 0.5, player.y + Y_OFFSETS[i] * 0.5);
        }for(int i = 4; i < 8; i++){ // corners
            boolean adjacentEdgesClear = !edgesOccupied[i - 4] && !edgesOccupied[(i - 4 + 1) % 4];
            explosionSpots[i] = boomAble(player.x + X_OFFSETS[i] * 0.5, player.y + Y_OFFSETS[i] * 0.5) && adjacentEdgesClear;
            // corner as explosion src is only valid if adjacent edges are clear
        }

        // U shape only boost you toward the opening
        for(int i = 0; i < 2; i++) {
            if(edgesOccupied[i] && edgesOccupied[i + 2] && ( edgesOccupied[i + 1] ^ edgesOccupied[(i + 3) % 4] ) ){
                explosionSpots[i] = false;  explosionSpots[i + 2] = false;
            }
        }

        // Calculate boost from corners, registered only if adjacent edges are clear
        for(int i = 0; i < 4; i++){
            if (!explosionSpots[i + 4]) continue;
            double vecX = relX - cornerVectors[2 * i] + Math.signum(relX - cornerVectors[2 * i]) * 0.5;
            double vecY = relY - cornerVectors[2 * i + 1] + Math.signum(relY - cornerVectors[2 * i + 1]) * 0.5;
            double invDistanceCubed = 1 / (vecX * vecX + vecY * vecY);
            invDistanceCubed *= Math.sqrt(invDistanceCubed);

            forceX += vecX * invDistanceCubed;
            forceY += vecY * invDistanceCubed;
        }

        // Calculate boost from edges
        for(int i = 0; i < 4; i++){
            if (!explosionSpots[i]) continue;
            if (edgeVectors[2 * i] != -1) { // x edge
                double distX = relX - edgeVectors[2 * i] + Math.signum(relX - edgeVectors[2 * i]) * 0.7;
                forceX += Math.signum(distX) / (distX * distX);
            }
            if (edgeVectors[2 * i + 1] != -1) { // y edge
                double distY = relY - edgeVectors[2 * i + 1] + Math.signum(relY - edgeVectors[2 * i + 1]) * 0.7;
                forceY += Math.signum(distY) / (distY * distY);
            }
        }

        // Player controlled direction for two-sided corner boosts
        if((explosionSpots[0] ^ explosionSpots[2]) && (explosionSpots[1] ^ explosionSpots[3])){
            if(player.normWasdVec[0] == 0 && player.normWasdVec[1] != 0){
                forceX = 0;
            }else if(player.normWasdVec[1] == 0 && player.normWasdVec[0] != 0){
                forceY = 0;
            }else { // nerf double axis boost
                forceX *= 0.8; forceY *= 0.8;
            }
        }

        return forceX != 0 || forceY != 0;
    }

    private boolean boomAble(double x, double y){
        return isObstacle(loc.getBlockID(x, y)) || ( player.z < -0.1 && isGround(loc.getBlockID(x, y)));
    }

}
