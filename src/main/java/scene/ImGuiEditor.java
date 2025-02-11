package scene;

import block.BlockLoader;
import core.Application;
import core.Game;
import data.Map;
import data.Space;
import entity.EntityLoader;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImVec2;
import imgui.ImVec4;
import imgui.flag.*;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import imgui.type.ImString;
import manager.AssetPool;
import manager.MouseInput;
import org.joml.Vector4f;
import util.Util;
import org.lwjgl.glfw.GLFW;
import component.Sprite;
import renderer.RendererLibrary;

import java.util.Arrays;
import java.util.stream.Stream;

import static block.BlockLoader.blockSprites;
import static entity.EntityLoader.entitySprites;
import static manager.MouseInput.getMouseWorldCoords;
import static org.lwjgl.glfw.GLFW.glfwGetCurrentContext;
import static scene.EditorScene.*;

public class ImGuiEditor {
    private EditorScene editor;
    private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();
    private ImGuiIO io;


    public ImGuiEditor(long windowPtr, String glslVersion) {
        init(windowPtr, glslVersion);
    }

    public void init(long windowPtr, String glslVersion) {
        ImGui.createContext();
        io = ImGui.getIO();
        io.setIniFilename("imgui.ini");
        io.addConfigFlags(ImGuiConfigFlags.ViewportsEnable);
        imGuiGlfw.init(windowPtr, true);
        imGuiGl3.init(glslVersion);
    }

    boolean isItTimeToShowPopup = true;
    public void closeAllPopups(){
        isItTimeToShowPopup = false;
        render();
        isItTimeToShowPopup = true;
    }

    @FunctionalInterface
    private interface popupExecutor{
        void execute();
    }

    private void executePopupIfPossible(String popupName, popupExecutor executor){
        if(ImGui.beginPopup(popupName)){
            if(isItTimeToShowPopup) executor.execute();
            else ImGui.closeCurrentPopup();
            ImGui.endPopup();
        }
    }

    public void render() {

        imGuiGlfw.newFrame();
        imGuiGl3.newFrame();
        ImGui.newFrame();

        levelEditorStuff(); // all the custom stuff happens here

        ImGui.render();
        imGuiGl3.renderDrawData(ImGui.getDrawData());

        if (ImGui.getIO().hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
            final long backupWindowPtr = glfwGetCurrentContext();
            ImGui.updatePlatformWindows();
            ImGui.renderPlatformWindowsDefault();
            GLFW.glfwMakeContextCurrent(backupWindowPtr);
        }

    }


