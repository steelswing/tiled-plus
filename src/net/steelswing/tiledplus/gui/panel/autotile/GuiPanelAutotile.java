/*
 * Ну вы же понимаете, что код здесь только мой?
 * Well, you do understand that the code here is only mine?
 */

package net.steelswing.tiledplus.gui.panel.autotile;

import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiDir;
import imgui.type.ImBoolean;
import net.steelswing.tiledplus.gui.DockMenuBase;
import net.steelswing.tiledplus.gui.GuiEditorMain;
import net.steelswing.tiledplus.layer.TileSet;
import lombok.Getter;
import lombok.Setter;

/**
 * Wang Tile Editor Panel - полноценный редактор автотайлов как в Tiled
 * Поддерживает Mixed Wang Sets с визуальным overlay
 */
@Getter
@Setter
public class GuiPanelAutotile extends DockMenuBase {

    protected final GuiEditorMain editor;
    public WangTileEditorRenderer editorRenderer;

    protected TileSet currentTileSet;

    // Палитра цветов Wang
    private WangColor[] wangColors;
    private int selectedColorIndex = 1;

    // Режимы редактирования
    private EditMode editMode = EditMode.PAINT;

    // Настройки отображения
    private ImBoolean showOverlay = new ImBoolean(true);
    private ImBoolean showShadow = new ImBoolean(true);
    private ImBoolean showOutlin = new ImBoolean(true);
    private ImBoolean showOutline = new ImBoolean(true);
    private ImBoolean transparentFill = new ImBoolean(false);

    public GuiPanelAutotile(GuiEditorMain editor) {
        super("AutoTile", ImGuiDir.Left, 300f, 200f);
        this.editor = editor;
        initializeWangColors();
    }

    private void initializeWangColors() {
        // Дефолтные цвета из Tiled (defaultWangColors из wangset.cpp)
        float[][] defaultColors = {
            {255f / 255f, 0f / 255f, 0f / 255f}, // Красный
            {0f / 255f, 255f / 255f, 0f / 255f}, // Зелёный
            {0f / 255f, 0f / 255f, 255f / 255f}, // Синий
            {255f / 255f, 119f / 255f, 0f / 255f}, // Оранжевый
            {0f / 255f, 233f / 255f, 255f / 255f}, // Голубой
            {255f / 255f, 0f / 255f, 216f / 255f}, // Розовый
            {255f / 255f, 255f / 255f, 0f / 255f}, // Жёлтый
            {160f / 255f, 0f / 255f, 255f / 255f}, // Фиолетовый
            {0f / 255f, 255f / 255f, 161f / 255f}, // Светло-зелёный
            {255f / 255f, 168f / 255f, 168f / 255f}, // Светло-розовый
            {180f / 255f, 168f / 255f, 255f / 255f}, // Светло-фиолетовый
            {150f / 255f, 255f / 255f, 167f / 255f}, // Мятный
            {142f / 255f, 120f / 255f, 72f / 255f}, // Коричневый
            {90f / 255f, 90f / 255f, 90f / 255f}, // Серый
            {14f / 255f, 122f / 255f, 70f / 255f} // Тёмно-зелёный
        };

        wangColors = new WangColor[15];
        for (int i = 0; i < wangColors.length; i++) {
            wangColors[i] = new WangColor(
                    i + 1,
                    "Color " + (i + 1),
                    defaultColors[i][0],
                    defaultColors[i][1],
                    defaultColors[i][2]
            );
        }
    }

    @Override
    public void render() {
        if (ImGui.begin(getTitle())) {

            // Панель инструментов
            renderToolbar();

            ImGui.separator();

            // Палитра цветов
            renderColorPalette();

            ImGui.separator();

            // Настройки отображения
            renderDisplaySettings();

            ImGui.separator();

            final TileSet currentTileSet = editor.guiPanelTileSets.currentTileSet;

            if (currentTileSet != null && currentTileSet.atlasTexture != null) {
                if (this.currentTileSet == null || this.currentTileSet != currentTileSet) {
                    this.currentTileSet = currentTileSet;
                    editorRenderer = new WangTileEditorRenderer(currentTileSet, this);
                }
            }

            if (editorRenderer != null) {
                editorRenderer.render();
            } else {
                ImGui.textDisabled("No tileset selected");
            }

            ImGui.end();
        }
    }

    private void renderToolbar() {
        ImGui.text("Edit Mode:");
        ImGui.sameLine();

        if (ImGui.radioButton("Paint", editMode == EditMode.PAINT)) {
            editMode = EditMode.PAINT;
        }
        ImGui.sameLine();

        if (ImGui.radioButton("Erase", editMode == EditMode.ERASE)) {
            editMode = EditMode.ERASE;
        }
        ImGui.sameLine();

        if (ImGui.radioButton("Eyedropper", editMode == EditMode.EYEDROPPER)) {
            editMode = EditMode.EYEDROPPER;
        }
    }

