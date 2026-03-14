/*
 * Ну вы же понимаете, что код здесь только мой?
 * Well, you do understand that the code here is only mine?
 */
package net.steelswing.tiledplus.gui.modal;

import imgui.ImDrawList;
import imgui.flag.ImGuiMouseButton;
import imgui.flag.ImGuiWindowFlags;
import imgui.internal.ImGui;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.steelswing.flopslop.render.texture.type.Texture2D;
import net.steelswing.tiledplus.TiledPlus;
import net.steelswing.tiledplus.gui.IconManager;
import net.steelswing.tiledplus.gui.ModalWindow;
import net.steelswing.tiledplus.layer.Tile;
import net.steelswing.tiledplus.layer.TilePattern;
import net.steelswing.tiledplus.layer.TileSet;
import org.lwjgl.glfw.GLFW;

/**
 * File: ModalTileSetPatterns.java
 * Created on 2026 Mar 7, 21:50:33
 *
 * @author LWJGL2
 */
public class ModalTileSetPatterns extends ModalWindow {

    private TileSet tileSet;
    private Texture2D texture;

    private int selectedPatternIndex = -1;

    // Редактор паттерна
    private TilePattern editingPattern = null;
    private Set<String> editingSelection = new HashSet<>();
    private float editorZoom = 1.0f;

    /**
     * Режим редактирования:
     * false — обычное выделение тайлов паттерна (синяя подсветка)
     * true — включение/отключение renderLast для тайлов паттерна (красная подсветка)
     */
    private boolean renderLastMode = false;

    // Drag-выделение в редакторе
    private int dragStartX = -1, dragStartY = -1;
    private int dragEndX = -1, dragEndY = -1;
    private boolean isDragSelecting = false;

    private float leftPanelWidth = 180f;
    private float previewPanelWidth = 180f;

    // ─── Редактор коллизий ───────────────────────────────────────────────────
    public final ModalCollisionEditor collisionEditor = new ModalCollisionEditor();

    private List<TilePattern> getPatterns() {
        return tileSet != null ? tileSet.patterns : new ArrayList<>();
    }

    public ModalTileSetPatterns() {
        super("Tileset Patterns");
    }

    private void renderPatternPreviewFixed(TilePattern pattern, float startX, float startY, float previewSize, ImDrawList drawList) {
        if (texture == null || pattern.tileCoords.isEmpty()) {
            return;
        }

        int minX = pattern.tileCoords.stream().mapToInt(c -> c[0]).min().getAsInt();
        int maxX = pattern.tileCoords.stream().mapToInt(c -> c[0]).max().getAsInt();
        int minY = pattern.tileCoords.stream().mapToInt(c -> c[1]).min().getAsInt();
        int maxY = pattern.tileCoords.stream().mapToInt(c -> c[1]).max().getAsInt();

        int pw = maxX - minX + 1;
        int ph = maxY - minY + 1;

        float tileSize = Math.min(previewSize / pw, previewSize / ph);

        float totalW = pw * tileSize;
        float totalH = ph * tileSize;
        float offsetX = startX + (previewSize - totalW) / 2f;
        float offsetY = startY + (previewSize - totalH) / 2f;

        for (int[] c : pattern.tileCoords) {
            int ix = c[0] - minX;
            int iy = c[1] - minY;
            float sx = offsetX + ix * tileSize;
            float sy = offsetY + iy * tileSize;
            Tile tile = tileSet.tiles[tileSet.index(c[0], c[1])];
            if (tile != null && tile.icon != null) {
                drawList.addImage(tile.icon.getTextureId(), sx, sy, sx + tileSize, sy + tileSize, tile.icon.getMinU(), tile.icon.getMinV(), tile.icon.getMaxU(), tile.icon.getMaxV());
            }
            drawList.addRect(sx, sy, sx + tileSize, sy + tileSize, ImGui.getColorU32(0f, 0f, 0f, 0.3f), 0, 0, 0.5f);
        }
    }

    public void open(TileSet tileSet) {
        if (this.tileSet != tileSet) {
            editingPattern = null;
            editingSelection.clear();
            texture = null;
            renderLastMode = false;
        }

        this.tileSet = tileSet;
        this.texture = tileSet.atlasTexture;

        isDragSelecting = false;
        selectedPatternIndex = -1;
        editingSelection.clear();
        super.open();
    }

