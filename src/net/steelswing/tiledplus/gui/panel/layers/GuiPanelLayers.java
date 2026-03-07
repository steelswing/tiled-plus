/*
 * Ну вы же понимаете, что код здесь только мой?
 * Well, you do understand that the code here is only mine?
 */

package net.steelswing.tiledplus.gui.panel.layers;

import imgui.ImDrawList;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiDir;
import java.util.List;
import net.steelswing.engine.api.util.interfaces.Nullable;
import net.steelswing.tiledplus.gui.DockMenuBase;
import net.steelswing.tiledplus.gui.EditableText;
import net.steelswing.tiledplus.gui.GuiEditorMain;
import net.steelswing.tiledplus.gui.IconManager;
import net.steelswing.tiledplus.layer.EditorSession;
import net.steelswing.tiledplus.layer.Layer;
import net.steelswing.tiledplus.layer.LayerType;
import net.steelswing.tiledplus.layer.type.TileLayer;
import org.lwjgl.glfw.GLFW;

/**
 * File: GuiPanelLayers.java
 * Created on 2026 Feb 10, 03:22:42
 *
 * @author LWJGL2
 */
public class GuiPanelLayers extends DockMenuBase {

    protected final GuiEditorMain editor;
    protected int selectedLayerIndex = 0;

    public GuiPanelLayers(GuiEditorMain editor) {
        super("Layers", ImGuiDir.Right, 400f, 200f);
        this.editor = editor;
    }

    @Override
    public void render() {
        if (ImGui.begin(getTitle())) {

            float width = ImGui.getContentRegionAvailX();
            float height = ImGui.getContentRegionAvailY() - 60;
            final EditorSession editorSession = editor.editorSession;

            List<Layer> layers = editorSession.layers;
            if (ImGui.beginChild("###", width, height)) {
                // Обработка F2 для переименования выбранного слоя
                if (selectedLayerIndex >= 0 && selectedLayerIndex < layers.size()) {
                    Layer selected = layers.get(selectedLayerIndex);
                    if (ImGui.isWindowFocused() && ImGui.isKeyPressed(GLFW.GLFW_KEY_F2)) {
                        LayerUserObject userObject = (LayerUserObject) selected.userObject;
                        if (userObject != null) {
                            if (!userObject.editableText.isEditing()) {
                                userObject.editableText.startEditing();
                            }
                        }
                    }
                }

                for (int i = 0; i < layers.size(); i++) {
                    renderLayer(layers.get(i), i);
                }

                ImGui.endChild();
            }

            ImGui.separator();

            int iconSize = (int) (imgui.internal.ImGui.getFontSize() * 1.2f);

            if (ImGui.imageButton(IconManager.TILED.PAGE_ADD, iconSize, iconSize)) {
                editorSession.layers.add(new TileLayer(editorSession.levelWidth, editorSession.levelHeight).setName("TileLayer " + (editorSession.layers.size() + 1)));
            }
            ImGui.sameLine();
            if (ImGui.imageButton(IconManager.TILED.PAGE_DELETE, iconSize, iconSize)) {
                if (selectedLayerIndex >= 0 && selectedLayerIndex < layers.size()) {
                    Layer selected = layers.get(selectedLayerIndex);
                    final Layer setToRemove = selected;
                    if (setToRemove != null) {
                        editor.modalDelete.desc = "Do you really want to delete layer '" + selected.name + "'?";
                        editor.modalDelete.onClick = () -> {
                            editorSession.layers.remove(setToRemove);
                        };
                        editor.modalDelete.open();
                        selectedLayerIndex = 0;
                    }
                }
            }

            ImGui.end();
        }
    }

    @Nullable
    public Layer getSelectedLayer() {
        List<Layer> layers = editor.editorSession.layers;
        if (selectedLayerIndex >= 0 && selectedLayerIndex < layers.size()) {
            return layers.get(selectedLayerIndex);
        }
        return null;
    }

    @Nullable
    public TileLayer getSelectedLayerTileLayer() {
        List<Layer> layers = editor.editorSession.layers;
        if (selectedLayerIndex >= 0 && selectedLayerIndex < layers.size()) {
            Layer layer = layers.get(selectedLayerIndex);
            if (layer instanceof TileLayer) {
                return (TileLayer) layer;
            }
        }
        return null;
    }
    
    public List<Layer> getLayers() {
        return editor.editorSession.layers;
    }

    protected void renderLayer(Layer layer, int index) {
        if (layer.userObject == null) {
            final LayerUserObject layerUserObject = new LayerUserObject(layer.name);
            layer.userObject = layerUserObject;
        }
        LayerUserObject userObject = (LayerUserObject) layer.userObject;
        layer.name = userObject.editableText.getText();

        boolean isSelected = (selectedLayerIndex == index);

        // Получаем позицию и размеры для фона
        float cursorX = ImGui.getCursorScreenPosX();
        float cursorY = ImGui.getCursorScreenPosY();
        float availWidth = ImGui.getContentRegionAvailX();

        int iconSize = (int) (ImGui.getFontSize() * 1);
        float itemHeight = Math.max(iconSize, ImGui.getFontSize()) + ImGui.getStyle().getFramePaddingY() * 2;

        // Рисуем фон для выбранного слоя
        if (isSelected) {
            ImDrawList drawList = ImGui.getWindowDrawList();
            int selectedColor = ImGui.getColorU32(ImGuiCol.Header);
            drawList.addRectFilled(
                    cursorX,
                    cursorY,
                    cursorX + availWidth,
                    cursorY + itemHeight,
                    selectedColor
            );
        }

        // Проверка клика по области слоя (AABB)
        float mouseX = ImGui.getMousePosX();
        float mouseY = ImGui.getMousePosY();
        boolean isMouseOver = mouseX >= cursorX && mouseX <= cursorX + availWidth && mouseY >= cursorY && mouseY <= cursorY + itemHeight;

        if (isMouseOver && ImGui.isMouseClicked(0)) {
            selectedLayerIndex = index;
        }

        // Отступ слева
        ImGui.dummy(5, itemHeight);
        ImGui.sameLine();

        // Иконка слоя
        ImGui.setCursorPosY(ImGui.getCursorPosY() + (itemHeight - iconSize) * 0.5f);
        ImGui.image(layer.layerType == LayerType.TILESET ? IconManager.TILED.LAYER_TILES : IconManager.TILED.LAYER_OBJECT, iconSize, iconSize);
        ImGui.sameLine();

        // Текст с возможностью редактирования
        ImGui.pushItemWidth(ImGui.getContentRegionAvailX() - 50);
        if (userObject.editableText.render()) {
            System.out.println("Слой переименован: " + userObject.editableText.getText());
        }
        ImGui.popItemWidth();

        ImGui.sameLine();
        ImGui.setCursorScreenPos(ImGui.getCursorScreenPosX(), cursorY);
        if (ImGui.checkbox("##visible_" + index, userObject.visible)) {
            userObject.visible = !userObject.visible;
            layer.visible = userObject.visible;
            System.out.println("Видимость слоя изменена: " + userObject.visible);
        }
    }

    protected class LayerUserObject {

        public EditableText editableText;
        public boolean visible = true;

        public LayerUserObject(String name) {
            this.editableText = new EditableText(name, hashCode() + "");
        }
    }
}
