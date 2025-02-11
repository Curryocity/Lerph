package data;

import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class GameOption {

    final public static String optionFilePath = "assets/configs" + File.separator + "options.msgpack";

    public byte musicVolume; // 0-10
    public byte soundVolume; // 0-10

    public byte aspectRatio; // 16:9, 16:10
    static public final byte ASPECT_16_9 = 0, ASPECT_16_10 = 1;
    public byte fpsSetting; // vsync, 60, 120, 180, 240, unlimited
    static public final byte VSYNC = 0, F60 = 1, F120 = 2, F180 = 3, F240 = 4, UNLIMITED = 5;

    public byte cursorMode; // show when active, always show, always hide
    static public final byte SHOW_WHEN_ACTIVE = 0, ALWAYS_SHOW = 1, ALWAYS_HIDE = 2;
    public byte cameraMode; // fixed, smooth tracking, smooth look ahead
    public byte motionBlur; // 0-3 frames


    public boolean screenShakeOn;
    public boolean deathsCounterOn;
    public boolean speedrunTimerOn;

    public GameOption() {
        File optionFile = new File(optionFilePath);
        if(optionFile.exists()){
            loadOptions();
        }else{
            resetToDefault();
        }
    }

    public void resetToDefault() {
        musicVolume = 10;
        soundVolume = 10;
        aspectRatio = ASPECT_16_9;
        fpsSetting = F120;
        cameraMode = 0;
        motionBlur = 0;
        cursorMode = SHOW_WHEN_ACTIVE;
        screenShakeOn = false;
        deathsCounterOn = false;
        speedrunTimerOn = false;
    }

    public void loadOptions() {

        try (FileInputStream fileIn = new FileInputStream(optionFilePath)) {
            MessageUnpacker unPacker = MessagePack.newDefaultUnpacker(fileIn);

            unPacker.unpackMapHeader();
            while (unPacker.hasNext()) {
                String key = unPacker.unpackString();
                switch (key) {
                    case "music":
                        musicVolume = unPacker.unpackByte();
                        break;
                    case "sound":
                        soundVolume = unPacker.unpackByte();
                        break;
                    case "fps":
                        fpsSetting = unPacker.unpackByte();
                        break;
                    case "ratio":
                        aspectRatio = unPacker.unpackByte();
                        break;
                    case "cam":
                        cameraMode = unPacker.unpackByte();
                        break;
                    case "blur":
                        motionBlur = unPacker.unpackByte();
                        break;
                    case "cursor":
                        cursorMode = unPacker.unpackByte();
                        break;
                    case "shake":
                        screenShakeOn = unPacker.unpackBoolean();
                        break;
                    case "die":
                        deathsCounterOn = unPacker.unpackBoolean();
                        break;
                    case "timer":
                        speedrunTimerOn = unPacker.unpackBoolean();
                        break;
                    default:
                        break;
                }
            }

            fileIn.close();

            System.out.println("Show loaded options: " + musicVolume + " " + soundVolume + " " + fpsSetting + " " + aspectRatio + " " + cameraMode + " " + motionBlur + " " + cursorMode + " " + screenShakeOn + " " + deathsCounterOn + " " + speedrunTimerOn);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveOptions() {

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            MessagePacker packer = MessagePack.newDefaultPacker(out);

            packer.packMapHeader(10);
            packer.packString("music");
            packer.packByte(musicVolume);
            packer.packString("sound");
            packer.packByte(soundVolume);
            packer.packString("fps");
            packer.packByte(fpsSetting);
            packer.packString("ratio");
            packer.packByte(aspectRatio);
            packer.packString("cam");
            packer.packByte(cameraMode);
            packer.packString("blur");
            packer.packByte(motionBlur);
            packer.packString("cursor");
            packer.packByte(cursorMode);
            packer.packString("shake");
            packer.packBoolean(screenShakeOn);
            packer.packString("die");
            packer.packBoolean(deathsCounterOn);
            packer.packString("timer");
            packer.packBoolean(speedrunTimerOn);

            packer.flush();
            byte[] data = out.toByteArray();

            File optionFile = new File(optionFilePath);
            File parentDir = optionFile.getParentFile();

            if (parentDir != null && !parentDir.exists()) {
                if (parentDir.mkdirs())
                    System.out.println("Parent directories created.");
                else
                    System.out.println("Failed to create parent directories.");

            }

            if(!optionFile.exists()){
                if (optionFile.createNewFile())
                    System.out.println("Option file created.");
                else
                    System.out.println("Failed to create option file.");
            }

            FileOutputStream fos = new FileOutputStream(optionFilePath);
            fos.write(data);
            fos.flush();
            fos.close();

        }catch (IOException e) {e.printStackTrace();}

    }

}
