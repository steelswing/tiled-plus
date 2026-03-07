/*
 * Ну вы же понимаете, что код здесь только мой?
 * Well, you do understand that the code here is only mine?
 */

package net.steelswing.tiledplus.imgui;

import imgui.ImDrawData;
import imgui.ImFontAtlas;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImGuiViewport;
import imgui.ImVec2;
import imgui.ImVec4;
import imgui.callback.ImPlatformFuncViewport;
import imgui.flag.ImGuiBackendFlags;
import imgui.flag.ImGuiConfigFlags;
import imgui.flag.ImGuiViewportFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.type.ImInt;
import java.nio.ByteBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;

/**
 * File: ImGuiImplGl2.java
 * Created on 30 авг. 2023 г., 21:35:43
 *
 * @author LWJGL2
 */
public class ImGuiImplGl2 {
    // OpenGL Data

    private int glVersion = 0;
    private String glslVersion = "";
    private int gFontTexture = 0;
    private int gShaderHandle = 0;
    private int gVertHandle = 0;
    private int gFragHandle = 0;
    private int gAttribLocationTex = 0;
    private int gAttribLocationProjMtx = 0;
    private int gAttribLocationVtxPos = 0;
    private int gAttribLocationVtxUV = 0;
    private int gAttribLocationVtxColor = 0;
    private int gVboHandle = 0;
    private int gElementsHandle = 0;
//    private int gVertexArrayObjectHandle = 0;

    // Used to store tmp renderer data
    private final ImVec2 displaySize = new ImVec2();
    private final ImVec2 framebufferScale = new ImVec2();
    private final ImVec2 displayPos = new ImVec2();
    private final ImVec4 clipRect = new ImVec4();
    private final float[] orthoProjMatrix = new float[4 * 4];

    // Variables used to backup GL state before and after the rendering of Dear ImGui
    private final int[] lastActiveTexture = new int[1];
    private final int[] lastProgram = new int[1];
    private final int[] lastTexture = new int[1];
    private final int[] lastArrayBuffer = new int[1];
    private final int[] lastVertexArrayObject = new int[1];
    private final int[] lastViewport = new int[4];
    private final int[] lastScissorBox = new int[4];
    private final int[] lastBlendSrcRgb = new int[1];
    private final int[] lastBlendDstRgb = new int[1];
    private final int[] lastBlendSrcAlpha = new int[1];
    private final int[] lastBlendDstAlpha = new int[1];
    private final int[] lastBlendEquationRgb = new int[1];
    private final int[] lastBlendEquationAlpha = new int[1];
    private boolean lastEnableBlend = false;
    private boolean lastEnableCullFace = false;
    private boolean lastEnableDepthTest = false;
    private boolean lastEnableStencilTest = false;
    private boolean lastEnableScissorTest = false;

    /**
     * Method to do an initialization of the {@link ImGuiImplGl3} state.
     * It SHOULD be called before calling of the {@link ImGuiImplGl3#renderDrawData(ImDrawData)} method.
     * <p>
     * Unlike in the {@link #init(String)} method, here the glslVersion argument is omitted.
     * Thus a "#version 130" string will be used instead.
     */
    public void init() {
        init(null);
    }

    /**
     * Method to do an initialization of the {@link ImGuiImplGl3} state.
     * It SHOULD be called before calling of the {@link ImGuiImplGl3#renderDrawData(ImDrawData)} method.
     * <p>
     * Method takes an argument, which should be a valid GLSL string with the version to use.
     * <pre>
     * ----------------------------------------
     * OpenGL    GLSL      GLSL
     * version   version   string
     * ---------------------------------------
     *  2.0       110       "#version 110"
     *  2.1       120       "#version 120"
     *  3.0       130       "#version 130"
     *  3.1       140       "#version 140"
     *  3.2       150       "#version 150"
     *  3.3       330       "#version 330 core"
     *  4.0       400       "#version 400 core"
     *  4.1       410       "#version 410 core"
     *  4.2       420       "#version 410 core"
     *  4.3       430       "#version 430 core"
     *  ES 3.0    300       "#version 300 es"   = WebGL 2.0
     * ---------------------------------------
     * </pre>
     * <p>
     * If the argument is null, then a "#version 130" string will be used by default.
     *
     * @param glslVersion string with the version of the GLSL
     */
    public void init(final String glslVersion) {
        readGlVersion();
        setupBackendCapabilitiesFlags();

        if (glslVersion == null) {
            this.glslVersion = "#version 130";
        } else {
            this.glslVersion = glslVersion;
        }

        createDeviceObjects();

        if (ImGui.getIO().hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
            initPlatformInterface();
        }
    }

