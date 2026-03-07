/*
 * Ну вы же понимаете, что код здесь только мой?
 * Well, you do understand that the code here is only mine?
 */

package net.steelswing.tiledplus.imgui;

import imgui.*;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiConfigFlags;
import imgui.glfw.ImGuiImplGlfw;

import java.awt.*;
import java.io.IOException;
import java.util.logging.Level;

import org.apache.commons.io.IOUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import static org.lwjgl.glfw.GLFW.glfwGetMonitors;
import static org.lwjgl.glfw.GLFW.glfwGetVideoMode;
import org.lwjgl.glfw.GLFWVidMode;

/**
 * File: ImGui.java
 * Created on 30 авг. 2023 г., 19:10:18
 *
 * @author LWJGL2
 */
public class ImGuImp {

    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(ImGuImp.class.getName());

    private ImGuiImplGlfw imGuiGlfw;
    private ImGuiImplGl2 imGuiGl3;

    public static boolean hightDPIMonitor = false;

    public void initImgui(long windowHandle) {
        try {
            imGuiGlfw = new ImGuiImplGlfw();
            imGuiGl3 = new ImGuiImplGl2();
            ImGui.createContext();
            imGuiGlfw.init(windowHandle, true);

            final ImGuiIO io = ImGui.getIO();
            io.setIniFilename(null);
            io.addConfigFlags(ImGuiConfigFlags.DockingEnable);      // Enable Docking
            io.setConfigFlags(io.getConfigFlags() & ~ImGuiConfigFlags.NavEnableKeyboard);
//            io.addConfigFlags(ImGuiConfigFlags.DpiEnableScaleFonts);
//            io.addConfigFlags(ImGuiConfigFlags.ViewportsEnable);    // Enable Multi-Viewport / Platform Windows
//            io.setConfigViewportsNoTaskBarIcon(true);
//            io.addConfigFlags(ImGuiConfigFlags.ViewportsEnable);
            updateStyles();

            try {
                initFonts(io);
            } catch (Throwable e) {
                LOG.log(Level.SEVERE, "Failed to load font", e);
            }
            imGuiGl3.init("#version 120"); // najifka supported
        } catch (Throwable e) {
            e.printStackTrace();
            imGuiGlfw = null;
            imGuiGl3 = null;
        }
    }

