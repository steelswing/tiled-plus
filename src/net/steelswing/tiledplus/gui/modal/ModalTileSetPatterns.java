/*
 * Ну вы же понимаете, что код здесь только мой?
 * Well, you do understand that the code here is only mine?
 */
package net.steelswing.tiledplus.gui.modal;

import net.steelswing.tiledplus.layer.TilePattern;
import imgui.ImDrawList;
import imgui.flag.ImGuiMouseButton;
import imgui.flag.ImGuiWindowFlags;
import imgui.internal.ImGui;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import net.steelswing.flopslop.render.texture.type.Texture2D;
import net.steelswing.tiledplus.TiledPlus;
import net.steelswing.tiledplus.gui.IconManager;
import net.steelswing.tiledplus.gui.ModalWindow;
import net.steelswing.tiledplus.layer.Tile;
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


    // Drag-выделение в редакторе
    private int dragStartX = -1, dragStartY = -1;
    private int dragEndX = -1, dragEndY = -1;
    private boolean isDragSelecting = false;

    private float leftPanelWidth = 180f;
    private float previewPanelWidth = 180f;
 
    // Геттер вместо него:
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

        // Подгоняем размер тайла чтобы паттерн вписался в квадрат
        float tileSize = Math.min(previewSize / pw, previewSize / ph);

        // Центрируем паттерн в квадрате превью
        float totalW = pw * tileSize;
        float totalH = ph * tileSize;
        float offsetX = startX + (previewSize - totalW) / 2f;
        float offsetY = startY + (previewSize - totalH) / 2f;

        Set<String> coordSet = new HashSet<>();
        for (int[] c : pattern.tileCoords) {
            coordSet.add(c[0] + "," + c[1]);
        }

        for (int[] c : pattern.tileCoords) {
            int ix = c[0] - minX;
            int iy = c[1] - minY;
            float sx = offsetX + ix * tileSize;
            float sy = offsetY + iy * tileSize;
            Tile tile = tileSet.tiles[tileSet.index(c[0], c[1])];
            if (tile != null && tile.icon != null) {
                drawList.addImage(tile.icon.getTextureId(), sx, sy, sx + tileSize, sy + tileSize, tile.icon.getMinU(), tile.icon.getMinV(), tile.icon.getMaxU(), tile.icon.getMaxV());
            }
            // Тонкая сетка на превью
            drawList.addRect(sx, sy, sx + tileSize, sy + tileSize, ImGui.getColorU32(0f, 0f, 0f, 0.3f), 0, 0, 0.5f);
        }
    }

    public void open(TileSet tileSet) {
        this.tileSet = tileSet;
        this.texture = tileSet.atlasTexture;

//        editingPattern = null;
        editingSelection.clear();
        super.open();
    }

    @Override
    public void render() {

        int width = (int) (TiledPlus.getWidth() * 0.66);
        int height = (int) (TiledPlus.getHeight() * 0.66);

        if (begin(width, height)) {
            int iconSize = (int) (ImGui.getFontSize() * 1.2f);
            float totalHeight = ImGui.getContentRegionAvailY() - iconSize * 2 - 12;

            float leftWidth = 180f;
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
                    }
                    // Двойной клик — переименование
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
                    ImGui.text("Editing: " + editingPattern.name + "  (Shift — add to selection)");
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
                    renderPatternPreviewFixed(getPatterns().get(selectedPatternIndex), previewStartX, previewStartY, previewSize, dl);
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
                }
            }
            if (ImGui.isItemHovered()) {
                ImGui.setTooltip("Delete pattern");
            }

            ImGui.sameLine();
            if (ImGui.imageButton(IconManager.FORMATS.PAGE_WHITE_16ICON, iconSize, iconSize)) {
                System.out.println(toJSON());

                try {
                    SwingUtilities.invokeAndWait(() -> {
                        JFrame frame = new JFrame();
                        frame.setAlwaysOnTop(true);
                        frame.setUndecorated(true);
                        frame.setLocationRelativeTo(null);
                        frame.setVisible(true);
                        frame.toFront();
                        frame.requestFocus();

                        JFileChooser chooser = new JFileChooser();
                        chooser.setCurrentDirectory(new File("."));
//                        chooser.setAcceptAllFileFilterUsed(false);

                        chooser.setDialogTitle("Save Patterns");
                        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JSON files (*.json)", "json"));
                        chooser.setSelectedFile(new java.io.File("patterns.json"));
                        try {
                            int result = chooser.showSaveDialog(null);
                            if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
                                java.io.File file = chooser.getSelectedFile();
                                // Добавляем расширение если не указано
                                if (!file.getName().toLowerCase().endsWith(".json")) {
                                    file = new java.io.File(file.getAbsolutePath() + ".json");
                                }
                                final java.io.File finalFile = file;
                                try {
                                    String json = toJSON().toString(2); // pretty print
                                    java.nio.file.Files.writeString(finalFile.toPath(), json);
                                    System.out.println("Saved to: " + finalFile.getAbsolutePath());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        frame.dispose();
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (ImGui.isItemHovered()) {
                ImGui.setTooltip("Save pattern");
            }

            ImGui.sameLine();
            float closeW = 80f;
            ImGui.setCursorPosX(ImGui.getCursorPosX() + ImGui.getContentRegionAvailX() - closeW);
            if (ImGui.button("Close", closeW, 0) || ImGui.isKeyPressed(GLFW.GLFW_KEY_ESCAPE)) {
                close();
            }

            end();
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

                if (ImGui.isMouseClicked(ImGuiMouseButton.Left)) {
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

                    if (isSingleClick && !shift) {
                        // Одиночный клик без шифта — если уже выбран, убираем, иначе заменяем выделение
                        String key = dragStartX + "," + dragStartY;
                        if (editingSelection.contains(key)) {
                            editingSelection.clear();
                        } else {
                            editingSelection.clear();
                            editingSelection.add(key);
                        }
                    } else if (isSingleClick && shift) {
                        // Одиночный клик с шифтом — тоглим конкретный тайл
                        String key = dragStartX + "," + dragStartY;
                        if (editingSelection.contains(key)) {
                            editingSelection.remove(key);
                        } else {
                            editingSelection.add(key);
                        }
                    } else if (shift) {
                        // Drag с шифтом — тоглим прямоугольник
                        // Проверяем: все ли тайлы в прямоугольнике уже выбраны?
                        boolean allSelected = true;
                        for (int tx = minX; tx <= maxX; tx++) {
                            for (int ty = minY; ty <= maxY; ty++) {
                                if (!editingSelection.contains(tx + "," + ty)) {
                                    allSelected = false;
                                    break;
                                }
                            }
                            if (!allSelected) {
                                break;
                            }
                        }

                        if (allSelected) {
                            // Все выбраны — убираем
                            for (int tx = minX; tx <= maxX; tx++) {
                                for (int ty = minY; ty <= maxY; ty++) {
                                    editingSelection.remove(tx + "," + ty);
                                }
                            }
                        } else {
                            // Не все выбраны — добавляем все
                            for (int tx = minX; tx <= maxX; tx++) {
                                for (int ty = minY; ty <= maxY; ty++) {
                                    editingSelection.add(tx + "," + ty);
                                }
                            }
                        }
                    } else {
                        // Drag без шифта — заменяем прямоугольником
                        editingSelection.clear();
                        for (int tx = minX; tx <= maxX; tx++) {
                            for (int ty = minY; ty <= maxY; ty++) {
                                editingSelection.add(tx + "," + ty);
                            }
                        }
                    }

                    syncSelectionToPattern();
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
                drawList.addRectFilled(rx1, ry1, rx2, ry2, ImGui.getColorU32(0.2f, 0.6f, 1f, 0.25f));
                drawList.addRect(rx1, ry1, rx2, ry2, ImGui.getColorU32(0.2f, 0.6f, 1f, 1f), 0, 0, 1.5f);
            }

            // Рендер выделенных тайлов
            for (String key : editingSelection) {
                String[] parts = key.split(",");
                int tx = Integer.parseInt(parts[0]);
                int ty = Integer.parseInt(parts[1]);
                float sx = cursorPosX + tx * tileW;
                float sy = cursorPosY + ty * tileH;
                drawList.addRectFilled(sx, sy, sx + tileW, sy + tileH, ImGui.getColorU32(0.2f, 0.6f, 1f, 0.35f));
                drawList.addRect(sx, sy, sx + tileW, sy + tileH, ImGui.getColorU32(0.2f, 0.6f, 1f, 1f), 0, 0, 1.5f);
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

    private void syncSelectionToPattern() {
        if (editingPattern == null) {
            return;
        }
        editingPattern.tileCoords.clear();
        for (String key : editingSelection) {
            String[] parts = key.split(",");
            editingPattern.tileCoords.add(new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])});
        }
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

    public org.json.JSONArray toJSON() {
        org.json.JSONArray array = new org.json.JSONArray();
        for (TilePattern pattern : getPatterns()) {
            org.json.JSONObject obj = new org.json.JSONObject();
            obj.put("name", pattern.name);
            obj.put("tileSetName", pattern.tileSet != null ? pattern.tileSet.name : "");

            // Находим bounding box для формирования 2D массива
            if (!pattern.tileCoords.isEmpty()) {
                int minX = pattern.tileCoords.stream().mapToInt(c -> c[0]).min().getAsInt();
                int maxX = pattern.tileCoords.stream().mapToInt(c -> c[0]).max().getAsInt();
                int minY = pattern.tileCoords.stream().mapToInt(c -> c[1]).min().getAsInt();
                int maxY = pattern.tileCoords.stream().mapToInt(c -> c[1]).max().getAsInt();

                int w = maxX - minX + 1;
                int h = maxY - minY + 1;

                obj.put("width", w);
                obj.put("height", h);
                obj.put("offsetX", minX);
                obj.put("offsetY", minY);

                // Строим Set для быстрого поиска
                java.util.Map<String, Integer> coordToTileId = new java.util.HashMap<>();
                for (int[] c : pattern.tileCoords) {
                    Tile tile = pattern.tileSet != null ? pattern.tileSet.tiles[pattern.tileSet.index(c[0], c[1])] : null;
                    coordToTileId.put(c[0] + "," + c[1], tile != null ? tile.id : 0);
                }

                // 2D массив: 0 = пустая ячейка, иначе tile.id
                org.json.JSONArray rows = new org.json.JSONArray();
                for (int row = 0; row < h; row++) {
                    org.json.JSONArray rowArr = new org.json.JSONArray();
                    for (int col = 0; col < w; col++) {
                        String key = (minX + col) + "," + (minY + row);
                        rowArr.put(coordToTileId.getOrDefault(key, 0));
                    }
                    rows.put(rowArr);
                }
                obj.put("pattern", rows);

                // Так же храним coords с tile id для точного восстановления произвольных форм
                org.json.JSONArray coords = new org.json.JSONArray();
                for (int[] c : pattern.tileCoords) {
                    Tile tile = pattern.tileSet != null ? pattern.tileSet.tiles[pattern.tileSet.index(c[0], c[1])] : null;
                    org.json.JSONObject coord = new org.json.JSONObject();
                    coord.put("x", c[0]);
                    coord.put("y", c[1]);
                    coord.put("tileId", tile != null ? tile.id : 0);
                    coords.put(coord);
                }
                obj.put("tiles", coords);
            } else {
                obj.put("width", 0);
                obj.put("height", 0);
                obj.put("pattern", new org.json.JSONArray());
                obj.put("tiles", new org.json.JSONArray());
            }

            array.put(obj);
        }
        return array;
    }
}