    /**
     * Method to render {@link ImDrawData} into current OpenGL context.
     *
     * @param drawData draw data to render
     */
    public void renderDrawData(final ImDrawData drawData) {
        if (drawData.getCmdListsCount() <= 0) {
            return;
        }

        // Will project scissor/clipping rectangles into framebuffer space
        drawData.getDisplaySize(displaySize);           // (0,0) unless using multi-viewports
        drawData.getDisplayPos(displayPos);
        drawData.getFramebufferScale(framebufferScale); // (1,1) unless using retina display which are often (2,2)

        final float clipOffX = displayPos.x;
        final float clipOffY = displayPos.y;
        final float clipScaleX = framebufferScale.x;
        final float clipScaleY = framebufferScale.y;

        // Avoid rendering when minimized, scale coordinates for retina displays (screen coordinates != framebuffer coordinates)
        final int fbWidth = (int) (displaySize.x * framebufferScale.x);
        final int fbHeight = (int) (displaySize.y * framebufferScale.y);

        if (fbWidth <= 0 || fbHeight <= 0) {
            return;
        }

        backupGlState();
        bind(fbWidth, fbHeight);

        // Render command lists
        for (int cmdListIdx = 0; cmdListIdx < drawData.getCmdListsCount(); cmdListIdx++) {
            // Upload vertex/index buffers
            GL30.glBufferData(GL30.GL_ARRAY_BUFFER, drawData.getCmdListVtxBufferData(cmdListIdx), GL21.GL_STREAM_DRAW);
            GL30.glBufferData(GL30.GL_ELEMENT_ARRAY_BUFFER, drawData.getCmdListIdxBufferData(cmdListIdx), GL21.GL_STREAM_DRAW);

            for (int cmdBufferIdx = 0; cmdBufferIdx < drawData.getCmdListCmdBufferSize(cmdListIdx); cmdBufferIdx++) {
                drawData.getCmdListCmdBufferClipRect(cmdListIdx, cmdBufferIdx, clipRect);

                final float clipMinX = (clipRect.x - clipOffX) * clipScaleX;
                final float clipMinY = (clipRect.y - clipOffY) * clipScaleY;
                final float clipMaxX = (clipRect.z - clipOffX) * clipScaleX;
                final float clipMaxY = (clipRect.w - clipOffY) * clipScaleY;

                if (clipMaxX <= clipMinX || clipMaxY <= clipMinY) {
                    continue;
                }

                // Apply scissor/clipping rectangle (Y is inverted in OpenGL)
                GL21.glScissor((int) clipMinX, (int) (fbHeight - clipMaxY), (int) (clipMaxX - clipMinX), (int) (clipMaxY - clipMinY));
                // Bind texture, Draw
                final int textureId = drawData.getCmdListCmdBufferTextureId(cmdListIdx, cmdBufferIdx);
                final int elemCount = drawData.getCmdListCmdBufferElemCount(cmdListIdx, cmdBufferIdx);
                final int idxBufferOffset = drawData.getCmdListCmdBufferIdxOffset(cmdListIdx, cmdBufferIdx);
                final int vtxBufferOffset = drawData.getCmdListCmdBufferVtxOffset(cmdListIdx, cmdBufferIdx);
                final int indices = idxBufferOffset * ImDrawData.SIZEOF_IM_DRAW_IDX;

                GL21.glBindTexture(GL21.GL_TEXTURE_2D, textureId);

                if (glVersion >= 320) {
                    GL33.glDrawElementsBaseVertex(GL21.GL_TRIANGLES, elemCount, GL21.GL_UNSIGNED_SHORT, indices, vtxBufferOffset);
                } else {
                    GL21.glDrawElements(GL21.GL_TRIANGLES, elemCount, GL21.GL_UNSIGNED_SHORT, indices);
                }
            }
        }

        unbind();
        restoreModifiedGlState();
    }

