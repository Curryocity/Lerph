package util;

import component.Texture;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glReadPixels;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL30.glDeleteFramebuffers;

public class Util {

    //useful shits
    private Util(){}

    public static boolean isArrayFullOfZeros(byte[] array) {
        for (byte b : array) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }

    public static boolean isArrayFullOfZeros(float[] array) {
        for (float b : array) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }

    public static void deleteDirectory(Path directoryPath) throws IOException {
        Files.walk(directoryPath)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.delete(path);
                        System.out.println("Deleted: " + path);
                    } catch (IOException e) {
                        System.err.println("Failed to delete: " + path + " - " + e.getMessage());
                    }
                });
    }

    public static boolean contains(String[] array, String target) {
        for (String s : array) {
            if (s.equals(target)) {
                return true;
            }
        }
        return false;
    }

    public static boolean contains(int[] array, int target) {
        for (int num : array) {
            if (num == target) {
                return true;
            }
        }
        return false;
    }

    public static int searchArrayIndexByName(String[] stringArray, String name){
        for (int i = 0; i < stringArray.length - 1; i++){
            if(stringArray[i].equals(name)){
                return i;
            }
        }
        return 0;
    }

    private static void outputPicture(Texture texture, String filePath){
        try {
            int framebuffer = glGenFramebuffers();
            glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);

            // Attach the texture to the framebuffer
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture.texID, 0);

            // Check if the framebuffer is complete
            if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
                throw new RuntimeException("Framebuffer is not complete!");
            }

            // Set the viewport to the texture's dimensions
            int width = texture.width;  // Texture width
            int height = texture.height; // Texture height
            glViewport(0, 0, width, height);

            ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 4); // RGBA format
            glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, buffer);

            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

            // Convert OpenGL's RGBA format to BufferedImage's ARGB format
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int i = (x + (width * y)) * 4; // RGBA format
                    int r = buffer.get(i) & 0xFF;
                    int g = buffer.get(i + 1) & 0xFF;
                    int b = buffer.get(i + 2) & 0xFF;
                    int a = buffer.get(i + 3) & 0xFF;
                    image.setRGB(x, height - 1 - y, (a << 24) | (r << 16) | (g << 8) | b);
                }
            }

            // Save the BufferedImage as PNG
            File outputFile = new File(filePath);

            ImageIO.write(image, "PNG", outputFile);

            glBindFramebuffer(GL_FRAMEBUFFER, 0);
            glDeleteFramebuffers(framebuffer);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
}
