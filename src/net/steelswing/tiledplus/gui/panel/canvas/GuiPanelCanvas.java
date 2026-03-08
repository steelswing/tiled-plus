
package net.steelswing.tiledplus.gui.panel.canvas;

import net.steelswing.tiledplus.gui.panel.canvas.tool.Tool;
import net.steelswing.tiledplus.gui.panel.canvas.tool.PutTool;
import net.steelswing.tiledplus.gui.panel.canvas.tool.EraseTool;
import net.steelswing.tiledplus.gui.panel.canvas.tool.FillTool;
import imgui.ImDrawList;
import imgui.ImVec2;
import imgui.flag.ImGuiDir;
import imgui.flag.ImGuiMouseButton;
import imgui.flag.ImGuiWindowFlags;
import imgui.internal.ImGui;
import imgui.internal.ImGuiDockNode;
import imgui.type.ImInt;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import net.steelswing.tiledplus.gui.DockMenuBase;
import net.steelswing.tiledplus.gui.GuiEditorMain;
import net.steelswing.tiledplus.gui.panel.canvas.tool.WangBrushToolSimplified;
import net.steelswing.tiledplus.layer.Layer;
import net.steelswing.tiledplus.layer.Tile;
import net.steelswing.tiledplus.layer.type.ObjectLayer;
import net.steelswing.tiledplus.layer.type.TileLayer;
import net.steelswing.tiledplus.utils.Util;
import org.lwjgl.glfw.GLFW;

/**
 * File: GuiPanelCanvas.java
 * Created on 2026 Feb 10, 15:42:39
 *
 * @author LWJGL2
 */
public class GuiPanelCanvas extends DockMenuBase {

    public final GuiEditorMain editor;

    protected float zoom = 1.0f;
    protected float minZoom = 0.1f;
    protected float maxZoom = 10.0f;
    protected boolean isDragging = false;
    protected float offsetX = 0.0f;
    protected float offsetY = 0.0f;

    // ================= ИСТОРИЯ ИЗМЕНЕНИЙ =================
    public final List<HistoryAction> history = new ArrayList<>();
    public int historyIndex = -1;
    public final int maxHistorySize = 100;
    public boolean isDrawing = false;
    public HistoryAction currentAction = null;

    // ================= ИНСТРУМЕНТЫ =================
    public Tool currentTool;
    public final PutTool putTool;
    public final EraseTool eraseTool;
    public final FillTool fillTool;
    // В классе GuiPanelCanvas добавьте поле:
    public final WangBrushToolSimplified wangBrushToolSimplified;

    public GuiPanelCanvas(GuiEditorMain editor) {
        super("MapEditor", ImGuiDir.Down, 500f, 200f);
        this.editor = editor;

        // Инициализируем инструменты
        this.putTool = new PutTool(this);
        this.eraseTool = new EraseTool(this);
        this.fillTool = new FillTool(this);
        this.wangBrushToolSimplified = new WangBrushToolSimplified(this);

        this.currentTool = putTool;
    }

    // Снапшот слоя до начала рисования
    private java.util.HashMap<Long, Tile> strokeSnapshot = new java.util.HashMap<>();

    public void beginStroke() {
        strokeSnapshot.clear();
        currentAction = new HistoryAction();
    }

    public void placeTileStroked(TileLayer layer, int x, int y, Tile newTile) {
        // Простой ключ — достаточно если слой один, для мульти-слоя используем индекс
        int layerIndex = editor.guiPanelLayers.getLayers().indexOf(layer);
        long key = ((long) layerIndex << 32) | ((long) (x & 0xFFFF) << 16) | (y & 0xFFFF);

        // Запоминаем oldTile ДО любых изменений — только при первом касании
        if (!strokeSnapshot.containsKey(key)) {
            strokeSnapshot.put(key, layer.get(x, y)); // get ПЕРЕД set
        }

        layer.set(x, y, newTile); // только после снапшота
    }