    /**
     * Call this method in the end of your application cycle to dispose resources used by {@link ImGuiImplGl3}.
     */
    public void dispose() {
        GL21.glDeleteBuffers(gVboHandle);
        GL21.glDeleteBuffers(gElementsHandle);
        GL21.glDetachShader(gShaderHandle, gVertHandle);
        GL21.glDetachShader(gShaderHandle, gFragHandle);
        GL21.glDeleteProgram(gShaderHandle);
        GL21.glDeleteTextures(gFontTexture);
        shutdownPlatformInterface();
    }

    /**
     * Method rebuilds the font atlas for Dear ImGui. Could be used to update application fonts in runtime.
     */
    public void updateFontsTexture() {
        GL21.glDeleteTextures(gFontTexture);

        final ImFontAtlas fontAtlas = ImGui.getIO().getFonts();
        final ImInt width = new ImInt();
        final ImInt height = new ImInt();
        final ByteBuffer buffer = fontAtlas.getTexDataAsRGBA32(width, height);

        gFontTexture = GL21.glGenTextures();

        GL21.glBindTexture(GL21.GL_TEXTURE_2D, gFontTexture);
        GL21.glTexParameteri(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_MIN_FILTER, GL21.GL_LINEAR);
        GL21.glTexParameteri(GL21.GL_TEXTURE_2D, GL21.GL_TEXTURE_MAG_FILTER, GL21.GL_LINEAR);

        GL21.glTexImage2D(GL21.GL_TEXTURE_2D, 0, GL21.GL_RGBA, width.get(), height.get(), 0, GL21.GL_RGBA, GL21.GL_UNSIGNED_BYTE, buffer);

        fontAtlas.setTexID(gFontTexture);
    }

    private void readGlVersion() {
        final int[] major = new int[1];
        final int[] minor = new int[1];
        GL21.glGetIntegerv(GL33.GL_MAJOR_VERSION, major);
        GL21.glGetIntegerv(GL33.GL_MINOR_VERSION, minor);
        glVersion = major[0] * 100 + minor[0] * 10;
    }

    private void setupBackendCapabilitiesFlags() {
        final ImGuiIO io = ImGui.getIO();
        io.setBackendRendererName("imgui_java_impl_opengl3");

        // We can honor the ImDrawCmd::VtxOffset field, allowing for large meshes.
        if (glVersion >= 320) {
            io.addBackendFlags(ImGuiBackendFlags.RendererHasVtxOffset);
        }

        // We can create multi-viewports on the Renderer side (optional)
        io.addBackendFlags(ImGuiBackendFlags.RendererHasViewports);
    }

    private void createDeviceObjects() {
        // Backup GL state
        final int[] lastTexture = new int[1];
        final int[] lastArrayBuffer = new int[1];
        final int[] lastVertexArray = new int[1];
        GL21.glGetIntegerv(GL21.GL_TEXTURE_BINDING_2D, lastTexture);
        GL21.glGetIntegerv(GL21.GL_ARRAY_BUFFER_BINDING, lastArrayBuffer);
        GL21.glGetIntegerv(GL33.GL_VERTEX_ARRAY_BINDING, lastVertexArray);

        createShaders();

        gAttribLocationTex = GL21.glGetUniformLocation(gShaderHandle, "Texture");
        gAttribLocationProjMtx = GL21.glGetUniformLocation(gShaderHandle, "ProjMtx");
        gAttribLocationVtxPos = GL21.glGetAttribLocation(gShaderHandle, "Position");
        gAttribLocationVtxUV = GL21.glGetAttribLocation(gShaderHandle, "UV");
        gAttribLocationVtxColor = GL21.glGetAttribLocation(gShaderHandle, "Color");

        // Create buffers
        gVboHandle = GL21.glGenBuffers();
        gElementsHandle = GL21.glGenBuffers();

        updateFontsTexture();

        // Restore modified GL state
        GL21.glBindTexture(GL21.GL_TEXTURE_2D, lastTexture[0]);
        GL21.glBindBuffer(GL21.GL_ARRAY_BUFFER, lastArrayBuffer[0]);
//        GLWrapper.glBindVertexArray(lastVertexArray[0]);
    }

