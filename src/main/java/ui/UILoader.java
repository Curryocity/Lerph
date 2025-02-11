package ui;

import component.AdvPacket;
import component.Sprite;
import component.SpriteSheet;
import core.Game;
import manager.AssetPool;
import renderer.RendererLibrary;

public class UILoader {
    static SpriteSheet uiSheet;
    private UILoader() {}

    public static void load() {
        uiSheet = AssetPool.getSpriteSheet("assets/SpriteSheet/uiSheet.png");
        RendererLibrary.addTexture(uiSheet.getTexture());

        Sprite cursor = uiSheet.crop(0, 0, 16, 16);
        cursor.anchorX = 4f/16;
        cursor.anchorY = 13.5f/16;
        Game.cursor = new AdvPacket(cursor, 99.99f);
        Game.cursor.usedLocalCoords = false;
        Game.cursor.relativeToCamera = true;

        Game.showCursor();
    }

}
