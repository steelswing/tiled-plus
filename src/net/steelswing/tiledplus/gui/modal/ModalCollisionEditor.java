/*
 * Ну вы же понимаете, что код здесь только мой?
 * Well, you do understand that the code here is only mine?
 */
package net.steelswing.tiledplus.gui.modal;

import imgui.ImDrawList;
import imgui.flag.ImGuiMouseButton;
import imgui.flag.ImGuiWindowFlags;
import imgui.internal.ImGui;
import java.util.List;
import net.steelswing.flopslop.render.texture.type.Texture2D;
import net.steelswing.tiledplus.TiledPlus;
import net.steelswing.tiledplus.gui.ModalWindow;
import net.steelswing.tiledplus.layer.CollisionRect;
import net.steelswing.tiledplus.layer.TilePattern;
import net.steelswing.tiledplus.layer.TileSet;
import org.lwjgl.glfw.GLFW;

/**
 * File: ModalCollisionEditor.java
 * Created on 2026 Mar 10
 * <p>
 * Редактор прямоугольников коллизий для TilePattern.
 * <p>
 * Управление:
 * - ЛКМ на пустом месте + drag — рисует новый прямоугольник
 * - ЛКМ на прямоугольнике — выбрать / начать перемещение
 * - 8 ручек — масштабирование по сетке
 * - Shift + ручка — пропорционально
 * - Del / Backspace — удалить выбранный (только один раз за нажатие)
 * - Ctrl + колесо — зум холста
 * - Кнопка "← Back" — вернуться в редактор паттернов
 *
 * @author LWJGL2
 */
public class ModalCollisionEditor extends ModalWindow {

    private static float GRID_PX = 4f;
    private static float HANDLE_HALF = 5f;
    private static float LIST_WIDTH = 160f;

    // ─── Состояние ───────────────────────────────────────────────────────────

    private TilePattern pattern;
    private TileSet tileSet;
    private Texture2D texture;

    /** Вызывается при нажатии "← Back" — открывает родительское окно обратно */
    private Runnable onBack;

    private float zoom = 2.0f;

    /** Выбранный прямоугольник (-1 = ничего) */
    private int selectedIdx = -1;

    /**
     * Флаг: клавиша Delete/Backspace была нажата в прошлом кадре.
     * Нужен чтобы удаление срабатывало ровно один раз за нажатие,
     * а не повторялось пока клавиша удерживается (key repeat).
     */
    private boolean deleteKeyWasDown = false;

    // Рисование нового прямоугольника
    private boolean isDrawing = false;
    private float drawStartPx, drawStartPy;

    // Перемещение
    private boolean isMoving = false;
    private float moveStartMousePx, moveStartMousePy;
    private float moveOrigX, moveOrigY;

    // Масштабирование (ручки)
    // 0=TL 1=TC 2=TR / 3=ML 4=MR / 5=BL 6=BC 7=BR  /  -1=нет
    private int activeHandle = -1;
    private float handleStartMousePx, handleStartMousePy;
    private float handleOrigX, handleOrigY, handleOrigW, handleOrigH;

    // ─── Конструктор ─────────────────────────────────────────────────────────

    public ModalCollisionEditor() {
        super("Collision Editor");
    }

    // ─── Открытие ────────────────────────────────────────────────────────────

    /**
     * @param pattern паттерн для редактирования
     * @param onBack callback при нажатии "← Back" (обычно reopens ModalTileSetPatterns)
     */
    public void open(TilePattern pattern, Runnable onBack) {
        this.pattern = pattern;
        this.tileSet = pattern.tileSet;
        this.texture = (tileSet != null) ? tileSet.atlasTexture : null;
        this.onBack = onBack;
        this.selectedIdx = -1;
        this.isDrawing = false;
        this.isMoving = false;
        this.activeHandle = -1;
        this.deleteKeyWasDown = false;
        super.open();
    }

    // ─── Render ──────────────────────────────────────────────────────────────

    @Override
    public void render() {
        if (pattern == null) {
            return;
        }

        int width = (int) (TiledPlus.getWidth() * 0.72);
        int height = (int) (TiledPlus.getHeight() * 0.72);

        if (begin(width, height)) {
            renderTopBar();
            ImGui.separator();
            renderBody();
            ImGui.separator();
            renderBottomBar();
            end();
        }
    }

