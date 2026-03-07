/*
 * Ну вы же понимаете, что код здесь только мой?
 * Well, you do understand that the code here is only mine?
 */

package net.steelswing.tiledplus.gui.modal;

import imgui.internal.ImGui;
import imgui.type.ImString;
import java.util.function.Consumer;
import net.steelswing.tiledplus.gui.ModalWindow;
import org.lwjgl.glfw.GLFW;

/**
 * File: ModalRename.java
 * Created on 2026 Mar 7, 22:15:39
 *
 * @author LWJGL2
 */
public class ModalRename extends ModalWindow {

    private final ImString input = new ImString(256);
    public Consumer<String> onConfirm; // принимает новое имя
    public Runnable onClose; // принимает новое имя

    public ModalRename() {
        super("Rename");
    }

    public void open(String currentName, Consumer<String> onConfirm, Runnable onClose) {
        input.set(currentName);
        this.onConfirm = onConfirm;
        this.onClose = onClose;
        super.open();
    }

    @Override
    public void render() {
        if (begin(360, 120)) {
            ImGui.text("Name:");
            ImGui.setNextItemWidth(ImGui.getContentRegionAvailX());

            // Фокус на поле при открытии
            if (ImGui.isWindowAppearing()) {
                ImGui.setKeyboardFocusHere();
            }

            boolean confirmed = ImGui.inputText("##rename_input", input,
                    imgui.flag.ImGuiInputTextFlags.EnterReturnsTrue);

            ImGui.spacing();
            ImGui.separator();

            float buttonWidth = 90f;
            ImGui.setCursorPosX(ImGui.getContentRegionAvailX() - buttonWidth * 2 - 10f);

            if (ImGui.button("OK", buttonWidth, 0) || confirmed || ImGui.isKeyPressed(GLFW.GLFW_KEY_ENTER)) {
                if (onConfirm != null && !input.get().isBlank()) {
                    onConfirm.accept(input.get().trim());
                    onConfirm = null;
                }
                close();
                if (onClose != null) {
                    onClose.run();
                }
            }
            ImGui.sameLine();
            if (ImGui.button("Cancel", buttonWidth, 0) || ImGui.isKeyPressed(GLFW.GLFW_KEY_ESCAPE)) {
                close();
                if (onClose != null) {
                    onClose.run();
                }
            }

            end();
        }
    }
}
