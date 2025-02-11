package entity;

import component.Sprite;
import component.SpriteSheet;
import component.Texture;
import manager.AssetPool;
import renderer.RendererLibrary;

import java.util.ArrayList;
import java.util.List;

public class EntityLoader {
    private static SpriteSheet entitySheet;
    public static List<Sprite> entitySprites = new ArrayList<>();;
    public static final int[] entityID_SpriteID_MAP = new int[256];
    public static final String[] entityNames = new String[256];
    private EntityLoader(){}

    public static void load(){
        entitySheet = AssetPool.getSpriteSheet("assets/SpriteSheet/entitySheet.png");
        RendererLibrary.addTexture(entitySheet.getTexture());


        loadAllEntities();
        initEntityClasses();
    }

    private static void initEntityClasses() {}

    public static void loadAllEntities(){

        //player is skipped

        loadEntity("checkpoint", CheckPoint.loadSprite());
        loadEntity("portal", Portal.loadSprite());

    }

    private static int spriteIDPointer, entityIDPointer;

    public static void loadEntity(String entityName, Sprite[] sprites){
        entityID_SpriteID_MAP[entityIDPointer] = spriteIDPointer;
        entityNames[entityIDPointer] = entityName;

        for(Sprite sprite : sprites){
            entitySprites.add(sprite);
            spriteIDPointer++;
        }

        entityIDPointer ++;
    }

    public final static int PLAYER_ID = -1, CHECKPOINT_ID = 0, PORTAL_ID = 1;

    public static SpriteSheet getEntitySheet() {
        entitySheet = AssetPool.getSpriteSheet("assets/SpriteSheet/entitySheet.png");
        return entitySheet;
    }

    public static Texture getEntitySheetTexture(){
        entitySheet = AssetPool.getSpriteSheet("assets/SpriteSheet/entitySheet.png");
        return entitySheet.getTexture();
    }

}
