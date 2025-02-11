package font;

import java.nio.ByteBuffer;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import component.Sprite;
import component.Texture;
import org.joml.Vector4f;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_REPEAT;

public class FontLoader { // this is so stupid lmao, there is only one font
    private static final String GARMAMOND_FONT_PATH = "assets/fonts/garamond_ascii.ttf";
    private static final int[] supportSizes = {128, 256};
    public static final Map<Integer, FontAtlas> fontAtlases = new HashMap<>();
    private static Font garamondFont;
    private FontLoader() {}

    public static void LoadFont(){
        Arrays.sort(supportSizes); // just in case

        try {
            garamondFont = Font.createFont(Font.TRUETYPE_FONT, new File(GARMAMOND_FONT_PATH));
            for(int size : supportSizes){
                generateFontAtlas(size);
            }
        } catch (FontFormatException | IOException e) {e.printStackTrace();}

    }

    public static FontAtlas getBestApproximateFont(float fontSize){
        for(int size : supportSizes){
            if(size > fontSize) return fontAtlases.get(size);
        }
        return fontAtlases.get(supportSizes[supportSizes.length - 1]);
    }

    public static float baseLineYRatio = 0;
    private static void generateFontAtlas(int fontSize) {
        if (fontAtlases.containsKey(fontSize)) {
            fontAtlases.get(fontSize);
            return;
        }

        Font font = garamondFont.deriveFont((float) fontSize);
        Map<Character, Sprite> glyphs = new HashMap<>();

        int atlasSize = fontSize > 128 ? 2048 : 1024;

        BufferedImage textureImage = new BufferedImage(atlasSize, atlasSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = textureImage.createGraphics();
        g2d.setFont(font);
        FontMetrics metrics = g2d.getFontMetrics();

        int textureID = glGenTextures();
        Texture fontTexture = new Texture(textureID, atlasSize, atlasSize);

        baseLineYRatio =  (float) metrics.getDescent() / (metrics.getAscent() + metrics.getDescent());

        int x = 0, y = 0;
        int maxHeight = 0;

        int textureWidth = textureImage.getWidth();
        int textureHeight = textureImage.getHeight();

        // Create a texture atlas with ASCII printable character
        for (char c = 32; c < 127; c++) {
            BufferedImage charImage = createCharImage(font, c, metrics);
            if (x + charImage.getWidth() > textureImage.getWidth()) {
                x = 0;
                y += maxHeight;
                maxHeight = 0;
            }

            Vector4f dimensions = new Vector4f((float) x / textureWidth,
                    1 - (float) (y + charImage.getHeight()) / textureHeight,
                    (float) charImage.getWidth() / textureWidth,
                    (float) charImage.getHeight() / textureHeight );

            Sprite glyphSprite = new Sprite(fontTexture, dimensions);
            glyphSprite.anchorX = 0;
            glyphSprite.anchorY = baseLineYRatio;
            glyphs.put(c, glyphSprite);
            g2d.drawImage(charImage, x, y, null);
            x += charImage.getWidth();
            maxHeight = Math.max(maxHeight, charImage.getHeight());
        }

        reloadTexture(textureImage, textureID);

        g2d.dispose();

        FontAtlas atlas = new FontAtlas(fontTexture, glyphs, fontSize);

        fontAtlases.put(fontSize, atlas);

    }

    private static BufferedImage createCharImage(Font font, char c, FontMetrics metrics) {
        BufferedImage image = new BufferedImage(metrics.charWidth(c), metrics.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setFont(font);
        g2d.setColor(Color.BLACK);
        g2d.drawString(String.valueOf(c), 0, metrics.getAscent());
        g2d.dispose();
        return image;
    }

    private static void reloadTexture(BufferedImage image, int texID) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(image.getWidth() * image.getHeight() * 4);
        for (int y = image.getHeight() - 1; y >= 0; y--) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getRGB(x, y); // forced white + transparency
                buffer.put((byte) (0xFF)); // Red
                buffer.put((byte) (0xFF));  // Green
                buffer.put((byte) (0xFF));   // Blue
                buffer.put((byte) ((pixel >> 24) & 0xFF)); // Alpha
            }
        }
        buffer.flip();

        // Generate OpenGL texture ID and upload the texture data
        glBindTexture(GL_TEXTURE_2D, texID);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, image.getWidth(), image.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
    }
}