    public void endStroke() {
        if (currentAction == null) {
            return;
        }

        List<Layer> layers = editor.guiPanelLayers.getLayers();

        for (java.util.Map.Entry<Long, Tile> entry : strokeSnapshot.entrySet()) {
            long key = entry.getKey();
            int layerIndex = (int) (key >> 32);
            int x = (int) ((key >> 16) & 0xFFFF);
            int y = (int) (key & 0xFFFF);
            Tile oldTile = entry.getValue();

            if (layerIndex < 0 || layerIndex >= layers.size()) {
                continue;
            }
            Layer l = layers.get(layerIndex);
            if (!(l instanceof TileLayer tl)) {
                continue;
            }

            Tile newTile = tl.get(x, y);
            if (oldTile != newTile) {
                currentAction.addChange(tl, x, y, oldTile, newTile);
            }
        }

        pushHistory(currentAction);
        currentAction = null;
        strokeSnapshot.clear();
    }

    /**
     * Добавляет действие в историю
     */
    public void pushHistory(HistoryAction action) {
        if (action == null || action.isEmpty()) {
            return;
        }

        while (history.size() > historyIndex + 1) {
            history.remove(history.size() - 1);
        }

        history.add(action);
        historyIndex++;

        while (history.size() > maxHistorySize) {
            history.remove(0);
            historyIndex--;
        }
    }

    public void undo() {
        if (historyIndex < 0 || history.isEmpty()) {
            return;
        }

        HistoryAction action = history.get(historyIndex);
        for (TileChange change : action.changes) {
            change.layer.set(change.x, change.y, change.oldTile);
        }
        historyIndex--;
    }

    public void redo() {
        if (historyIndex >= history.size() - 1) {
            return;
        }

        historyIndex++;
        HistoryAction action = history.get(historyIndex);
        for (TileChange change : action.changes) {
            change.layer.set(change.x, change.y, change.newTile);
        }
    }

    public void placeTile(TileLayer layer, int x, int y, Tile newTile) {
        Tile oldTile = layer.get(x, y);
        if (oldTile == newTile) {
            return;
        }

        if (currentAction != null) {
            currentAction.addChange(layer, x, y, oldTile, newTile); // <-- передаём layer
        }

        layer.set(x, y, newTile);
    }