    /**
     * Переоткрыть окно с уже установленным tileSet (используется при возврате из редактора коллизий).
     */
    @Override
    public void open() {
        if (tileSet != null) {
            super.open();
        }
    }

    @Override
    public void render() {
        int width = (int) (TiledPlus.getWidth() * 0.66);
        int height = (int) (TiledPlus.getHeight() * 0.66);

        if (begin(width, height)) {
            int iconSize = (int) (ImGui.getFontSize() * 1.2f);
            float totalHeight = ImGui.getContentRegionAvailY() - iconSize * 2 - 12;

            float availWidth = ImGui.getContentRegionAvailX();
            float minPanelWidth = 80f;

            // ===== ЛЕВАЯ ПАНЕЛЬ =====
            if (ImGui.beginChild("##patterns_left", leftPanelWidth, totalHeight, true)) {
                ImGui.text("Patterns");
                ImGui.separator();
                for (int i = 0; i < getPatterns().size(); i++) {
                    TilePattern p = getPatterns().get(i);
                    boolean selected = (selectedPatternIndex == i);
                    if (ImGui.selectable(p.name + "##pat" + i, selected)) {
                        selectedPatternIndex = i;
                        editingPattern = p;
                        editingSelection.clear();
                        for (int[] c : p.tileCoords) {
                            editingSelection.add(c[0] + "," + c[1]);
                        }
                        renderLastMode = false;
                    }
                    if (ImGui.isItemHovered() && ImGui.isMouseDoubleClicked(ImGuiMouseButton.Left)) {
                        final TilePattern target = p;
                        TiledPlus.getInstance().editorMain.modalRename.open(p.name, newName -> target.name = newName, () -> {
                            ModalTileSetPatterns.this.open();
                        });
                    }
                    if (ImGui.isItemHovered()) {
                        ImGui.setTooltip("Double-click to rename");
                    }
                }
                ImGui.endChild();
            }

            ImGui.sameLine();

            // ===== СЕПАРАТОР 1 =====
            ImGui.button("##sep1", 4f, totalHeight);
            if (ImGui.isItemActive()) {
                leftPanelWidth += ImGui.getIO().getMouseDeltaX();
                leftPanelWidth = Math.max(minPanelWidth, Math.min(leftPanelWidth, availWidth - previewPanelWidth - minPanelWidth - 20f));
            }
            if (ImGui.isItemHovered() || ImGui.isItemActive()) {
                ImGui.setMouseCursor(imgui.flag.ImGuiMouseCursor.ResizeEW);
            }

            ImGui.sameLine();

            // ===== ЦЕНТР — редактор =====
            float centerWidth = availWidth - leftPanelWidth - previewPanelWidth - 20f;
            if (ImGui.beginChild("##patterns_center", centerWidth, totalHeight, true)) {
                if (editingPattern != null) {
                    if (!renderLastMode) {
                        ImGui.text("Editing: " + editingPattern.name + "  (Shift — add to selection)");
                    } else {
                        ImGui.textColored(1f, 0.2f, 0.2f, 1f, "RenderLast mode: " + editingPattern.name + "  (Shift — add to selection)");
                    }
                    ImGui.separator();
                    renderPatternEditor();
                } else {
                    ImGui.textDisabled("Select or create a pattern to edit");
                }
                ImGui.endChild();
            }

            ImGui.sameLine();

            // ===== СЕПАРАТОР 2 =====
            ImGui.button("##sep2", 4f, totalHeight);
            if (ImGui.isItemActive()) {
                previewPanelWidth -= ImGui.getIO().getMouseDeltaX();
                previewPanelWidth = Math.max(minPanelWidth, Math.min(previewPanelWidth, availWidth - leftPanelWidth - minPanelWidth - 20f));
            }
            if (ImGui.isItemHovered() || ImGui.isItemActive()) {
                ImGui.setMouseCursor(imgui.flag.ImGuiMouseCursor.ResizeEW);
            }

            ImGui.sameLine();

            // ===== ПРАВАЯ ПАНЕЛЬ — превью =====
            if (ImGui.beginChild("##patterns_preview", previewPanelWidth, totalHeight, true)) {
                ImGui.text("Preview");
                ImGui.separator();

                float previewSize = previewPanelWidth - 46f;
                float previewStartX = ImGui.getCursorScreenPosX();
                float previewStartY = ImGui.getCursorScreenPosY();

                ImDrawList dl = ImGui.getWindowDrawList();
                dl.addRectFilled(previewStartX, previewStartY, previewStartX + previewSize, previewStartY + previewSize, ImGui.getColorU32(0.1f, 0.1f, 0.1f, 1f));
                dl.addRect(previewStartX, previewStartY, previewStartX + previewSize, previewStartY + previewSize, ImGui.getColorU32(0.4f, 0.4f, 0.4f, 1f));

                if (selectedPatternIndex >= 0 && selectedPatternIndex < getPatterns().size() && texture != null && !getPatterns().get(selectedPatternIndex).tileCoords.isEmpty()) {
                    TilePattern selPat = getPatterns().get(selectedPatternIndex);
                    renderPatternPreviewFixed(selPat, previewStartX, previewStartY, previewSize, dl);

                    // Превью коллизий поверх тайлов
                    renderCollisionPreview(selPat, previewStartX, previewStartY, previewSize, dl);
                } else {
                    dl.addText(previewStartX + 4f, previewStartY + previewSize / 2f - 7f, ImGui.getColorU32(0.5f, 0.5f, 0.5f, 1f), "(empty)");
                }
                ImGui.dummy(previewSize, previewSize);

                ImGui.endChild();
            }

            ImGui.separator();

            // ===== КНОПКИ ВНИЗУ =====
            if (ImGui.imageButton(IconManager.TILED.PAGE_ADD, iconSize, iconSize)) {
                TilePattern newP = new TilePattern("Pattern " + (getPatterns().size() + 1), tileSet);
                getPatterns().add(newP);
                selectedPatternIndex = getPatterns().size() - 1;
                editingPattern = newP;
                editingSelection.clear();
                renderLastMode = false;
            }
            if (ImGui.isItemHovered()) {
                ImGui.setTooltip("New pattern");
            }

            ImGui.sameLine();

            if (ImGui.imageButton(IconManager.TILED.PAGE_DELETE, iconSize, iconSize)) {
                if (selectedPatternIndex >= 0 && selectedPatternIndex < getPatterns().size()) {
                    getPatterns().remove(selectedPatternIndex);
                    selectedPatternIndex = Math.min(selectedPatternIndex, getPatterns().size() - 1);
                    if (selectedPatternIndex >= 0 && !getPatterns().isEmpty()) {
                        editingPattern = getPatterns().get(selectedPatternIndex);
                        editingSelection.clear();
                        for (int[] c : editingPattern.tileCoords) {
                            editingSelection.add(c[0] + "," + c[1]);
                        }
                    } else {
                        editingPattern = null;
                        editingSelection.clear();
                    }
                    renderLastMode = false;
                }
            }
            if (ImGui.isItemHovered()) {
                ImGui.setTooltip("Delete pattern");
            }

            ImGui.sameLine();

            // ===== КНОПКА renderLast =====
            if (editingPattern != null) {
                boolean renderLastMode1 = renderLastMode;

                if (renderLastMode1) {
                    ImGui.pushStyleColor(imgui.flag.ImGuiCol.Button, 0.6f, 0.1f, 0.1f, 1f);
                    ImGui.pushStyleColor(imgui.flag.ImGuiCol.ButtonHovered, 0.8f, 0.2f, 0.2f, 1f);
                }
                if (ImGui.button("selectRenderLastTiles")) {
                    renderLastMode = !renderLastMode;
                }
                if (renderLastMode1) {
                    ImGui.popStyleColor(2);
                }
                if (ImGui.isItemHovered()) {
                    ImGui.setTooltip(renderLastMode1 ? "Currently editing renderLast tiles (click to switch back to pattern editing)" : "Switch to renderLast tile selection mode");
                }

                ImGui.sameLine();

                // ===== КНОПКА РЕДАКТОРА КОЛЛИЗИЙ =====
                boolean hasCollisions = !editingPattern.collisionRects.isEmpty();
                if (hasCollisions) {
                    ImGui.pushStyleColor(imgui.flag.ImGuiCol.Button, 0.1f, 0.45f, 0.1f, 1f);
                    ImGui.pushStyleColor(imgui.flag.ImGuiCol.ButtonHovered, 0.15f, 0.6f, 0.15f, 1f);
                }
                if (ImGui.button("Edit Collision##colbtn")) {
                    close();
                    collisionEditor.open(editingPattern, () -> ModalTileSetPatterns.this.open());
                }
                if (hasCollisions) {
                    ImGui.popStyleColor(2);
                }
                if (ImGui.isItemHovered()) {
                    ImGui.setTooltip(hasCollisions ? "Edit collision rects (" + editingPattern.collisionRects.size() + " rect(s))" : "Open collision rect editor for this pattern");
                }

                ImGui.sameLine();
            }

            float closeW = 80f;
            ImGui.setCursorPosX(ImGui.getCursorPosX() + ImGui.getContentRegionAvailX() - closeW);
            if (ImGui.button("Close", closeW, 0) || ImGui.isKeyPressed(GLFW.GLFW_KEY_ESCAPE)) {
                close();
            }

            end();
        }
    }