    private void createShaders() {
        final int glslVersionValue = parseGlslVersionString();

        // Select shaders matching our GLSL versions
        final CharSequence vertShaderSource;
        final CharSequence fragShaderSource;

        if (glslVersionValue < 130) {
            vertShaderSource = getVertexShaderGlsl120();
            fragShaderSource = getFragmentShaderGlsl120();
        } else if (glslVersionValue == 300) {
            vertShaderSource = getVertexShaderGlsl300es();
            fragShaderSource = getFragmentShaderGlsl300es();
        } else if (glslVersionValue >= 410) {
            vertShaderSource = getVertexShaderGlsl410Core();
            fragShaderSource = getFragmentShaderGlsl410Core();
        } else {
            vertShaderSource = getVertexShaderGlsl130();
            fragShaderSource = getFragmentShaderGlsl130();
        }

        gVertHandle = createAndCompileShader(GL30.GL_VERTEX_SHADER, vertShaderSource);
        gFragHandle = createAndCompileShader(GL30.GL_FRAGMENT_SHADER, fragShaderSource);

        gShaderHandle = GL30.glCreateProgram();
        GL30.glAttachShader(gShaderHandle, gVertHandle);
        GL30.glAttachShader(gShaderHandle, gFragHandle);
        GL30.glLinkProgram(gShaderHandle);

        if (GL30.glGetProgrami(gShaderHandle, GL30.GL_LINK_STATUS) == GL11.GL_FALSE) {
            throw new IllegalStateException("Failed to link shader program:\n" + GL21.glGetProgramInfoLog(gShaderHandle));
        }
    }

    private int parseGlslVersionString() {
        final Pattern p = Pattern.compile("\\d+");
        final Matcher m = p.matcher(glslVersion);

        if (m.find()) {
            return Integer.parseInt(m.group());
        } else {
            throw new IllegalArgumentException("Invalid GLSL version string: " + glslVersion);
        }
    }

    private void backupGlState() {
        GL21.glGetIntegerv(GL21.GL_ACTIVE_TEXTURE, lastActiveTexture);
        GL21.glActiveTexture(GL21.GL_TEXTURE0);
        GL21.glGetIntegerv(GL21.GL_CURRENT_PROGRAM, lastProgram);
        GL21.glGetIntegerv(GL21.GL_TEXTURE_BINDING_2D, lastTexture);
        GL21.glGetIntegerv(GL21.GL_ARRAY_BUFFER_BINDING, lastArrayBuffer);
//        GL21.glGetIntegerv(GL33.GL_VERTEX_ARRAY_BINDING, lastVertexArrayObject);
        GL21.glGetIntegerv(GL21.GL_VIEWPORT, lastViewport);
        GL21.glGetIntegerv(GL21.GL_SCISSOR_BOX, lastScissorBox);
        GL21.glGetIntegerv(GL21.GL_BLEND_SRC_RGB, lastBlendSrcRgb);
        GL21.glGetIntegerv(GL21.GL_BLEND_DST_RGB, lastBlendDstRgb);
        GL21.glGetIntegerv(GL21.GL_BLEND_SRC_ALPHA, lastBlendSrcAlpha);
        GL21.glGetIntegerv(GL21.GL_BLEND_DST_ALPHA, lastBlendDstAlpha);
        GL21.glGetIntegerv(GL21.GL_BLEND_EQUATION_RGB, lastBlendEquationRgb);
        GL21.glGetIntegerv(GL21.GL_BLEND_EQUATION_ALPHA, lastBlendEquationAlpha);
        lastEnableBlend = GL21.glIsEnabled(GL21.GL_BLEND);
        lastEnableCullFace = GL21.glIsEnabled(GL21.GL_CULL_FACE);
        lastEnableDepthTest = GL21.glIsEnabled(GL21.GL_DEPTH_TEST);
        lastEnableStencilTest = GL21.glIsEnabled(GL21.GL_STENCIL_TEST);
        lastEnableScissorTest = GL21.glIsEnabled(GL21.GL_SCISSOR_TEST);
    }

