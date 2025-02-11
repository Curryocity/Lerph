package font;

import component.Sprite;
import component.Texture;
import renderer.RendererLibrary;

import java.util.Map;

public class FontAtlas {
    public final Map<Character, Sprite> glyphs;
    public final Texture texture;
    public final int fontSize;

    public FontAtlas(Texture texture, Map<Character, Sprite> glyphs, int fontSize) {
        this.glyphs = glyphs;
        this.texture = texture;
        this.fontSize = fontSize;
        RendererLibrary.addTexture(texture);
    }
}