    private void setupDockSpace() {
        int windowFlags = ImGuiWindowFlags.MenuBar | ImGuiWindowFlags.NoDocking;

        ImGui.setNextWindowPos(0.0f, 0.0f, ImGuiCond.Always);
        ImGui.setNextWindowSize(Application.viewPortWidth * 0.5f, Application.viewPortHeight * 0.5f, ImGuiCond.Always);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 0.0f);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowBorderSize, 0.0f);
        windowFlags |= ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoCollapse |
                ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoMove |
                ImGuiWindowFlags.NoBringToFrontOnFocus | ImGuiWindowFlags.NoNavFocus;

        ImGui.begin("Dockspace Demo", new ImBoolean(true), windowFlags);
        ImGui.popStyleVar(2);

        // Dockspace
        ImGui.dockSpace(ImGui.getID("Dockspace"));
    }

    private void levelEditorStuff() {
        editorMenu();
    }

    public void bindLevelEditorScene(EditorScene editor){
        this.editor = editor;
        refreshMapOptions();
    }

    private final ImInt objectPicker = new ImInt(0);
    private final ImVec2 windowPos = new ImVec2(), windowSize = new ImVec2(), itemSpacing = new ImVec2();
    String[] pickerOptions = {"blocks", "entities"};

    private void editorMenu(){
        ImGui.begin("Editor");

        if (ImGui.collapsingHeader("Map selection and settings", ImGuiTreeNodeFlags.DefaultOpen) ) {
            if (editor.map == null) {
                curMapName = "N/A";
                curSpaceName = "N/A";
            } else {
                curMapName = editor.map.name;
                curSpaceName = editor.map.getCurrentSpace() == null ? "N/A" : editor.map.getCurrentSpace().name;
            }


            mapSelectMenu();
            if(editor.map != null) {
                spaceSelectorMenu();
            }

            // Refresh Button
            if (ImGui.button("Refresh Maps Folder")) {
                editor.saveCurrentMap();
                refreshMapOptions();
            }

            if(editor.currentSpace() != null) {
                ImGui.text(String.format("SpawnPoint: ( %.1f , %.1f )", editor.currentSpace().getSpawnX(), editor.currentSpace().getSpawnY()));
            }

            if (ImGui.button("Return to SpawnPoint")) {
                editor.camera().setWorldPosition(editor.currentSpace().getSpawnX(), editor.currentSpace().getSpawnY());
                editor.setZoom(2.5f, editor.camera.worldCoords().x, editor.camera.worldCoords().y);
            }

            ImGui.sameLine();

            if(editor.settingMode != SPAWNPOINT) {
                if (ImGui.button("Set New SpawnPoint")) {
                    editor.settingMode = SPAWNPOINT;
                    selectedBlock = -1;
                    selectedEntity = -1;
                }
            }else{
                if (ImGui.button("Cancel Set Spawn")) {
                    editor.settingMode = NONE;
                }
            }

        }


        if (ImGui.collapsingHeader("Main editor", ImGuiTreeNodeFlags.DefaultOpen)) {

            if(ImGui.button("play")){
                editor.playQueue = true;
            }

            ImGui.sameLine();

            if(editor.settingMode != PLAYTEST) {
                if (ImGui.button("PlayTest")) {
                    editor.settingMode = PLAYTEST;
                    selectedBlock = -1;
                    selectedEntity = -1;
                }
            }else{
                if (ImGui.button("Cancel PlayTest")) {
                    editor.settingMode = NONE;
                }
            }

            float mouseX = getMouseWorldCoords().x;
            float mouseY = getMouseWorldCoords().y;
            ImGui.text(String.format("Looking At: ( %.1f , %.1f )", mouseX, mouseY));

            String selectedBlockName = "N/A";
            if (selectedBlock > 0) {
                if (BlockLoader.blocks[selectedBlock] != null) {
                    selectedBlockName = BlockLoader.blocks[selectedBlock].name;
                }
            }
            ImGui.text("Selected Block: " + selectedBlockName);

            // show grid if there's space in editor
            if (editor.map != null) {
                if (editor.map.getCurrentSpace() != null) {
                    if (ImGui.checkbox("Show Chunk Border", editor.chunkBorderOn)) {
                        editor.chunkBorderOn = !editor.chunkBorderOn;
                    }
                    if (ImGui.checkbox("Show Block Grids", editor.blockGridOn)) {
                        editor.blockGridOn = !editor.blockGridOn;
                    }
                }
            }

            ImGui.text("Zoom: " + (int) (editor.zoom / EditorScene.defaultZoom * 100) + "%%");
            float[] zoomValue = {editor.zoom / EditorScene.defaultZoom};
            if (ImGui.sliderFloat("##Zoom", zoomValue, editor.minZoom, editor.maxZoom, ImGuiSliderFlags.Logarithmic)) {
                editor.setZoom(zoomValue[0], editor.camera.worldCoords().x, editor.camera.worldCoords().y);
            }

            ImGui.combo("picker", objectPicker, pickerOptions);
            ImGui.getWindowPos(windowPos);
            ImGui.getWindowSize(windowSize);
            ImGui.getStyle().getItemSpacing(itemSpacing);
            ImGui.pushStyleColor(ImGuiCol.ButtonHovered, new ImVec4(1.0f, 1.0f, 1.0f, 1.0f));

            //block
            switch (objectPicker.intValue()) {
                case 0:
                    blockPicker();
                    break;
                case 1:
                    entityPicker();
                    break;
            }

            ImGui.popStyleColor();

        }

        if (ImGui.collapsingHeader("Debug info") ){
            ImGui.text("FPS: " + Application.realTimeFPS());
            ImGui.text("Screen Size: ( " + Application.viewPortWidth + " , " + Application.viewPortHeight + " )");
            ImGui.text("Mouse Screen Coords: ( " + (int) MouseInput.getX() + " , " + (int) MouseInput.getY() + " )");
            float camX = editor.camera().worldCoords().x;
            float camY =  editor.camera().worldCoords().y;
            ImGui.text(String.format("Camera At: ( %.1f , %.1f )", camX, camY));
        }

        // delete section
        if (ImGui.collapsingHeader("Dangerous Zone")) {
            ImGui.text("Do not accidentally click here.");
            if (ImGui.button("Delete current space")) {
                Space currentSpace = editor.currentSpace();
                if(currentSpace != null) {
                    editor.map.removeSpace(editor.currentSpace());
                    editor.map.unSwitchSpace();
                    refreshMapOptions();
                }
            }
            if (ImGui.button("Delete current map") && editor.map != null) {
                Map.delete(editor.map.name);
                editor.map = null;
                refreshMapOptions();
            }
        }

        ImGui.end();
    }

    public int selectedBlock = -1;
    public final Vector4f blankDimension = new Vector4f();
    public void blockPicker(){
        float window_maxX = windowPos.x + windowSize.x;
        selectedEntity = -1;

        for (int id = 0; id < BlockLoader.blocks.length; id++) {
            if (BlockLoader.blocks[id] == null) break;

            Sprite sprite = blockSprites[BlockLoader.blockID_SpriteID_MAP[id]];

            float spriteWidth, spriteHeight;
            int texID;
            Vector4f dimension;
            if (id != 0) {
                spriteWidth = sprite.sizeX * 4;
                spriteHeight = sprite.sizeY * 4;
                texID = RendererLibrary.getTextureID(sprite.textureSrc);
                dimension = sprite.txcDimensions;
            } else {
                spriteWidth = 64;
                spriteHeight = 64;
                texID = 0;
                dimension = blankDimension;
            }

            if (id == selectedBlock) {
                ImGui.pushStyleColor(ImGuiCol.Button, new ImVec4(1.0f, 1.0f, 1.0f, 1.0f));
            }

            ImGui.pushID(id);
            ImGui.imageButton(texID + 1, spriteWidth, spriteHeight, dimension.x, dimension.y + dimension.w, dimension.x + dimension.z, dimension.y);
            if (id == selectedBlock) {
                ImGui.popStyleColor();
            }

            if (ImGui.isItemClicked()) {
                selectedBlock = (selectedBlock == id) ? -1 : id;
            }
            ImGui.popID();

            ImVec2 buttonPos = new ImVec2();
            ImGui.getItemRectMax(buttonPos);
            if (buttonPos.x + itemSpacing.x + spriteWidth < window_maxX) {
                ImGui.sameLine();
            }

        }

        ImGui.newLine();
    }

    public int selectedEntity = -1;
    public void entityPicker(){
        float window_maxX = windowPos.x + windowSize.x;
        final int texID =  RendererLibrary.getTextureID(EntityLoader.getEntitySheetTexture());
        selectedBlock = -1;

        for (int id = 0; id < EntityLoader.entityID_SpriteID_MAP.length; id++) {

            int spriteID = EntityLoader.entityID_SpriteID_MAP[id];
            Sprite sprite = entitySprites.get(spriteID);

            if(id != 0 && spriteID == 0) break;

            float spriteWidth, spriteHeight;

            spriteWidth = sprite.sizeX * 4;
            spriteHeight = sprite.sizeY * 4;
            Vector4f dimension = sprite.txcDimensions;

            if (id == selectedEntity) {
                ImGui.pushStyleColor(ImGuiCol.Button, new ImVec4(1.0f, 1.0f, 1.0f, 1.0f));
            }
            ImGui.pushID(id);
            ImGui.imageButton(texID + 1, spriteWidth, spriteHeight, dimension.x, dimension.y + dimension.w, dimension.x + dimension.z, dimension.y);
            if (id == selectedEntity) { ImGui.popStyleColor(); }
            if (ImGui.isItemClicked()) {
                selectedEntity = (selectedEntity == id) ? -1 : id;
            }
            ImGui.popID();

            ImVec2 buttonPos = new ImVec2();
            ImGui.getItemRectMax(buttonPos);
            if (buttonPos.x + itemSpacing.x + spriteWidth < window_maxX) {
                ImGui.sameLine();
            }
        }
        ImGui.newLine();
    }

    private final ImInt mapSelector = new ImInt(0), spaceSelector = new ImInt(0);
    private final int[] spaceWidth = {1}, spaceHeight = {1};
    String[] mapOptions = new String[0], spaceOptions = new String[0];
    ImString inputName = new ImString(64);
    String curSpaceName, curMapName, warningText = "";

    public void mapSelectMenu(){

        ImGui.text("Map:");
        mapSelector.set(Util.searchArrayIndexByName(mapOptions, curMapName));
        if (ImGui.combo("##select map", mapSelector, mapOptions)) {
            if(!mapOptions[mapSelector.get()].equals("N/A")) {

                if (mapSelector.get() == mapOptions.length - 1) {
                    inputName.clear();
                    warningText = "";
                    int[] windowPos = Application.getWindowPos();
                    ImGui.setNextWindowPos(new ImVec2(windowPos[0] + Application.viewPortWidth * 0.25f , windowPos[1] + Application.viewPortHeight * 0.25f) , new ImVec2(0.5f, 0.5f) );
                    ImGui.openPopup("map creation");
                } else {
                    saveCurrentAndLoadMap(mapOptions[mapSelector.get()]);
                }

            }
        }
        executePopupIfPossible("map creation", this::mapCreationPopup);
    }

    public void mapCreationPopup(){
        Map.reloadSavesFolder();

        ImGui.text("Creating new map");
        ImGui.text("Name:");
        ImGui.sameLine();
        ImGui.inputText("##map name", inputName);

        if (ImGui.button("Cancel")) {ImGui.closeCurrentPopup();}
        ImGui.sameLine();
        if (ImGui.button("Done")) {
            if(inputName.get().isEmpty()){
                warningText = "The name cannot be blank";
            } else if (Map.existMaps.contains(inputName.get())) {
                warningText = "The name was occupied already";
            }else{
                saveCurrentAndLoadMap(inputName.get());
                ImGui.closeCurrentPopup();
            }

        }
        showWarningText(warningText);
    }

    public void spaceSelectorMenu(){

        ImGui.text("Current Space:");
        spaceSelector.set(Util.searchArrayIndexByName(spaceOptions, curSpaceName));
        if (ImGui.combo("##select space", spaceSelector, spaceOptions)) {
            if(!spaceOptions[spaceSelector.get()].equals("N/A") && editor.map != null) {

                if (spaceSelector.get() == spaceOptions.length - 1) {
                    int[] windowPos = Application.getWindowPos();
                    ImGui.setNextWindowPos(new ImVec2(windowPos[0] + Application.viewPortWidth * 0.25f , windowPos[1] + Application.viewPortHeight * 0.25f) , new ImVec2(0.5f, 0.5f) );
                    inputName.clear();
                    warningText = "";
                    ImGui.openPopup("space creation");
                } else {
                    loadSpace(spaceOptions[spaceSelector.get()]);
                }

            }
        }

        executePopupIfPossible("space creation", this::spaceCreationPopup);

    }

    public void spaceCreationPopup(){

        ImGui.text("Creating new space");
        ImGui.text("Name:");
        ImGui.sameLine();
        ImGui.inputText("##space name", inputName);
        ImGui.sliderInt("Width(in chunks)", spaceWidth, 1, 16);
        ImGui.sliderInt("Height(in chunks)", spaceHeight, 1, 16);

        if (ImGui.button("Cancel")) {ImGui.closeCurrentPopup();}
        ImGui.sameLine();
        if (ImGui.button("Done")) {
            if(inputName.get().isEmpty()){
                warningText = "The name cannot be blank";
            } else if (Util.contains(editor.map.getAllSpaceNames(), inputName.get())) {
                warningText = "The name was occupied already";
            }else{
                Space newSpace = editor.map.createSpace(inputName.get(), spaceWidth[0], spaceHeight[0]);
                loadSpace(newSpace);
                ImGui.closeCurrentPopup();
            }
        }
        showWarningText(warningText);

    }

    public void showWarningText(String text){
        ImGui.pushStyleColor(ImGuiCol.Text, new ImVec4(1f, 0.39f,0.28f,1));
        ImGui.text(text);
        ImGui.popStyleColor();
    }

    public void refreshMapOptions(){
        Map.reloadSavesFolder();
        Game.renderSystem.chunkRenderer.syncScene(editor);


        int i = (editor.map != null) ? 0 : 1;
        mapOptions = new String[Map.existMaps.size() + 1 + i];
        mapOptions[0] = "N/A";

        for(String mapName : Map.existMaps){
            mapOptions[i] = mapName;
            i++;
        }
        mapOptions[mapOptions.length - 1] = "> Create a New Map...";

        spaceOptions = (editor.map != null) ? editor.map.getAllSpaceNames() : new String[0];
        if (spaceOptions.length == 0) spaceOptions = new String[]{"N/A"};
        spaceOptions = Stream.concat( Arrays.stream(spaceOptions),
                Stream.of("> Create a New Space...")  ).toArray(String[]::new);
    }

    public void loadSpace(String spaceName){
        editor.map.switchSpace(spaceName, true);
        refreshMapOptions();
        editor.resetCameraPosition();
    }

    public void loadSpace(Space space){
        editor.map.switchSpace(space, true);
        refreshMapOptions();
        editor.resetCameraPosition();
    }

    public void saveCurrentAndLoadMap(String newMapName){
        editor.saveCurrentMap();
        editor.map = Map.load(newMapName);
        editor.map.switchDefaultSpace(true);
        editor.resetCameraPosition();
        refreshMapOptions();
    }

    public void destroy() { ImGui.destroyContext(); }
}