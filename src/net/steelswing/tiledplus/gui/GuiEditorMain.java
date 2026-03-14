/*
 * Ну вы же понимаете, что код здесь только мой?
 * Well, you do understand that the code here is only mine?
 */

package net.steelswing.tiledplus.gui;

import net.steelswing.tiledplus.gui.modal.ModalWindowDelete;
import imgui.ImGuiViewport;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import imgui.internal.ImGui;
import imgui.internal.flag.ImGuiDockNodeFlags;
import imgui.type.ImInt;
import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.LinkedHashMap;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import net.steelswing.engine.api.util.interfaces.Renderable;
import net.steelswing.tiledplus.gui.modal.ModalRename;
import net.steelswing.tiledplus.gui.modal.ModalTileSetPatterns;
import net.steelswing.tiledplus.gui.modal.ModalWindowImportTileset;
import net.steelswing.tiledplus.gui.panel.autotile.GuiPanelAutotile;
import net.steelswing.tiledplus.gui.panel.canvas.GuiPanelCanvas;
import net.steelswing.tiledplus.gui.panel.layers.GuiPanelLayers;
import net.steelswing.tiledplus.gui.panel.tilesets.GuiPanelTileSets;
import net.steelswing.tiledplus.layer.EditorSession;
import org.json.JSONObject;

/**
 * File: GuiEditorMain.java
 * Created on 2026 Feb 10, 03:12:46
 *
 * @author LWJGL2
 */
public class GuiEditorMain implements Renderable {

    protected boolean isBottomDockedWindowInit;
    private final HashMap<Class<?>, DockMenuBase> dockPanels = new LinkedHashMap<>();

    public GuiPanelLayers guiPanelLayers;
    public GuiPanelTileSets guiPanelTileSets;
    public GuiPanelCanvas guiPanelCanvas;
    public GuiPanelAutotile guiPanelAutotile;

    public EditorSession editorSession = new EditorSession();

    public ModalWindowDelete modalDelete = new ModalWindowDelete();
    public ModalWindowImportTileset modalImportTileSets = new ModalWindowImportTileset(this);
    public ModalTileSetPatterns modalTileSetPatterns = new ModalTileSetPatterns();

    public ModalRename modalRename = new ModalRename();

    protected Runnable onMainThread;

    private static final String LAST_FILE_PREF_KEY = "last_file_path";
    private static final java.util.prefs.Preferences PREFS =
            java.util.prefs.Preferences.userNodeForPackage(GuiEditorMain.class);


    public GuiEditorMain() {
        dockPanels.put(GuiPanelLayers.class, guiPanelLayers = new GuiPanelLayers(this));
//        dockPanels.put(GuiPanelAutotile.class, guiPanelAutotile = new GuiPanelAutotile(this));
        dockPanels.put(GuiPanelTileSets.class, guiPanelTileSets = new GuiPanelTileSets(this));
        dockPanels.put(GuiPanelCanvas.class, guiPanelCanvas = new GuiPanelCanvas(this));
    }

    @Override
    public void render() {
        if (onMainThread != null) {
            onMainThread.run();
            onMainThread = null;
        }
        int dockspaceId = ImGui.getID("MyDockSpace");
        showDockSpace(dockspaceId);

        if (ImGui.beginMainMenuBar()) {
            if (ImGui.beginMenu("File")) {
                if (ImGui.menuItem("Load")) {
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

                            // Восстанавливаем последний путь
                            String lastPath = PREFS.get(LAST_FILE_PREF_KEY, null);
                            if (lastPath != null) {
                                File lastFile = new File(lastPath);
                                if (lastFile.exists()) {
                                    fileChooser.setSelectedFile(lastFile);
                                    fileChooser.setCurrentDirectory(lastFile.getParentFile());
                                } else {
                                    fileChooser.setCurrentDirectory(new File("."));
                                }
                            } else {
                                fileChooser.setCurrentDirectory(new File("."));
                            }

                            fileChooser.setAcceptAllFileFilterUsed(false);
                            FileNameExtensionFilter filter = new FileNameExtensionFilter("Tiled JSON (*.json)", "json");
                            fileChooser.setFileFilter(filter);

                            int result = fileChooser.showOpenDialog(null);
                            switch (result) {
                                case JFileChooser.APPROVE_OPTION:
                                    File selectedFile = fileChooser.getSelectedFile();
                                    // Сохраняем путь для следующего раза
                                    PREFS.put(LAST_FILE_PREF_KEY, selectedFile.getAbsolutePath());
                                    onMainThread = () -> {
                                        try {
                                            editorSession.load(
                                                    selectedFile.getParentFile(),
                                                    new JSONObject(new String(Files.readAllBytes(selectedFile.toPath())))
                                            );
                                        } catch (Throwable ex) {
                                            System.getLogger(GuiEditorMain.class.getName())
                                                    .log(System.Logger.Level.ERROR, (String) null, ex);
                                        }
                                    };
                                    break;
                                case JFileChooser.CANCEL_OPTION:
                                case JFileChooser.ERROR_OPTION:
                                    break;
                            }

                            frame.dispose();
                        });
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
                ImGui.endMenu();
            }
            ImGui.endMainMenuBar();
        }

        if (!isBottomDockedWindowInit) {
            ImGui.dockBuilderRemoveNode(dockspaceId);
            ImGui.dockBuilderAddNode(dockspaceId, ImGuiDockNodeFlags.DockSpace);
            {
                ImInt out = new ImInt(dockspaceId);
                for (DockMenuBase value : dockPanels.values()) {
                    try {
                        value.dockNodeId = value.setupDockSpace(dockspaceId, out.get(), out, dockPanels);
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            }

            ImGui.dockBuilderFinish(dockspaceId);
        }

        dockPanels.values().forEach(renderable -> {
            renderable.render();
        });

        if (!isBottomDockedWindowInit) {
//            ImGui.setTabItemClosed(getDockPanelByClass(ActiveTileMenu.class).getTitle());
            isBottomDockedWindowInit = true;
        }
        modalImportTileSets.render();
        modalDelete.render();
        modalTileSetPatterns.render();
        modalTileSetPatterns.collisionEditor.render();

        modalRename.render();
    }

    private void showDockSpace(final int dockspaceId) {
        final ImGuiViewport mainViewport = ImGui.getMainViewport();
        final int windowFlags = ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoBringToFrontOnFocus | ImGuiWindowFlags.NoNavFocus | ImGuiWindowFlags.NoBackground;
        int toolbarHeight = 0;
        ImGui.setNextWindowPos(mainViewport.getWorkPosX(), mainViewport.getWorkPosY() + toolbarHeight);
        ImGui.setNextWindowSize(mainViewport.getWorkSizeX(), mainViewport.getWorkSizeY() - toolbarHeight);
        ImGui.setNextWindowViewport(mainViewport.getID());
        ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 0);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowBorderSize, 0);

        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0, 0);
        if (ImGui.begin("Dockspace", windowFlags)) {
            ImGui.popStyleVar(3);
            ImGui.dockSpace(dockspaceId, 0, 0, ImGuiDockNodeFlags.PassthruCentralNode);
            ImGui.end();
        }
    }
}
