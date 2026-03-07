package net.steelswing.tiledplus.gui.panel.canvas.tool;

import imgui.ImDrawList;
import imgui.flag.ImGuiMouseButton;
import imgui.internal.ImGui;
import net.steelswing.tiledplus.gui.IconManager;
import net.steelswing.tiledplus.gui.panel.autotile.WangId;
import net.steelswing.tiledplus.gui.panel.autotile.WangOverlay;
import net.steelswing.tiledplus.gui.panel.canvas.GuiPanelCanvas;
import net.steelswing.tiledplus.gui.panel.canvas.HistoryAction;
import net.steelswing.tiledplus.layer.Tile;
import net.steelswing.tiledplus.layer.TileSet;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import net.steelswing.tiledplus.gui.panel.canvas.Point;
import net.steelswing.tiledplus.layer.type.TileLayer;

/**
 * Wang Brush Tool - ИСПРАВЛЕННАЯ реализация
 */
public class WangBrushToolSimplified extends Tool {

    private int lastTileX = -1;
    private int lastTileY = -1;
    private boolean lineStartSet = false;
    private int lineStartX = -1;
    private int lineStartY = -1;
    private boolean largeBrushMode = false;

    // Кеш расстояний между цветами
    private Map<Integer, Map<Integer, Integer>> colorDistanceCache;

    public WangBrushToolSimplified(GuiPanelCanvas canvas) {
        super(canvas);
        this.colorDistanceCache = new HashMap<>();
    }

    @Override
    public String getName() {
        return "WANG";
    }

    @Override
    public int getHotkey() {
        return GLFW.GLFW_KEY_W;
    }

    @Override
    public String getTooltip() {
        return "Wang Brush (W)\nCtrl: 3x3\nShift: Line";
    }

    @Override
    public int getIcon() {
        return IconManager.TILED.FILL;
    }

