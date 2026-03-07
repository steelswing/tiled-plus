
package net.steelswing.tiledplus.gui.panel.autotile;

import imgui.ImDrawList;
import imgui.ImVec2;
import imgui.flag.ImDrawFlags;
import java.util.ArrayList;
import java.util.List;

public class WangOverlay {

    private static final float D = 1.0f / 6.0f;

    public static final int WO_SHADOW = 1;
    public static final int WO_OUTLINE = 2;
    public static final int WO_TRANSPARENT_FILL = 4;

    private static final float PI = 3.14159265358979324f;

    public interface ColorProvider {

        int[] getColor(int colorIndex);
    }

    public static void paintWangOverlay(
            ImDrawList drawList,
            WangId wangId,
            float tileX, float tileY,
            float tileWidth, float tileHeight,
            ColorProvider colorProvider,
            int options) {

        if (wangId == null || wangId.isEmpty()) {
            return;
        }

        boolean hasShadow = (options & WO_SHADOW) != 0;
        boolean hasOutline = (options & WO_OUTLINE) != 0;
        boolean transparentFill = true;//(options & WO_TRANSPARENT_FILL) != 0;
        float fillOpacity = transparentFill ? 0.3f : 1.0f;
        float penWidth = Math.min(2.0f, tileWidth / 16.0f);

        for (int color = 1; color <= 15; color++) {
            long mask = wangId.mask(color).getId();
            if (mask == 0) {
                continue;
            }

            int[] rgba = colorProvider.getColor(color);
            if (rgba == null) {
                continue;
            }

            paintColorMask(drawList, tileX, tileY, tileWidth, tileHeight,
                    mask, rgba, penWidth, fillOpacity, hasShadow, hasOutline);
        }
    }

    private static void paintColorMask(
            ImDrawList drawList,
            float x, float y, float width, float height,
            long mask, int[] rgba,
            float penWidth, float fillOpacity,
            boolean hasShadow, boolean hasOutline) {

        int fillColor = toImGuiColor(rgba[0], rgba[1], rgba[2], (int) (rgba[3] * fillOpacity));
        int outlineColor = hasOutline ? toImGuiColor(rgba[0], rgba[1], rgba[2], 255) : 0;
        int shadowColor = toImGuiColor(0, 0, 0, 102);

        // Сначала пробуем найти составной путь для комбинации edges
        long edgeMask = mask & 0x00FF00FF00FF00FFL; // Маска для edges (индексы 0,2,4,6)
        WangPath edgePath = getEdgePathForMask(edgeMask);

        if (edgePath != null) {
            if (hasShadow) {
                drawPath(drawList, edgePath, x + 1, y + 1, width, height, 0, shadowColor, penWidth);
            }
            drawPath(drawList, edgePath, x, y, width, height, fillColor, outlineColor, hasOutline ? penWidth : 0);
        } else {
            // Если нет составного пути, рисуем каждый edge отдельно
            for (int i = 0; i < 8; i += 2) {
                if (!hasEdge(mask, i)) {
                    continue;
                }

                WangPath individualEdgePath = getIndividualEdgePath(i);
                if (individualEdgePath == null) {
                    continue;
                }

                if (hasShadow) {
                    drawPath(drawList, individualEdgePath, x + 1, y + 1, width, height, 0, shadowColor, penWidth);
                }
                drawPath(drawList, individualEdgePath, x, y, width, height, fillColor, outlineColor, hasOutline ? penWidth : 0);
            }
        }

        // Рисуем каждый corner отдельно 
        for (int i = 1; i < 8; i += 2) {
            if (!hasCorner(mask, i)) {
                continue;
            }

            WangPath cornerPath = getIndividualCornerPath(i);
            if (cornerPath == null) {
                continue;
            }

            if (hasShadow) {
                drawPath(drawList, cornerPath, x + 1, y + 1, width, height, 0, shadowColor, penWidth);
            }
            drawPath(drawList, cornerPath, x, y, width, height, fillColor, outlineColor, hasOutline ? penWidth : 0);
        }
    }


