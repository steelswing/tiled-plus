/*
 * Ну вы же понимаете, что код здесь только мой?
 * Well, you do understand that the code here is only mine?
 */

package net.steelswing.tiledplus.gui.panel.tilesets;

import imgui.ImDrawList;
import imgui.flag.ImGuiDir;
import imgui.flag.ImGuiMouseButton;
import imgui.flag.ImGuiTabBarFlags;
import imgui.flag.ImGuiWindowFlags;
import net.steelswing.tiledplus.gui.DockMenuBase;
import net.steelswing.tiledplus.gui.GuiEditorMain;
import net.steelswing.tiledplus.gui.panel.layers.GuiPanelLayers;
import imgui.internal.ImGui;
import imgui.type.ImInt;
import java.io.FileInputStream;
import java.util.HashMap;
import net.steelswing.engine.api.util.interfaces.Nullable;
import net.steelswing.flopslop.render.texture.TextureFilter;
import net.steelswing.flopslop.render.texture.type.ImageData;
import net.steelswing.flopslop.render.texture.type.Texture2D;
import net.steelswing.tiledplus.gui.IconManager;
import net.steelswing.tiledplus.layer.Tile;
import net.steelswing.tiledplus.layer.TileSet;
import org.lwjgl.glfw.GLFW;

/**
 * File: GuiPanelTileSets.java
 * Created on 2026 Feb 10, 14:07:49
 *
 * @author LWJGL2
 */
public class GuiPanelTileSets extends DockMenuBase {

    protected final GuiEditorMain editor;

    public TileSet currentTileSet;

    public GuiPanelTileSets(GuiEditorMain editor) {
        super("TileSets", ImGuiDir.Right, 500f, 200f);
        this.editor = editor;
    }

    @Override
    public void render() {
        if (ImGui.begin(getTitle())) {

            float footerHeight = ImGui.getFontSize() * 2.2f; // высота панели с иконками
            float width = ImGui.getContentRegionAvailX();
            float height = ImGui.getContentRegionAvailY() - footerHeight;

            if (ImGui.beginChild("tabsRegion", width, height - 2, false)) {
                if (ImGui.beginTabBar("TileSetsList", ImGuiTabBarFlags.FittingPolicyResizeDown)) {
                    for (TileSet tileSet : editor.editorSession.tileSets) {
                        if (ImGui.beginTabItem(tileSet.name)) {
                            currentTileSet = tileSet;
                            if (currentTileSet.userObject == null) {
                                currentTileSet.userObject = new TileSetRenderer(currentTileSet);
                            }

                            if (currentTileSet.userObject instanceof TileSetRenderer renderer) {
                                renderer.render();
                            }

                            ImGui.endTabItem();
                        }
                    }

                    ImGui.endTabBar();
                }
                ImGui.endChild();
            }
            ImGui.separator();

            int iconSize = (int) (ImGui.getFontSize() * 1.2f);

            if (ImGui.imageButton(IconManager.TILED.PAGE_ADD, iconSize, iconSize)) {
                System.out.println("New tileset");
                editor.modalImportTileSets.open();
            }

            ImGui.sameLine();

            if (ImGui.imageButton(IconManager.TILED.PAGE_DELETE, iconSize, iconSize)) {
                System.out.println("Delete tileset");
                if (currentTileSet != null) {
                    final TileSet setToRemove = currentTileSet;

                    editor.modalDelete.desc = "Do you really want to delete tileset '" + currentTileSet.name + "'?";
                    editor.modalDelete.onClick = () -> {
                        editor.editorSession.tileSets.remove(setToRemove);
                        if (setToRemove.userObject instanceof TileSetRenderer renderer) {
                            renderer.destruct();
                        }
                    };
                    editor.modalDelete.open();
                    currentTileSet = null;
                }
            }
            // В render(), после кнопки PAGE_DELETE:
            ImGui.sameLine();
            if (ImGui.imageButton(IconManager.ICONS.EDIT_ICON32, iconSize, iconSize)) {
                if (currentTileSet != null) {
                    editor.modalTileSetPatterns.open(currentTileSet);
                }
            }
            if (ImGui.isItemHovered()) {
                ImGui.setTooltip("Pattern settings");
            }

            ImGui.end();
        }
    }
    // Добавьте эти методы в класс GuiPanelTileSets

    private Tile[][] customPattern = null;

    public void setCustomPattern(Tile[][] pattern) {
        this.customPattern = pattern;

        // Если у нас есть текущий рендерер, сбрасываем его выделение
        if (currentTileSet != null && currentTileSet.userObject instanceof TileSetRenderer renderer) {
            renderer.clearSelection();
        }
    }

