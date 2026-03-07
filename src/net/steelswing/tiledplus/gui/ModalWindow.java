/*
 * Ну вы же понимаете, что код здесь только мой?
 * Well, you do understand that the code here is only mine?
 */
package net.steelswing.tiledplus.gui;

import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.steelswing.engine.api.util.interfaces.Nullable;
import net.steelswing.tiledplus.TiledPlus;

/**
 * File: ModalWindow.java
 * Created on 19 мар. 2024 г., 00:31:31
 *
 * @author LWJGL2
 */
public class ModalWindow {

    protected static List<ModalWindow> modalWindows = new CopyOnWriteArrayList<>();
    private static int instanceCounter = 0;

    protected String title;
    protected boolean opened = false;
    protected boolean needClose;
    protected ImBoolean modalDialog = new ImBoolean(true);
    protected boolean firstBegin = true;
    protected boolean needOpen = false;

    public ModalWindow(String title) {
        this.title = title + "##mw" + (instanceCounter++);
        modalWindows.add(this);
    }

    public void destruct() {
        modalWindows.remove(this);
    }

    public void open() {
        needOpen = true;
    }

    public void close() {
        opened = false;
        needClose = true;
    }

    public void render() {
    }

    public boolean begin(int width, int height) {
        if (needOpen) {
            needOpen = false;
            modalDialog.set(true);
            firstBegin = true;
            ImGui.openPopup(title);
        }

        if (ImGui.beginPopupModal(title, modalDialog, ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoResize)) {
            ImGui.setWindowSize(width, height);
            ImGui.setWindowPos(TiledPlus.getWidth() / 2 - width / 2, TiledPlus.getHeight() / 2 - height / 2);

            float padding = 10;
            ImGui.beginChild("model.child." + title, ImGui.getContentRegionAvailX(), ImGui.getContentRegionAvailY(), false, ImGuiWindowFlags.NoScrollbar);
            ImGui.setCursorPosX(ImGui.getCursorPosX() + padding);
            ImGui.setCursorPosY(ImGui.getCursorPosY() + padding);
            ImGui.beginChild("model.child.sub." + title, ImGui.getContentRegionAvailX() - padding, ImGui.getContentRegionAvailY(), false, ImGuiWindowFlags.NoScrollbar);

            opened = true;
            return true;
        }

        opened = false;
        return false;
    }

    public void end() {
        if (needClose) {
            needClose = false;
            ImGui.closeCurrentPopup();
            modalDialog.set(false);
            firstBegin = true;
        }
        ImGui.endChild();
        ImGui.endChild();
        ImGui.endPopup();
    }

    public boolean isFirstBegin() {
        if (firstBegin) {
            firstBegin = false;
            return true;
        }
        return false;
    }

    @Nullable
    public static ModalWindow getFirstOpenedPopup() {
        for (ModalWindow modalWindow : modalWindows) {
            if (modalWindow.isOpened()) {
                return modalWindow;
            }
        }
        return null;
    }

    public boolean isOpened() {
        return opened;
    }
}