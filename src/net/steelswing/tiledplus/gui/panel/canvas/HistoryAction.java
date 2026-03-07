/*
 * Ну вы же понимаете, что код здесь только мой?
 * Well, you do understand that the code here is only mine?
 */

package net.steelswing.tiledplus.gui.panel.canvas;

import java.util.ArrayList;
import java.util.List;
import net.steelswing.tiledplus.layer.Tile;
import net.steelswing.tiledplus.layer.type.TileLayer;

/**
 * Класс для хранения одного действия в истории
 */
// HistoryAction.java
public class HistoryAction {

    public final List<TileChange> changes = new ArrayList<>();

    public void addChange(TileLayer layer, int x, int y, Tile oldTile, Tile newTile) {
        changes.add(new TileChange(layer, x, y, oldTile, newTile));
    }

    public boolean isEmpty() {
        return changes.isEmpty();
    }
}