    // ─── Превью коллизий в правой панели ─────────────────────────────────────

    private void renderCollisionPreview(TilePattern pattern, float startX, float startY, float previewSize, ImDrawList dl) {
        if (pattern.collisionRects.isEmpty() || pattern.tileCoords.isEmpty() || tileSet == null) {
            return;
        }

        int minTX = pattern.tileCoords.stream().mapToInt(c -> c[0]).min().getAsInt();
        int maxTX = pattern.tileCoords.stream().mapToInt(c -> c[0]).max().getAsInt();
        int minTY = pattern.tileCoords.stream().mapToInt(c -> c[1]).min().getAsInt();
        int maxTY = pattern.tileCoords.stream().mapToInt(c -> c[1]).max().getAsInt();

        float patPixW = (maxTX - minTX + 1) * tileSet.tileWidth;
        float patPixH = (maxTY - minTY + 1) * tileSet.tileHeight;

        float scaleX = previewSize / patPixW;
        float scaleY = previewSize / patPixH;
        float scale = Math.min(scaleX, scaleY);

        float offX = startX + (previewSize - patPixW * scale) / 2f;
        float offY = startY + (previewSize - patPixH * scale) / 2f;

        for (net.steelswing.tiledplus.layer.CollisionRect r : pattern.collisionRects) {
            float rx1 = offX + r.x * scale;
            float ry1 = offY + r.y * scale;
            float rx2 = rx1 + r.w * scale;
            float ry2 = ry1 + r.h * scale;
            dl.addRectFilled(rx1, ry1, rx2, ry2, ImGui.getColorU32(0.2f, 1f, 0.2f, 0.25f));
            dl.addRect(rx1, ry1, rx2, ry2, ImGui.getColorU32(0.2f, 1f, 0.2f, 0.9f), 0, 0, 1f);
        }
    }