    private void renderDisplaySettings() {
        ImGui.text("Display Options:");

        ImGui.checkbox("Show Overlay", showOverlay);

        ImGui.checkbox("Show Shadow", showShadow);

        ImGui.checkbox("Show Outline", showOutline);

        ImGui.checkbox("Transparent Fill", transparentFill);
    }

    private void renderColorPalette() {
        ImGui.text("Wang Colors:");

        float buttonSize = 32f;
        float spacing = 4f;
        int colorsPerRow = 5;

        for (int i = 0; i < wangColors.length; i++) {
            WangColor color = wangColors[i];

            ImGui.pushID("color_" + i);

            // Рамка вокруг выбранного цвета
            boolean isSelected = selectedColorIndex == color.index;

            if (isSelected) {
                ImGui.pushStyleColor(ImGuiCol.Border, 1f, 1f, 1f, 1f);
                ImGui.pushStyleVar(imgui.flag.ImGuiStyleVar.FrameBorderSize, 2f);
            }

            ImGui.pushStyleColor(ImGuiCol.Button, color.r, color.g, color.b, 1f);
            ImGui.pushStyleColor(ImGuiCol.ButtonHovered,
                    Math.min(color.r * 1.2f, 1f),
                    Math.min(color.g * 1.2f, 1f),
                    Math.min(color.b * 1.2f, 1f), 1f);
            ImGui.pushStyleColor(ImGuiCol.ButtonActive,
                    Math.min(color.r * 1.4f, 1f),
                    Math.min(color.g * 1.4f, 1f),
                    Math.min(color.b * 1.4f, 1f), 1f);

            if (ImGui.button("##color" + i, buttonSize, buttonSize)) {
                selectedColorIndex = color.index;
            }

            ImGui.popStyleColor(3);

            if (isSelected) {
                ImGui.popStyleVar();
                ImGui.popStyleColor();
            }

            if (ImGui.isItemHovered()) {
                ImGui.setTooltip(color.name + " (Index: " + color.index + ")");
            }

            ImGui.popID();

            if ((i + 1) % colorsPerRow != 0 && i < wangColors.length - 1) {
                ImGui.sameLine(0, spacing);
            }
        }

        // Кнопка "Erase" (цвет 0)
        ImGui.sameLine(0, spacing * 2);

        boolean isEraseSelected = selectedColorIndex == 0;
        if (isEraseSelected) {
            ImGui.pushStyleColor(ImGuiCol.Border, 1f, 1f, 1f, 1f);
            ImGui.pushStyleVar(imgui.flag.ImGuiStyleVar.FrameBorderSize, 2f);
        }

        ImGui.pushStyleColor(ImGuiCol.Button, 0.2f, 0.2f, 0.2f, 1f);
        if (ImGui.button("X", buttonSize, buttonSize)) {
            selectedColorIndex = 0;
            editMode = EditMode.ERASE;
        }
        ImGui.popStyleColor();

        if (isEraseSelected) {
            ImGui.popStyleVar();
            ImGui.popStyleColor();
        }

        if (ImGui.isItemHovered()) {
            ImGui.setTooltip("Erase (Color 0)");
        }
    }

    public WangColor getColorByIndex(int index) {
        if (index <= 0 || index > wangColors.length) {
            return null;
        }
        return wangColors[index - 1];
    }

    public int getSelectedColorIndex() {
        return selectedColorIndex;
    }

    public void setSelectedColorIndex(int index) {
        this.selectedColorIndex = index;
    }

    public EditMode getEditMode() {
        return editMode;
    }

    public boolean isShowOverlay() {
        return showOverlay.get();
    }

    public boolean isShowShadow() {
        return showShadow.get();
    }

    public boolean isShowOutline() {
        return showOutline.get();
    }

    public boolean isTransparentFill() {
        return transparentFill.get();
    }

    public enum EditMode {
        PAINT,
        ERASE,
        EYEDROPPER
    }

    // ==================== Wang Color ====================

    public static class WangColor {

        public final int index;
        public final String name;
        public final float r, g, b;

        public WangColor(int index, String name, float r, float g, float b) {
            this.index = index;
            this.name = name;
            this.r = r;
            this.g = g;
            this.b = b;
        }

        public int getColorU32() {
            return imgui.internal.ImGui.getColorU32(r, g, b, 1f);
        }

        public int getColorU32(float alpha) {
            return imgui.internal.ImGui.getColorU32(r, g, b, alpha);
        }
    }
}