    /**
     * Заливка области (алгоритм flood fill)
     */
    public void fillArea(TileLayer layer, int startX, int startY, Tile newTile) {
        if (startX < 0 || startX >= layer.width || startY < 0 || startY >= layer.height) {
            return;
        }

        Tile targetTile = layer.get(startX, startY);
        if (targetTile == newTile) {
            return;
        }

        Queue<Point> queue = new LinkedList<>();
        boolean[][] visited = new boolean[layer.width][layer.height];

        queue.add(new Point(startX, startY));
        visited[startX][startY] = true;

        while (!queue.isEmpty()) {
            Point p = queue.poll();

            placeTileStroked(layer, p.x, p.y, newTile); // <-- вместо placeTile

            int[][] directions = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};
            for (int[] dir : directions) {
                int newX = p.x + dir[0];
                int newY = p.y + dir[1];
                if (newX >= 0 && newX < layer.width && newY >= 0 && newY < layer.height && !visited[newX][newY]) {
                    Tile tile = layer.get(newX, newY);
                    if (tile == targetTile) {
                        queue.add(new Point(newX, newY));
                        visited[newX][newY] = true;
                    }
                }
            }
        }
    }


    /**
     * Рендер сетки с пунктирными линиями и крупной сеткой через каждые 10 тайлов
     */
    public void renderGrid(ImDrawList drawList, int mapWidth, int mapHeight, float imageX, float imageY, float tileWidthZ, float tileHeightZ, float scaledWidth, float scaledHeight) {
        int gridColor = ImGui.getColorU32(0f, 0f, 0f, 0.4f);
        float dashLength = 4f;
        float gapLength = 4f;

        for (int x = 0; x <= mapWidth; x++) {
            float screenX = imageX + (x * tileWidthZ);
            Util.drawDashedLine(drawList, screenX, imageY, screenX, imageY + scaledHeight, gridColor, 1f, dashLength, gapLength);
        }

        for (int y = 0; y <= mapHeight; y++) {
            float screenY = imageY + (y * tileHeightZ);
            Util.drawDashedLine(drawList, imageX, screenY, imageX + scaledWidth, screenY, gridColor, 1f, dashLength, gapLength);
        }

        int majorGridColor = ImGui.getColorU32(0f, 0f, 0f, 0.2f);
        float majorThickness = 0.5f;

        for (int x = 0; x <= mapWidth; x += 10) {
            float screenX = imageX + (x * tileWidthZ);
            drawList.addLine(screenX, imageY, screenX, imageY + scaledHeight, majorGridColor, majorThickness);
        }

        for (int y = 0; y <= mapHeight; y += 10) {
            float screenY = imageY + (y * tileHeightZ);
            drawList.addLine(imageX, screenY, imageX + scaledWidth, screenY, majorGridColor, majorThickness);
        }
    }

    @Override
    public void render() {
        if (ImGui.begin(getTitle())) {
            List<Layer> layers = editor.guiPanelLayers.getLayers();

            TileLayer selectedLayer = editor.guiPanelLayers.getSelectedLayerTileLayer();

            final int mapWidth = editor.editorSession.levelWidth;
            final int mapHeight = editor.editorSession.levelHeight;
            final float tileWidth = editor.editorSession.tileWidth;
            final float tileHeight = editor.editorSession.tileHeight;

            float viewportWidth = ImGui.getContentRegionAvailX();
            float viewportHeight = ImGui.getContentRegionAvailY();

            // ================= ПАНЕЛЬ ИНСТРУМЕНТОВ =================
            putTool.renderButton();
            ImGui.sameLine();
            eraseTool.renderButton();
            ImGui.sameLine();
            fillTool.renderButton();
            ImGui.sameLine();
            wangBrushToolSimplified.renderButton();

            {
                if (ImGui.beginChild("MapViewport##child", viewportWidth, viewportHeight - 40 - 40, false, ImGuiWindowFlags.HorizontalScrollbar)) {

                    float cursorPosX = ImGui.getCursorScreenPosX();
                    float cursorPosY = ImGui.getCursorScreenPosY();

                    ImDrawList drawList = ImGui.getWindowDrawList();

                    // ================= ГОРЯЧИЕ КЛАВИШИ =================
                    if (selectedLayer != null && ImGui.isWindowFocused()) {
                        boolean ctrl = ImGui.isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL) || ImGui.isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL);
                        boolean shift = ImGui.isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT) || ImGui.isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT);

                        if (ctrl && !shift && ImGui.isKeyPressed(GLFW.GLFW_KEY_Z)) {
                            undo(); // без параметра
                        }
                        if ((ctrl && ImGui.isKeyPressed(GLFW.GLFW_KEY_Y)) || (ctrl && shift && ImGui.isKeyPressed(GLFW.GLFW_KEY_Z))) {
                            redo(); // без параметра
                        }

                        if (ImGui.isKeyPressed(putTool.getHotkey())) {
                            currentTool = putTool;
                        }
                        if (ImGui.isKeyPressed(eraseTool.getHotkey())) {
                            currentTool = eraseTool;
                        }
                        if (ImGui.isKeyPressed(fillTool.getHotkey())) {
                            currentTool = fillTool;
                        }
                        if (ImGui.isKeyPressed(wangBrushToolSimplified.getHotkey())) {
                            currentTool = wangBrushToolSimplified;
                        }
                    }

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

                            float worldPointX = (mouseX - offsetX) / oldZoom;
                            float worldPointY = (mouseY - offsetY) / oldZoom;

                            offsetX = mouseX - (worldPointX * zoom);
                            offsetY = mouseY - (worldPointY * zoom);
                        }
                    }

                    // ================= DRAG (как в Tiled) =================
                    boolean middleMouseDown = ImGui.isMouseDown(ImGuiMouseButton.Middle);
                    boolean spaceDrag = ImGui.isKeyDown(GLFW.GLFW_KEY_SPACE) && ImGui.isMouseDown(ImGuiMouseButton.Left);

                    if (ImGui.isWindowHovered() && (middleMouseDown || spaceDrag)) {
                        if (!isDragging) {
                            isDragging = true;
                        }

                        float dx = ImGui.getIO().getMouseDeltaX();
                        float dy = ImGui.getIO().getMouseDeltaY();
                        offsetX += dx;
                        offsetY += dy;
                    } else {
                        isDragging = false;
                    }

                    // ================= ПОЗИЦИЯ КАРТЫ =================
                    float imageX = cursorPosX + offsetX;
                    float imageY = cursorPosY + offsetY;

                    final float tileWidthZ = tileWidth * zoom;
                    final float tileHeightZ = tileHeight * zoom;
                    final float scaledWidth = mapWidth * tileWidthZ;
                    final float scaledHeight = mapHeight * tileHeightZ;

                    // ================= ФОН ПОД КАРТОЙ =================
                    int bgColor = ImGui.getColorU32(0.15f, 0.15f, 0.15f, 1.0f);
                    drawList.addRectFilled(cursorPosX, cursorPosY, cursorPosX + viewportWidth, cursorPosY + viewportHeight - 40, bgColor);

                    for (int li = layers.size() - 1; li >= 0; li--) {
                        Layer layer = layers.get(li);
                        if (!layer.visible) {
                            continue;
                        }

                        if (layer instanceof TileLayer tileLayer) {
                            for (int i = 0; i < mapWidth; i++) {
                                for (int j = 0; j < mapHeight; j++) {
                                    Tile tile = tileLayer.get(i, j);
                                    if (tile != null) {
                                        float x = imageX + (i * tileWidthZ);
                                        float y = imageY + (j * tileHeightZ);
                                        drawList.addImage(tile.icon.getTextureId(), x, y, x + tileWidthZ, y + tileHeightZ, tile.icon.getMinU(), tile.icon.getMinV(), tile.icon.getMaxU(), tile.icon.getMaxV());
                                    }
                                }
                            }
                        }
                    }

                    // ================= РЕНДЕР ТАЙЛОВ ВСЕХ СЛОЁВ =================
                    // ================= PREVIEW И РИСОВАНИЕ ТАЙЛОВ =================
                    if (selectedLayer != null) {
                        Tile[][] selectedTiles = editor.guiPanelTileSets.getSelectedTiles();

                        if (/* !isDragging && */ImGui.isWindowHovered()) {
                            float mouseX = ImGui.getMousePosX() - imageX;
                            float mouseY = ImGui.getMousePosY() - imageY;

                            int hoveredTileX = (int) Math.floor(mouseX / tileWidthZ);
                            int hoveredTileY = (int) Math.floor(mouseY / tileHeightZ);
                            currentTool.handleInput(selectedLayer, hoveredTileX, hoveredTileY, mapWidth, mapHeight, selectedTiles);
                            currentTool.renderPreview(drawList, hoveredTileX, hoveredTileY, mapWidth, mapHeight, imageX, imageY, tileWidthZ, tileHeightZ, selectedTiles);
                        }

//                        if (!ImGui.isMouseDown(ImGuiMouseButton.Left) && isDrawing) {
//                            isDrawing = false;
//                            pushHistory(currentAction);
//                            currentAction = null;
//                        }
                    }

                    // ================= СЕТКА =================
                    renderGrid(drawList, mapWidth, mapHeight, imageX, imageY, tileWidthZ, tileHeightZ, scaledWidth, scaledHeight);

                    for (int li = layers.size() - 1; li >= 0; li--) {
                        Layer layer = layers.get(li);
                        if (!layer.visible) {
                            continue;
                        }
                        if (layer instanceof ObjectLayer objectLayer) {
                            for (ObjectLayer.ObjectInfo obj : objectLayer.objects) {
                                if (!obj.visible) {
                                    continue;
                                }

                                float ox = imageX + obj.x * zoom;
                                float oy = imageY + obj.y * zoom;
                                float ow = obj.width * zoom;
                                float oh = obj.height * zoom;

                                // Цвета как в Tiled — серый
                                int fillColor = ImGui.getColorU32(0.5f, 0.5f, 0.5f, 0.25f);
                                // Заливка + рамка
                                drawList.addRectFilled(ox, oy, ox + ow, oy + oh, fillColor);

                                // Тень рамки (смещение вниз-вправо на 1px, тёмная)
                                int shadowBorder = ImGui.getColorU32(0.2f, 0.2f, 0.2f, 1f);
                                drawList.addRect(ox + 1f, oy + 1f, ox + ow + 1f, oy + oh + 1f, shadowBorder, 0, 0, 1.5f);

                                int borderColor = ImGui.getColorU32(0.6f, 0.6f, 0.6f, 1.0f);
                                drawList.addRect(ox, oy, ox + ow, oy + oh, borderColor, 0, 0, 1.5f);

                                // Маленький квадратик в левом верхнем углу
                                float dotSize = Math.max(3f, 4f * zoom);

                                // Название над объектом
                                if (!obj.name.isEmpty()) {
                                    final ImVec2 calcTextSize = ImGui.calcTextSize(obj.name);

                                    float textW = calcTextSize.x;
                                    float textH = calcTextSize.y;
                                    float padding = 6f;

                                    // Плашка центрируется по ширине объекта, прилегает снизу к верхней грани
                                    float bgW = textW + padding * 2f;
                                    float bgH = textH + padding * 1f;
                                    float bgX = ox + (ow - bgW) / 2f;
                                    float bgY = oy - bgH;

                                    // Фон плашки — тот же серый цвет что и рамка
                                    int labelBg = ImGui.getColorU32(0.6f, 0.6f, 0.6f, 1.0f);
                                    drawList.addRectFilled(bgX + 1, bgY + 1, bgX + bgW + 1, bgY + bgH + 1, shadowBorder, 6);

                                    drawList.addRectFilled(bgX, bgY, bgX + bgW, bgY + bgH, labelBg, 6);

                                    // Тень текста
                                    int shadowColor = ImGui.getColorU32(0f, 0f, 0f, 0.6f);
                                    drawList.addText(bgX + padding + 1f, bgY + padding + 1f, shadowColor, obj.name);

                                    // Белый текст
                                    int textColor = ImGui.getColorU32(1f, 1f, 1f, 1.0f);
                                    drawList.addText(bgX + padding, bgY + padding, textColor, obj.name);
                                }
                            }
                        }
                    }

                    ImGui.endChild();
                }
            }

            // ================= ИНФОРМАЦИЯ =================
            ImGui.text(String.format("Zoom: %.0f%% | Map: %dx%d | Tool: %s", zoom * 100f, mapWidth, mapHeight, currentTool.getName()));

            if (selectedLayer != null) {
                Tile[][] selectedTiles = editor.guiPanelTileSets.getSelectedTiles();
                String extraInfo = currentTool.getExtraInfo(selectedTiles);
                if (!extraInfo.isEmpty()) {
                    ImGui.sameLine();
                    ImGui.text(extraInfo);
                }

                if (historyIndex >= 0 || historyIndex < history.size() - 1) {
                    ImGui.sameLine();
                    ImGui.text(String.format("| History: %d/%d", historyIndex + 1, history.size()));
                }
            }

            ImGui.end();
        }
    }

    @Override
    public int setupDockSpace(int dockspaceId, int prevNodeId, ImInt out, HashMap<Class<?>, DockMenuBase> dockPanels) {
        final int nodeId = prevNodeId;
        ImGuiDockNode centralNode = ImGui.dockBuilderGetCentralNode(dockspaceId);
        ImGui.dockBuilderDockWindow(getTitle(), centralNode.getID());
        return nodeId;
    }
}
