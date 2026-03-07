/*
Ну вы же понимаете, что код здесь только мой?
Well, you do understand that the code here is only mine?
 */

package net.steelswing.tiledplus.imgui;

import imgui.ImGui;
import java.awt.*;
import lombok.experimental.UtilityClass;

/**
 * File: ColorUtility.java
 * Created on 2026 Feb 10, 02:52:16
 *
 * @author LWJGL2
 */
@UtilityClass
public class ColorUtility {

    public static float[] colorToRGBA(Color color) {
        float red = color.getRed() / 255.0f;
        float green = color.getGreen() / 255.0f;
        float blue = color.getBlue() / 255.0f;
        float alpha = color.getAlpha() / 255.0f;
        return new float[]{red, green, blue, alpha};
    }

    public static float[] colorToRGBA(net.steelswing.engine.api.vecmath.Color color) {
        float red = color.getRed() / 255.0f;
        float green = color.getGreen() / 255.0f;
        float blue = color.getBlue() / 255.0f;
        float alpha = color.getAlpha() / 255.0f;
        return new float[]{red, green, blue, alpha};
    }

    public static int toImGui(Color color) {
        float[] colors = colorToRGBA(color);
        if (colors.length != 4) {
            return 0;
        }
        return ImGui.colorConvertFloat4ToU32(colors[0], colors[1], colors[2], colors[3]);
    }

    public static int toImGui(net.steelswing.engine.api.vecmath.Color color) {
        float[] colors = colorToRGBA(color);
        if (colors.length != 4) {
            return 0;
        }
        return ImGui.colorConvertFloat4ToU32(colors[0], colors[1], colors[2], colors[3]);
    }

}