    public void updateStyles() {
        ImGuiStyle style = ImGui.getStyle();

        style.setWindowTitleAlign(0.5f, 0.5f);
        style.setWindowBorderSize(1);
        style.setFrameBorderSize(1);
        style.setChildBorderSize(1);
        style.setFramePadding(5, 5);
        style.setItemSpacing(12, 8);
        style.setScrollbarSize(15);
        style.setScrollbarRounding(15);
        style.setGrabMinSize(15);
        style.setGrabRounding(7);
        style.setChildRounding(4);
        style.setFrameRounding(4);

        float[][] colors = style.getColors();

        // Основные цвета текста
        colors[ImGuiCol.Text] = ColorUtility.colorToRGBA(new Color(220, 220, 220, 255));
        colors[ImGuiCol.TextDisabled] = ColorUtility.colorToRGBA(new Color(120, 120, 120, 255));

        // Фон окон и панелей
        colors[ImGuiCol.WindowBg] = ColorUtility.colorToRGBA(new Color(45, 45, 48, 255));
        colors[ImGuiCol.ChildBg] = ColorUtility.colorToRGBA(new Color(37, 37, 38, 255));

        colors[ImGuiCol.PopupBg] = ColorUtility.colorToRGBA(new Color(50, 50, 55, 245));

        // Границы
        colors[ImGuiCol.Border] = ColorUtility.colorToRGBA(new Color(60, 60, 60, 255));
        colors[ImGuiCol.BorderShadow] = ColorUtility.colorToRGBA(new Color(0, 0, 0, 0));

        // Поля ввода и фреймы
        colors[ImGuiCol.FrameBg] = ColorUtility.colorToRGBA(new Color(60, 60, 65, 255));
        colors[ImGuiCol.FrameBgHovered] = ColorUtility.colorToRGBA(new Color(70, 70, 75, 255));
        colors[ImGuiCol.FrameBgActive] = ColorUtility.colorToRGBA(new Color(75, 75, 80, 255));

        // Заголовки окон
        colors[ImGuiCol.TitleBg] = ColorUtility.colorToRGBA(new Color(35, 35, 38, 255));
        colors[ImGuiCol.TitleBgActive] = ColorUtility.colorToRGBA(new Color(42, 42, 45, 255));
        colors[ImGuiCol.TitleBgCollapsed] = ColorUtility.colorToRGBA(new Color(35, 35, 38, 200));

        colors[ImGuiCol.MenuBarBg] = ColorUtility.colorToRGBA(new Color(40, 40, 43, 255));

        // Скроллбар
        colors[ImGuiCol.ScrollbarBg] = ColorUtility.colorToRGBA(new Color(30, 30, 33, 255));
        colors[ImGuiCol.ScrollbarGrab] = ColorUtility.colorToRGBA(new Color(80, 80, 85, 255));
        colors[ImGuiCol.ScrollbarGrabHovered] = ColorUtility.colorToRGBA(new Color(100, 100, 105, 255));
        colors[ImGuiCol.ScrollbarGrabActive] = ColorUtility.colorToRGBA(new Color(120, 120, 125, 255));

        colors[ImGuiCol.CheckMark] = ColorUtility.colorToRGBA(new Color(100, 180, 255, 255));

        // Слайдеры
        colors[ImGuiCol.SliderGrab] = ColorUtility.colorToRGBA(new Color(100, 180, 255, 255));
        colors[ImGuiCol.SliderGrabActive] = ColorUtility.colorToRGBA(new Color(120, 200, 255, 255));

        // Кнопки
        colors[ImGuiCol.Button] = ColorUtility.colorToRGBA(new Color(60, 60, 65, 255));
        colors[ImGuiCol.ButtonHovered] = ColorUtility.colorToRGBA(new Color(70, 70, 75, 255));
        colors[ImGuiCol.ButtonActive] = ColorUtility.colorToRGBA(new Color(75, 75, 80, 255));

        // Заголовки (используется для выделения элементов в списках)
        colors[ImGuiCol.Header] = ColorUtility.colorToRGBA(new Color(70, 120, 180, 100));
        colors[ImGuiCol.HeaderHovered] = ColorUtility.colorToRGBA(new Color(80, 130, 190, 150));
        colors[ImGuiCol.HeaderActive] = ColorUtility.colorToRGBA(new Color(90, 140, 200, 200));

        // Разделители
        colors[ImGuiCol.Separator] = ColorUtility.colorToRGBA(new Color(60, 60, 60, 255));
        colors[ImGuiCol.SeparatorHovered] = ColorUtility.colorToRGBA(new Color(80, 80, 85, 255));
        colors[ImGuiCol.SeparatorActive] = ColorUtility.colorToRGBA(new Color(100, 140, 180, 255));

        colors[ImGuiCol.ResizeGrip] = ColorUtility.colorToRGBA(new Color(60, 60, 60, 100));
        colors[ImGuiCol.ResizeGripHovered] = ColorUtility.colorToRGBA(new Color(80, 80, 85, 150));
        colors[ImGuiCol.ResizeGripActive] = ColorUtility.colorToRGBA(new Color(100, 140, 180, 255));

        // Вкладки
        colors[ImGuiCol.Tab] = ColorUtility.colorToRGBA(new Color(50, 50, 55, 255));
        colors[ImGuiCol.TabHovered] = ColorUtility.colorToRGBA(new Color(70, 120, 180, 200));
        colors[ImGuiCol.TabActive] = ColorUtility.colorToRGBA(new Color(60, 60, 65, 255));
        colors[ImGuiCol.TabUnfocused] = ColorUtility.colorToRGBA(new Color(45, 45, 48, 255));
        colors[ImGuiCol.TabUnfocusedActive] = ColorUtility.colorToRGBA(new Color(55, 55, 60, 255));

        // Графики
        colors[ImGuiCol.PlotLines] = ColorUtility.colorToRGBA(new Color(100, 180, 255, 255));
        colors[ImGuiCol.PlotLinesHovered] = ColorUtility.colorToRGBA(new Color(120, 200, 255, 255));
        colors[ImGuiCol.PlotHistogram] = ColorUtility.colorToRGBA(new Color(100, 180, 255, 255));
        colors[ImGuiCol.PlotHistogramHovered] = ColorUtility.colorToRGBA(new Color(120, 200, 255, 255));

        colors[ImGuiCol.TextSelectedBg] = ColorUtility.colorToRGBA(new Color(70, 120, 180, 150));
        colors[ImGuiCol.DragDropTarget] = ColorUtility.colorToRGBA(new Color(100, 180, 255, 200));
        colors[ImGuiCol.NavHighlight] = ColorUtility.colorToRGBA(new Color(100, 180, 255, 255));
        colors[ImGuiCol.NavWindowingHighlight] = ColorUtility.colorToRGBA(new Color(255, 255, 255, 180));
        colors[ImGuiCol.NavWindowingDimBg] = ColorUtility.colorToRGBA(new Color(20, 20, 20, 150));

        style.setColors(colors);
    }

