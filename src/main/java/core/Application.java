package core;

import component.FrameBuffer;
import renderer.PostProcessor;
import scene.EditorScene;
import scene.ImGuiEditor;
import manager.KeyInput;
import manager.MouseInput;

import org.lwjgl.Version;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import java.util.Objects;
import java.util.concurrent.locks.LockSupport;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Application {

    private final String title;
    //Version: major(-128 to 127, negative for non-release), minor(0-255), build(0-255)
    public static final byte[] version = new byte[]{0, 0, 3};
    private static long glfwWindow;
    private static Application singleton = null;
    public static Game game;
    public static ImGuiEditor levelEditorImGui;
    public static FrameBuffer frameBuffer;
    public static PostProcessor postProcessor;

    public static final int TPS = 20;
    private static int FPS = 120;

    public static final float aspectRatio = 9f/16;
    private static int screenWidth, screenHeight;
    public static int viewPortX, viewPortY, viewPortWidth, viewPortHeight;
    public static int fpsMeasurement = 0;


    private Application(){
        title = "Lerph";
        screenWidth = Math.round(Game.screenWidth_BLOCK * Game.block_px);
        screenHeight = Math.round(screenWidth * aspectRatio);
    }

    /**
     * Get the singleton instance of the Application class. If the instance hasn't been created yet, this method will create it.
     * @return The singleton instance of the Application class
     */
    public static Application get(){
        if(Application.singleton == null){
            Application.singleton = new Application();
        }
        return Application.singleton;
    }

    /**
     * Version: 3bytes, major(0-255), minor(0-255), build(0-255)
     * @return the 3 bytes of version packed into an int
     */
    public static int getVersionAsInt() {
        return (version[0] << 16) | (version[1] << 8) | version[2];
    }
    public static int getVersionAsInt(byte major, byte minor, byte build) {
        return (major << 16) | (minor << 8) | build;
    }

    public static String versionIntToString(int versionInt){
        int build = versionInt & 0xFF;
        int minor = (versionInt >>> 8) & 0xFF;
        byte major = (byte) (versionInt >> 16);

        String releaseText = (major > 0) ? "Lerph " : "Lerph demo ";

        return releaseText + Math.abs(major) + "." + minor + "." + build;
    }

    public static String getVersionText(){
        return versionIntToString(getVersionAsInt());
    }

    /**
     * This method starts the application. It first calls {@link #init()}, then runs the main loop with {@link #loop()}, and finally calls {@link #exit()}.
     */
    public void launch(){
        init();
        loop();
        exit();
    }

    /**
     * Initialize the application. This method is private and should only be called by {@link #launch()}.
     * It initializes the GLFW library, sets up the window and OpenGL context, and initializes the {@link Game} and {@link ImGuiEditor} objects.
     */
    private void init(){
        System.out.println("Hello LWJGL " + Version.getVersion() + "!");
        // Setup an error callback
        System.setProperty("java.awt.headless", "true");
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW
        if (!glfwInit()) throw new IllegalStateException("Unable to initialize GLFW.");


        // Configure GLFW
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);


        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        // Create the window, you need to divided by 2 idk
        glfwWindow = glfwCreateWindow(screenWidth/2, screenHeight/2, this.title, NULL, NULL);
        if (glfwWindow == NULL) throw new IllegalStateException("Failed to create the GLFW window.");

        //window resize callback
        GLFW.glfwSetFramebufferSizeCallback(glfwWindow, (window, newWidth, newHeight) -> {
            screenWidth = newWidth;
            screenHeight = newHeight;

            updateViewport(true);

            handleResize();
        });

        glfwSetWindowSizeLimits(glfwWindow, 960, 540, GLFW_DONT_CARE, GLFW_DONT_CARE);
        glfwSetCursorPosCallback(glfwWindow, MouseInput::mousePosCallBack);
        glfwSetMouseButtonCallback(glfwWindow, MouseInput::mouseButtonCallBack);
        glfwSetScrollCallback(glfwWindow, MouseInput::mouseScrollCallback);
        glfwSetKeyCallback(glfwWindow,KeyInput::keyCallback);
        GLFW.glfwSetInputMode(glfwWindow, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN);

        glfwMakeContextCurrent(glfwWindow);
        glfwShowWindow(glfwWindow);
        GL.createCapabilities();

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_DEPTH_TEST);

        updateViewport(true);
        game = new Game();
        levelEditorImGui = new ImGuiEditor(glfwWindow, "#version 410");
        frameBuffer = new FrameBuffer(viewPortWidth, viewPortHeight);
        postProcessor = new PostProcessor();

        handleResize();
    }



    /**
     * Main game loop that handles rendering and updates at a consistent tick rate.
     * The loop runs while the application window is open and performs the following:
     * - Polls for window events.
     * - Clears the OpenGL color and depth buffers.
     * - Updates the game state at a specified tick rate.
     * - Renders the current frame and swaps buffers.
     * - Calculates and updates the frames per second (FPS).
     * - Utilizes precise sleeping to reduce CPU usage and maintain consistent frame timing.
     */
    private void loop(){

        long timeSinceLastFrame = 0;
        long timeSinceLastTick = 0;
        long tempTime = System.nanoTime();
        long deltaTime;
        int fpsCounter = 0;
        long timer = 0;

        long frameDurationNano = 1_000_000_000 / FPS;
        long tickDurationNano = 1_000_000_000 / TPS;
        long sleepPaddingNano = 1000;

        while (!glfwWindowShouldClose(glfwWindow)){

            long currentTime = System.nanoTime();
            deltaTime = currentTime - tempTime;
            tempTime = currentTime;

            timeSinceLastTick += deltaTime;
            if (timeSinceLastTick >= tickDurationNano){
                game.tick();
                timeSinceLastTick -= tickDurationNano;
            }

            timeSinceLastFrame += deltaTime;
            if (timeSinceLastFrame >= frameDurationNano){
                glfwPollEvents();

                if (GLFW.glfwGetWindowAttrib(glfwWindow, GLFW.GLFW_FOCUSED) == GLFW.GLFW_TRUE && !(Game.getCurrentScene() instanceof EditorScene)) {
                    GLFW.glfwSetInputMode(glfwWindow, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN);
                }

                glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
                glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

                frameBuffer.bind();
                glViewport(0,0, viewPortWidth, viewPortHeight);
                glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
                glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
                game.frame((float) timeSinceLastTick / tickDurationNano);
                frameBuffer.unbind();

                glViewport(viewPortX,viewPortY, viewPortWidth, viewPortHeight);
                postProcessor.renderFrameBuffer(frameBuffer);

                glfwSwapBuffers(glfwWindow);
                timeSinceLastFrame -= frameDurationNano;
                fpsCounter ++;

            }

            timer += deltaTime;
            if (timer > 1_000_000_000){
                fpsMeasurement = fpsCounter;
                timer = 0;
                fpsCounter = 0;
            }

            //precise sleeping to reduce CPU usage
            long sleepTime = frameDurationNano - timeSinceLastFrame - sleepPaddingNano;
            if (sleepTime > 0) {
                LockSupport.parkNanos(sleepTime);
            }

        }
    }

    private void exit(){
        game.exit();
        levelEditorImGui.destroy();
        glfwFreeCallbacks(glfwWindow);
        glfwDestroyWindow(glfwWindow);
        glfwTerminate();
        Objects.requireNonNull(glfwSetErrorCallback(null)).free();
    }

    public static void standardExit(){
        glfwSetWindowShouldClose(glfwWindow, true);
    }

    public void handleResize() {
        frameBuffer.resize(viewPortWidth, viewPortHeight);
        game.handleResize((viewPortWidth + 2f)/(Game.screenWidth_BLOCK * Game.original_block_px));
    }

    public void updateViewport(boolean setQ){
        float windowAspect = (float) screenHeight/ screenWidth;

        if(windowAspect > aspectRatio){ // window narrower than aspect ratio, top and bottom letterbox
            viewPortWidth = screenWidth;
            viewPortHeight = Math.round(viewPortWidth * aspectRatio);
            viewPortX = 0;
            viewPortY = (screenHeight - viewPortHeight) / 2;
        }else { // window wider than aspect ratio, left and right letterbox
            viewPortHeight = screenHeight;
            viewPortWidth = Math.round(viewPortHeight / aspectRatio);
            viewPortX = (screenWidth - viewPortWidth) / 2;
            viewPortY = 0;
        }
        if(setQ) {
            glViewport(viewPortX, viewPortY, viewPortWidth, viewPortHeight);
        }
    }


    public static int[] getWindowPos(){
        int[] windowPosX = new int[1], windowPosY = new int[1];
        glfwGetWindowPos(glfwWindow, windowPosX, windowPosY);
        return new int[]{windowPosX[0], windowPosY[0]};
    }

    public static float getAbsoluteX(float relX){return relX * viewPortWidth / 2;}
    public static float getAbsoluteY(float relY){return relY * viewPortHeight / 2;}
    public static float getRelativeX(float absX){ return 2 * absX / viewPortWidth;}
    public static float getRelativeY(float absY){return 2 * absY / viewPortHeight;}

    public static double secToFrame(double sec){ return sec * FPS;}
    public static int realTimeFPS(){ return fpsMeasurement;}

    
}
