/*
Ну вы же понимаете, что код здесь только мой?
Well, you do understand that the code here is only mine?
 */

package net.steelswing.tiledplus.gui;

import imgui.internal.ImGui;
import imgui.type.ImInt;
import java.util.HashMap;
import lombok.Getter;
import net.steelswing.engine.api.util.interfaces.Renderable;

/**
 * File: DockMenuBase.java
 * Created on 2026 Feb 10, 03:17:51
 *
 * @author LWJGL2
 */
@Getter
public abstract class DockMenuBase implements Renderable {

    protected final String title;

    protected int dockMenuDir;
    protected float dockMenuWidth, dockMenuHeight, sizeRatioForNodeAtDir;
    
    public int dockNodeId;

    /**
     * 
     * @param title
     * @param dockMenuDir example: ImGuiDir.Left
     * @param dockMenuWidth
     * @param dockMenuHeight
     * @param sizeRatioForNodeAtDir 
     */
    public DockMenuBase(String title, int dockMenuDir, float dockMenuWidth, float dockMenuHeight, float sizeRatioForNodeAtDir) {
        this.title = title;
        this.dockMenuDir = dockMenuDir;
        this.dockMenuWidth = dockMenuWidth;
        this.dockMenuHeight = dockMenuHeight;
        this.sizeRatioForNodeAtDir = sizeRatioForNodeAtDir;
    }

    public DockMenuBase(String title, int dockMenuDir, float dockMenuWidth, float dockMenuHeight) {
        this(title, dockMenuDir, dockMenuWidth, dockMenuHeight, 0.25f);
    }

    public int setupDockSpace(int dockspaceId, int prevNodeId, ImInt out, HashMap<Class<?>, DockMenuBase> dockPanels) {
        final int nodeId = ImGui.dockBuilderSplitNode(prevNodeId, dockMenuDir, sizeRatioForNodeAtDir, null, out);
        ImGui.dockBuilderDockWindow(getTitle(), nodeId);
        ImGui.dockBuilderSetNodeSize(nodeId, dockMenuWidth, dockMenuHeight);
        return nodeId;
    }
}