    private static void drawPath(ImDrawList drawList, WangPath path,
            float x, float y, float width, float height,
            int fillColor, int outlineColor, float outlineWidth) {

        List<ImVec2> scaledPoints = buildPathPoints(path, x, y, width, height);

        if (scaledPoints.isEmpty()) {
            return;
        }

        ImVec2[] pointsArray = scaledPoints.toArray(new ImVec2[0]);

        if (fillColor != 0) {
            drawList.addConvexPolyFilled(pointsArray, pointsArray.length, fillColor);
        }

        if (outlineColor != 0 && outlineWidth > 0f) {
            drawList.addPolyline(pointsArray, pointsArray.length, outlineColor, ImDrawFlags.Closed, outlineWidth);
        }
        drawList.pathClear();
    }

    private static List<ImVec2> buildPathPoints(WangPath path, float x, float y, float width, float height) {
        List<ImVec2> points = new ArrayList<>();

        for (WangPathElement element : path.elements) {
            if (element instanceof LineElement) {
                LineElement line = (LineElement) element;
                points.add(new ImVec2(x + line.x * width, y + line.y * height));
            } else if (element instanceof ArcElement) {
                ArcElement arc = (ArcElement) element;
                appendArcPoints(points, x, y, width, height, arc);
            }
        }

        return points;
    }

    private static void appendArcPoints(List<ImVec2> points, float x, float y,
            float width, float height, ArcElement arc) {
        int segments = 10;

        // Центр эллипса находится в центре прямоугольника
        float centerX = x + (arc.rectX + arc.rectW / 2.0f) * width;
        float centerY = y + (arc.rectY + arc.rectH / 2.0f) * height;
        float radiusX = (arc.rectW / 2.0f) * width;
        float radiusY = (arc.rectH / 2.0f) * height;

        // Конвертация из Qt системы координат (0°=право, 90°=вниз, против часовой)
        // в математическую (0°=право, 90°=вверх, против часовой)
        float startAngleRad = -arc.startAngle * PI / 180f;
        float sweepAngleRad = -arc.sweepAngle * PI / 180f;

        // Генерируем точки дуги
        for (int i = 0; i <= segments; i++) {
            float t = (float) i / segments;
            float angle = startAngleRad + sweepAngleRad * t;

            // Вычисляем точку на эллипсе
            float px = centerX + radiusX * (float) Math.cos(angle);
            float py = centerY + radiusY * (float) Math.sin(angle);

            points.add(new ImVec2(px, py));
        }
    }

    private static boolean hasEdge(long mask, int index) {
        return ((mask >> (index * 8)) & 0xFF) != 0;
    }

    private static boolean hasCorner(long mask, int index) {
        return ((mask >> (index * 8)) & 0xFF) != 0;
    }

    private static WangPath getIndividualEdgePath(int index) {
        WangPath ONE_EDGE = createOneEdgePath();
        // index: 0=top, 2=right, 4=bottom, 6=left
        return rotatedPath(ONE_EDGE, index / 2);
    }

    private static WangPath getIndividualCornerPath(int index) {
        WangPath ONE_CORNER = createOneCornerPath();
        // index: 1=top-right, 3=bottom-right, 5=bottom-left, 7=top-left
        return rotatedPath(ONE_CORNER, (index - 1) / 2);
    }

    // Новый метод для получения составных путей edges
    private static WangPath getEdgePathForMask(long edgeMask) {
        // Проверяем, какие edges присутствуют
        boolean top = hasEdge(edgeMask, 0);
        boolean right = hasEdge(edgeMask, 2);
        boolean bottom = hasEdge(edgeMask, 4);
        boolean left = hasEdge(edgeMask, 6);

        int count = (top ? 1 : 0) + (right ? 1 : 0) + (bottom ? 1 : 0) + (left ? 1 : 0);

        WangPath TWO_ADJACENT_EDGES = createTwoAdjacentEdgesPath();
        WangPath TWO_OPPOSITE_EDGES = createTwoOppositeEdgesPath();
        WangPath THREE_EDGES = createThreeEdgesPath();
        WangPath FOUR_EDGES = createFourEdgesPath();

        if (count == 4) {
            // Four edges
            return FOUR_EDGES;
        } else if (count == 3) {
            // Three edges - определяем, какой отсутствует
            if (!top) {
                return rotatedPath(THREE_EDGES, 1);
            }
            if (!right) {
                return rotatedPath(THREE_EDGES, 2);
            }
            if (!bottom) {
                return rotatedPath(THREE_EDGES, 3);
            }
            if (!left) {
                return rotatedPath(THREE_EDGES, 0);
            }
        } else if (count == 2) {
            // Two edges - проверяем adjacent или opposite
            if (top && right) {
                return TWO_ADJACENT_EDGES;
            }
            if (right && bottom) {
                return rotatedPath(TWO_ADJACENT_EDGES, 1);
            }
            if (bottom && left) {
                return rotatedPath(TWO_ADJACENT_EDGES, 2);
            }
            if (left && top) {
                return rotatedPath(TWO_ADJACENT_EDGES, 3);
            }
            if (top && bottom) {
                return TWO_OPPOSITE_EDGES;
            }
            if (left && right) {
                return rotatedPath(TWO_OPPOSITE_EDGES, 1);
            }
        }

        // Для одного edge или если комбинация не найдена, возвращаем null
        // (будет использован fallback на отдельные edges)
        return null;
    }

