/*
 * Ну вы же понимаете, что код здесь только мой?
 * Well, you do understand that the code here is only mine?
 */

package net.steelswing.tiledplus.layer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.steelswing.flopslop.render.texture.type.Texture2D;

/**
 * File: TileSet.java
 * Created on 2026 Feb 10, 03:39:02
 *
 * @author LWJGL2
 */
@Getter
@Setter
@Accessors(chain = true)
public class TileSet {

    public String name;
    public File file;
    public Texture2D atlasTexture;

    public Tile[] tiles;
    public String imagePath;

    public int columns;
    public int firstgid;
    public int tileWidth, tileHeight;

    public int tilesCount;

    public Object userObject;

    public List<TilePattern> patterns = new ArrayList<>();

    public TileSet() {
    }

    public TileSet(String name, int tileWidth, int tileHeight, File file) {
        this.name = name;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        this.file = file;
    }

    public int index(int x, int y) {
        return y * columns + x;
    }

    public void init(Texture2D texture) {
    }


    public org.json.JSONObject toJSON() {
        org.json.JSONObject root = new org.json.JSONObject();
        root.put("tileSetName", this.name);

        org.json.JSONArray array = new org.json.JSONArray();
        for (TilePattern pattern : getPatterns()) {
            org.json.JSONObject obj = new org.json.JSONObject();
            obj.put("name", pattern.name);

            if (!pattern.tileCoords.isEmpty()) {
                int minX = pattern.tileCoords.stream().mapToInt(c -> c[0]).min().getAsInt();
                int maxX = pattern.tileCoords.stream().mapToInt(c -> c[0]).max().getAsInt();
                int minY = pattern.tileCoords.stream().mapToInt(c -> c[1]).min().getAsInt();
                int maxY = pattern.tileCoords.stream().mapToInt(c -> c[1]).max().getAsInt();

                int w = maxX - minX + 1;
                int h = maxY - minY + 1;

                obj.put("width", w);
                obj.put("height", h);
                obj.put("offsetX", minX);
                obj.put("offsetY", minY);

                // flat array row by row
                org.json.JSONArray flat = new org.json.JSONArray();
                java.util.Map<String, Integer> coordToTileId = new java.util.HashMap<>();
                for (int[] c : pattern.tileCoords) {
                    Tile tile = pattern.tileSet != null ? pattern.tileSet.tiles[pattern.tileSet.index(c[0], c[1])] : null;
                    coordToTileId.put(c[0] + "," + c[1], tile != null ? tile.id : 0);
                }
                for (int row = 0; row < h; row++) {
                    for (int col = 0; col < w; col++) {
                        String key = (minX + col) + "," + (minY + row);
                        flat.put(coordToTileId.getOrDefault(key, 0));
                    }
                }
                obj.put("pattern", flat);

                // renderLast — flat boolean array, true если тайл помечен renderLast
                org.json.JSONArray renderLastFlat = new org.json.JSONArray();
                for (int row = 0; row < h; row++) {
                    for (int col = 0; col < w; col++) {
                        String key = (minX + col) + "," + (minY + row);
                        renderLastFlat.put(pattern.renderLastCoords.contains(key));
                    }
                }
                obj.put("renderLast", renderLastFlat);

                // вычисляем renderLastW и renderLastH — размер области где есть true
                int renderLastW = 0;
                int renderLastH = 0;
                for (int row = 0; row < h; row++) {
                    for (int col = 0; col < w; col++) {
                        String key = (minX + col) + "," + (minY + row);
                        if (pattern.renderLastCoords.contains(key)) {
                            if (col + 1 > renderLastW) {
                                renderLastW = col + 1;
                            }
                            if (row + 1 > renderLastH) {
                                renderLastH = row + 1;
                            }
                        }
                    }
                }
                obj.put("renderLastW", renderLastW);
                obj.put("renderLastH", renderLastH);

                // Сериализация прямоугольников коллизий
                obj.put("collisionRects", pattern.collisionRectsToJSON());

            } else {
                obj.put("width", 0);
                obj.put("height", 0);
                obj.put("offsetX", 0);
                obj.put("offsetY", 0);
                obj.put("pattern", new org.json.JSONArray());
                obj.put("renderLast", new org.json.JSONArray());
                obj.put("renderLastW", 0);
                obj.put("renderLastH", 0);
            }

            array.put(obj);
        }

        root.put("patterns", array);
        return root;
    }

    public void fromJSON(org.json.JSONObject root, java.util.List<TileSet> tileSets) {
        getPatterns().clear();
        org.json.JSONArray array = root.getJSONArray("patterns");
        for (int i = 0; i < array.length(); i++) {
            org.json.JSONObject obj = array.getJSONObject(i);
            String name = obj.getString("name");

            TilePattern pattern = new TilePattern(name, this);

            int w = obj.getInt("width");
            int h = obj.getInt("height");
            int offsetX = obj.getInt("offsetX");
            int offsetY = obj.getInt("offsetY");
            org.json.JSONArray flat = obj.getJSONArray("pattern");

            // renderLast — опциональный массив для обратной совместимости
            org.json.JSONArray renderLastFlat = obj.has("renderLast") ? obj.getJSONArray("renderLast") : null;

            for (int row = 0; row < h; row++) {
                for (int col = 0; col < w; col++) {
                    int idx = row * w + col;
                    int tileId = flat.getInt(idx);
                    if (tileId != 0) {
                        pattern.tileCoords.add(new int[]{offsetX + col, offsetY + row});
                    }
                    // renderLast
                    if (renderLastFlat != null && idx < renderLastFlat.length()) {
                        if (renderLastFlat.getBoolean(idx)) {
                            pattern.renderLastCoords.add((offsetX + col) + "," + (offsetY + row));
                        }
                    }
                }
            }

            if (obj.has("collisionRects")) {
                pattern.collisionRectsFromJSON(obj.getJSONArray("collisionRects"));
            }

            getPatterns().add(pattern);
        }
    }
}
