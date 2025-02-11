package data;

import core.Game;
import util.Util;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Map {
    // TODO: the entire file structure and checkpoints
    public static Set<String> existMaps = new HashSet<>();
    public static final String savesDirectory = "assets/saves";
    public static final String propertiesFile = "properties.cat"; // this is only decided in creation
    public static final String progressFile = "player.cat"; // this could be modified in gameplay, storing the game state
    public String name;
    public String directoryPath;
    private Space primeSpace; //the entry point of the world
    private Space currentSpace;
    private final List<Space> spaces = new ArrayList<>();

    private Map(String name){
        this.name = name;
        directoryPath = savesDirectory + "/" + name;
    }

    public static Map load(String name){
        Map theMap = new Map(name);
        Path worldPath = Paths.get(theMap.directoryPath);
        try {
            if (Files.notExists(worldPath)) {
                Files.createDirectories(worldPath);
                System.out.println("Map '" + name + "' does not exists, creating a new one...");
            }
            Path spaceDir = worldPath.resolve(Space.spaceDirectory);
            if (Files.notExists(spaceDir)) {
                Files.createDirectories(spaceDir);
                System.out.println("Space directory created: " + spaceDir);
            }
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(spaceDir)) {
                for (Path entry : stream) {
                    String spaceName = entry.getFileName().toString();
                    if(spaceName.endsWith(Space.fileExtension)){
                        theMap.spaces.add(new Space(theMap, spaceName.substring(0, spaceName.length() - Space.fileExtension.length())));
                    }else{
                        System.out.println("Exception: sus file detected in spaces directory");
                    }
                }
            } catch (DirectoryIteratorException e) {
                e.printStackTrace();
            }
        }catch (IOException e){e.printStackTrace();}

        return theMap;
    }

    public static void delete(String name){
        Path worldPath = Paths.get(savesDirectory + "/" + name);
        try {
            Util.deleteDirectory(worldPath);
            System.out.println("File deleted successfully.");
        } catch (IOException e) {
            System.out.println("Failed to delete the file.");
            e.printStackTrace();
        }
    }

    public void save(){
        Path basePath = Paths.get(directoryPath, Space.spaceDirectory);
        try {
            if (Files.notExists(basePath)) {
                Files.createDirectories(basePath);
                System.out.println("Map directories created: " + basePath);
            }
            for(Space space : spaces){
                Path spacePath = Paths.get(space.filePath);
                if(Files.notExists(spacePath)){
                    Files.createFile(spacePath);
                    space.setModified();
                }
                space.save();
            }

        } catch (IOException e) {e.printStackTrace();}
    }


    public void switchSpace(String spaceName, boolean saveQ){
        if(spaceName == null){
            currentSpace = null;
            return;
        }
        if(currentSpace != null){
            if(saveQ) currentSpace.save();
            currentSpace.exit();
        }
        for(Space space : spaces){
            if( space.name.equals(spaceName) ){
                currentSpace = space;
                space.load();
                Game.renderSystem.chunkRenderer.queueResizeEvent();
                return;
            }
        }
        throw new RuntimeException("Specified space with name '"+ spaceName + "' not found. It should be managed by system side.");
    }

    public void switchSpace(Space space, boolean saveQ){
        if(space == null){
            currentSpace = null;
            return;
        }
        if(currentSpace != null){
            if(saveQ) currentSpace.save();
            currentSpace.exit();
        }
        currentSpace = space;
        space.load();
        Game.renderSystem.chunkRenderer.queueResizeEvent();
    }

    public void refreshCurrentSpace(boolean saveQ){
        if(currentSpace != null){
            switchSpace(currentSpace, saveQ);
        }
    }

    public void switchDefaultSpace(boolean saveQ) {
        if(spaces.isEmpty()) return;
        if(primeSpace == null){
            primeSpace = spaces.getFirst();
        }
        switchSpace(primeSpace, saveQ);
    }

    public void unSwitchSpace(){
        currentSpace = null;
    }

    public static Map reload(Map world){
        return Map.load(world.name);
    }

    public Space getCurrentSpace(){
        return currentSpace;
    }

    public Space createSpace(String name, int chunkLengthX, int chunkLengthY){
        Space existSpace = findSpace(name);
        if(existSpace != null){
            return existSpace;
        }

        Space theSpace = new Space(this, name, chunkLengthX, chunkLengthY);
        spaces.add(theSpace);
        return theSpace;
    }

    public void removeSpace(String name){
        Space existSpace = findSpace(name);
        if(existSpace == null){
            throw new RuntimeException("Specified space with name '"+ name + "' not found. It should be managed by system side.");
        }
        removeSpace(existSpace);
    }

    public void removeSpace(Space space){
        if(space == null){
            throw new RuntimeException("Specified space with name '"+ name + "' not found. It should be managed by system side.");
        }
        Path spacePath = Paths.get(space.filePath);
        try {
            spaces.remove(space);
            Files.delete(spacePath);
            System.out.println("File deleted successfully.");
        } catch (IOException e) {
            System.out.println("Failed to delete the file.");
            e.printStackTrace();
        }
    }

    public Space findSpace(String name){
        for (Space existSpace : spaces) {
            if (existSpace.name.equals(name)) {
                return existSpace;
            }
        }
        return null;
    }

    public String[] getAllSpaceNames(){
        String[] spaceNames = new String[spaces.size()];
        for (int i = 0; i < spaceNames.length; i++) {
            spaceNames[i] = spaces.get(i).name;
        }
        return spaceNames;
    }

    public static void reloadSavesFolder(){
        Path worldDir = Paths.get(savesDirectory);
        existMaps.clear();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(worldDir)) {
            for (Path entry : stream) {
                String worldName = entry.getFileName().toString();
                if(Files.isDirectory(entry)){
                    existMaps.add(worldName);
                }
            }
        } catch (IOException | DirectoryIteratorException e) {
            e.printStackTrace();
        }
    }


    public void saveProgress() {

    }
}
