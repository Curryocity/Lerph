package renderer;

import org.joml.*;
import org.lwjgl.BufferUtils;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL20.glGetProgramInfoLog;
import static org.lwjgl.opengl.GL32.GL_GEOMETRY_SHADER;

public class Shader {
    private int shaderProgramID;

    private String vertexSrc, fragmentSrc;
    private String geometrySrc = null;
    private String filePath;

    public Shader(String filePath){
        this.filePath = filePath;
        try{
            String source = new String(Files.readAllBytes(Paths.get(filePath)));
            String[] splitString = source.split("(//type)( )+([a-zA-Z]+)");

            if (splitString.length < 3) {
                throw new RuntimeException("Error: input shader does not include a vertex and fragment shader each. File: '" + filePath + "'");
            }

            // Process each type header and its corresponding shader source
            int patternIndex = 1;
            int searchIndex = 0;
            while (patternIndex < splitString.length) {
                searchIndex = source.indexOf("//type", searchIndex) + 7;
                int endOfLine = source.indexOf("\n", searchIndex);
                String pattern = source.substring(searchIndex, endOfLine).trim();

                readPattern(pattern, splitString[patternIndex].trim());
                patternIndex++;
                searchIndex = endOfLine;
            }

        }catch(IOException e) {throw new RuntimeException("Error: Failed to open shader file '"+filePath + "'");}
    }

    public void readPattern(String pattern, String content){
        switch (pattern) {
            case "vertex" -> vertexSrc = content;
            case "fragment" -> fragmentSrc = content;
            case "geometry" -> geometrySrc = content;
            default -> throw new RuntimeException("Unexpected shader type token '" + pattern + "'");
        }
    }
    public void compile() {
        int vertexID = compileShader(GL_VERTEX_SHADER, vertexSrc, "vertex");
        int fragmentID = compileShader(GL_FRAGMENT_SHADER, fragmentSrc, "fragment");
        int geometryID = -1;

        if (geometrySrc != null) {
            geometryID = compileShader(GL_GEOMETRY_SHADER, geometrySrc, "geometry");
        }

        shaderProgramID = glCreateProgram();
        glAttachShader(shaderProgramID, vertexID);
        glAttachShader(shaderProgramID, fragmentID);
        if (geometrySrc != null) {
            glAttachShader(shaderProgramID, geometryID);
        }
        glLinkProgram(shaderProgramID);
        /*  Linking is not redundant.
            Attaching does not do this final check, it’s just like gathering the parts.
            Linking turns those parts into a usable shader program that the GPU can run.
            -- ChatGPT ;)                                                                  */

        // Error handling for linking
        if (glGetProgrami(shaderProgramID, GL_LINK_STATUS) == GL_FALSE) {
            int len = glGetProgrami(shaderProgramID, GL_INFO_LOG_LENGTH);
            throw new RuntimeException("ERROR: '" + filePath + "'\n\tLinking of shaders failed.\n" + glGetProgramInfoLog(shaderProgramID, len));
        }

        String log = glGetProgramInfoLog(shaderProgramID, glGetProgrami(shaderProgramID, GL_INFO_LOG_LENGTH));
        if (!log.isEmpty()) {
            System.err.println("Program Linking Error: " + log);
        }

        glDetachShader(shaderProgramID, vertexID);
        glDetachShader(shaderProgramID, fragmentID);
        glDeleteShader(vertexID);
        glDeleteShader(fragmentID);
        if (geometrySrc != null) {
            glDetachShader(shaderProgramID, geometryID);
            glDeleteShader(geometryID);
        }
    }

    private int compileShader(int shaderType, String source, String shaderName) {
        int shaderID = glCreateShader(shaderType);
        glShaderSource(shaderID, source);
        glCompileShader(shaderID);

        if (glGetShaderi(shaderID, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new RuntimeException("ERROR: '" + filePath + "'\n\t" + shaderName + " shader compilation failed.\n" + glGetShaderInfoLog(shaderID, glGetShaderi(shaderID, GL_INFO_LOG_LENGTH)));
        }
        return shaderID;
    }


    public void use(){
        // Bind shader program
        glUseProgram(shaderProgramID);
    }

    public void detach(){
        glUseProgram(0);
    }

    public void uploadMat4f(String varName, Matrix4f mat4) {
        int varLocation = glGetUniformLocation(shaderProgramID, varName);
        FloatBuffer matBuffer = BufferUtils.createFloatBuffer(16);
        mat4.get(matBuffer);
        glUniformMatrix4fv(varLocation, false, matBuffer);
    }

    public void uploadMat3f(String varName, Matrix3f mat3) {
        int varLocation = glGetUniformLocation(shaderProgramID, varName);
        use();
        FloatBuffer matBuffer = BufferUtils.createFloatBuffer(9);
        mat3.get(matBuffer);
        glUniformMatrix3fv(varLocation, false, matBuffer);
    }

    public void uploadVec4f(String varName, Vector4f vec) {
        int varLocation = glGetUniformLocation(shaderProgramID, varName);
        use();
        glUniform4f(varLocation, vec.x, vec.y, vec.z, vec.w);
    }

    public void uploadVec3f(String varName, Vector3f vec) {
        int varLocation = glGetUniformLocation(shaderProgramID, varName);
        use();
        glUniform3f(varLocation, vec.x, vec.y, vec.z);
    }

    public void uploadVec2f(String varName, Vector2f vec) {
        int varLocation = glGetUniformLocation(shaderProgramID, varName);
        use();
        glUniform2f(varLocation, vec.x, vec.y);
    }

    public void uploadFloat(String varName, float val) {
        int varLocation = glGetUniformLocation(shaderProgramID, varName);
        use();
        glUniform1f(varLocation, val);
    }

    public void uploadInt(String varName, int val) {
        int varLocation = glGetUniformLocation(shaderProgramID, varName);
        use();
        glUniform1i(varLocation, val);
    }

    public void uploadTexture(String varName, int slot) {
        int varLocation = glGetUniformLocation(shaderProgramID, varName);
        use();
        glUniform1i(varLocation, slot);
    }

    public void uploadIntArray(String varName, int[] array) {
        int varLocation = glGetUniformLocation(shaderProgramID, varName);
        use();
        glUniform1iv(varLocation, array);
    }
}