    // ─── Верхняя панель ──────────────────────────────────────────────────────

    private void renderTopBar() {
        if (ImGui.button("< Back")) {
            close();
            if (onBack != null) {
                onBack.run();
            }
        }
        if (ImGui.isItemHovered()) {
            ImGui.setTooltip("Return to Pattern Editor");
        }

        ImGui.sameLine();
        ImGui.text("|");
        ImGui.sameLine();

        ImGui.text("Collision Editor  —  " + pattern.name + "   Grid: " + (int) GRID_PX + "px" +
                "   Zoom: " + String.format("%.1f", zoom) + "x");

        ImGui.sameLine();
        ImGui.setCursorPosX(ImGui.getCursorPosX() + 16f);

        if (ImGui.button("+ Add")) {
            addNewRect();
        }
        if (ImGui.isItemHovered()) {
            ImGui.setTooltip("Add new collision rect (or draw on canvas)");
        }

        ImGui.sameLine();

        // Кнопка Delete — вызываем deleteSelected() напрямую, отдельно от хоткея.
        // Объединять их через || нельзя — оба сработают в один кадр.
        if (ImGui.button("Delete")) {
            deleteSelected();
        }
        if (ImGui.isItemHovered()) {
            ImGui.setTooltip("Delete selected rect  [Del / Backspace]");
        }

        ImGui.sameLine();
        if (ImGui.button("Clear All")) {
            pattern.collisionRects.clear();
            selectedIdx = -1;
        }
        if (ImGui.isItemHovered()) {
            ImGui.setTooltip("Remove ALL collision rects");
        }
    }

    // ─── Тело: список слева + холст справа ───────────────────────────────────

    private void renderBody() {
        float totalH = ImGui.getContentRegionAvailY() - ImGui.getFontSize() - 36f;

        // ── Левая панель: список прямоугольников ──────────────────────────────
        if (ImGui.beginChild("##col_list", LIST_WIDTH, totalH, true)) {
            ImGui.text("Rects (" + pattern.collisionRects.size() + ")");
            ImGui.separator();

            List<CollisionRect> rects = pattern.collisionRects;
            for (int i = 0; i < rects.size(); i++) {
                CollisionRect r = rects.get(i);
                String label = String.format("#%d   %dx%d", i, (int) r.w, (int) r.h);
                boolean sel = (i == selectedIdx);

                if (ImGui.selectable(label + "##rect" + i, sel)) {
                    selectedIdx = i;
                    finishAllDrags();
                }

                // ПКМ: удалить
                if (ImGui.beginPopupContextItem("##rectctx" + i)) {
                    if (ImGui.menuItem("Delete #" + i)) {
                        rects.remove(i);
                        if (selectedIdx >= rects.size()) {
                            selectedIdx = rects.size() - 1;
                        }
                        if (rects.isEmpty()) {
                            selectedIdx = -1;
                        }
                        ImGui.closeCurrentPopup();
                    }
                    ImGui.endPopup();
                }

                if (ImGui.isItemHovered()) {
                    ImGui.setTooltip(String.format("x=%.0f  y=%.0f  w=%.0f  h=%.0f", r.x, r.y, r.w, r.h));
                }
            }

            ImGui.endChild();
        }

        ImGui.sameLine();

        // ── Холст ─────────────────────────────────────────────────────────────
        float canvasW = ImGui.getContentRegionAvailX();
        renderCanvas(canvasW, totalH);
    }

    // ─── Нижняя строка статуса ───────────────────────────────────────────────

    private void renderBottomBar() {
        if (selectedIdx >= 0 && selectedIdx < pattern.collisionRects.size()) {
            CollisionRect r = pattern.collisionRects.get(selectedIdx);
            ImGui.text(String.format("  #%d   x=%.0f  y=%.0f  w=%.0f  h=%.0f", selectedIdx, r.x, r.y, r.w, r.h));
        } else {
            ImGui.textDisabled("  No rect selected  |  Draw on canvas to create  |  Ctrl+Wheel = zoom");
        }

        float closeW = 80f;
        ImGui.sameLine();
        ImGui.setCursorPosX(ImGui.getCursorPosX() + ImGui.getContentRegionAvailX() - closeW);
        if (ImGui.button("Close", closeW, 0)) {
            close();
            if (onBack != null) {
                onBack.run();
            }
        }
    }