    private void renderPatternEditor() {
        if (texture == null || tileSet == null) {
            return;
        }

        int textureWidth = texture.getWidth();
        int textureHeight = texture.getHeight();
        int tilesCountX = textureWidth / tileSet.tileWidth;
        int tilesCountY = textureHeight / tileSet.tileHeight;

        float viewportWidth = ImGui.getContentRegionAvailX();
        float viewportHeight = ImGui.getContentRegionAvailY();

        if (ImGui.beginChild("##pattern_editor_scroll", viewportWidth, viewportHeight, false, ImGuiWindowFlags.HorizontalScrollbar)) {

            float cursorPosX = ImGui.getCursorScreenPosX();
            float cursorPosY = ImGui.getCursorScreenPosY();
            ImDrawList drawList = ImGui.getWindowDrawList();

            if (ImGui.isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL) && ImGui.isWindowHovered()) {
                float wheel = ImGui.getIO().getMouseWheel();
                if (wheel > 0) {
                    editorZoom = Math.min(editorZoom * 1.2f, 10f);
                } else if (wheel < 0) {
                    editorZoom = Math.max(editorZoom / 1.2f, 0.2f);
                }
            }

            float tileW = tileSet.tileWidth * editorZoom;
            float tileH = tileSet.tileHeight * editorZoom;
            float scaledW = textureWidth * editorZoom;
            float scaledH = textureHeight * editorZoom;

            drawList.addImage(texture.getTextureId(), cursorPosX, cursorPosY, cursorPosX + scaledW, cursorPosY + scaledH, 0, 0, 1, 1);
            ImGui.dummy(scaledW, scaledH);

            boolean shift = ImGui.isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT) || ImGui.isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT);

            if (ImGui.isWindowHovered()) {
                float mouseX = ImGui.getMousePosX() - cursorPosX;
                float mouseY = ImGui.getMousePosY() - cursorPosY;
                int tileX = (int) (mouseX / tileW);
                int tileY = (int) (mouseY / tileH);
                tileX = Math.max(0, Math.min(tilesCountX - 1, tileX));
                tileY = Math.max(0, Math.min(tilesCountY - 1, tileY));

                // Подсветка под курсором
                float hx = cursorPosX + tileX * tileW;
                float hy = cursorPosY + tileY * tileH;
                drawList.addRect(hx, hy, hx + tileW, hy + tileH, ImGui.getColorU32(1f, 1f, 1f, 0.5f), 0, 0, 1.5f);

                // В режиме renderLast — кликать можно только по тайлам которые есть в паттерне
                boolean allowInteraction = !renderLastMode || editingPattern == null || editingSelection.contains(tileX + "," + tileY);

                if (ImGui.isMouseClicked(ImGuiMouseButton.Left) && allowInteraction) {
                    dragStartX = tileX;
                    dragStartY = tileY;
                    isDragSelecting = true;
                }

                if (isDragSelecting && ImGui.isMouseDown(ImGuiMouseButton.Left)) {
                    dragEndX = tileX;
                    dragEndY = tileY;
                }

                if (ImGui.isMouseReleased(ImGuiMouseButton.Left) && isDragSelecting) {
                    isDragSelecting = false;

                    int minX = Math.min(dragStartX, dragEndX);
                    int maxX = Math.max(dragStartX, dragEndX);
                    int minY = Math.min(dragStartY, dragEndY);
                    int maxY = Math.max(dragStartY, dragEndY);

                    boolean isSingleClick = (dragStartX == dragEndX && dragStartY == dragEndY);

                    if (renderLastMode) {
                        applySelectionToRenderLast(minX, maxX, minY, maxY, isSingleClick, shift);
                    } else {
                        if (isSingleClick && !shift) {
                            String key = dragStartX + "," + dragStartY;
                            if (editingSelection.contains(key)) {
                                editingSelection.clear();
                            } else {
                                editingSelection.clear();
                                editingSelection.add(key);
                            }
                        } else if (isSingleClick && shift) {
                            String key = dragStartX + "," + dragStartY;
                            if (editingSelection.contains(key)) {
                                editingSelection.remove(key);
                            } else {
                                editingSelection.add(key);
                            }
                        } else if (shift) {
                            boolean allSelected = true;
                            outer:
                            for (int tx = minX; tx <= maxX; tx++) {
                                for (int ty = minY; ty <= maxY; ty++) {
                                    if (!editingSelection.contains(tx + "," + ty)) {
                                        allSelected = false;
                                        break outer;
                                    }
                                }
                            }
                            for (int tx = minX; tx <= maxX; tx++) {
                                for (int ty = minY; ty <= maxY; ty++) {
                                    String k = tx + "," + ty;
                                    if (allSelected) {
                                        editingSelection.remove(k);
                                    } else {
                                        editingSelection.add(k);
                                    }
                                }
                            }
                        } else {
                            editingSelection.clear();
                            for (int tx = minX; tx <= maxX; tx++) {
                                for (int ty = minY; ty <= maxY; ty++) {
                                    editingSelection.add(tx + "," + ty);
                                }
                            }
                        }
                        syncSelectionToPattern();
                    }

                    dragStartX = dragEndX = dragStartY = dragEndY = -1;
                }
            }

            // Рендер drag-прямоугольника
            if (isDragSelecting && dragStartX >= 0) {
                int minX = Math.min(dragStartX, dragEndX);
                int maxX = Math.max(dragStartX, dragEndX);
                int minY = Math.min(dragStartY, dragEndY);
                int maxY = Math.max(dragStartY, dragEndY);
                float rx1 = cursorPosX + minX * tileW;
                float ry1 = cursorPosY + minY * tileH;
                float rx2 = cursorPosX + (maxX + 1) * tileW;
                float ry2 = cursorPosY + (maxY + 1) * tileH;

                if (renderLastMode) {
                    drawList.addRectFilled(rx1, ry1, rx2, ry2, ImGui.getColorU32(1f, 0.2f, 0.2f, 0.25f));
                    drawList.addRect(rx1, ry1, rx2, ry2, ImGui.getColorU32(1f, 0.2f, 0.2f, 1f), 0, 0, 1.5f);
                } else {
                    drawList.addRectFilled(rx1, ry1, rx2, ry2, ImGui.getColorU32(0.2f, 0.6f, 1f, 0.25f));
                    drawList.addRect(rx1, ry1, rx2, ry2, ImGui.getColorU32(0.2f, 0.6f, 1f, 1f), 0, 0, 1.5f);
                }
            }

            // Рендер выделенных тайлов паттерна (всегда синие)
            for (String key : editingSelection) {
                String[] parts = key.split(",");
                int tx = Integer.parseInt(parts[0]);
                int ty = Integer.parseInt(parts[1]);
                float sx = cursorPosX + tx * tileW;
                float sy = cursorPosY + ty * tileH;
                drawList.addRectFilled(sx, sy, sx + tileW, sy + tileH, ImGui.getColorU32(0.2f, 0.6f, 1f, 0.35f));
                drawList.addRect(sx, sy, sx + tileW, sy + tileH, ImGui.getColorU32(0.2f, 0.6f, 1f, 1f), 0, 0, 1.5f);
            }

            // Рендер renderLast тайлов (красные) — поверх синих
            if (editingPattern != null) {
                for (String key : editingPattern.renderLastCoords) {
                    if (!editingSelection.contains(key)) {
                        continue;
                    }
                    String[] parts = key.split(",");
                    int tx = Integer.parseInt(parts[0]);
                    int ty = Integer.parseInt(parts[1]);
                    float sx = cursorPosX + tx * tileW;
                    float sy = cursorPosY + ty * tileH;
                    drawList.addRectFilled(sx, sy, sx + tileW, sy + tileH, ImGui.getColorU32(1f, 0.2f, 0.2f, 0.35f));
                    drawList.addRect(sx, sy, sx + tileW, sy + tileH, ImGui.getColorU32(1f, 0.2f, 0.2f, 1f), 0, 0, 1.5f);
                }
            }

            // Сетка
            int gridColor = ImGui.getColorU32(1f, 1f, 1f, 0.2f);
            for (int x = 0; x <= tilesCountX; x++) {
                float sx = cursorPosX + x * tileW;
                drawList.addLine(sx, cursorPosY, sx, cursorPosY + scaledH, gridColor, 1f);
            }
            for (int y = 0; y <= tilesCountY; y++) {
                float sy = cursorPosY + y * tileH;
                drawList.addLine(cursorPosX, sy, cursorPosX + scaledW, sy, gridColor, 1f);
            }

            ImGui.endChild();
        }
    }

    /**
     * Применяет drag-выделение к renderLast, но только для тайлов из паттерна.
     */
    private void applySelectionToRenderLast(int minX, int maxX, int minY, int maxY, boolean isSingleClick, boolean shift) {
        if (editingPattern == null) {
            return;
        }
        Set<String> renderLast = editingPattern.renderLastCoords;

        List<String> candidates = new ArrayList<>();
        for (int tx = minX; tx <= maxX; tx++) {
            for (int ty = minY; ty <= maxY; ty++) {
                String key = tx + "," + ty;
                if (editingSelection.contains(key)) {
                    candidates.add(key);
                }
            }
        }

        if (candidates.isEmpty()) {
            return;
        }

        if (isSingleClick && !shift) {
            String key = candidates.get(0);
            if (renderLast.contains(key)) {
                renderLast.remove(key);
            } else {
                renderLast.add(key);
            }
        } else if (isSingleClick && shift) {
            String key = candidates.get(0);
            if (renderLast.contains(key)) {
                renderLast.remove(key);
            } else {
                renderLast.add(key);
            }
        } else if (shift) {
            boolean allSelected = candidates.stream().allMatch(renderLast::contains);
            for (String key : candidates) {
                if (allSelected) {
                    renderLast.remove(key);
                } else {
                    renderLast.add(key);
                }
            }
        } else {
            for (int tx = minX; tx <= maxX; tx++) {
                for (int ty = minY; ty <= maxY; ty++) {
                    renderLast.remove(tx + "," + ty);
                }
            }
            renderLast.addAll(candidates);
        }
    }

    private void syncSelectionToPattern() {
        if (editingPattern == null) {
            return;
        }
        editingPattern.tileCoords.clear();
        for (String key : editingSelection) {
            String[] parts = key.split(",");
            editingPattern.tileCoords.add(new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])});
        }
        editingPattern.renderLastCoords.retainAll(editingSelection);
    }

    /**
     * Получить паттерн как Tile[][] для использования в кисти
     */
    public Tile[][] getPatternTiles(int index) {
        if (index < 0 || index >= getPatterns().size()) {
            return null;
        }
        return getPatterns().get(index).toTileArray();
    }
}
