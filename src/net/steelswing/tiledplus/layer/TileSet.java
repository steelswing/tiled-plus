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


    public void fromJSON(org.json.JSONObject root, java.util.List<TileSet> tileSets) {
        getPatterns().clear();
        org.json.JSONArray array = root.getJSONArray("patterns");
        for (int i = 0; i < array.length(); i++) {
            org.json.JSONObject obj = array.getJSONObject(i);
            String name = obj.getString("name");

            TilePattern pattern = new TilePattern(name, this);

            org.json.JSONArray coords = obj.getJSONArray("tiles");
            for (int j = 0; j < coords.length(); j++) {
                org.json.JSONObject coord = coords.getJSONObject(j);
                pattern.tileCoords.add(new int[]{coord.getInt("x"), coord.getInt("y")});
            }

            getPatterns().add(pattern);
        }
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

                java.util.Map<String, Integer> coordToTileId = new java.util.HashMap<>();
                for (int[] c : pattern.tileCoords) {
                    Tile tile = pattern.tileSet != null ? pattern.tileSet.tiles[pattern.tileSet.index(c[0], c[1])] : null;
                    coordToTileId.put(c[0] + "," + c[1], tile != null ? tile.id : 0);
                }

                org.json.JSONArray rows = new org.json.JSONArray();
                for (int row = 0; row < h; row++) {
                    org.json.JSONArray rowArr = new org.json.JSONArray();
                    for (int col = 0; col < w; col++) {
                        String key = (minX + col) + "," + (minY + row);
                        rowArr.put(coordToTileId.getOrDefault(key, 0));
                    }
                    rows.put(rowArr);
                }
                obj.put("pattern", rows);

                org.json.JSONArray coords = new org.json.JSONArray();
                for (int[] c : pattern.tileCoords) {
                    Tile tile = pattern.tileSet != null ? pattern.tileSet.tiles[pattern.tileSet.index(c[0], c[1])] : null;
                    org.json.JSONObject coord = new org.json.JSONObject();
                    coord.put("x", c[0]);
                    coord.put("y", c[1]);
                    coord.put("tileId", tile != null ? tile.id : 0);
                    coords.put(coord);
                }
                obj.put("tiles", coords);
            } else {
                obj.put("width", 0);
                obj.put("height", 0);
                obj.put("pattern", new org.json.JSONArray());
                obj.put("tiles", new org.json.JSONArray());
            }

            array.put(obj);
        }

        root.put("patterns", array);
        return root;
    }
}