    // ─── Холст ───────────────────────────────────────────────────────────────

    private void renderCanvas(float vpW, float vpH) {
        if (!ImGui.beginChild("##collision_canvas", vpW, vpH, false, ImGuiWindowFlags.HorizontalScrollbar | ImGuiWindowFlags.NoScrollWithMouse)) {
            ImGui.endChild();
            return;
        }

        // Зум Ctrl+колесо
        if (ImGui.isWindowHovered() && ImGui.isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL)) {
            float wheel = ImGui.getIO().getMouseWheel();
            if (wheel > 0) {
                zoom = Math.min(zoom * 1.2f, 16f);
            } else if (wheel < 0) {
                zoom = Math.max(zoom / 1.2f, 0.25f);
            }
        }

        // Размер паттерна в пикселях
        float patW = 0, patH = 0;
        if (!pattern.tileCoords.isEmpty() && tileSet != null) {
            int minX = pattern.tileCoords.stream().mapToInt(c -> c[0]).min().getAsInt();
            int maxX = pattern.tileCoords.stream().mapToInt(c -> c[0]).max().getAsInt();
            int minY = pattern.tileCoords.stream().mapToInt(c -> c[1]).min().getAsInt();
            int maxY = pattern.tileCoords.stream().mapToInt(c -> c[1]).max().getAsInt();
            patW = (maxX - minX + 1) * tileSet.tileWidth;
            patH = (maxY - minY + 1) * tileSet.tileHeight;
        } else if (tileSet != null) {
            patW = tileSet.tileWidth;
            patH = tileSet.tileHeight;
        }

        float scaledW = patW * zoom;
        float scaledH = patH * zoom;
        float originX = ImGui.getCursorScreenPosX();
        float originY = ImGui.getCursorScreenPosY();

        ImDrawList dl = ImGui.getWindowDrawList();

        dl.addRectFilled(originX, originY, originX + scaledW, originY + scaledH, ImGui.getColorU32(0.12f, 0.12f, 0.15f, 1f));

        drawPatternTiles(dl, originX, originY);
        drawGrid(dl, originX, originY, patW, patH, scaledW, scaledH);
        drawCollisionRects(dl, originX, originY);

        ImGui.dummy(scaledW + 20f, scaledH + 20f);

        if (ImGui.isWindowHovered()) {
            float mx = ImGui.getMousePosX() - originX;
            float my = ImGui.getMousePosY() - originY;
            handleMouseInput(mx, my, patW, patH, originX, originY, dl);
        } else if (!ImGui.isMouseDown(ImGuiMouseButton.Left)) {
            finishAllDrags();
        }

        // Хоткей Delete — один раз за нажатие, не зависит от hovered.
        tickDeleteHotkey();

