/*
 * Ну вы же понимаете, что код здесь только мой?
 * Well, you do understand that the code here is only mine?
 */
package net.steelswing.tiledplus.layer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    /**
     * Тайлы из паттерна, которые рендерятся последними (renderLast).
     * Хранится как "x,y" — координаты в тайлсете, как в tileCoords.
     */
    public Set<String> renderLastCoords = new HashSet<>();

    /**
     * Прямоугольники коллизий паттерна.
     * Координаты в пикселях, относительно левого верхнего угла паттерна.
     */
    public List<CollisionRect> collisionRects = new ArrayList<>();

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
        for (int[] c : tileCoords) {
            int ix = c[0] - minX;
            int iy = c[1] - minY;
            tiles[ix][iy] = tileSet.tiles[tileSet.index(c[0], c[1])];
        }
        return tiles;
    }

    /**
     * Возвращает true если тайл по координатам тайлсета помечен как renderLast.
     */
    public boolean isRenderLast(int tileX, int tileY) {
        return renderLastCoords.contains(tileX + "," + tileY);
    }

    public boolean isEmpty() {
        return tileCoords.isEmpty();
    }

    // ─── JSON serialization for collisionRects ──────────────────────────────

    public org.json.JSONArray collisionRectsToJSON() {
        org.json.JSONArray arr = new org.json.JSONArray();
        for (CollisionRect r : collisionRects) {
            arr.put(r.toJSON());
        }
        return arr;
    }

    public void collisionRectsFromJSON(org.json.JSONArray arr) {
        collisionRects.clear();
        for (int i = 0; i < arr.length(); i++) {
            collisionRects.add(CollisionRect.fromJSON(arr.getJSONObject(i)));
        }
    }
}