    private static WangPath rotatedPath(WangPath path, int rotations) {
        if (path == null) {
            return null;
        }
        rotations = ((rotations % 4) + 4) % 4; // 0..3
        if (rotations == 0) {
            return path;
        }

        List<WangPathElement> out = new ArrayList<>();

        for (WangPathElement e : path.elements) {

            // ===== LINE =====
            if (e instanceof LineElement) {
                LineElement l = (LineElement) e;
                float x = l.x;
                float y = l.y;

                for (int i = 0; i < rotations; i++) {
                    float nx = 1f - y;
                    float ny = x;
                    x = nx;
                    y = ny;
                }
                out.add(new LineElement(x, y));
            } // ===== ARC =====
            else if (e instanceof ArcElement) {
                ArcElement a = (ArcElement) e;

                float x = a.rectX;
                float y = a.rectY;
                float w = a.rectW;
                float h = a.rectH;
                float start = a.startAngle;

                for (int i = 0; i < rotations; i++) {
                    // поворот bbox на 90°
                    float nx = 1f - y - h;
                    float ny = x;
                    float nw = h;
                    float nh = w;

                    x = nx;
                    y = ny;
                    w = nw;
                    h = nh;

                    // ВАЖНО: угол в Qt системе
                    start -= 90f;
                }

                out.add(new ArcElement(x, y, w, h, start, a.sweepAngle));
            }
        }

        return new WangPath(out);
    }


    private static int toImGuiColor(int r, int g, int b, int a) {
        return (a << 24) | (b << 16) | (g << 8) | r;
    }

    // ==================== Path Definitions ====================



    private static WangPath createOneEdgePath() {
        // Edge at index 0 (top) - выпуклость наружу
        List<WangPathElement> elements = new ArrayList<>();
        elements.add(new LineElement(2 * D, 0));
        elements.add(new LineElement(4 * D, 0));
        elements.add(new LineElement(4 * D, D));
        elements.add(new ArcElement(2 * D, 0, 2 * D, 2 * D, 0, -180));
        return new WangPath(elements);
    }

    private static WangPath createOneCornerPath() {
        // Corner at index 1 (top-right) - маленький уголок
        List<WangPathElement> elements = new ArrayList<>();
        elements.add(new LineElement(4 * D, 0));
        elements.add(new LineElement(6 * D, 0));
        elements.add(new LineElement(6 * D, 2 * D));
        elements.add(new LineElement(5 * D, 2 * D));
        elements.add(new ArcElement(4 * D, 0, 2 * D, 2 * D, -90, -90));
        return new WangPath(elements);
    }


    private static WangPath createTwoAdjacentEdgesPath() {
        // Two adjacent edges (top and right) - соединены как "дорога"
        List<WangPathElement> elements = new ArrayList<>();
        elements.add(new LineElement(2 * D, 0));
        elements.add(new LineElement(4 * D, 0));
        elements.add(new ArcElement(4 * D, 0, 2 * D, 2 * D, -180, 90));
        elements.add(new LineElement(6 * D, 2 * D));
        elements.add(new LineElement(6 * D, 4 * D));
        elements.add(new ArcElement(2 * D, -2 * D, 6 * D, 6 * D, -90, -90));
        return new WangPath(elements);
    }

