/*
 * Ну вы же понимаете, что код здесь только мой?
 * Well, you do understand that the code here is only mine?
 */

package net.steelswing.tiledplus.gui.panel.autotile;

import imgui.ImDrawList;
import imgui.ImGui;
import imgui.flag.ImGuiMouseButton;
import imgui.flag.ImGuiWindowFlags;
import net.steelswing.flopslop.render.texture.type.Texture2D;
import net.steelswing.tiledplus.layer.TileSet;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Wang Tile Editor Renderer - визуальный редактор Wang tiles с overlay
 */
public class WangTileEditorRenderer {

    protected TileSet tileSet;
    protected Texture2D texture;
    protected GuiPanelAutotile panel;

    private float zoom = 2.0f;
    private float minZoom = 0.5f;
    private float maxZoom = 120.0f;

    private boolean isDragging = false;

    // Хранилище WangId для каждого тайла
    private Map<Integer, WangId> tileWangIds;

    // Редактирование конкретного тайла
    private int editingTileX = -1;
    private int editingTileY = -1;

    // Режим рисования (для линий)
    private boolean isDrawing = false;
    private int lastEditedIndex = -1;

    public WangTileEditorRenderer(TileSet tileSet, GuiPanelAutotile panel) {
        this.tileSet = tileSet;
        this.texture = tileSet.atlasTexture;
        this.panel = panel;
        this.tileWangIds = new HashMap<>();
    }

    public Map<Integer, WangId> getTileWangIds() {
        return tileWangIds;
    }

    protected void render() {
        if (texture == null) {
            return;
        }

        int textureWidth = texture.getWidth();
        int textureHeight = texture.getHeight();
        int textureId = texture.getTextureId();

        float scaledWidth = textureWidth * zoom;
        float scaledHeight = textureHeight * zoom;

        float viewportWidth = ImGui.getContentRegionAvailX();
        float viewportHeight = ImGui.getContentRegionAvailY();

//        tileWangIds.clear();
        for (Iterator<Map.Entry<Integer, WangId>> iterator = tileWangIds.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<Integer, WangId> next = iterator.next();
            if (next.getValue().isEmpty()) {
                iterator.remove();
            } 
        }

        if (ImGui.beginChild("WangTileEditorRenderer##child", viewportWidth, viewportHeight - 40, false,
                ImGuiWindowFlags.HorizontalScrollbar)) {

            float cursorPosX = ImGui.getCursorScreenPosX();
            float cursorPosY = ImGui.getCursorScreenPosY();

            float scrollX = ImGui.getScrollX();
            float scrollY = ImGui.getScrollY();

            ImDrawList drawList = ImGui.getWindowDrawList();

            // ================= ZOOM К КУРСОРУ =================
            handleZoom(cursorPosX, cursorPosY, scrollX, scrollY);

            // ================= DRAG =================
            handleDrag(scrollX, scrollY);

            // ================= ПОЗИЦИЯ КАРТИНКИ =================
            float imageX = cursorPosX;
            float imageY = cursorPosY;

            // Фон
            drawList.addRectFilled(imageX, imageY, imageX + scaledWidth, imageY + scaledHeight,
                    ImGui.getColorU32(0.1f, 0.1f, 0.1f, 1f));

            // Тайлсет
            drawList.addImage(textureId, imageX, imageY, imageX + scaledWidth, imageY + scaledHeight, 0, 0, 1, 1);
            ImGui.dummy(scaledWidth, scaledHeight);

            // ================= СЕТКА =================
            renderGrid(drawList, imageX, imageY, textureWidth, textureHeight, scaledWidth, scaledHeight);

            // ================= WANG OVERLAY =================
            if (panel.isShowOverlay()) {
                renderWangOverlays(drawList, imageX, imageY, textureWidth, textureHeight);
            }

            // ================= HOVER ЭФФЕКТ =================
            renderHoverEffect(drawList, imageX, imageY, textureWidth, textureHeight);

            // ================= ОБРАБОТКА КЛИКОВ =================
            handleMouseInput(imageX, imageY, textureWidth, textureHeight, scaledWidth, scaledHeight);

            ImGui.endChild();
        }

        // Информация
        renderStatusBar();
    }