    @Override
    public void handleInput(TileLayer layer, int hoveredTileX, int hoveredTileY, int mapWidth, int mapHeight, Tile[][] selectedTiles) {
        boolean leftMouseDown = ImGui.isMouseDown(ImGuiMouseButton.Left) && 
                               !ImGui.isKeyDown(GLFW.GLFW_KEY_SPACE);
        boolean lineMode = ImGui.isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT);
        largeBrushMode = ImGui.isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL);

        if (ImGui.isMouseClicked(ImGuiMouseButton.Left) && !ImGui.isKeyDown(GLFW.GLFW_KEY_SPACE)) {
            canvas.isDrawing = true;
            canvas.currentAction = new HistoryAction();
            
            if (lineMode) {
                if (!lineStartSet) {
                    lineStartX = hoveredTileX;
                    lineStartY = hoveredTileY;
                    lineStartSet = true;
                } else {
                    drawLine(layer, lineStartX, lineStartY, hoveredTileX, hoveredTileY, mapWidth, mapHeight);
                    lineStartSet = false;
                }
            } else {
                lineStartSet = false;
                lastTileX = hoveredTileX;
                lastTileY = hoveredTileY;
                placeWangBrush(layer, hoveredTileX, hoveredTileY, mapWidth, mapHeight);
            }
        }

        if (leftMouseDown && canvas.isDrawing && !lineMode) {
            if (lastTileX != -1 && lastTileY != -1 && (lastTileX != hoveredTileX || lastTileY != hoveredTileY)) {
                List<int[]> points = bresenhamLine(lastTileX, lastTileY, hoveredTileX, hoveredTileY);
                for (int i = 1; i < points.size(); i++) {
                    placeWangBrush(layer, points.get(i)[0], points.get(i)[1], mapWidth, mapHeight);
                }
            }
            lastTileX = hoveredTileX;
            lastTileY = hoveredTileY;
        }

        if (ImGui.isMouseReleased(ImGuiMouseButton.Left) && canvas.isDrawing && !lineMode) {
            canvas.isDrawing = false;
            canvas.pushHistory(canvas.currentAction);
            canvas.currentAction = null;
            lastTileX = -1;
            lastTileY = -1;
        }

        if (lineStartSet && (ImGui.isKeyPressed(GLFW.GLFW_KEY_ESCAPE) || 
                            ImGui.isMouseClicked(ImGuiMouseButton.Right))) {
            lineStartSet = false;
        }
    }

    private void drawLine(TileLayer layer, int x0, int y0, int x1, int y1, int mapWidth, int mapHeight) {
        List<int[]> points = bresenhamLine(x0, y0, x1, y1);
        for (int[] p : points) {
            placeWangBrush(layer, p[0], p[1], mapWidth, mapHeight);
        }
    }

    private List<int[]> bresenhamLine(int x0, int y0, int x1, int y1) {
        List<int[]> points = new ArrayList<>();
        int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            points.add(new int[]{x0, y0});
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x0 += sx; }
            if (e2 < dx) { err += dx; y0 += sy; }
        }
        return points;
    }

    private void placeWangBrush(TileLayer layer, int centerX, int centerY, int mapWidth, int mapHeight) {
        int brushSize = largeBrushMode ? 3 : 2;
        int startX = centerX - brushSize / 2;
        int startY = centerY - brushSize / 2;
        
        Tile[][] brush = calculateBrush(layer, startX, startY, brushSize, mapWidth, mapHeight);
        
        for (int dy = 0; dy < brushSize; dy++) {
            for (int dx = 0; dx < brushSize; dx++) {
                int x = startX + dx;
                int y = startY + dy;
                
                if (x >= 0 && x < mapWidth && y >= 0 && y < mapHeight) {
                    Tile tile = brush[dy][dx];
                    if (tile != null) {
                        canvas.placeTile(layer, x, y, tile);
                    }
                }
            }
        }
    }

    
    /**
     * ИСПРАВЛЕНО: правильно вычисляет кисть с учётом границ региона
     */
    private Tile[][] calculateBrush(TileLayer layer, int startX, int startY, int brushSize, int mapWidth, int mapHeight) {
        Tile[][] result = new Tile[brushSize][brushSize];
        
        return  solveBrush(layer, startX, startY, brushSize);
    }

    /**
     * Вычисляет WangId на основе окружающих тайлов (вне региона кисти)
     */
    private WangId wangIdFromSurroundings(TileLayer layer, int x, int y, int regionStartX, int regionStartY, int regionSize, int mapWidth, int mapHeight) {
        WangId[] surroundingWangIds = new WangId[8];
        int[][] dirs = {{0,-1},{1,-1},{1,0},{1,1},{0,1},{-1,1},{-1,0},{-1,-1}};
        
        for (int i = 0; i < 8; i++) {
            surroundingWangIds[i] = new WangId(WangId.FULL_MASK);
            
            int nx = x + dirs[i][0];
            int ny = y + dirs[i][1];
            
            // Проверяем: сосед вне региона кисти?
            boolean isOutsideRegion = (nx < regionStartX || nx >= regionStartX + regionSize ||
                                       ny < regionStartY || ny >= regionStartY + regionSize);
            
            if (isOutsideRegion && nx >= 0 && nx < mapWidth && ny >= 0 && ny < mapHeight) {
                Tile neighborTile = layer.get(nx, ny);
                if (neighborTile != null) {
                    WangId neighborWangId = getWangIdForTile(neighborTile);
                    if (neighborWangId != null && !neighborWangId.isEmpty()) {
                        surroundingWangIds[i] = neighborWangId;
                    }
                }
            }
        }
        
        return wangIdFromSurroundingArray(surroundingWangIds);
    }

    /**
     * Собирает WangId из массива окружающих WangId
     * Портирование wangIdFromSurrounding() из Tiled
     */
    private WangId wangIdFromSurroundingArray(WangId[] surroundingWangIds) {
        WangId id = new WangId(WangId.FULL_MASK);
        
        // Сначала обрабатываем рёбра (Top, Right, Bottom, Left)
        int[] edges = {WangId.TOP, WangId.RIGHT, WangId.BOTTOM, WangId.LEFT};
        for (int i : edges) {
            int oppIndex = WangId.oppositeIndex(i);
            int color = surroundingWangIds[i].indexColor(oppIndex);
            id.setIndexColor(i, color);
        }
        
        // Затем обрабатываем углы (TopRight, BottomRight, BottomLeft, TopLeft)
        int[] corners = {WangId.TOP_RIGHT, WangId.BOTTOM_RIGHT, WangId.BOTTOM_LEFT, WangId.TOP_LEFT};
        for (int i : corners) {
            int oppIndex = WangId.oppositeIndex(i);
            int color = surroundingWangIds[i].indexColor(oppIndex);
            
            // Если угол не определён, пытаемся вывести из соседних рёбер
            if (color == WangId.INDEX_MASK || color == 0) {
                int leftSideCorner = surroundingWangIds[WangId.previousIndex(i)].indexColor((i + 2) % WangId.NUM_INDEXES);
                if (leftSideCorner != WangId.INDEX_MASK) {
                    color = leftSideCorner;
                }
            }
            
            if (color == WangId.INDEX_MASK || color == 0) {
                int rightSideCorner = surroundingWangIds[WangId.nextIndex(i)].indexColor((i + 6) % WangId.NUM_INDEXES);
                if (rightSideCorner != WangId.INDEX_MASK) {
                    color = rightSideCorner;
                }
            }
            
            id.setIndexColor(i, color);
        }
        
        return id;
    }

    /**
     * Обновляет desired/mask для соседних ячеек после размещения тайла
     * Портирование updateAdjacent() из Tiled
     */
    private void updateAdjacentCells(Map<String, CellInfo> grid, int x, int y, WangId placedWangId, 
                                     int regionStartX, int regionStartY, int regionSize, int mapWidth, int mapHeight) {
        int[][] dirs = {{0,-1},{1,-1},{1,0},{1,1},{0,1},{-1,1},{-1,0},{-1,-1}};
        
        for (int i = 0; i < 8; i++) {
            int nx = x + dirs[i][0];
            int ny = y + dirs[i][1];
            
            // Проверяем: сосед внутри региона?
            if (nx >= regionStartX && nx < regionStartX + regionSize &&
                ny >= regionStartY && ny < regionStartY + regionSize &&
                nx >= 0 && nx < mapWidth && ny >= 0 && ny < mapHeight) {
                
                CellInfo adjacentInfo = grid.get(nx + "," + ny);
                if (adjacentInfo != null) {
                    // updateAdjacent из Tiled
                    int index = WangId.oppositeIndex(i);
                    
                    adjacentInfo.desired.setIndexColor(index, placedWangId.indexColor(i));
                    adjacentInfo.mask.setIndexColor(index, (int)WangId.INDEX_MASK);
                    
                    if (!WangId.isCorner(index)) {
                        int cornerA = WangId.nextIndex(index);
                        int cornerB = WangId.previousIndex(index);
                        int adjacentCornerA = WangId.previousIndex(i);
                        int adjacentCornerB = WangId.nextIndex(i);
                        
                        adjacentInfo.desired.setIndexColor(cornerA, placedWangId.indexColor(adjacentCornerA));
                        adjacentInfo.mask.setIndexColor(cornerA, (int)WangId.INDEX_MASK);
                        
                        adjacentInfo.desired.setIndexColor(cornerB, placedWangId.indexColor(adjacentCornerB));
                        adjacentInfo.mask.setIndexColor(cornerB, (int)WangId.INDEX_MASK);
                    }
                }
            }
        }
    }

    /**
     * Ищет лучший тайл с учётом desired и mask
     */
    private Tile findBestMatch(WangId desired, WangId mask) {
        var data = canvas.editor.guiPanelAutotile.editorRenderer;
        if (data == null) return null;

        var wangIds = data.getTileWangIds();
        if (wangIds.isEmpty()) return null;

        WangId maskedWangId = new WangId(desired.getId() & mask.getId());
        List<TileCandidate> matches = new ArrayList<>();
        int lowestPenalty = Integer.MAX_VALUE;

        for (Map.Entry<Integer, WangId> entry : wangIds.entrySet()) {
            WangId candidateWangId = entry.getValue();
            int tileIndex = entry.getKey();
            
            // Проверяем строгое соответствие маске
            WangId maskedCandidate = new WangId(candidateWangId.getId() & mask.getId());
            if (!maskedCandidate.equals(maskedWangId)) {
                continue;
            }

            int totalPenalty = 0;

            // Вычисляем penalty для индексов вне маски
            for (int i = 0; i < WangId.NUM_INDEXES; i++) {
                int desiredColor = desired.indexColor(i);
                if (desiredColor == WangId.INDEX_MASK) continue;
                
                int candidateColor = candidateWangId.indexColor(i);
                if (desiredColor == candidateColor) continue;
                
                int penalty = getTransitionPenalty(desiredColor, candidateColor);
                
                if (penalty < 0) {
                    // Переход невозможен
                    totalPenalty = Integer.MAX_VALUE;
                    break;
                }
                
                totalPenalty += penalty;
            }

            if (totalPenalty == Integer.MAX_VALUE) continue;

            if (totalPenalty < lowestPenalty) {
                matches.clear();
                lowestPenalty = totalPenalty;
            }
            
            if (totalPenalty == lowestPenalty) {
                Tile tile = getTileByIndex(tileIndex);
                if (tile != null) {
                    matches.add(new TileCandidate(tile, candidateWangId, totalPenalty));
                }
            }
        }

        if (matches.isEmpty()) return null;
        return matches.get((int)(Math.random() * matches.size())).tile;
    }

    /**
     * Вспомогательный класс для хранения информации о ячейке
     */
    private static class CellInfo {
       WangId desired = new WangId(); // ВСЁ 0 = wildcard
    WangId mask    = new WangId(); // какие индексы зафиксированы
    }
