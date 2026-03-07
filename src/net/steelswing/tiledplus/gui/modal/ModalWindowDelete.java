/*
 * Ну вы же понимаете, что код здесь только мой?
 * Well, you do understand that the code here is only mine?
 */

package net.steelswing.tiledplus.gui.modal;

import imgui.internal.ImGui;
import net.steelswing.tiledplus.gui.ModalWindow;
import org.lwjgl.glfw.GLFW;

/**
 * File: ModalWindowDelete.java
 * Created on 2026 Feb 10, 17:53:19
 *
 * @author LWJGL2
 */
public class ModalWindowDelete extends ModalWindow {

    public String desc = "";
    public Runnable onClick;

    public ModalWindowDelete() {
        super("Delete");
    }

    @Override
    public void open() {
        super.open();
    }

    @Override
    public void render() {
        if (begin(520, 160)) {

            ImGui.text(desc);
            ImGui.spacing();
            ImGui.separator();

            float buttonWidth = 90;
            float posX = ImGui.getContentRegionAvailX() - buttonWidth * 2 - 10;
            ImGui.setCursorPosX(posX);

            if (ImGui.button("OK", buttonWidth, 0) || ImGui.isKeyPressed(GLFW.GLFW_KEY_ENTER)) {
                if (onClick != null) {
                    try {
                        onClick.run();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    onClick = null;
                }
                close();
            }
            ImGui.sameLine();
            if (ImGui.button("Cancel", buttonWidth, 0)) {
                close();
            }
            end();
        }
    }
}