    @Nullable
    public Tile[][] getSelectedTiles() {
        // Если установлен пользовательский паттерн, возвращаем его
        if (customPattern != null) {
            return customPattern;
        }

        // Иначе возвращаем выделение из тайлсета
        if (currentTileSet != null && currentTileSet.userObject instanceof TileSetRenderer renderer) {
            return renderer.getSelectedTiles();
        }
        return null;
    }


    @Override
    public int setupDockSpace(int dockspaceId, int prevNodeId, ImInt out, HashMap<Class<?>, DockMenuBase> dockPanels) {
        final int baseNodeId = dockPanels.get(GuiPanelLayers.class).dockNodeId;

        // сделал сплит потому что так удобнее :)
        final int nodeId = ImGui.dockBuilderSplitNode(baseNodeId, ImGuiDir.Down, sizeRatioForNodeAtDir, null, out);

        ImGui.dockBuilderDockWindow(getTitle(), nodeId);
        ImGui.dockBuilderSetNodeSize(nodeId, dockMenuWidth, dockMenuHeight);
        return nodeId;
    }

    protected class TileSetRenderer {

        protected TileSet tileSet;
        protected Texture2D texture;

        private float zoom = 1.0f;
        private float minZoom = 0.1f;
        private float maxZoom = 10.0f;

        private boolean isDragging = false;

        // Выделение тайлов
        private int selectionStartX = -1;
        private int selectionStartY = -1;
        private int selectionEndX = -1;
        private int selectionEndY = -1;
        private boolean isSelecting = false;

        public TileSetRenderer(TileSet tileSet) {
            this.tileSet = tileSet;
            try {
                texture = new Texture2D(ImageData.ofAwt(new FileInputStream(tileSet.file)), TextureFilter.NEAREST_NO_MIP_MAPS);
                texture.loadTexture();
                texture.uploadTextureData();

                tileSet.init(texture);
            } catch (Exception e) {
                e.printStackTrace();
            }
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

            if (ImGui.beginChild("TileSetViewport##child", viewportWidth, viewportHeight - 40, false, ImGuiWindowFlags.HorizontalScrollbar)) {

                float cursorPosX = ImGui.getCursorScreenPosX();
                float cursorPosY = ImGui.getCursorScreenPosY();

                float scrollX = ImGui.getScrollX();
                float scrollY = ImGui.getScrollY();

                ImDrawList drawList = ImGui.getWindowDrawList();

                // ================= ZOOM К КУРСОРУ =================
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

                // ================= DRAG =================
                boolean middleMouseDown = ImGui.isMouseDown(ImGuiMouseButton.Middle);
                boolean spaceDrag = ImGui.isKeyDown(GLFW.GLFW_KEY_SPACE) && ImGui.isMouseDown(ImGuiMouseButton.Left);

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

                // ================= ПОЗИЦИЯ КАРТИНКИ =================
                float imageX = cursorPosX;
                float imageY = cursorPosY;

                drawList.addImage(textureId, imageX, imageY, imageX + scaledWidth, imageY + scaledHeight, 0, 0, 1, 1);
                ImGui.dummy(scaledWidth, scaledHeight);

                // ================= ВЫДЕЛЕНИЕ ТАЙЛОВ =================
                if (ImGui.isWindowHovered() && !isDragging && !ImGui.isKeyDown(GLFW.GLFW_KEY_SPACE) && !ImGui.isMouseDown(ImGuiMouseButton.Middle)) {

                    // Позиция мыши относительно окна viewport
                    float mouseX = ImGui.getMousePosX() - cursorPosX;
                    float mouseY = ImGui.getMousePosY() - cursorPosY;

                    // Позиция мыши в координатах изображения с учетом скролла
                    float imageMouseX = mouseX;
                    float imageMouseY = mouseY;

                    // Проверка, что мышь находится над изображением
                    if (imageMouseX >= 0 && imageMouseX < scaledWidth && imageMouseY >= 0 && imageMouseY < scaledHeight) {

                        // Координаты тайла
                        int tileX = (int) ((imageMouseX / zoom) / tileSet.tileWidth);
                        int tileY = (int) ((imageMouseY / zoom) / tileSet.tileHeight);

                        int tilesCountX = textureWidth / tileSet.tileWidth;
                        int tilesCountY = textureHeight / tileSet.tileHeight;

                        if (tileX >= 0 && tileX < tilesCountX && tileY >= 0 && tileY < tilesCountY) {

                            // Начало выделения
                            if (ImGui.isMouseClicked(ImGuiMouseButton.Left)) {
                                selectionStartX = tileX;
                                selectionStartY = tileY;
                                selectionEndX = tileX;
                                selectionEndY = tileY;
                                isSelecting = true;
                            }

                            // Процесс выделения
                            if (isSelecting && ImGui.isMouseDown(ImGuiMouseButton.Left)) {
                                selectionEndX = tileX;
                                selectionEndY = tileY;
                            }
                        }
                    }

                    // Завершение выделения
                    if (ImGui.isMouseReleased(ImGuiMouseButton.Left)) {
                        isSelecting = false;
                    }

                    if (ImGui.isMouseReleased(ImGuiMouseButton.Left)) {
                        isSelecting = false;
                        // Сбрасываем пользовательский паттерн при новом выделении
                        customPattern = null;
                    }
                }

                // ================= РЕНДЕР ВЫДЕЛЕНИЯ =================
                if (selectionStartX >= 0 && selectionStartY >= 0) {
                    int minX = Math.min(selectionStartX, selectionEndX);
                    int maxX = Math.max(selectionStartX, selectionEndX);
                    int minY = Math.min(selectionStartY, selectionEndY);
                    int maxY = Math.max(selectionStartY, selectionEndY);

                    float rectX1 = imageX + (minX * tileSet.tileWidth * zoom);
                    float rectY1 = imageY + (minY * tileSet.tileHeight * zoom);
                    float rectX2 = imageX + ((maxX + 1) * tileSet.tileWidth * zoom);
                    float rectY2 = imageY + ((maxY + 1) * tileSet.tileHeight * zoom);

                    // Заливка
                    int fillColor = ImGui.getColorU32(0.2f, 0.5f, 1.0f, 0.3f);
                    drawList.addRectFilled(rectX1, rectY1, rectX2, rectY2, fillColor);

                    // Рамка
                    int borderColor = ImGui.getColorU32(0.2f, 0.5f, 1.0f, 1.0f);
                    drawList.addRect(rectX1, rectY1, rectX2, rectY2, borderColor, 0, 0, 2f);
                }

                // ================= СЕТКА =================
                int gridColor = ImGui.getColorU32(1f, 1f, 1f, 0.25f);
                for (float x = 0; x <= textureWidth; x += tileSet.tileWidth) {
                    float screenX = imageX + (x * zoom);
                    drawList.addLine(screenX, imageY, screenX, imageY + scaledHeight, gridColor, 1f);
                }

                for (float y = 0; y <= textureHeight; y += tileSet.tileHeight) {
                    float screenY = imageY + (y * zoom);
                    drawList.addLine(imageX, screenY, imageX + scaledWidth, screenY, gridColor, 1f);
                }

                ImGui.endChild();
            }

            ImGui.text(String.format("Zoom: %.0f%%", zoom * 100f));
            if (selectionStartX >= 0) {
                ImGui.sameLine();
                int selWidth = Math.abs(selectionEndX - selectionStartX) + 1;
                int selHeight = Math.abs(selectionEndY - selectionStartY) + 1;
                ImGui.text(String.format("Selection: %dx%d tiles", selWidth, selHeight));
            }
        }



