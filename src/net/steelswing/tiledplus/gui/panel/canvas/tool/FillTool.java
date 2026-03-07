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
import net.steelswing.tiledplus.layer.Tile;
import net.steelswing.tiledplus.layer.type.TileLayer;
import org.lwjgl.glfw.GLFW;

// ================= ИНСТРУМЕНТ ЗАЛИВКИ =================

public class FillTool extends Tool {

    public FillTool(GuiPanelCanvas canvas) {
        super(canvas);
    }

    @Override
    public String getName() {
        return "FILL";
    }

    @Override
    public int getHotkey() {
        return GLFW.GLFW_KEY_F;
    }

    @Override
    public String getTooltip() {
        return "Fill Tool (F)";
    }

    @Override
    public int getIcon() {
        return IconManager.TILED.FILL;
    }

    @Override
    public void renderPreview(ImDrawList drawList, int hoveredTileX, int hoveredTileY, int mapWidth, int mapHeight, float imageX, float imageY, float tileWidthZ, float tileHeightZ, Tile[][] selectedTiles) {
        boolean isOutOfBounds = hoveredTileX < 0 || hoveredTileX >= mapWidth || hoveredTileY < 0 || hoveredTileY >= mapHeight;
        float xx = imageX + (hoveredTileX * tileWidthZ);
        float yy = imageY + (hoveredTileY * tileHeightZ);
        if (isOutOfBounds) {
            drawList.addRectFilled(xx, yy, xx + tileWidthZ, yy + tileHeightZ, ImGui.getColorU32(1f, 0f, 0f, 0.3f));
            drawList.addRect(xx, yy, xx + tileWidthZ, yy + tileHeightZ, ImGui.getColorU32(1f, 0f, 0f, 0.8f), 0f, 0, 2f);
        } else {
            drawList.addRectFilled(xx, yy, xx + tileWidthZ, yy + tileHeightZ, ImGui.getColorU32(0.2f, 0.5f, 1f, 0.3f));
            drawList.addRect(xx, yy, xx + tileWidthZ, yy + tileHeightZ, ImGui.getColorU32(0.2f, 0.5f, 1f, 0.8f), 0f, 0, 2f);
        }
    }

    @Override
    public void handleInput(TileLayer layer, int hoveredTileX, int hoveredTileY, int mapWidth, int mapHeight, Tile[][] selectedTiles) {
        if (selectedTiles == null) {
            return;
        }

        if (ImGui.isMouseClicked(ImGuiMouseButton.Left) && !ImGui.isKeyDown(GLFW.GLFW_KEY_SPACE)) {
            if (hoveredTileX >= 0 && hoveredTileX < mapWidth && hoveredTileY >= 0 && hoveredTileY < mapHeight) {
                canvas.beginStroke();
                Tile fillTile = selectedTiles[0][0];
                canvas.fillArea(layer, hoveredTileX, hoveredTileY, fillTile);
                canvas.endStroke();
            }
        }
    }

    @Override
    public String getExtraInfo(Tile[][] selectedTiles) {
        return "";
    }

    @Override
    public void handleRightClickSelection(TileLayer layer, int minX, int minY, int maxX, int maxY) {
    }

}
