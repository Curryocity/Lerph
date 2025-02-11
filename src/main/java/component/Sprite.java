package component;

import org.joml.Vector4f;

public class Sprite {
    public final Texture textureSrc;
    public final Vector4f txcDimensions; // 0 - leftX, 1 - bottomY, 2 - width, 3 - height
    public final float sizeX, sizeY;
    public float anchorX, anchorY;

    public Sprite(Texture textureSrc, Vector4f dimensions){
        this(textureSrc, dimensions, 1f, 1f, 0.5f, 0.5f);
    }

    public Sprite(Texture textureSrc, Vector4f dimensions, float scaleX, float scaleY){
        this(textureSrc, dimensions, scaleX, scaleY, 0.5f, 0.5f);
    }

    public Sprite(Texture textureSrc, Vector4f dimensions, float scaleX, float scaleY, float anchorX, float anchorY){
        this.textureSrc = textureSrc;
        this.txcDimensions = dimensions;
        sizeX = (txcDimensions.z * textureSrc.width) * scaleX;
        sizeY = (txcDimensions.w * textureSrc.height) * scaleY;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
    }

    public static void setOffset(Sprite[] sprites, float xOffset, float yOffset){
        for (Sprite sprite : sprites) {
            sprite.anchorX = xOffset;
            sprite.anchorY = yOffset;
        }
    }
}