    private void handleZoom(float cursorPosX, float cursorPosY, float scrollX, float scrollY) {
        if (ImGui.isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL) && ImGui.isWindowHovered()) {

            float wheel = ImGui.getIO().getMouseWheel();
            if (wheel != 0) {
                float mouseX = ImGui.getMousePosX() - cursorPosX;
                float mouseY = ImGui.getMousePosY() - cursorPosY;

                float oldZoom = zoom;

                if (wheel > 0) {
                    zoom *= 1.2f;
                } else {
                    zoom /= 1.2f;
                }

                zoom = Math.max(minZoom, Math.min(maxZoom, zoom));

                float imagePointX = (scrollX + mouseX) / oldZoom;
                float imagePointY = (scrollY + mouseY) / oldZoom;

                float newScrollX = imagePointX * zoom - mouseX;
                float newScrollY = imagePointY * zoom - mouseY;

                ImGui.setScrollX(newScrollX);
                ImGui.setScrollY(newScrollY);
            }
        }
    }

    private void handleDrag(float scrollX, float scrollY) {
        boolean middleMouseDown = ImGui.isMouseDown(ImGuiMouseButton.Middle);
        boolean spaceDrag = ImGui.isKeyDown(GLFW.GLFW_KEY_SPACE) &&
                ImGui.isMouseDown(ImGuiMouseButton.Left);

        if (ImGui.isWindowHovered() && (middleMouseDown || spaceDrag)) {
            if (!isDragging) {
                isDragging = true;
            }
        } else {
            isDragging = false;
        }

        if (isDragging) {
            float dx = ImGui.getIO().getMouseDeltaX();
            float dy = ImGui.getIO().getMouseDeltaY();
            ImGui.setScrollX(scrollX - dx);
            ImGui.setScrollY(scrollY - dy);
        }
    }

    private void renderGrid(ImDrawList drawList, float imageX, float imageY,
            int textureWidth, int textureHeight, float scaledWidth, float scaledHeight) {

        int gridColor = ImGui.getColorU32(1f, 1f, 1f, 0.3f);

        for (float x = 0; x <= textureWidth; x += tileSet.tileWidth) {
            float screenX = imageX + (x * zoom);
            drawList.addLine(screenX, imageY, screenX, imageY + scaledHeight, gridColor, 1f);
        }

        for (float y = 0; y <= textureHeight; y += tileSet.tileHeight) {
            float screenY = imageY + (y * zoom);
            drawList.addLine(imageX, screenY, imageX + scaledWidth, screenY, gridColor, 1f);
        }
    }

    private void renderWangOverlays(ImDrawList drawList, float imageX, float imageY,
            int textureWidth, int textureHeight) {

        int tilesCountX = textureWidth / tileSet.tileWidth;
        int tilesCountY = textureHeight / tileSet.tileHeight;

        // Подготавливаем провайдер цветов
        WangOverlay.ColorProvider colorProvider = colorIndex -> {
            GuiPanelAutotile.WangColor color = panel.getColorByIndex(colorIndex);
            if (color == null) {
                return null;
            }
            return new int[]{
                (int) (color.r * 255),
                (int) (color.g * 255),
                (int) (color.b * 255),
                255
            };
        };

        // Опции рендеринга
        int options = 0;
        if (panel.isShowShadow()) {
            options |= WangOverlay.WO_SHADOW;
        }
        if (panel.isShowOutline()) {
            options |= WangOverlay.WO_OUTLINE;
        }
        if (panel.isTransparentFill()) {
            options |= WangOverlay.WO_TRANSPARENT_FILL;
        }

        // Рисуем overlay для каждого тайла
        for (int tileY = 0; tileY < tilesCountY; tileY++) {
            for (int tileX = 0; tileX < tilesCountX; tileX++) {
                int tileIndex = tileX + tileY * tilesCountX;
                WangId wangId = tileWangIds.get(tileIndex);

                if (wangId == null || wangId.isEmpty()) {
                    continue;
                }

                float tilePosX = imageX + (tileX * tileSet.tileWidth * zoom);
                float tilePosY = imageY + (tileY * tileSet.tileHeight * zoom);
                float tileWidth = tileSet.tileWidth * zoom;
                float tileHeight = tileSet.tileHeight * zoom;

                WangOverlay.paintWangOverlay(drawList, wangId,
                        tilePosX, tilePosY, tileWidth, tileHeight,
                        colorProvider, options);
            }
        }
    }

    private void renderHoverEffect(ImDrawList drawList, float imageX, float imageY,
            int textureWidth, int textureHeight) {

        if (!ImGui.isWindowHovered() || isDragging) {
            return;
        }

        float mouseX = ImGui.getMousePosX() - imageX;
        float mouseY = ImGui.getMousePosY() - imageY;

        int scaledWidth = (int) (textureWidth * zoom);
        int scaledHeight = (int) (textureHeight * zoom);

        if (mouseX < 0 || mouseX >= scaledWidth || mouseY < 0 || mouseY >= scaledHeight) {
            return;
        }

        int tileX = (int) ((mouseX / zoom) / tileSet.tileWidth);
        int tileY = (int) ((mouseY / zoom) / tileSet.tileHeight);

        int tilesCountX = textureWidth / tileSet.tileWidth;
        int tilesCountY = textureHeight / tileSet.tileHeight;

        if (tileX < 0 || tileX >= tilesCountX || tileY < 0 || tileY >= tilesCountY) {
            return;
        }

        // Подсветка тайла
        float tilePosX = imageX + (tileX * tileSet.tileWidth * zoom);
        float tilePosY = imageY + (tileY * tileSet.tileHeight * zoom);
        float tileWidth = tileSet.tileWidth * zoom;
        float tileHeight = tileSet.tileHeight * zoom;

        drawList.addRect(tilePosX, tilePosY, tilePosX + tileWidth, tilePosY + tileHeight,
                ImGui.getColorU32(1f, 1f, 1f, 0.5f), 0f, 0, 2f);

        // Если режим рисования - показываем preview точки
        if (panel.getEditMode() == GuiPanelAutotile.EditMode.PAINT) {
            renderPointPreview(drawList, tileX, tileY, tilePosX, tilePosY, tileWidth, tileHeight, mouseX, mouseY);
        }
    }

    private void renderPointPreview(ImDrawList drawList, int tileX, int tileY,
            float tilePosX, float tilePosY, float tileWidth, float tileHeight,
            float mouseX, float mouseY) {

        float localMouseX = mouseX - (tileX * tileSet.tileWidth * zoom);
        float localMouseY = mouseY - (tileY * tileSet.tileHeight * zoom);

        int pointIndex = getClosestWangPoint(localMouseX, localMouseY, tileWidth, tileHeight);

        if (pointIndex >= 0) {
            float[][] positions = getWangPointPositions(tilePosX, tilePosY, tileWidth, tileHeight);
            float pointRadius = Math.max(5f, 6f * zoom);

            GuiPanelAutotile.WangColor color = panel.getColorByIndex(panel.getSelectedColorIndex());
            int previewColor;

            if (panel.getEditMode() == GuiPanelAutotile.EditMode.ERASE || color == null) {
                previewColor = ImGui.getColorU32(0.3f, 0.3f, 0.3f, 0.7f);
            } else {
                previewColor = color.getColorU32(0.7f);
            }

            drawList.addCircleFilled(positions[pointIndex][0], positions[pointIndex][1],
                    pointRadius, previewColor);

            // Обводка
            int borderColor = ImGui.getColorU32(1f, 1f, 1f, 0.9f);
            drawList.addCircle(positions[pointIndex][0], positions[pointIndex][1],
                    pointRadius, borderColor, 0, 2f);
        }
    }

    private void handleMouseInput(float imageX, float imageY, int textureWidth, int textureHeight,
            float scaledWidth, float scaledHeight) {

        if (!ImGui.isWindowHovered() || isDragging) {
            if (isDrawing) {
                isDrawing = false;
                lastEditedIndex = -1;
            }
            return;
        }

        if (ImGui.isKeyDown(GLFW.GLFW_KEY_SPACE) ||
                ImGui.isMouseDown(ImGuiMouseButton.Middle)) {
            if (isDrawing) {
                isDrawing = false;
                lastEditedIndex = -1;
            }
            return;
        }

        float mouseX = ImGui.getMousePosX() - imageX;
        float mouseY = ImGui.getMousePosY() - imageY;

        if (mouseX < 0 || mouseX >= scaledWidth || mouseY < 0 || mouseY >= scaledHeight) {
            if (isDrawing) {
                isDrawing = false;
                lastEditedIndex = -1;
            }
            return;
        }

        int tileX = (int) ((mouseX / zoom) / tileSet.tileWidth);
        int tileY = (int) ((mouseY / zoom) / tileSet.tileHeight);

        int tilesCountX = textureWidth / tileSet.tileWidth;
        int tilesCountY = textureHeight / tileSet.tileHeight;

        if (tileX < 0 || tileX >= tilesCountX || tileY < 0 || tileY >= tilesCountY) {
            if (isDrawing) {
                isDrawing = false;
                lastEditedIndex = -1;
            }
            return;
        }

        int tileIndex = tileX + tileY * tilesCountX;

        float tilePosX = tileX * tileSet.tileWidth * zoom;
        float tilePosY = tileY * tileSet.tileHeight * zoom;
        float tileWidth = tileSet.tileWidth * zoom;
        float tileHeight = tileSet.tileHeight * zoom;

        float localMouseX = mouseX - tilePosX;
        float localMouseY = mouseY - tilePosY;

        int pointIndex = getClosestWangPoint(localMouseX, localMouseY, tileWidth, tileHeight);

        // Левая кнопка мыши - рисование
        if (ImGui.isMouseDown(ImGuiMouseButton.Left)) {
            if (!isDrawing) {
                isDrawing = true;
            }

            if (pointIndex >= 0 && pointIndex != lastEditedIndex) {
                editingTileX = tileX;
                editingTileY = tileY;

                WangId wangId = tileWangIds.get(tileIndex);
                if (wangId == null) {
                    wangId = new WangId();
                    tileWangIds.put(tileIndex, wangId);
                }

                if (panel.getEditMode() == GuiPanelAutotile.EditMode.ERASE) {
                    wangId.setIndexColor(pointIndex, 0);
                } else {
                    wangId.setIndexColor(pointIndex, panel.getSelectedColorIndex());
                }

                lastEditedIndex = pointIndex;
            }
        } else {
            if (isDrawing) {
                isDrawing = false;
                lastEditedIndex = -1;
            }
        }

        // Правая кнопка - eyedropper
        if (ImGui.isMouseClicked(ImGuiMouseButton.Right) && pointIndex >= 0) {
            WangId wangId = tileWangIds.get(tileIndex);
            if (wangId != null) {
                int color = wangId.indexColor(pointIndex);
                if (color > 0) {
                    panel.setSelectedColorIndex(color);
                }
            }
        }

        // Shift + клик - удалить весь Wang ID тайла
        if (ImGui.isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT) &&
                ImGui.isMouseClicked(ImGuiMouseButton.Left)) {
            tileWangIds.remove(tileIndex);
        }
    }

    private int getClosestWangPoint(float localX, float localY, float tileWidth, float tileHeight) {
        float midX = tileWidth / 2f;
        float midY = tileHeight / 2f;

        float[][] pointPositions = {
            {midX, 0}, // 0 - Top
            {tileWidth, 0}, // 1 - TopRight
            {tileWidth, midY}, // 2 - Right
            {tileWidth, tileHeight}, // 3 - BottomRight
            {midX, tileHeight}, // 4 - Bottom
            {0, tileHeight}, // 5 - BottomLeft
            {0, midY}, // 6 - Left
            {0, 0} // 7 - TopLeft
        };

        float threshold = Math.max(12f, 15f * zoom);
        float minDist = Float.MAX_VALUE;
        int closestIndex = -1;

        for (int i = 0; i < pointPositions.length; i++) {
            float dx = localX - pointPositions[i][0];
            float dy = localY - pointPositions[i][1];
            float dist = (float) Math.sqrt(dx * dx + dy * dy);

            if (dist < minDist && dist < threshold) {
                minDist = dist;
                closestIndex = i;
            }
        }

        return closestIndex;
    }

    private float[][] getWangPointPositions(float tilePosX, float tilePosY, float tileWidth, float tileHeight) {
        float[][] points = new float[8][2];

        float midX = tilePosX + tileWidth / 2f;
        float midY = tilePosY + tileHeight / 2f;

        points[WangId.TOP] = new float[]{midX, tilePosY};
        points[WangId.TOP_RIGHT] = new float[]{tilePosX + tileWidth, tilePosY};
        points[WangId.RIGHT] = new float[]{tilePosX + tileWidth, midY};
        points[WangId.BOTTOM_RIGHT] = new float[]{tilePosX + tileWidth, tilePosY + tileHeight};
        points[WangId.BOTTOM] = new float[]{midX, tilePosY + tileHeight};
        points[WangId.BOTTOM_LEFT] = new float[]{tilePosX, tilePosY + tileHeight};
        points[WangId.LEFT] = new float[]{tilePosX, midY};
        points[WangId.TOP_LEFT] = new float[]{tilePosX, tilePosY};

        return points;
    }

    private void renderStatusBar() {
        ImGui.text(String.format("Zoom: %.0f%%", zoom * 100f));

        if (editingTileX >= 0) {
            ImGui.sameLine();
            ImGui.text(String.format("| Tile: (%d, %d)", editingTileX, editingTileY));
        }

        ImGui.sameLine();
        ImGui.text(String.format("| Total Wang Tiles: %d", tileWangIds.size()));

        ImGui.sameLine();
        ImGui.textDisabled("| Shift+Click: Clear tile | RMB: Pick color");
    }

    public void destruct() {
        if (texture != null) {
            texture.destruct();
            texture = null;
        }
    }
}
