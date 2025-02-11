package block;

import static block.BlockLoader.blocks;

public class Block {

    public final static int VOID = 0, FLUID = 1, GROUND = 2, OBSTACLE = 3;
    public final static double DEFAULT_SLIP = 0.5;
    public final String name;
    public final double slip;
    public final int blockType;

    public Block(String name, int blockType) {
        this(name, blockType, 0.5);
    }
    public Block(String name, int blockType, double slip) {
        this.name = name;
        this.blockType = blockType;
        this.slip = slip;
    }

    public static double getSlip(int blockID) {
        return blocks[blockID].slip;
    }

    public static String getName(int blockID) {
        return blocks[blockID].name;
    }

    public static boolean isVoid(int blockID) {
        return blocks[blockID].blockType == VOID;
    }

    public static boolean isFluid(int blockID) {
        return blocks[blockID].blockType == FLUID;
    }

    public static boolean isGround(int blockID) {
        return blocks[blockID].blockType == GROUND;
    }

    public static boolean isObstacle(int blockID) {
        return blocks[blockID].blockType == OBSTACLE;
    }
}