    /**
     * Example of fonts configuration
     * For more information read: https://github.com/ocornut/imgui/blob/33cdbe97b8fd233c6c12ca216e76398c2e89b0d8/docs/FONTS.md
     */
    private void initFonts(final ImGuiIO io) {
        try {
            final ImFontGlyphRangesBuilder rangesBuilder = new ImFontGlyphRangesBuilder(); // Glyphs ranges provide
            rangesBuilder.addRanges(io.getFonts().getGlyphRangesDefault());
            rangesBuilder.addRanges(io.getFonts().getGlyphRangesCyrillic());

            // Добавляем специальные символы и эмодзи
            rangesBuilder.addText("═│─┌┐└┘├┤┬┴┼"); // Box drawing characters
            rangesBuilder.addText("▀▁▂▃▄▅▆▇█▉▊▋▌▍▎▏"); // Block elements
            rangesBuilder.addText("░▒▓"); // Shade characters
            rangesBuilder.addText("→←↑↓"); // Arrows
            rangesBuilder.addText("•◦∙"); // Bullets
            rangesBuilder.addText("µ"); // Micro sign
            rangesBuilder.addText("═"); // Micro sign
            rangesBuilder.addText("Σ"); // Micro sign

//            rangesBuilder.addRanges(FontAwesomeIcons._IconRange);
            final ImFontConfig fontConfig = new ImFontConfig();
//
            final short[] glyphRanges = rangesBuilder.buildRanges();

            PointerBuffer monitors = glfwGetMonitors();
            if (monitors != null) {
                for (int i = 0; i < monitors.limit(); i++) {
                    long monitor = monitors.get(i);
                    GLFWVidMode vidMode = glfwGetVideoMode(monitor);
                    int width = vidMode.width();
                    int height = vidMode.height();

                    System.out.println("Monitor " + i + ": " + width + "x" + height);
                    if (width >= 3840 && height >= 2160) {
                        hightDPIMonitor = true;
                        System.out.println(" → 4K monitor");
                    } else if (width >= 2560 && height >= 1440) {
                        hightDPIMonitor = true;
                        System.out.println(" → 2K monitor");
                    } else {
                        System.out.println(" → Other resolution: ");
                    }
                }
            }

//             io.getFonts().addFontFromMemoryTTF(IOUtils.toByteArray(getClass().getResourceAsStream("/assets/gui/Philosopher-Bold.ttf")), 18, fontConfig, glyphRanges); // cyrillic glyphs
            io.getFonts().addFontFromMemoryTTF(IOUtils.toByteArray(getClass().getResourceAsStream("/assets/gui/RFDewi-Regular.ttf")), hightDPIMonitor ? 25 : 15, fontConfig, glyphRanges); // cyrillic glyphs
//            io.getFonts().addFontFromMemoryTTF(IOUtils.toByteArray(getClass().getResourceAsStream("/assets/gui/consolas.ttf")), hightDPIMonitor ? 25 : 15, fontConfig, glyphRanges); // cyrillic glyphs
//            io.getFonts().addFontFromMemoryTTF(IOUtils.toByteArray(getClass().getResourceAsStream("/assets/gui/fa-regular-400.ttf")), 14, fontConfig, glyphRanges); // font awesome
//            io.getFonts().addFontFromMemoryTTF(IOUtils.toByteArray(getClass().getResourceAsStream("/assets/gui/fa-solid-900.ttf")), 14, fontConfig, glyphRanges); // font awesome
            io.getFonts().build();
            fontConfig.destroy();
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }


    /**
     * Method called at the beginning of the main cycle.
     * It clears OpenGL buffer and starts an ImGui frame.
     */
    public boolean startImgui() {
        if (imGuiGlfw == null) {
            return false;
        }
        imGuiGlfw.newFrame();
        ImGui.newFrame();
        return true;
    }

    /**
     * Method called in the end of the main cycle.
     * It renders ImGui and swaps GLFW buffers to show an updated frame.
     */
    public void endImgui() {
        if (imGuiGlfw == null) {
            return;
        }
        ImGui.render();
        imGuiGl3.renderDrawData(ImGui.getDrawData());

//        System.out.println(ImGui.getIO().hasConfigFlags(ImGuiConfigFlags.NavEnableKeyboard));
        if (ImGui.getIO().hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
            final long backupWindowPtr = GLFW.glfwGetCurrentContext();
            ImGui.updatePlatformWindows();
            ImGui.renderPlatformWindowsDefault();
            GLFW.glfwMakeContextCurrent(backupWindowPtr);
        }
    }
}
