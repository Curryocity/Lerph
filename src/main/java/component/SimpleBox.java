package component;

import org.joml.Vector2f;

public class SimpleBox {
    public double posX, posY;
    public double halfDx, halfDy;
    public SimpleBox(double halfDx, double halfDy){
        this.halfDx = halfDx;
        this.halfDy = halfDy;
    }

    public SimpleBox(double PosX, double PosY, double halfDx, double halfDy){
        this.posX = PosX;
        this.posY = PosY;
        this.halfDx = halfDx;
        this.halfDy = halfDy;
    }

    public void setPos(double x,double y){
        posX = x;
        posY = y;
    }

    public void movePos(double dx,double dy){
        posX += dx;
        posY += dy;
    }

    public boolean include(double x, double y){
        return (x > posX - halfDx) && (x < posX + halfDx ) && (y > posY - halfDy) && (y < posY + halfDy);
    }

    public static boolean intersect(SimpleBox A, SimpleBox B){
        return Math.abs(A.posX - B.posX) < (A.halfDx + B.halfDx) ||  Math.abs(A.posY - B.posY) < (A.halfDy + B.halfDy);
    }

    /**
     * in the order bottom left, bottom right, top right, top left.
     * @return an array of four Vector2f, each representing a corner of box
     */
    public Vector2f[] getCorners(){
        Vector2f[] corners = new Vector2f[4];
        corners[0] = new Vector2f((float) (posX - halfDx), (float) (posY - halfDy));
        corners[1] = new Vector2f((float) (this.posX + this.halfDx), (float) (this.posY - this.halfDy));
        corners[2] = new Vector2f((float) (this.posX + halfDx), (float) (posY + halfDy));
        corners[3] = new Vector2f((float) (posX - halfDx), (float) (posY + halfDy));
        return corners;
    }

    public double[] getBoundaries(){ // left, right, down, up
        double[] boundaries = new double[4];
        boundaries[0] = posX - halfDx;
        boundaries[1] = posX + halfDx;
        boundaries[2] = posY - halfDy;
        boundaries[3] = posY + halfDy;
        return boundaries;
    }
}
