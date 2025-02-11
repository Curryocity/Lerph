package component;

import core.Application;
import core.Game;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import static core.Game.BLOCK_SCALE;

public class Camera {

    final private Matrix4f projectionMatrix, viewMatrix;
    public Vector2f position;

    public Camera() {
        this(new Vector2f(0.0f, 0.0f));
    }
    public Camera(Vector2f position) {
        this.position = position;
        this.projectionMatrix = new Matrix4f();
        this.viewMatrix = new Matrix4f();
        adjustProjection();
    }

    public void adjustProjection() {
        projectionMatrix.identity();
        projectionMatrix.ortho(-Application.viewPortWidth * 0.5f, Application.viewPortWidth * 0.5f, -Application.viewPortHeight * 0.5f, Application.viewPortHeight * 0.5f, 0.0f, 100.0f);
    }

    public void setPosition(double x, double y) {
        this.position.x = (float) x;
        this.position.y = (float) y;
    }

    public void setWorldPosition(double x, double y) {
        this.position.x = (float) x * Game.getScale(BLOCK_SCALE);
        this.position.y = (float) y * Game.getScale(BLOCK_SCALE);
    }

    public Vector2f worldCoords(){
        float translation = 1.0f / Game.getScale(BLOCK_SCALE);
        return new Vector2f(position.x * translation, position.y * translation);
    }

    final Vector3f eye = new Vector3f(0.0f, 0.0f, 100.0f);
    final Vector3f center = new Vector3f(0.0f, 0.0f, -1.0f);
    final Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);
    public Matrix4f getViewMatrix() {
        float integerX = Math.round(position.x);
        float integerY = Math.round(position.y);

        eye.x = integerX;
        eye.y = integerY;
        center.x = integerX;
        center.y = integerY;

        viewMatrix.identity();
        viewMatrix.lookAt(eye, center, up);

        return viewMatrix;
    }

    public Matrix4f getProjectionMatrix() {
        return this.projectionMatrix;
    }
}
