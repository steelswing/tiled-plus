/*
 * Ну вы же понимаете, что код здесь только мой?
 * Well, you do understand that the code here is only mine?
 */
package net.steelswing.tiledplus.gui.panel.canvas.tool;

import imgui.ImDrawList;
import imgui.flag.ImGuiMouseButton;
import imgui.internal.ImGui;
import net.steelswing.tiledplus.gui.IconManager;
import net.steelswing.tiledplus.gui.panel.canvas.GuiPanelCanvas;
import net.steelswing.tiledplus.gui.panel.canvas.HistoryAction;
import net.steelswing.tiledplus.layer.Tile;
import net.steelswing.tiledplus.layer.type.TileLayer;
import org.lwjgl.glfw.GLFW;

// ================= ИНСТРУМЕНТ СТИРАНИЯ =================
public class EraseTool extends Tool {

    // Хранение предыдущей позиции для интерполяции
    private int lastTileX = -1;
    private int lastTileY = -1;

    public EraseTool(GuiPanelCanvas canvas) {
        super(canvas);
    }

    @Override
    public String getName() {
        return "ERASE";
    }

    @Override
    public int getHotkey() {
        return GLFW.GLFW_KEY_E;
    }

    @Override
    public String getTooltip() {
        return "Eraser Tool (E)\nRight-click drag to erase area";
    }

    @Override
    public int getIcon() {
        return IconManager.TILED.ERASE;
    }

    @Override
    public void renderPreview(ImDrawList drawList, int hoveredTileX, int hoveredTileY, int mapWidth, int mapHeight, float imageX, float imageY, float tileWidthZ, float tileHeightZ, Tile[][] selectedTiles) {
        // Рендер выделения правой кнопкой
        renderRightClickSelection(drawList, mapWidth, mapHeight, imageX, imageY, tileWidthZ, tileHeightZ);

        int brushWidth = selectedTiles != null ? selectedTiles.length : 1;
        int brushHeight = selectedTiles != null ? selectedTiles[0].length : 1;
        int startTileX = hoveredTileX - brushWidth / 2;
        int startTileY = hoveredTileY - brushHeight / 2;
        for (int i = 0; i < brushWidth; i++) {
            for (int j = 0; j < brushHeight; j++) {
                int targetX = startTileX + i;
                int targetY = startTileY + j;
                float xx = imageX + (targetX * tileWidthZ);
                float yy = imageY + (targetY * tileHeightZ);
                boolean isOutOfBounds = targetX < 0 || targetX >= mapWidth || targetY < 0 || targetY >= mapHeight;
                if (isOutOfBounds) {
                    drawList.addRectFilled(xx, yy, xx + tileWidthZ, yy + tileHeightZ, ImGui.getColorU32(1f, 0f, 0f, 0.3f));
                } else {
                    drawList.addRectFilled(xx, yy, xx + tileWidthZ, yy + tileHeightZ, ImGui.getColorU32(1f, 0f, 0f, 0.2f));
                }
                int borderColor = isOutOfBounds ? ImGui.getColorU32(1f, 0f, 0f, 0.8f) : ImGui.getColorU32(1f, 0f, 0f, 0.6f);
                drawList.addRect(xx, yy, xx + tileWidthZ, yy + tileHeightZ, borderColor, 0f, 0, 1f);
            }
        }
    }

    @Override
    public void handleInput(TileLayer layer, int hoveredTileX, int hoveredTileY, int mapWidth, int mapHeight, Tile[][] selectedTiles) {
        // ================= ПРАВАЯ КНОПКА МЫШИ - ВЫДЕЛЕНИЕ ОБЛАСТИ ДЛЯ СТИРАНИЯ =================
        if (ImGui.isMouseClicked(ImGuiMouseButton.Right)) {
            rightClickStartX = hoveredTileX;
            rightClickStartY = hoveredTileY;
            rightClickEndX = hoveredTileX;
            rightClickEndY = hoveredTileY;
            isRightClickSelecting = true;
        }

        if (isRightClickSelecting && ImGui.isMouseDown(ImGuiMouseButton.Right)) {
            rightClickEndX = hoveredTileX;
            rightClickEndY = hoveredTileY;
        }

        if (ImGui.isMouseReleased(ImGuiMouseButton.Right) && isRightClickSelecting) {
            isRightClickSelecting = false;

            int minX = Math.min(rightClickStartX, rightClickEndX);
            int maxX = Math.max(rightClickStartX, rightClickEndX);
            int minY = Math.min(rightClickStartY, rightClickEndY);
            int maxY = Math.max(rightClickStartY, rightClickEndY);

            // Проверка границ
            minX = Math.max(0, minX);
            minY = Math.max(0, minY);
            maxX = Math.min(mapWidth - 1, maxX);
            maxY = Math.min(mapHeight - 1, maxY);

            handleRightClickSelection(layer, minX, minY, maxX, maxY);

            // Сброс выделения
            rightClickStartX = -1;
            rightClickStartY = -1;
            rightClickEndX = -1;
            rightClickEndY = -1;
        }

        // ================= ЛЕВАЯ КНОПКА МЫШИ - СТИРАНИЕ =================
        int brushWidth = selectedTiles != null ? selectedTiles.length : 1;
        int brushHeight = selectedTiles != null ? selectedTiles[0].length : 1;
        int startTileX = hoveredTileX - brushWidth / 2;
        int startTileY = hoveredTileY - brushHeight / 2;

        // Создаем массив с null-тайлами для стирания
        Tile[][] eraseTiles = new Tile[brushWidth][brushHeight];

        boolean leftMouseDown = ImGui.isMouseDown(ImGuiMouseButton.Left) && !ImGui.isKeyDown(GLFW.GLFW_KEY_SPACE);
        if (ImGui.isMouseClicked(ImGuiMouseButton.Left) && !ImGui.isKeyDown(GLFW.GLFW_KEY_SPACE)) {
            canvas.isDrawing = true;
            canvas.beginStroke();  // вместо canvas.currentAction = new HistoryAction()
            lastTileX = hoveredTileX;
            lastTileY = hoveredTileY;
            paintBrush(layer, startTileX, startTileY, brushWidth, brushHeight, mapWidth, mapHeight, selectedTiles);
        }

        if (ImGui.isMouseReleased(ImGuiMouseButton.Left) && canvas.isDrawing) {
            canvas.isDrawing = false;
            canvas.endStroke();  // вместо pushHistory + null
            lastTileX = -1;
            lastTileY = -1;
        }
        
        if (leftMouseDown && canvas.isDrawing) {
            // Интерполяция между предыдущей и текущей позицией
            if (lastTileX != -1 && lastTileY != -1) {
                interpolateAndPaint(layer, lastTileX, lastTileY, hoveredTileX, hoveredTileY,
                        brushWidth, brushHeight, mapWidth, mapHeight, eraseTiles);
            }

            lastTileX = hoveredTileX;
            lastTileY = hoveredTileY;
        }

    }

    @Override
    public void handleRightClickSelection(TileLayer layer, int minX, int minY, int maxX, int maxY) {
        // Стираем выделенную область
        canvas.currentAction = new HistoryAction();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                canvas.placeTile(layer, x, y, null);
            }
        }

        canvas.pushHistory(canvas.currentAction);
        canvas.currentAction = null;
    }

    @Override
    public String getExtraInfo(Tile[][] selectedTiles) {
        if (selectedTiles != null) {
            return String.format("| Brush: %dx%d", selectedTiles.length, selectedTiles[0].length);
        }
        return "";
    }
}
