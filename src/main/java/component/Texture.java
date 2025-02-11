package component;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.stb.STBImage.stbi_image_free;
import static org.lwjgl.stb.STBImage.stbi_load;

public class Texture {

    public final int texID;
    public final int width, height;
    public Texture(String filePath){

        // Set vertical flipping for loaded textures
        STBImage.stbi_set_flip_vertically_on_load(true);

        texID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texID);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        IntBuffer widthBuffer = BufferUtils.createIntBuffer(1);
        IntBuffer heightBuffer = BufferUtils.createIntBuffer(1);
        IntBuffer channels = BufferUtils.createIntBuffer(1);
        ByteBuffer image = stbi_load(filePath, widthBuffer, heightBuffer, channels, 0);

        if (image == null)  throw new RuntimeException("Error: Could not load image '" + filePath + "'");

        this.width = widthBuffer.get(0);
        this.height = heightBuffer.get(0);
        if (channels.get(0) == 3) {
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, widthBuffer.get(0), heightBuffer.get(0), 0, GL_RGB, GL_UNSIGNED_BYTE, image);
        } else if (channels.get(0) == 4) {
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, widthBuffer.get(0), heightBuffer.get(0), 0, GL_RGBA, GL_UNSIGNED_BYTE, image);
        } else {
            System.err.println("Error: Unknown number of channels '" + channels.get(0) + "' in texture: " + filePath);
        }

        stbi_image_free(image);  // Free the image memory
    }

    public Texture(int texID, int width, int height){ // You want to manually bind texture to texID
        this.texID = texID;
        this.width = width;
        this.height = height;
    }

    public Texture(int width, int height){ // generate new texture
        this.width = width;
        this.height = height;

        texID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texID);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, width, height, 0, GL_RGB, GL_UNSIGNED_BYTE, 0);
    }

    public void bind() {
        glBindTexture(GL_TEXTURE_2D, texID);
    }

    public void unbind() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }

}
