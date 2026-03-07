/*
 * Ну вы же понимаете, что код здесь только мой?
 * Well, you do understand that the code here is only mine?
 */
package net.steelswing.tiledplus.gui;

import imgui.ImGui;
import imgui.ImGuiInputTextCallbackData;
import imgui.callback.ImGuiInputTextCallback;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiKey;
import imgui.type.ImString;

/**
 * File: EditableText.java
 * Created on 2026 Feb 10, 04:15:34
 *
 * @author LWJGL2
 */
public class EditableText {

    protected ImString text;
    protected ImString editBuffer;
    protected String lastText;
    protected boolean isEditing;
    protected boolean justStartedEditing;
    protected String id;
    protected int maxLength;

    protected float yOffset = 4;

    public EditableText(String initialText, String id) {
        this(initialText, id, 256);
    }

    public EditableText(String initialText, String id, int maxLength) {
        this.maxLength = maxLength;
        this.text = new ImString(initialText, maxLength);
        this.editBuffer = new ImString(maxLength);
        this.isEditing = false;
        this.justStartedEditing = false;
        this.id = id;
    }

    public String getText() {
        return text.get();
    }

    public void setText(String newText) {
        text.set(newText);
    }

    public boolean isEditing() {
        return isEditing;
    }

    public void startEditing() {
        editBuffer.clear();
        editBuffer.set(text.get());
        lastText = editBuffer.get();
        isEditing = true;
        justStartedEditing = true;
    }

    public void cancelEditing() {
        isEditing = false;
        justStartedEditing = false;
    }

    public boolean render() {
        boolean valueChanged = false;

        if (isEditing) {
            valueChanged = renderEditMode();
        } else {
            renderDisplayMode();
        }

        return valueChanged;
    }

    protected boolean renderEditMode() {
        if (justStartedEditing) {
            ImGui.setKeyboardFocusHere();
        }

        int flags = ImGuiInputTextFlags.CallbackAlways |
                ImGuiInputTextFlags.EnterReturnsTrue |
                ImGuiInputTextFlags.AutoSelectAll;

        float cursorScreenPosY = ImGui.getCursorScreenPosY();
        ImGui.setCursorScreenPos(ImGui.getCursorScreenPosX(), cursorScreenPosY - 1 - yOffset);

        boolean enterPressed = ImGui.inputText("##edit_" + id, editBuffer, flags, new ImGuiInputTextCallback() {
            @Override
            public void accept(ImGuiInputTextCallbackData data) {
                lastText = data.getBuf();
            }
        });

        ImGui.setCursorScreenPos(ImGui.getCursorScreenPosX(), cursorScreenPosY);
        if (enterPressed) {
            text.set(lastText);
            isEditing = false;
            justStartedEditing = false;
            return true;
        }

        if (!justStartedEditing && !ImGui.isItemActive()) {
            text.set(lastText);
            isEditing = false;
        }

        if (justStartedEditing) {
            justStartedEditing = false;
        }

        if (ImGui.isKeyPressed(ImGui.getKeyIndex(ImGuiKey.Escape))) {
            cancelEditing();
        }

        return false;
    }

    protected void renderDisplayMode() {

        float cursorScreenPosY = ImGui.getCursorScreenPosY();
        ImGui.setCursorScreenPos(ImGui.getCursorScreenPosX(), cursorScreenPosY - yOffset);

        ImGui.pushStyleVar(imgui.flag.ImGuiStyleVar.FrameBorderSize, 0);
        ImGui.pushStyleColor(imgui.flag.ImGuiCol.FrameBg, 0, 0, 0, 0);
        ImGui.inputText("##display_" + id, text, ImGuiInputTextFlags.ReadOnly);
        ImGui.setCursorScreenPos(ImGui.getCursorScreenPosX(), cursorScreenPosY);
        
        
        
        ImGui.popStyleColor();
        ImGui.popStyleVar(1);

        if (ImGui.isItemHovered() && ImGui.isMouseDoubleClicked(0)) {
            startEditing();
        }
    }
}