        ImGui.endChild();
    }

    // ─── Рисование тайлов ────────────────────────────────────────────────────

    private void drawPatternTiles(ImDrawList dl, float ox, float oy) {
        if (texture == null || tileSet == null || pattern.tileCoords.isEmpty()) {
            return;
        }
        int minTX = pattern.tileCoords.stream().mapToInt(c -> c[0]).min().getAsInt();
        int minTY = pattern.tileCoords.stream().mapToInt(c -> c[1]).min().getAsInt();
        for (int[] c : pattern.tileCoords) {
            int ix = c[0] - minTX, iy = c[1] - minTY;
            float sx = ox + ix * tileSet.tileWidth * zoom;
            float sy = oy + iy * tileSet.tileHeight * zoom;
            float ex = sx + tileSet.tileWidth * zoom;
            float ey = sy + tileSet.tileHeight * zoom;
            net.steelswing.tiledplus.layer.Tile tile = tileSet.tiles[tileSet.index(c[0], c[1])];
            if (tile != null && tile.icon != null) {
                dl.addImage(tile.icon.getTextureId(), sx, sy, ex, ey, tile.icon.getMinU(), tile.icon.getMinV(), tile.icon.getMaxU(), tile.icon.getMaxV());
            } else {
                dl.addRectFilled(sx, sy, ex, ey, ImGui.getColorU32(0.2f, 0.2f, 0.25f, 1f));
            }
        }
    }

    // ─── Сетка ───────────────────────────────────────────────────────────────

    private void drawGrid(ImDrawList dl, float ox, float oy, float patW, float patH, float scaledW, float scaledH) {
        int cNorm = ImGui.getColorU32(1f, 1f, 1f, 0.12f);
        int cMajor = ImGui.getColorU32(1f, 1f, 1f, 0.28f);
        float step = GRID_PX * zoom;
        for (int i = 0; i <= (int) Math.ceil(patW / GRID_PX); i++) {
            float x = ox + i * step;
            boolean major = tileSet != null && (i * GRID_PX % tileSet.tileWidth == 0);
            dl.addLine(x, oy, x, oy + scaledH, major ? cMajor : cNorm, major ? 1.5f : 0.5f);
        }
        for (int j = 0; j <= (int) Math.ceil(patH / GRID_PX); j++) {
            float y = oy + j * step;
            boolean major = tileSet != null && (j * GRID_PX % tileSet.tileHeight == 0);
            dl.addLine(ox, y, ox + scaledW, y, major ? cMajor : cNorm, major ? 1.5f : 0.5f);
        }
        dl.addRect(ox, oy, ox + scaledW, oy + scaledH, ImGui.getColorU32(0.8f, 0.8f, 0.8f, 0.8f), 0, 0, 1.5f);
    }

    // ─── Прямоугольники + ручки ──────────────────────────────────────────────

    private void drawCollisionRects(ImDrawList dl, float ox, float oy) {
        List<CollisionRect> rects = pattern.collisionRects;
        for (int i = 0; i < rects.size(); i++) {
            CollisionRect r = rects.get(i);
            float rx1 = ox + r.x * zoom, ry1 = oy + r.y * zoom;
            float rx2 = ox + (r.x + r.w) * zoom, ry2 = oy + (r.y + r.h) * zoom;
            boolean sel = (i == selectedIdx);

            dl.addRectFilled(rx1, ry1, rx2, ry2, sel ? ImGui.getColorU32(0.2f, 0.85f, 0.3f, 0.28f) : ImGui.getColorU32(0.2f, 0.7f, 1f, 0.18f));
            dl.addRect(rx1, ry1, rx2, ry2, sel ? ImGui.getColorU32(0.2f, 1f, 0.3f, 1f) : ImGui.getColorU32(0.3f, 0.8f, 1f, 0.85f), 0, 0, sel ? 2f : 1.2f);
            dl.addText(rx1 + 3f, ry1 + 1f, ImGui.getColorU32(1f, 1f, 1f, 0.8f), "#" + i);

            if (sel) {
                drawHandles(dl, rx1, ry1, rx2, ry2);
            }
        }
    }

    private void drawHandles(ImDrawList dl, float rx1, float ry1, float rx2, float ry2) {
        for (float[] p : getHandlePoints(rx1, ry1, rx2, ry2)) {
            dl.addRectFilled(p[0] - HANDLE_HALF, p[1] - HANDLE_HALF, p[0] + HANDLE_HALF, p[1] + HANDLE_HALF, ImGui.getColorU32(0.1f, 0.1f, 0.1f, 1f));
            dl.addRect(p[0] - HANDLE_HALF, p[1] - HANDLE_HALF, p[0] + HANDLE_HALF, p[1] + HANDLE_HALF, ImGui.getColorU32(0.2f, 1f, 0.3f, 1f), 0, 0, 1.5f);
        }
    }

    /** 0=TL 1=TC 2=TR / 3=ML 4=MR / 5=BL 6=BC 7=BR */
    private float[][] getHandlePoints(float rx1, float ry1, float rx2, float ry2) {
        float mx = (rx1 + rx2) * 0.5f, my = (ry1 + ry2) * 0.5f;
        return new float[][]{{rx1, ry1}, {mx, ry1}, {rx2, ry1}, {rx1, my}, {rx2, my}, {rx1, ry2}, {mx, ry2}, {rx2, ry2}};
    }

    // ─── Обработка мыши ──────────────────────────────────────────────────────

    private void handleMouseInput(float mx, float my, float patW, float patH, float originX, float originY, ImDrawList dl) {
        float px = mx / zoom, py = my / zoom;

        boolean lmbDown = ImGui.isMouseDown(ImGuiMouseButton.Left);
        boolean lmbClicked = ImGui.isMouseClicked(ImGuiMouseButton.Left);
        boolean lmbReleased = ImGui.isMouseReleased(ImGuiMouseButton.Left);
        boolean shift = ImGui.isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT) || ImGui.isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT);

        // Resize
        if (activeHandle >= 0) {
            if (lmbDown) {
                applyHandleDrag(px - handleStartMousePx, py - handleStartMousePy, shift);
                return;
            }
            if (lmbReleased) {
                activeHandle = -1;
                return;
            }
        }

        // Move
        if (isMoving) {
            if (lmbDown) {
                CollisionRect r = pattern.collisionRects.get(selectedIdx);
                r.x = snapGrid(moveOrigX + px - moveStartMousePx);
                r.y = snapGrid(moveOrigY + py - moveStartMousePy);
                return;
            }
            if (lmbReleased) {
                isMoving = false;
                return;
            }
        }

        // Draw preview / commit
        if (isDrawing) {
            float ex = snapGrid(px), ey = snapGrid(py);
            float newX = Math.min(drawStartPx, ex), newY = Math.min(drawStartPy, ey);
            float newW = Math.max(GRID_PX, Math.abs(ex - drawStartPx));
            float newH = Math.max(GRID_PX, Math.abs(ey - drawStartPy));
            if (lmbDown) {
                float sx = originX + newX * zoom, sy = originY + newY * zoom;
                dl.addRectFilled(sx, sy, sx + newW * zoom, sy + newH * zoom, ImGui.getColorU32(0.4f, 1f, 0.4f, 0.22f));
                dl.addRect(sx, sy, sx + newW * zoom, sy + newH * zoom, ImGui.getColorU32(0.4f, 1f, 0.4f, 1f), 0, 0, 1.5f);
                return;
            }
            if (lmbReleased) {
                isDrawing = false;
                pattern.collisionRects.add(new CollisionRect(newX, newY, newW, newH));
                selectedIdx = pattern.collisionRects.size() - 1;
                return;
            }
        }

        // Click — проверяем ручки → прямоугольники → рисуем новый
        if (lmbClicked) {
            if (selectedIdx >= 0 && selectedIdx < pattern.collisionRects.size()) {
                CollisionRect r = pattern.collisionRects.get(selectedIdx);
                float[][] hpts = getHandlePoints(r.x * zoom, r.y * zoom, (r.x + r.w) * zoom, (r.y + r.h) * zoom);
                for (int h = 0; h < 8; h++) {
                    if (Math.abs(mx - hpts[h][0]) <= HANDLE_HALF + 2f && Math.abs(my - hpts[h][1]) <= HANDLE_HALF + 2f) {
                        activeHandle = h;
                        handleStartMousePx = px;
                        handleStartMousePy = py;
                        handleOrigX = r.x;
                        handleOrigY = r.y;
                        handleOrigW = r.w;
                        handleOrigH = r.h;
                        return;
                    }
                }
            }

            int hit = hitTest(px, py);
            if (hit >= 0) {
                selectedIdx = hit;
                CollisionRect r = pattern.collisionRects.get(hit);
                isMoving = true;
                moveStartMousePx = px;
                moveStartMousePy = py;
                moveOrigX = r.x;
                moveOrigY = r.y;
            } else {
                selectedIdx = -1;
                if (px >= 0 && py >= 0 && px <= patW && py <= patH) {
                    isDrawing = true;
                    drawStartPx = snapGrid(px);
                    drawStartPy = snapGrid(py);
                }
            }
        }
    }

    // ─── Хоткей Delete — только передний фронт нажатия (rising edge) ─────────

    /**
     * Удаляет выбранный прямоугольник ровно один раз за нажатие клавиши.
     * Вызывается каждый кадр внутри renderCanvas().
     * <p>
     * Суть: запоминаем состояние клавиши в прошлом кадре (deleteKeyWasDown).
     * Удаление происходит только если клавиша нажата СЕЙЧАС, но НЕ была нажата
     * в прошлом кадре — то есть строго в момент первого касания (rising edge).
     * Это полностью исключает повторные срабатывания от key repeat.
     */
    private void tickDeleteHotkey() {
        boolean deleteDown = ImGui.isKeyDown(GLFW.GLFW_KEY_DELETE) || ImGui.isKeyDown(GLFW.GLFW_KEY_BACKSPACE);

        if (deleteDown && !deleteKeyWasDown) {
            deleteSelected();
        }

        deleteKeyWasDown = deleteDown;
    }

    // ─── Resize ──────────────────────────────────────────────────────────────

    private void applyHandleDrag(float dpx, float dpy, boolean shift) {
        if (selectedIdx < 0 || selectedIdx >= pattern.collisionRects.size()) {
            activeHandle = -1;
            return;
        }
        CollisionRect r = pattern.collisionRects.get(selectedIdx);
        float ox = handleOrigX, oy = handleOrigY, ow = handleOrigW, oh = handleOrigH;
        float nx = ox, ny = oy, nw = ow, nh = oh;

        switch (activeHandle) {
            case 0:
                nx = snapGrid(ox + dpx);
                ny = snapGrid(oy + dpy);
                nw = snapGrid(ow - (nx - ox));
                nh = snapGrid(oh - (ny - oy));
                break;
            case 1:
                ny = snapGrid(oy + dpy);
                nh = snapGrid(oh - (ny - oy));
                break;
            case 2:
                nw = snapGrid(ow + dpx);
                ny = snapGrid(oy + dpy);
                nh = snapGrid(oh - (ny - oy));
                break;
            case 3:
                nx = snapGrid(ox + dpx);
                nw = snapGrid(ow - (nx - ox));
                break;
            case 4:
                nw = snapGrid(ow + dpx);
                break;
            case 5:
                nx = snapGrid(ox + dpx);
                nw = snapGrid(ow - (nx - ox));
                nh = snapGrid(oh + dpy);
                break;
            case 6:
                nh = snapGrid(oh + dpy);
                break;
            case 7:
                nw = snapGrid(ow + dpx);
                nh = snapGrid(oh + dpy);
                break;
        }

        if (shift && ow > 0 && oh > 0) {
            float aspect = ow / oh;
            boolean wDriven = activeHandle == 3 || activeHandle == 4 || activeHandle == 2 || activeHandle == 7 || activeHandle == 5;
            if (wDriven) {
                nh = snapGrid(oh + (nw - ow) / aspect);
                if (activeHandle == 2 || activeHandle == 5) {
                    ny = snapGrid(oy - (nh - oh));
                }
            } else {
                nw = snapGrid(ow + (nh - oh) * aspect);
                if (activeHandle == 0 || activeHandle == 1) {
                    nx = snapGrid(ox - (nw - ow));
                }
            }
        }

        r.x = nx;
        r.y = ny;
        r.w = Math.max(nw, GRID_PX);
        r.h = Math.max(nh, GRID_PX);
    }

    // ─── Утилиты ─────────────────────────────────────────────────────────────

    private float snapGrid(float v) {
        return Math.round(v / GRID_PX) * GRID_PX;
    }

    private int hitTest(float px, float py) {
        List<CollisionRect> rects = pattern.collisionRects;
        for (int i = rects.size() - 1; i >= 0; i--) {
            CollisionRect r = rects.get(i);
            if (px >= r.x && px <= r.x + r.w && py >= r.y && py <= r.y + r.h) {
                return i;
            }
        }
        return -1;
    }

    private void deleteSelected() {
        if (selectedIdx >= 0 && selectedIdx < pattern.collisionRects.size()) {
            pattern.collisionRects.remove(selectedIdx);
            selectedIdx = pattern.collisionRects.isEmpty() ? -1 : Math.min(selectedIdx, pattern.collisionRects.size() - 1);
        }
    }

    private void addNewRect() {
        float defW = snapGrid(tileSet != null ? tileSet.tileWidth : 16);
        float defH = snapGrid(tileSet != null ? tileSet.tileHeight : 16);
        pattern.collisionRects.add(new CollisionRect(0, 0, defW, defH));
        selectedIdx = pattern.collisionRects.size() - 1;
    }

    private void finishAllDrags() {
        isDrawing = false;
        isMoving = false;
        activeHandle = -1;
    }
}
