/*
 * Ну вы же понимаете, что код здесь только мой?
 * Well, you do understand that the code here is only mine?
 */
package net.steelswing.tiledplus.gui.panel.canvas.tool;

import imgui.ImDrawList;
import imgui.flag.ImGuiCol;
import imgui.internal.ImGui;
import net.steelswing.tiledplus.gui.panel.canvas.GuiPanelCanvas;
import net.steelswing.tiledplus.layer.Tile;
import net.steelswing.tiledplus.layer.type.TileLayer;

// ================= БАЗОВЫЙ КЛАСС ИНСТРУМЕНТА =================
public abstract class Tool {

    protected final GuiPanelCanvas canvas;

    // Выделение правой кнопкой мыши
    protected int rightClickStartX = -1;
    protected int rightClickStartY = -1;
    protected int rightClickEndX = -1;
    protected int rightClickEndY = -1;
    protected boolean isRightClickSelecting = false;

    public Tool(GuiPanelCanvas canvas) {
        this.canvas = canvas;
    }

    public abstract String getName();

    public abstract int getHotkey();

    public abstract String getTooltip();

    public abstract int getIcon();

    public abstract void renderPreview(ImDrawList drawList, int hoveredTileX, int hoveredTileY, int mapWidth, int mapHeight, float imageX, float imageY, float tileWidthZ, float tileHeightZ, Tile[][] selectedTiles);

    public abstract void handleInput(TileLayer layer, int hoveredTileX, int hoveredTileY, int mapWidth, int mapHeight, Tile[][] selectedTiles);

    /**
     * Обработка выделения правой кнопкой мыши (переопределяется в наследниках)
     */
    public abstract void handleRightClickSelection(TileLayer layer, int minX, int minY, int maxX, int maxY);

    public abstract String getExtraInfo(Tile[][] selectedTiles);

    public boolean isSelected() {
        return canvas.currentTool == this;
    }

    public void renderButton() {
        final boolean selected = isSelected();
        if (selected) {
            ImGui.pushStyleColor(ImGuiCol.Button, ImGui.getColorU32(ImGuiCol.Header));
        }
        if (ImGui.imageButton(getIcon(), ImGui.getFontSize(), ImGui.getFontSize())) {
            canvas.currentTool = this;
        }
        if (selected) {
            ImGui.popStyleColor();
        }
        if (ImGui.isItemHovered()) {
            ImGui.setTooltip(getTooltip());
        }
    }

    /**
     * Рисует кисть в заданной позиции
     */
    public void paintBrush(TileLayer layer, int startTileX, int startTileY, int brushWidth, int brushHeight, int mapWidth, int mapHeight, Tile[][] selectedTiles) {
        for (int i = 0; i < brushWidth; i++) {
            for (int j = 0; j < brushHeight; j++) {
                int targetX = startTileX + i;
                int targetY = startTileY + j;
                if (targetX >= 0 && targetX < mapWidth && targetY >= 0 && targetY < mapHeight) {
                    canvas.placeTileStroked(layer, targetX, targetY, selectedTiles[i][j]);
                }
            }
        }
    }

    /**
     * Интерполяция между двумя точками с использованием алгоритма Брезенхема
     */
    public void interpolateAndPaint(TileLayer layer, int x0, int y0, int x1, int y1, int brushWidth, int brushHeight, int mapWidth, int mapHeight, Tile[][] selectedTiles) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        int x = x0;
        int y = y0;
        while (true) {
            // Рисуем кисть в текущей позиции
            int startTileX = x - brushWidth / 2;
            int startTileY = y - brushHeight / 2;
            paintBrush(layer, startTileX, startTileY, brushWidth, brushHeight, mapWidth, mapHeight, selectedTiles);
            if (x == x1 && y == y1) {
                break;
            }
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
            }
        }
    }

    /**
     * Рендер выделения правой кнопкой мыши
     */
    protected void renderRightClickSelection(ImDrawList drawList, int mapWidth, int mapHeight, float imageX, float imageY, float tileWidthZ, float tileHeightZ) {
        if (rightClickStartX >= 0 && rightClickStartY >= 0) {
            int minX = Math.min(rightClickStartX, rightClickEndX);
            int maxX = Math.max(rightClickStartX, rightClickEndX);
            int minY = Math.min(rightClickStartY, rightClickEndY);
            int maxY = Math.max(rightClickStartY, rightClickEndY);

            float rectX1 = imageX + (minX * tileWidthZ);
            float rectY1 = imageY + (minY * tileHeightZ);
            float rectX2 = imageX + ((maxX + 1) * tileWidthZ);
            float rectY2 = imageY + ((maxY + 1) * tileHeightZ);

            // Заливка
            int fillColor = ImGui.getColorU32(1.0f, 1.0f, 0.0f, 0.3f);
            drawList.addRectFilled(rectX1, rectY1, rectX2, rectY2, fillColor);

            // Рамка
            int borderColor = ImGui.getColorU32(1.0f, 1.0f, 0.0f, 1.0f);
            drawList.addRect(rectX1, rectY1, rectX2, rectY2, borderColor, 0, 0, 2f);
        }
    }
}