    private void restoreModifiedGlState() {
        GL30.glUseProgram(lastProgram[0]);
        GL21.glBindTexture(GL21.GL_TEXTURE_2D, lastTexture[0]);
        GL21.glActiveTexture(lastActiveTexture[0]);
//        GLWrapper.glBindVertexArray(lastVertexArrayObject[0]);
        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, lastArrayBuffer[0]);
        GL21.glBlendEquationSeparate(lastBlendEquationRgb[0], lastBlendEquationAlpha[0]);
        GL21.glBlendFuncSeparate(lastBlendSrcRgb[0], lastBlendDstRgb[0], lastBlendSrcAlpha[0], lastBlendDstAlpha[0]);
        // @formatter:off CHECKSTYLE:OFF
        if (lastEnableBlend) {
            GL21.glEnable(GL21.GL_BLEND);
        } else {
            GL21.glDisable(GL21.GL_BLEND);
        }
        if (lastEnableCullFace) {
            GL21.glEnable(GL21.GL_CULL_FACE);
        } else {
            GL21.glDisable(GL21.GL_CULL_FACE);
        }
        if (lastEnableDepthTest) {
            GL21.glEnable(GL21.GL_DEPTH_TEST);
        } else {
            GL21.glDisable(GL21.GL_DEPTH_TEST);
        }
        if (lastEnableStencilTest) {
            GL21.glEnable(GL21.GL_STENCIL_TEST);
        } else {
            GL21.glDisable(GL21.GL_STENCIL_TEST);
        }
        if (lastEnableScissorTest) {
            GL21.glEnable(GL21.GL_SCISSOR_TEST);
        } else {
            GL21.glDisable(GL21.GL_SCISSOR_TEST);
        }
        // @formatter:on CHECKSTYLE:ON
        GL21.glViewport(lastViewport[0], lastViewport[1], lastViewport[2], lastViewport[3]);
        GL21.glScissor(lastScissorBox[0], lastScissorBox[1], lastScissorBox[2], lastScissorBox[3]);
    }

    // Setup desired GL state
    private void bind(final int fbWidth, final int fbHeight) {
        // Recreate the VAO every time (this is to easily allow multiple GL contexts to be rendered to. VAO are not shared among GL contexts)
        // The renderer would actually work without any VAO bound, but then our VertexAttrib calls would overwrite the default one currently bound.
//        gVertexArrayObjectHandle = GLWrapper.glGenVertexArrays();

        // Setup render state: alpha-blending enabled, no face culling, no depth testing, scissor enabled, polygon fill
        GL21.glEnable(GL21.GL_BLEND);
        GL21.glBlendEquation(GL21.GL_FUNC_ADD);
        GL21.glBlendFuncSeparate(GL21.GL_SRC_ALPHA, GL21.GL_ONE_MINUS_SRC_ALPHA, GL21.GL_ONE, GL21.GL_ONE_MINUS_SRC_ALPHA);
        GL21.glDisable(GL21.GL_CULL_FACE);
        GL21.glDisable(GL21.GL_DEPTH_TEST);
        GL21.glDisable(GL21.GL_STENCIL_TEST);
        GL21.glEnable(GL21.GL_SCISSOR_TEST);

        // Setup viewport, orthographic projection matrix
        // Our visible imgui space lies from draw_data->DisplayPos (top left) to draw_data->DisplayPos+data_data->DisplaySize (bottom right).
        // DisplayPos is (0,0) for single viewport apps.
        GL21.glViewport(0, 0, fbWidth, fbHeight);
        final float left = displayPos.x;
        final float right = displayPos.x + displaySize.x;
        final float top = displayPos.y;
        final float bottom = displayPos.y + displaySize.y;

        // Orthographic matrix projection
        orthoProjMatrix[0] = 2.0f / (right - left);
        orthoProjMatrix[5] = 2.0f / (top - bottom);
        orthoProjMatrix[10] = -1.0f;
        orthoProjMatrix[12] = (right + left) / (left - right);
        orthoProjMatrix[13] = (top + bottom) / (bottom - top);
        orthoProjMatrix[15] = 1.0f;

        // Bind shader
        GL30.glUseProgram(gShaderHandle);
        GL30.glUniform1i(gAttribLocationTex, 0);
        GL30.glUniformMatrix4fv(gAttribLocationProjMtx, false, orthoProjMatrix);

//        GLWrapper.glBindVertexArray(gVertexArrayObjectHandle);
        // Bind vertex/index buffers and setup attributes for ImDrawVert
        GL21.glBindBuffer(GL21.GL_ARRAY_BUFFER, gVboHandle);
        GL21.glBindBuffer(GL21.GL_ELEMENT_ARRAY_BUFFER, gElementsHandle);
        GL21.glEnableVertexAttribArray(gAttribLocationVtxPos);
        GL21.glEnableVertexAttribArray(gAttribLocationVtxUV);
        GL21.glEnableVertexAttribArray(gAttribLocationVtxColor);
        GL21.glVertexAttribPointer(gAttribLocationVtxPos, 2, GL21.GL_FLOAT, false, ImDrawData.SIZEOF_IM_DRAW_VERT, 0);
        GL21.glVertexAttribPointer(gAttribLocationVtxUV, 2, GL21.GL_FLOAT, false, ImDrawData.SIZEOF_IM_DRAW_VERT, 8);
        GL21.glVertexAttribPointer(gAttribLocationVtxColor, 4, GL21.GL_UNSIGNED_BYTE, true, ImDrawData.SIZEOF_IM_DRAW_VERT, 16);
    }

    private void unbind() {
        // Destroy the temporary VAO
//        GLWrapper.glDeleteVertexArrays(gVertexArrayObjectHandle);

        GL21.glDisableVertexAttribArray(gAttribLocationVtxPos);
        GL21.glDisableVertexAttribArray(gAttribLocationVtxUV);
        GL21.glDisableVertexAttribArray(gAttribLocationVtxColor);
    }

    //--------------------------------------------------------------------------------------------------------
    // MULTI-VIEWPORT / PLATFORM INTERFACE SUPPORT
    // This is an _advanced_ and _optional_ feature, allowing the back-end to create and handle multiple viewports simultaneously.
    // If you are new to dear imgui or creating a new binding for dear imgui, it is recommended that you completely ignore this section first..
    //--------------------------------------------------------------------------------------------------------

    private void initPlatformInterface() {
        ImGui.getPlatformIO().setRendererRenderWindow(new ImPlatformFuncViewport() {
            @Override
            public void accept(final ImGuiViewport vp) {
                if (!vp.hasFlags(ImGuiViewportFlags.NoRendererClear)) {
                    GL21.glClearColor(0, 0, 0, 0);
                    GL21.glClear(GL21.GL_COLOR_BUFFER_BIT);
                }
                renderDrawData(vp.getDrawData());
            }
        });
    }

    private void shutdownPlatformInterface() {
        ImGui.destroyPlatformWindows();
    }

    private int createAndCompileShader(final int type, final CharSequence source) {
        final int id = GL21.glCreateShader(type);

        GL21.glShaderSource(id, source);
        GL21.glCompileShader(id);

        if (GL21.glGetShaderi(id, GL21.GL_COMPILE_STATUS) == GL21.GL_FALSE) {
            throw new IllegalStateException("Failed to compile shader:\n" + GL21.glGetShaderInfoLog(id));
        }

        return id;
    }

    private String getVertexShaderGlsl120() {
        return glslVersion + "\n" +
                "uniform mat4 ProjMtx;\n" +
                "attribute vec2 Position;\n" +
                "attribute vec2 UV;\n" +
                "attribute vec4 Color;\n" +
                "varying vec2 Frag_UV;\n" +
                "varying vec4 Frag_Color;\n" +
                "void main()\n" +
                "{\n" +
                "    Frag_UV = UV;\n" +
                "    Frag_Color = Color;\n" +
                "    gl_Position = ProjMtx * vec4(Position.xy,0,1);\n" +
                "}\n";
    }

    private String getVertexShaderGlsl130() {
        return glslVersion + "\n" +
                "uniform mat4 ProjMtx;\n" +
                "in vec2 Position;\n" +
                "in vec2 UV;\n" +
                "in vec4 Color;\n" +
                "out vec2 Frag_UV;\n" +
                "out vec4 Frag_Color;\n" +
                "void main()\n" +
                "{\n" +
                "    Frag_UV = UV;\n" +
                "    Frag_Color = Color;\n" +
                "    gl_Position = ProjMtx * vec4(Position.xy,0,1);\n" +
                "}\n";
    }

    private String getVertexShaderGlsl300es() {
        return glslVersion + "\n" +
                "precision highp float;\n" +
                "layout (location = 0) in vec2 Position;\n" +
                "layout (location = 1) in vec2 UV;\n" +
                "layout (location = 2) in vec4 Color;\n" +
                "uniform mat4 ProjMtx;\n" +
                "out vec2 Frag_UV;\n" +
                "out vec4 Frag_Color;\n" +
                "void main()\n" +
                "{\n" +
                "    Frag_UV = UV;\n" +
                "    Frag_Color = Color;\n" +
                "    gl_Position = ProjMtx * vec4(Position.xy,0,1);\n" +
                "}\n";
    }

    private String getVertexShaderGlsl410Core() {
        return glslVersion + "\n" +
                "layout (location = 0) in vec2 Position;\n" +
                "layout (location = 1) in vec2 UV;\n" +
                "layout (location = 2) in vec4 Color;\n" +
                "uniform mat4 ProjMtx;\n" +
                "out vec2 Frag_UV;\n" +
                "out vec4 Frag_Color;\n" +
                "void main()\n" +
                "{\n" +
                "    Frag_UV = UV;\n" +
                "    Frag_Color = Color;\n" +
                "    gl_Position = ProjMtx * vec4(Position.xy,0,1);\n" +
                "}\n";
    }

    private String getFragmentShaderGlsl120() {
        return glslVersion + "\n" +
                "#ifdef GL_ES\n" +
                "    precision mediump float;\n" +
                "#endif\n" +
                "uniform sampler2D Texture;\n" +
                "varying vec2 Frag_UV;\n" +
                "varying vec4 Frag_Color;\n" +
                "void main()\n" +
                "{\n" +
                "    gl_FragColor = Frag_Color * texture2D(Texture, Frag_UV.st);\n" +
                "}\n";
    }

    private String getFragmentShaderGlsl130() {
        return glslVersion + "\n" +
                "uniform sampler2D Texture;\n" +
                "in vec2 Frag_UV;\n" +
                "in vec4 Frag_Color;\n" +
                "out vec4 Out_Color;\n" +
                "void main()\n" +
                "{\n" +
                "    Out_Color = Frag_Color * texture(Texture, Frag_UV.st);\n" +
                "}\n";
    }

    private String getFragmentShaderGlsl300es() {
        return glslVersion + "\n" +
                "precision mediump float;\n" +
                "uniform sampler2D Texture;\n" +
                "in vec2 Frag_UV;\n" +
                "in vec4 Frag_Color;\n" +
                "layout (location = 0) out vec4 Out_Color;\n" +
                "void main()\n" +
                "{\n" +
                "    Out_Color = Frag_Color * texture(Texture, Frag_UV.st);\n" +
                "}\n";
    }

    private String getFragmentShaderGlsl410Core() {
        return glslVersion + "\n" +
                "in vec2 Frag_UV;\n" +
                "in vec4 Frag_Color;\n" +
                "uniform sampler2D Texture;\n" +
                "layout (location = 0) out vec4 Out_Color;\n" +
                "void main()\n" +
                "{\n" +
                "    Out_Color = Frag_Color * texture(Texture, Frag_UV.st);\n" +
                "}\n";
    }
}