        public Tile[][] getSelectedTiles() {
            // Проверка на валидность выделения
            if (selectionStartX < 0 || selectionStartY < 0) {
                return null;
            }

            int minX = Math.min(selectionStartX, selectionEndX);
            int maxX = Math.max(selectionStartX, selectionEndX);
            int minY = Math.min(selectionStartY, selectionEndY);
            int maxY = Math.max(selectionStartY, selectionEndY);

            // Правильный расчет размеров (включительно)
            int w = maxX - minX + 1;
            int h = maxY - minY + 1;

            Tile[][] tiles = new Tile[w][h];

            for (int i = minX; i <= maxX; i++) {
                for (int j = minY; j <= maxY; j++) {
                    int xx = i - minX;
                    int yy = j - minY;  // Было: j - minX (ошибка!)

                    tiles[xx][yy] = tileSet.tiles[tileSet.index(i, j)];
                }
            }

            return tiles;
        }

        public void clearSelection() {
            selectionStartX = -1;
            selectionStartY = -1;
            selectionEndX = -1;
            selectionEndY = -1;
            isSelecting = false;
        }

        public void zoomIn() {
            zoom = Math.min(zoom * 1.2f, maxZoom);
        }

        public void zoomOut() {
            zoom = Math.max(zoom / 1.2f, minZoom);
        }

        public void resetZoom() {
            zoom = 1.0f;
        }

        public void fitToView(float viewWidth, float viewHeight) {
            if (texture == null) {
                return;
            }
            float scaleX = viewWidth / texture.getWidth();
            float scaleY = viewHeight / texture.getHeight();
            zoom = Math.max(minZoom, Math.min(maxZoom, Math.min(scaleX, scaleY) * 0.9f));
        }

        public void destruct() {
            if (texture != null) {
                texture.destruct();
                texture = null;
            }
        }
    }

}
