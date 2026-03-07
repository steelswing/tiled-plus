/*
 * Ну вы же понимаете, что код здесь только мой?
 * Well, you do understand that the code here is only mine?
 */

package net.steelswing.tiledplus.gui.modal;

import imgui.internal.ImGui;
import imgui.type.ImInt;
import imgui.type.ImString;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import net.steelswing.tiledplus.gui.GuiEditorMain;
import net.steelswing.tiledplus.gui.ModalWindow;
import net.steelswing.tiledplus.layer.TileSet;
import net.steelswing.tiledplus.utils.Util;
import org.lwjgl.glfw.GLFW;

/**
 * File: ModalWindowImportTileset.java
 * Created on 2026 Feb 10, 17:54:16
 *
 * @author LWJGL2
 */
public class ModalWindowImportTileset extends ModalWindow {

    public ImString tileSetName = new ImString();
    public ImString imagePath = new ImString();


    public final ImInt tileWidth = new ImInt(16);
    public final ImInt tileHeight = new ImInt(16);
    public final GuiEditorMain editorMain;
//        public final ImInt margin = new ImInt(0);
//        public final ImInt spacing = new ImInt(0);

    public ModalWindowImportTileset(GuiEditorMain editorMain) {
        super("Import TileSet");
        this.editorMain = editorMain;
    }

    @Override
    public void open() {
        super.open();

        tileSetName.set("");
        imagePath.set("");

        tileWidth.set(16);
        tileHeight.set(16);
    }

    @Override
    public void render() {
        if (begin(720, 400)) {

            ImGui.text("Набор тайлов");
            ImGui.separator();

            // --- ИМЯ ---
            ImGui.text("Имя:");
            ImGui.sameLine(120);
            ImGui.pushItemWidth(-1);
            ImGui.inputText("##name", tileSetName);
            ImGui.popItemWidth();

//                // --- ТИП ---
//                ImGui.text("Тип:");
//                ImGui.sameLine(120);
//                ImGui.pushItemWidth(260);
//                if (ImGui.beginCombo("##type", "Основано на изображении набора тайлов")) {
//                    if (ImGui.selectable("Основано на изображении набора тайлов", tileType == 0)) {
//                        tileType = 0;
//                    }
//                    if (ImGui.selectable("Атлас текстур", tileType == 1)) {
//                        tileType = 1;
//                    }
//                    ImGui.endCombo();
//                }
//                ImGui.sameLine();
//                ImGui.checkbox("Включить в карту", addToMap);
//                ImGui.popItemWidth();
            ImGui.spacing();
            ImGui.separator();
            ImGui.text("Изображение");
            ImGui.separator();

            ImGui.text("Источник:");
            ImGui.sameLine(120);
            ImGui.pushItemWidth(-110);
            if (ImGui.inputText("##image", imagePath)) {

            }
            ImGui.popItemWidth();
            ImGui.sameLine();
            if (ImGui.button("Обзор...")) {

                try {
                    SwingUtilities.invokeAndWait(() -> {
                        JFrame frame = new JFrame();
                        frame.setAlwaysOnTop(true);
                        frame.setUndecorated(true);
                        frame.setLocationRelativeTo(null);
                        frame.setVisible(true);
                        frame.toFront();
                        frame.requestFocus();

                        JFileChooser fileChooser = new JFileChooser();
                        fileChooser.setCurrentDirectory(new File("."));
                        fileChooser.setAcceptAllFileFilterUsed(false);
                        try {
                            FileNameExtensionFilter filter = new FileNameExtensionFilter("Images (*.png)", "png");
                            fileChooser.setFileFilter(filter);

                            int result = fileChooser.showOpenDialog(null);

                            switch (result) {
                                case JFileChooser.APPROVE_OPTION:
                                    File selectedFile = fileChooser.getSelectedFile();
                                    imagePath.set(selectedFile.getAbsoluteFile());
                                    tileSetName.set(Util.getFileNameWithoutExtension(selectedFile.getName()));
                                    break;
                                case JFileChooser.CANCEL_OPTION:
                                    break;
                                case JFileChooser.ERROR_OPTION:
                                    break;
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

//                // --- ПРОЗРАЧНОСТЬ ---
//                ImGui.checkbox("Использовать цвет прозрачности", useTransparency);
//                ImGui.sameLine();
//                ImGui.colorButton("##color", transparentColor);
//
            ImGui.spacing();

            // --- РАЗМЕРЫ ---
//            ImGui.text("Ширина тайла:");
//            ImGui.sameLine(160);
//            ImGui.setNextItemWidth(150);
//            ImGui.inputInt("##w", tileWidth);

//                ImGui.sameLine(340);
//                ImGui.text("Отступ:");
//                ImGui.sameLine(480);
//                ImGui.setNextItemWidth(150);
//                ImGui.inputInt("##margin", margin);
//            ImGui.text("Высота тайла:");
//            ImGui.sameLine(160);
//            ImGui.setNextItemWidth(150);
//            ImGui.inputInt("##h", tileHeight);

//                ImGui.sameLine(340);
//                ImGui.text("Промежуток:");
//                ImGui.sameLine(480);
//                ImGui.setNextItemWidth(150);
//                ImGui.inputInt("##spacing", spacing);
            // --- КНОПКИ ---
            ImGui.spacing();
            ImGui.separator();

            float buttonWidth = 90;
            float posX = ImGui.getContentRegionAvailX() - buttonWidth * 2 - 10;
            ImGui.setCursorPosX(posX);

            if (ImGui.button("OK", buttonWidth, 0) || ImGui.isKeyPressed(GLFW.GLFW_KEY_ENTER)) {
                File file = new File(imagePath.get());
                if (file.exists()) {
                    System.out.println("Import tileset");
                    editorMain.editorSession.tileSets.add(new TileSet(tileSetName.get(), editorMain.editorSession.tileWidth,  editorMain.editorSession.tileHeight, file));
                    close();
                } else {

                }
            }

            ImGui.sameLine();

            if (ImGui.button("Cancel", buttonWidth, 0)) {
                close();
            }
            end();
        }
    }
}
