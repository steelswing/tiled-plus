/*
 * Ну вы же понимаете, что код здесь только мой?
 * Well, you do understand that the code here is only mine?
 */

package net.steelswing.tiledplus;

import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.UIManager;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.steelswing.tiledplus.gui.GuiEditorMain;
import net.steelswing.tiledplus.imgui.ImGuImp;
import net.steelswing.tiledplus.utils.SwingUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL21;

/**
 * File: TiledPlus.java
 * Created on 2026 Feb 10, 02:43:12
 *
 * @author LWJGL2
 */
@Getter
@Setter
@Accessors(chain = true)
public class TiledPlus extends Window {

    protected static TiledPlus instance;

    protected ImGuImp imGuImp = new ImGuImp();

    public GuiEditorMain editorMain = new GuiEditorMain();

    @Override
    public void init() {
        instance = this;
        GLFW.glfwSetWindowRefreshCallback(getWindowHandle(), (window) -> loop(false));
        imGuImp.initImgui(windowHandle);
    }

    @Override
    public void fixedUpdate() {
    }

    @Override
    public void render() {
        GL21.glViewport(0, 0, width, height);
        GL21.glClearColor(0, 0, 0, 0);
        GL21.glClear(GL21.GL_COLOR_BUFFER_BIT | GL21.GL_DEPTH_BUFFER_BIT);

        if (imGuImp.startImgui()) {
            {
                editorMain.render();
            }
            imGuImp.endImgui();
        }
    }

    @Override
    public void destruct() {
    }

    public static void main(String[] args) throws Throwable {
        try {
            UIManager.setLookAndFeel(FlatDarkLaf.class.getName());
        } catch (Throwable e) {
            SwingUtils.showErrorDialog("Failed to set theme", e);
        }
        System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tF %1$tT] [%4$-7s] %5$s %n");
        new TiledPlus().create();
    }

    public static TiledPlus getInstance() {
        return instance;
    }

    public static void log(String TAG, String string) {
        System.out.println(TAG + " " + string);
    }
}