    private static WangPath createTwoOppositeEdgesPath() {
        // Two opposite edges (top and bottom) - прямоугольник
        float d = 1.0f / 3.0f;
        List<WangPathElement> elements = new ArrayList<>();
        elements.add(new LineElement(d, 0));
        elements.add(new LineElement(2 * d, 0));
        elements.add(new LineElement(2 * d, 3 * d));
        elements.add(new LineElement(d, 3 * d));
        return new WangPath(elements);
    }

    private static WangPath createThreeEdgesPath() {
        // Three edges (top, right, bottom) - T-образная форма
        List<WangPathElement> elements = new ArrayList<>();
        elements.add(new LineElement(2 * D, 0));
        elements.add(new LineElement(4 * D, 0));
        elements.add(new LineElement(4 * D, 1 * D));
        elements.add(new ArcElement(4 * D, 0, 2 * D, 2 * D, -180, 90));
        elements.add(new LineElement(6 * D, 2 * D));
        elements.add(new LineElement(6 * D, 4 * D));
        elements.add(new LineElement(5 * D, 4 * D));
        elements.add(new ArcElement(4 * D, 4 * D, 2 * D, 2 * D, 90, 90));
        elements.add(new LineElement(4 * D, 6 * D));
        elements.add(new LineElement(2 * D, 6 * D));
        return new WangPath(elements);
    }

    private static WangPath createFourEdgesPath() {
        // Four edges - крест
        List<WangPathElement> elements = new ArrayList<>();
        elements.add(new LineElement(2 * D, 0));
        elements.add(new LineElement(4 * D, 0));
        elements.add(new LineElement(4 * D, 1 * D));
        elements.add(new ArcElement(4 * D, 0, 2 * D, 2 * D, -180, 90));
        elements.add(new LineElement(6 * D, 2 * D));
        elements.add(new LineElement(6 * D, 4 * D));
        elements.add(new LineElement(5 * D, 4 * D));
        elements.add(new ArcElement(4 * D, 4 * D, 2 * D, 2 * D, 90, 90));
        elements.add(new LineElement(4 * D, 6 * D));
        elements.add(new LineElement(2 * D, 6 * D));
        elements.add(new ArcElement(0, 4 * D, 2 * D, 2 * D, 0, 90));
        elements.add(new LineElement(0, 4 * D));
        elements.add(new LineElement(0, 2 * D));
        elements.add(new LineElement(D, 2 * D));
        elements.add(new ArcElement(0, 0, 2 * D, 2 * D, -90, 90));
        return new WangPath(elements);
    }

    // ==================== Path Classes ====================

    private static class WangPath {

        final List<WangPathElement> elements;

        WangPath(List<WangPathElement> elements) {
            this.elements = elements;
        }
    }

    private interface WangPathElement {
    }

    private static class LineElement implements WangPathElement {

        final float x, y;

        LineElement(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    private static class ArcElement implements WangPathElement {

        final float rectX, rectY;      // Верхний левый угол прямоугольника дуги
        final float rectW, rectH;      // Ширина и высота прямоугольника дуги
        final float startAngle;        // Начальный угол в градусах (Qt: 0=право, 90=вниз)
        final float sweepAngle;        // Угол поворота (положительный = против часовой)

        ArcElement(float rectX, float rectY, float rectW, float rectH,
                float startAngle, float sweepAngle) {
            this.rectX = rectX;
            this.rectY = rectY;
            this.rectW = rectW;
            this.rectH = rectH;
            this.startAngle = startAngle;
            this.sweepAngle = sweepAngle;
        }
    }

    public static class SimpleColorProvider implements ColorProvider {

        private static final int[][] COLORS = {
            null,
            {255, 100, 100, 255},
            {100, 200, 100, 255},
            {100, 100, 255, 255},
            {255, 255, 100, 255},
            {255, 100, 255, 255},
            {100, 255, 255, 255},};

        @Override
        public int[] getColor(int colorIndex) {
            if (colorIndex < 1 || colorIndex >= COLORS.length) {
                return new int[]{200, 200, 200, 180};
            }
            return COLORS[colorIndex];
        }
    }
}
