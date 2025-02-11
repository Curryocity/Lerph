package manager;

import renderer.Shader;
import component.SpriteSheet;
import component.Texture;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class AssetPool {
    private static final Map<String, Shader> shaders = new HashMap<>();
    private static final Map<String, Texture> textures = new HashMap<>();
    private static final Map<String, SpriteSheet> spriteSheets = new HashMap<>();

    // Generic method to retrieve an asset with lazy loading
    private static <T> T getAsset(Map<String, T> assetMap, String resourcePath, AssetLoader<T> loader) {
        return assetMap.computeIfAbsent(new File(resourcePath).getPath(), key -> loader.load(resourcePath));
    }

    public static Shader getShader(String resourcePath) {
        return getAsset(shaders, resourcePath, path -> {
            Shader shader = new Shader(path);
            shader.compile();
            return shader;
        });
    }

    public static Texture getTexture(String resourcePath) {
        return getAsset(textures, resourcePath, Texture::new);
    }

    public static SpriteSheet getSpriteSheet(String resourcePath) {
        return getAsset(spriteSheets, resourcePath, path -> new SpriteSheet(getTexture(path)));
    }

    @FunctionalInterface
    interface AssetLoader<T> {
        T load(String path);
    }
}
