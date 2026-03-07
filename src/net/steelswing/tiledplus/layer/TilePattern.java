/*
Ну вы же понимаете, что код здесь только мой?
Well, you do understand that the code here is only mine?
 */

package net.steelswing.tiledplus.layer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.steelswing.tiledplus.layer.Tile;
import net.steelswing.tiledplus.layer.TileSet;

/**
 * File: TilePattern.java
 * Created on 2026 Mar 7, 23:16:17
 *
 * @author LWJGL2
 */
public class TilePattern {

    public String name;
    public List<int[]> tileCoords = new ArrayList<>(); // [tileX, tileY] в тайлсете
    public TileSet tileSet;

    public TilePattern(String name, TileSet tileSet) {
        this.name = name;
        this.tileSet = tileSet;
    }

    public Tile[][] toTileArray() {
        if (tileCoords.isEmpty()) {
            return null;
        }
        int minX = tileCoords.stream().mapToInt(c -> c[0]).min().getAsInt();
        int maxX = tileCoords.stream().mapToInt(c -> c[0]).max().getAsInt();
        int minY = tileCoords.stream().mapToInt(c -> c[1]).min().getAsInt();
        int maxY = tileCoords.stream().mapToInt(c -> c[1]).max().getAsInt();
        int w = maxX - minX + 1;
        int h = maxY - minY + 1;
        Tile[][] tiles = new Tile[w][h];
        Set<String> coordSet = new HashSet<>();
        for (int[] c : tileCoords) {
            coordSet.add(c[0] + "," + c[1]);
        }
        for (int[] c : tileCoords) {
            int ix = c[0] - minX;
            int iy = c[1] - minY;
            tiles[ix][iy] = tileSet.tiles[tileSet.index(c[0], c[1])];
        }
        return tiles;
    }

    public boolean isEmpty() {
        return tileCoords.isEmpty();
    }
}
