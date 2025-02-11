package component;

import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

public class SpriteSheet {
    private final Texture texture;
    private final List<Sprite> sprites;

    public SpriteSheet(Texture texture) {
        this.sprites = new ArrayList<>();
        this.texture = texture;
    }

    public void gridCrop(int spriteWidth, int spriteHeight, int numSprites){
        int currentX = 0;
        int currentY = texture.height - spriteHeight;
        for (int i=0; i < numSprites; i++) {

            float leftX = (float) currentX / texture.width;
            float bottomY = (float) currentY / texture.height;
            float width = (float) spriteWidth / texture.width;
            float height = (float) spriteHeight  / texture.height;

            Vector4f dimension = new Vector4f(leftX, bottomY, width, height);

            Sprite sprite = new Sprite(this.texture, dimension);
            this.sprites.add(sprite);

            currentX += spriteWidth;
            if (currentX >= texture.width) {
                currentX = 0;
                currentY -= spriteHeight;
            }
        }
    }

    public Sprite crop(int x, int y, int spriteWidth, int spriteHeight){
        float leftX = (float) x / texture.width;
        float topY = (float) y / texture.height;
        float width = (float) spriteWidth / texture.width;
        float height = (float) spriteHeight  / texture.height;

        Vector4f dimension = new Vector4f(leftX, 1 - (topY + height), width, height);

        Sprite sprite = new Sprite(this.texture, dimension);
        this.sprites.add(sprite);

        return sprite;
    }

    public Texture getTexture() {
        return texture;
    }

    public Sprite getSprite(int index) {
        if(index >= sprites.size()) throw new RuntimeException("sprite index out of bounds: " + index);
        return this.sprites.get(index);
    }

    public Sprite[] getSprites(int start, int end) {
        try {
            return this.sprites.subList(start, end + 1).toArray(new Sprite[0]);
        } catch (IndexOutOfBoundsException e) {
            throw new RuntimeException("Sprite index out of bounds: " + start + " to " + end);
        }
    }

    public Sprite[] getAllSprites(){
        return this.sprites.toArray(new Sprite[0]);
    }
}
