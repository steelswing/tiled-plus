/*
 * Ну вы же понимаете, что код здесь только мой?
 * Well, you do understand that the code here is only mine?
 */

package net.steelswing.tiledplus.utils;

import imgui.ImDrawList;

/**
 * File: Util.java
 * Created on 2026 Feb 10, 17:21:15
 *
 * @author LWJGL2
 */
public class Util {

    public static String getFileNameWithoutExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return fileName;
        }

        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(0, dotIndex);
        }

        return fileName;  // если точки нет или она в начале/конце
    }

    /**
     * Рисует пунктирную линию
     */
    public static void drawDashedLine(ImDrawList drawList, float x1, float y1, float x2, float y2, int color, float thickness, float dashLength, float gapLength) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);

        if (length == 0) {
            return;
        }

        float stepLength = dashLength + gapLength;
        int steps = (int) (length / stepLength);

        float stepX = (dx / length) * stepLength;
        float stepY = (dy / length) * stepLength;

        for (int i = 0; i <= steps; i++) {
            float startX = x1 + i * stepX;
            float startY = y1 + i * stepY;

            float endX = startX + (dx / length) * dashLength;
            float endY = startY + (dy / length) * dashLength;

            // Проверяем, не выходим ли за пределы линии
            if (i == steps) {
                float remainingLength = length - i * stepLength;
                if (remainingLength < dashLength) {
                    endX = x2;
                    endY = y2;
                }
            }

            drawList.addLine(startX, startY, endX, endY, color, thickness);
        }
    }
}
