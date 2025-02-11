package component;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL30.*;
public class FrameBuffer {
    public final int fboID, rboID;
    private Texture texture;

    public FrameBuffer(int width, int height) {

        // Generate and Bind the FrameBuffer
        fboID = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fboID);

        // Create and Attach a Texture
        this.texture = new Texture(width, height);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, this.texture.texID, 0);
//        glDrawBuffer(GL_COLOR_ATTACHMENT0);

        // OpenGL's default frameBuffer already provides a depth buffer.
        // However, when you create a custom frameBuffer (FBO), it does not automatically have a depth buffer.
        // So we'll need a renderBuffer to store the depth information
        rboID = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, rboID);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT32, width, height);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, rboID);
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("FrameBuffer is not complete!");
        }
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

    }

    public void resize(int newWidth, int newHeight) {
        glDeleteTextures(texture.texID);

        texture = new Texture(newWidth, newHeight);
        glBindFramebuffer(GL_FRAMEBUFFER, fboID);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture.texID, 0);

        glBindRenderbuffer(GL_RENDERBUFFER, rboID);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT32, newWidth, newHeight);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, rboID);

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Framebuffer resize failed!");
        }
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

    }

    public static void printFramebufferFirstPixelColor(FrameBuffer frameBuffer) {
        // Bind the framebuffer to read from it
        glBindFramebuffer(GL_FRAMEBUFFER, frameBuffer.fboID);

        // Create a buffer to hold the pixel data (4 bytes for RGBA)
        ByteBuffer buffer = BufferUtils.createByteBuffer(4);

        // Read the pixel at (0, 0) (the bottom-left corner of the framebuffer)
        glReadPixels(0, 0, 1, 1, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, buffer);

        // Unbind the framebuffer
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        // Extract the RGBA values from the buffer
        int r = buffer.get(0) & 0xFF;  // Red component
        int g = buffer.get(1) & 0xFF;  // Green component
        int b = buffer.get(2) & 0xFF;  // Blue component

        // Print the color of the first pixel (0, 0)
        System.out.println("Framebuffer Pixel (0,0): R=" + r + " G=" + g + " B=" + b);
    }

    public void bind() { glBindFramebuffer(GL_FRAMEBUFFER, fboID);}
    public void unbind() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }
    public Texture getTexture() {return texture; }
}