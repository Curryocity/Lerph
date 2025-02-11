package component;

import core.Game;
import org.joml.Vector4f;

public class AdvPacket extends StdPacket {
    public static final int GRADIENT_NONE = 0, GRADIENT_HORIZONTAL = 1, GRADIENT_VERTICAL = 2;
    public float[] colors = {1,1,1,1, 1,1,1,1};
    private int gradientType = GRADIENT_NONE;
    private boolean isColorsMultiplier = true;
    public AdvPacket(Sprite sprite, float zDepth) {
        super(sprite, zDepth);
    }
    public AdvPacket(Sprite sprite, float zDepth, float scaleX, float scaleY) {super(sprite, zDepth, scaleX, scaleY);}

    public AdvPacket(float r, float g, float b, float a, float zDepth, boolean usedLocalCoords){
        this(r, g, b, a, 1f, 1f, zDepth, usedLocalCoords);
    }

    public AdvPacket(float r, float g, float b, float a, float sizeX, float sizeY, float zDepth, boolean usedLocalCoords){
        super(null, zDepth, sizeX, sizeY);
        setColors(r,g,b,a);
        this.usedLocalCoords = usedLocalCoords;
    }

    public AdvPacket(float r1, float g1, float b1, float a1, float r2, float g2, float b2, float a2, int gradientType, float zDepth, boolean usedLocalCoords){
        this(r1, g1, b1, a1, r2, g2, b2, a2, gradientType, 1f, 1f, zDepth, usedLocalCoords);
    }

    public AdvPacket(float r1, float g1, float b1, float a1, float r2, float g2, float b2, float a2, int gradientType, float sizeX, float sizeY, float zDepth, boolean usedLocalCoords){
        super(null, zDepth, sizeX, sizeY);
        this.usedLocalCoords = usedLocalCoords;
        setColors(r1, g1, b1, a1, r2, g2, b2, a2, gradientType);
    }


    public AdvPacket(Vector4f color, float sizeX, float sizeY, float zDepth, boolean usedLocalCoords){
        super(null, zDepth, sizeX, sizeY);
        setColors(color);
        this.usedLocalCoords = usedLocalCoords;
    }

    public AdvPacket(Vector4f color1, Vector4f color2, int gradientType, float sizeX, float sizeY, float zDepth, boolean usedLocalCoords){
        super(null, zDepth, sizeX, sizeY);
        this.usedLocalCoords = usedLocalCoords;
        setColors(color1, color2, gradientType);
    }

    @Override
    public void show() {
        if(alive) return;
        Game.renderSystem.activate(this);
    }

    public void setColors(float r, float g, float b, float a) {
        colors[0] = r;
        colors[1] = g;
        colors[2] = b;
        colors[3] = a;
        gradientType = GRADIENT_NONE;
    }

    public void setColors(float r1, float g1, float b1, float a1, float r2, float g2, float b2, float a2, int gradientType){
        colors[0] = r1;
        colors[1] = g1;
        colors[2] = b1;
        colors[3] = a1;

        colors[4] = r2;
        colors[5] = g2;
        colors[6] = b2;
        colors[7] = a2;

        this.gradientType = gradientType;
    }

    public void setColors(Vector4f color){
        setColors(color.x, color.y, color.z, color.w);
    }

    public void setColors(Vector4f color1, Vector4f color2, int gradientType){ setColors(color1.x, color1.y, color1.z, color1.w, color2.x, color2.y, color2.z, color2.w, gradientType);}

    public void setTransparency(float transparency){
        colors[3] = transparency;
        colors[3 + 4] = transparency;
    }

    public void setColorsMode(boolean multiplierT_overrideF){
        isColorsMultiplier = multiplierT_overrideF;
    }

    public boolean isColorsMultiplier() {
        return isColorsMultiplier;
    }

    public int getGradientType() {
        return gradientType;
    }
}
