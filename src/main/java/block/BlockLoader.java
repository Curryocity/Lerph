package block;

import component.Texture;
import manager.AssetPool;
import component.Sprite;
import component.SpriteSheet;
import renderer.RendererLibrary;


import static block.Block.*;

public class BlockLoader {
    public static final String blockSheetPath = "assets/SpriteSheet/blockSheetV4.png";
    private static SpriteSheet blockSheet;
    public static final Block[] blocks = new Block[256];
    public static final int[] blockID_SpriteID_MAP = new int[256];
    public static Sprite[] blockSprites;
    public static boolean[] isSpriteObstacle;

    private BlockLoader(){}

    public static void load(){
        blockSheet = AssetPool.getSpriteSheet(blockSheetPath);
        RendererLibrary.addTexture(blockSheet.getTexture());

        loadAllBlocks();
    }

    public static void loadAllBlocks(){

        blockSprites = new Sprite[32];
        isSpriteObstacle = new boolean[blockSprites.length];

        blocks[0] = new Block("void", VOID); //void block
        blockSprites[0] = null;

        //blocks and variants are loaded in with spriteSheet order

        loadBlock("grass", GROUND); //1
        loadBlock("bush", OBSTACLE); //2
        loadBlock("wall", OBSTACLE); //3
        loadBlock("wood", GROUND); //4
        loadBlock("sand", GROUND); //5
        loadBlock("water", FLUID, 0.665); //6
        loadBlock("ice", GROUND, 0.96); //7
        loadBlock("lava", FLUID, 0.665); //8
        loadBlock("plank", GROUND); //9
        loadBlock("tropic_log", OBSTACLE); //10
        loadBlock("red_log", OBSTACLE); //11
        loadBlock("dark_log", OBSTACLE); //12
        loadBlock("black_stone", GROUND); //13
        loadBlock("snow", GROUND); //14
        loadBlock("red_land", GROUND); //15
        loadBlock("palette_bench", OBSTACLE); //16
    }

    public static void loadBlock(String blockName, int blockType) {
        loadBlock(blockName, blockType, DEFAULT_SLIP);
    }

    private static int spriteIDPointer = 1, blockIDPointer = 1;
    public static void loadBlock(String blockName, int blockType, double slip) {

        blockID_SpriteID_MAP[blockIDPointer] = spriteIDPointer;
        blocks[blockIDPointer] = new Block(blockName, blockType, slip);

        int x = ((blockIDPointer - 1) % 8) * 16;
        int y = ((blockIDPointer - 1) / 8) * 48;
        int width = 16;
        int height = 16;
        blockSprites[spriteIDPointer ++] = blockSheet.crop(x, y, width, height);

        if(blockType == OBSTACLE){
            isSpriteObstacle[spriteIDPointer - 1] = true;
            y += 32 - 6;
            height = 16 + 6;
            blockSprites[spriteIDPointer ++] = blockSheet.crop(x, y, width, height);
        }

        blockIDPointer++;
    }

    public static Texture getBlockSheetTexture(){
        blockSheet = AssetPool.getSpriteSheet(blockSheetPath);
        return blockSheet.getTexture();
    }
}
