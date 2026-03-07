/*
 * Ну вы же понимаете, что код здесь только мой?
 * Well, you do understand that the code here is only mine?
 */

package net.steelswing.tiledplus.gui.panel.canvas;

import net.steelswing.tiledplus.layer.Tile;
import net.steelswing.tiledplus.layer.type.TileLayer;

/**
 * Класс для хранения изменения одного тайла
 */
// TileChange.java
public class TileChange {

    public final TileLayer layer;
    public final int x, y;
    public final Tile oldTile, newTile;

    public TileChange(TileLayer layer, int x, int y, Tile oldTile, Tile newTile) {
        this.layer = layer;
        this.x = x;
        this.y = y;
        this.oldTile = oldTile;
        this.newTile = newTile;
    }
}