private void applyExternalConstraints(CellInfo cell, TileLayer layer, int x, int y) {

    // TOP
    Tile t = layer.get(x, y - 1);
    if (t != null) {
        WangId w = getWangIdForTile(t);
        cell.desired.setIndexColor(WangId.TOP, w.indexColor(WangId.BOTTOM));
        cell.mask.setIndexColor(WangId.TOP, 255);
    }

    // RIGHT
    t = layer.get(x + 1, y);
    if (t != null) {
        WangId w = getWangIdForTile(t);
        cell.desired.setIndexColor(WangId.RIGHT, w.indexColor(WangId.LEFT));
        cell.mask.setIndexColor(WangId.RIGHT, 255);
    }

    // BOTTOM
    t = layer.get(x, y + 1);
    if (t != null) {
        WangId w = getWangIdForTile(t);
        cell.desired.setIndexColor(WangId.BOTTOM, w.indexColor(WangId.TOP));
        cell.mask.setIndexColor(WangId.BOTTOM, 255);
    }

    // LEFT
    t = layer.get(x - 1, y);
    if (t != null) {
        WangId w = getWangIdForTile(t);
        cell.desired.setIndexColor(WangId.LEFT, w.indexColor(WangId.RIGHT));
        cell.mask.setIndexColor(WangId.LEFT, 255);
    }
}
private Tile pickTile(CellInfo info) {

    var data = canvas.editor.guiPanelAutotile.editorRenderer;
    if (data == null) return null;

    WangId requiredMask = new WangId(info.desired.getId() & info.mask.getId());

    List<TileCandidate> candidates = new ArrayList<>();
    int bestPenalty = Integer.MAX_VALUE;

    for (var e : data.getTileWangIds().entrySet()) {

        WangId candidateId = e.getValue();

        // строгое соответствие маске
        WangId masked = new WangId(candidateId.getId() & info.mask.getId());
        if (!masked.equals(requiredMask))
            continue;

        int penalty = 0;

        for (int i = 0; i < WangId.NUM_INDEXES; i++) {

            int desiredColor = info.desired.indexColor(i);
            if (desiredColor == 0) continue;

            int candidateColor = candidateId.indexColor(i);

            int p = getTransitionPenalty(desiredColor, candidateColor);
            if (p < 0) {
                penalty = Integer.MAX_VALUE;
                break;
            }

            penalty += p;
        }

        if (penalty == Integer.MAX_VALUE)
            continue;

        if (penalty < bestPenalty) {
            bestPenalty = penalty;
            candidates.clear();
        }

        if (penalty == bestPenalty) {
            Tile t = getTileByIndex(e.getKey());
            if (t != null)
                candidates.add(new TileCandidate(t, candidateId, penalty));
        }
    }

    if (candidates.isEmpty())
        return null;

    return candidates.get((int)(Math.random() * candidates.size())).tile;
}

