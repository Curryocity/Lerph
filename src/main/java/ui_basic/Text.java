package ui_basic;

import component.AdvPacket;
import component.SimpleBox;
import component.Sprite;
import font.FontAtlas;
import font.FontLoader;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

import static core.Game.renderSystem;
import static ui_basic.ColorPalette.silver;
import static ui_basic.ColorPalette.white;

public class Text implements AlignAble{
    public static final Vector4f defaultColor = white;
    public static int defaultSize = 48;
    private final List<AdvPacket> contentPacket = new ArrayList<>();
    public Vector4f color;
    public String content;
    private float x, y, fontSize, xLength, zDepth;
    public boolean relativeToCamera = true;

    public Text(String content){
        this(content, defaultColor, 99f);
    }

    public Text(String content, float zDepth){ this(content, defaultColor, zDepth);}

    public Text(String content, Vector4f color){this(content, color, 99f);}

    public Text(String content, Vector4f color, float zDepth){
        this.content = content;
        this.fontSize = defaultSize;
        this.color = color;
        this.zDepth = zDepth;
        generateGlyphsPacket();
    }


    public void setPos(float x, float y){
        float deltaX =  x - this.x, deltaY = y - this.y;
        for(AdvPacket charPacket : contentPacket) {
            charPacket.x += deltaX;
            charPacket.y += deltaY;
        }
        this.x = x;
        this.y = y;
    }


    public void setText(String content){
        this.content = content;
        generateGlyphsPacket();
    }

    public void setFontSize(float fontSize){
        this.fontSize = fontSize;
        generateGlyphsPacket();
    }

    public void setColor(Vector4f color){
        if(this.color == color || color == null) return;
        this.color = color;
        for (AdvPacket charPacket : contentPacket){
            charPacket.setColors(color);
        }
    }

    public float getZDepth(){return zDepth;}

    public void setZDepth(float zDepth){
        this.zDepth = zDepth;
        for (AdvPacket charPacket : contentPacket){
            charPacket.zDepth = zDepth;
        }
    }

    public void generateGlyphsPacket(){
        boolean shown = false;
        if(!contentPacket.isEmpty()){
            shown = contentPacket.getFirst().alive;
            reset();
        }

        FontAtlas approxFont = FontLoader.getBestApproximateFont(fontSize);
        final float scale = fontSize / approxFont.fontSize;
        final int atlasWidth = approxFont.texture.width;

        char[] contentArray = content.toCharArray();
        this.xLength = 0;
        for(char c : contentArray){ this.xLength += approxFont.glyphs.get(c).txcDimensions.z;}
        this.xLength *= scale * atlasWidth;

        float currentX = x - this.xLength / 2;

        for(char c : contentArray){
            Sprite charSprite = approxFont.glyphs.get(c);
            AdvPacket charPacket = new AdvPacket(charSprite, zDepth, scale, scale);
            charPacket.setPos(currentX, y);
            charPacket.setColors(color);
            charPacket.usedLocalCoords = false;
            charPacket.relativeToCamera = relativeToCamera;
            currentX += charSprite.txcDimensions.z * scale * atlasWidth;
            contentPacket.add(charPacket);

            if(shown) renderSystem.activate(charPacket);
        }
    }

    @Override
    public void setPos(double x, double y){
        setPos((float) x, (float) y);}
    @Override
    public void setSize(double sizeX, double sizeY) {setFontSize((float) sizeY);}
    @Override
    public void show(){
        for (AdvPacket charPacket : contentPacket){
            renderSystem.activate(charPacket);
        }
    }
    @Override
    public void hide(){
        for (AdvPacket charPacket : contentPacket){
            charPacket.alive = false;
        }
    }

    Vector4f dimensionAndPivot = new Vector4f();
    @Override
    public Vector4f getDimensionAndPivot() {
        dimensionAndPivot.x = xLength;
        dimensionAndPivot.y = fontSize;
        dimensionAndPivot.z = 0.5f;
        dimensionAndPivot.w = FontLoader.baseLineYRatio;

        return dimensionAndPivot;
    }

    @Override
    public boolean isCoordsLocal() { return false;}

    @Override
    public void setRelativeToCamera(boolean isRelativeToCamera) {
        this.relativeToCamera = isRelativeToCamera;
    }

    public SimpleBox boundingBox(float padding, int boundingBoxType){
        SimpleBox boundingBox = new SimpleBox(this.xLength /2 , fontSize/2);

        float xPos = x;
        float yPos = y + fontSize * (0.5f - FontLoader.baseLineYRatio);
        boundingBox.setPos(xPos, yPos);

        switch (boundingBoxType){
            case AdvText.UPPER:
                boundingBox.halfDy *= 2.0/3;
                boundingBox.posY += boundingBox.halfDy / 4;
                break;
            case AdvText.LOWER:
                boundingBox.halfDy *= 2.0/3;
                boundingBox.posY -= boundingBox.halfDy / 4;
                break;
            case AdvText.MID:
                boundingBox.halfDy *= 1.0/3;
                break;
        }

        float padding_px = (float) (boundingBox.halfDy * padding);
        boundingBox.halfDx += padding_px;
        boundingBox.halfDy += padding_px;

        return boundingBox;
    }

    public void reset(){
        for(AdvPacket charPacket : contentPacket){
            charPacket.alive = false;
        }
        contentPacket.clear();
    }

    public float getX(){return x;}
    public float getY(){return y;}

}