private void propagate(CellInfo neighbor, WangId placed, int dirToNeighbor) {

    int opposite = WangId.oppositeIndex(dirToNeighbor);

    neighbor.desired.setIndexColor(opposite, placed.indexColor(dirToNeighbor));
    neighbor.mask.setIndexColor(opposite, 255);

    if (!WangId.isCorner(opposite)) {

        int cornerA = WangId.nextIndex(opposite);
        int cornerB = WangId.previousIndex(opposite);

        int placedCornerA = WangId.previousIndex(dirToNeighbor);
        int placedCornerB = WangId.nextIndex(dirToNeighbor);

        neighbor.desired.setIndexColor(cornerA, placed.indexColor(placedCornerA));
        neighbor.mask.setIndexColor(cornerA, 255);

        neighbor.desired.setIndexColor(cornerB, placed.indexColor(placedCornerB));
        neighbor.mask.setIndexColor(cornerB, 255);
    }
}

private Tile[][] solveBrush(TileLayer layer, int sx, int sy, int size) {

    Map<Point, CellInfo> grid = new HashMap<>();

    // init
    for (int y = 0; y < size; y++) {
        for (int x = 0; x < size; x++) {
            CellInfo c = new CellInfo();
            applyExternalConstraints(c, layer, sx+x, sy+y);
            grid.put(new Point(x,y), c);
        }
    }

    Tile[][] result = new Tile[size][size];

    // проход по региону
    for (int y = 0; y < size; y++) {
        for (int x = 0; x < size; x++) {

            CellInfo cell = grid.get(new Point(x,y));

            Tile tile = pickTile(cell);
            result[y][x] = tile;
            if (tile == null) continue;

            WangId wid = getWangIdForTile(tile);

            // распространяем на соседей
            if (x+1 < size) propagate(grid.get(new Point(x+1,y)), wid, WangId.RIGHT);
            if (x-1 >=0)    propagate(grid.get(new Point(x-1,y)), wid, WangId.LEFT);
            if (y+1 < size) propagate(grid.get(new Point(x,y+1)), wid, WangId.BOTTOM);
            if (y-1 >=0)    propagate(grid.get(new Point(x,y-1)), wid, WangId.TOP);
        }
    }

    return result;
}

    // ... [getTransitionPenalty, calculateColorDistanceBFS, buildColorGraph - БЕЗ ИЗМЕНЕНИЙ]
    // ... [getTileByIndex, getWangIdForTile, getTileIndex - БЕЗ ИЗМЕНЕНИЙ]
 
    /**
     * Возвращает penalty между цветами
     */
    private int getTransitionPenalty(int colorA, int colorB) {
        // Wildcards
        if (colorA == 0 && colorB == 0) return 0;
        if (colorA == 0 || colorB == 0) return 1;
        
        // Одинаковые цвета
        if (colorA == colorB) return 0;
        
        // Проверяем кеш
        if (colorDistanceCache.containsKey(colorA) && 
            colorDistanceCache.get(colorA).containsKey(colorB)) {
            return colorDistanceCache.get(colorA).get(colorB);
        }
        
        // Вычисляем расстояние через BFS
        int distance = calculateColorDistanceBFS(colorA, colorB);
        
        // Сохраняем в кеш (симметрично)
        colorDistanceCache.computeIfAbsent(colorA, k -> new HashMap<>()).put(colorB, distance);
        colorDistanceCache.computeIfAbsent(colorB, k -> new HashMap<>()).put(colorA, distance);
        
        return distance;
    }

    /**
     * BFS для поиска кратчайшего пути между цветами
     */
    private int calculateColorDistanceBFS(int colorA, int colorB) {
        var data = canvas.editor.guiPanelAutotile.editorRenderer;
        if (data == null) return -1;
        
        var wangIds = data.getTileWangIds();
        
        // Строим граф соседства цветов
        Map<Integer, Set<Integer>> graph = buildColorGraph(wangIds.values());
        
        // BFS от colorA до colorB
        Queue<Integer> queue = new LinkedList<>();
        Map<Integer, Integer> distances = new HashMap<>();
        
        queue.add(colorA);
        distances.put(colorA, 0);
        
        while (!queue.isEmpty()) {
            int current = queue.poll();
            int currentDist = distances.get(current);
            
            if (current == colorB) {
                return currentDist;
            }
            
            Set<Integer> neighbors = graph.get(current);
            if (neighbors == null) continue;
            
            for (int neighbor : neighbors) {
                if (!distances.containsKey(neighbor)) {
                    distances.put(neighbor, currentDist + 1);
                    queue.add(neighbor);
                }
            }
        }
        
        // Пути нет
        return -1;
    }

    /**
     * Строит граф соседства цветов
     */
    private Map<Integer, Set<Integer>> buildColorGraph(Collection<WangId> wangIds) {
        Map<Integer, Set<Integer>> graph = new HashMap<>();
        
        for (WangId wangId : wangIds) {
            // Собираем цвета по углам и рёбрам отдельно
            Set<Integer> cornerColors = new HashSet<>();
            Set<Integer> edgeColors = new HashSet<>();
            
            for (int i = 0; i < WangId.NUM_INDEXES; i++) {
                int color = wangId.indexColor(i);
                if (color == 0) continue;
                
                if (WangId.isCorner(i)) {
                    cornerColors.add(color);
                } else {
                    edgeColors.add(color);
                }
            }
            
            // Соединяем углы между собой
            for (int c1 : cornerColors) {
                for (int c2 : cornerColors) {
                    if (c1 != c2) {
                        graph.computeIfAbsent(c1, k -> new HashSet<>()).add(c2);
                    }
                }
            }
            
            // Соединяем рёбра между собой
            for (int c1 : edgeColors) {
                for (int c2 : edgeColors) {
                    if (c1 != c2) {
                        graph.computeIfAbsent(c1, k -> new HashSet<>()).add(c2);
                    }
                }
            }
        }
        
        return graph;
    }

    public void invalidateColorDistances() {
        colorDistanceCache.clear();
    }

    private static class TileCandidate {
        final Tile tile;
        final WangId wangId;
        final int penalty;
        
        TileCandidate(Tile tile, WangId wangId, int penalty) {
            this.tile = tile;
            this.wangId = wangId;
            this.penalty = penalty;
        }
    }

    @Override
    public void renderPreview(ImDrawList drawList, int hoveredTileX, int hoveredTileY, int mapWidth, int mapHeight, 
                             float imageX, float imageY, float tileWidthZ, float tileHeightZ, Tile[][] selectedTiles) {
        
        if (hoveredTileX < 0 || hoveredTileX >= mapWidth || hoveredTileY < 0 || hoveredTileY >= mapHeight) {
            return;
        }

        var data = canvas.editor.guiPanelAutotile.editorRenderer;
        if (data == null) {
            showWarning(drawList, hoveredTileX, hoveredTileY, imageX, imageY, tileWidthZ, tileHeightZ, mapWidth, mapHeight);
            return;
        }

        TileLayer layer = canvas.editor.guiPanelLayers.getSelectedLayerTileLayer();
        if (layer == null) return;

        int brushSize = largeBrushMode ? 3 : 2;
        int startX = hoveredTileX - brushSize / 2;
        int startY = hoveredTileY - brushSize / 2;

        Tile[][] brush = calculateBrush(layer, startX, startY, brushSize, mapWidth, mapHeight);

        for (int dy = 0; dy < brushSize; dy++) {
            for (int dx = 0; dx < brushSize; dx++) {
                int x = startX + dx;
                int y = startY + dy;
                
                if (x < 0 || x >= mapWidth || y < 0 || y >= mapHeight) continue;

                Tile t = brush[dy][dx];
                float xx = imageX + x * tileWidthZ;
                float yy = imageY + y * tileHeightZ;

                if (t != null) {
                    drawList.addImage(t.icon.getTextureId(), xx, yy, xx + tileWidthZ, yy + tileHeightZ,
                        t.icon.getMinU(), t.icon.getMinV(), t.icon.getMaxU(), t.icon.getMaxV(), 
                        ImGui.getColorU32(1f, 1f, 1f, 0.7f));
                    
                    WangId wid = getWangIdForTile(t);
                    if (wid != null) {
                        WangOverlay.paintWangOverlay(drawList, wid, xx, yy, tileWidthZ, tileHeightZ,
                            ci -> {
                                var c = canvas.editor.guiPanelAutotile.getColorByIndex(ci);
                                return c == null ? null : new int[]{(int)(c.r*255),(int)(c.g*255),(int)(c.b*255),255};
                            },
                            WangOverlay.WO_SHADOW | WangOverlay.WO_OUTLINE | WangOverlay.WO_TRANSPARENT_FILL);
                    }
                } else {
                    drawList.addRectFilled(xx, yy, xx + tileWidthZ, yy + tileHeightZ, ImGui.getColorU32(1f,0f,0f,0.3f));
                }

                drawList.addRect(xx, yy, xx + tileWidthZ, yy + tileHeightZ, 
                    ImGui.getColorU32(t != null ? 0f : 1f, t != null ? 1f : 0f, 0f, 0.5f), 0, 0, 1f);
            }
        }

        float rx = imageX + startX * tileWidthZ;
        float ry = imageY + startY * tileHeightZ;
        drawList.addRect(rx, ry, rx + brushSize * tileWidthZ, ry + brushSize * tileHeightZ, 
            ImGui.getColorU32(0f, 1f, 0f, 0.8f), 0, 0, 2f);

        if (lineStartSet) {
            float sx = imageX + (lineStartX * tileWidthZ) + tileWidthZ/2;
            float sy = imageY + (lineStartY * tileHeightZ) + tileHeightZ/2;
            float ex = rx + brushSize * tileWidthZ / 2;
            float ey = ry + brushSize * tileHeightZ / 2;
            drawList.addLine(sx, sy, ex, ey, ImGui.getColorU32(1f, 1f, 1f, 0.6f), 2f);
        }
    }

    private void showWarning(ImDrawList dl, int hx, int hy, float ix, float iy, float tw, float th, int mw, int mh) {
        int bs = largeBrushMode ? 3 : 2;
        int sx = hx - bs/2, sy = hy - bs/2;
        
        for (int dy = 0; dy < bs; dy++) {
            for (int dx = 0; dx < bs; dx++) {
                int x = sx + dx, y = sy + dy;
                if (x >= 0 && x < mw && y >= 0 && y < mh) {
                    float xx = ix + x * tw, yy = iy + y * th;
                    dl.addRectFilled(xx, yy, xx + tw, yy + th, ImGui.getColorU32(1f, 0.5f, 0f, 0.3f));
                    dl.addRect(xx, yy, xx + tw, yy + th, ImGui.getColorU32(1f, 0.5f, 0f, 0.8f), 0, 0, 2f);
                }
            }
        }
    }

    private WangId getWangIdForTile(Tile tile) {
        var data = canvas.editor.guiPanelAutotile.editorRenderer;
        if (data == null) return null;
        int idx = getTileIndex(tile);
        return idx < 0 ? null : data.getTileWangIds().get(idx);
    }

    private int getTileIndex(Tile tile) {
        TileSet ts = canvas.editor.guiPanelTileSets.currentTileSet;
        if (ts == null || tile == null) return -1;
        if (tile.id >= 0 && tile.id < ts.tiles.length && ts.tiles[tile.id] == tile) return tile.id;
        for (int i = 0; i < ts.tiles.length; i++) {
            if (ts.tiles[i] == tile) return i;
        }
        return -1;
    }

    private Tile getTileByIndex(int idx) {
        TileSet ts = canvas.editor.guiPanelTileSets.currentTileSet;
        return (ts == null || idx < 0 || idx >= ts.tiles.length) ? null : ts.tiles[idx];
    }

    @Override
    public void handleRightClickSelection(TileLayer layer, int minX, int minY, int maxX, int maxY) {}

    @Override
    public String getExtraInfo(Tile[][] selectedTiles) {
        String s = "| Wang | " + (largeBrushMode ? "3x3" : "2x2");
        if (lineStartSet) s += " | Line mode";
        s += " | Ctrl: size | Shift: line";
        var data = canvas.editor.guiPanelAutotile.editorRenderer;
        s += data != null ? (" | Tiles: " + data.getTileWangIds().size()) : " | No data!";
        s += " | Cache: " + colorDistanceCache.size();
        return s;
    }
}