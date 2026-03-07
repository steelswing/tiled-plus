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
    
    
    public void fromJSON(org.json.JSONArray array, java.util.List<net.steelswing.tiledplus.layer.TileSet> tileSets) {
        getPatterns().clear();
        for (int i = 0; i < array.length(); i++) {
            org.json.JSONObject obj = array.getJSONObject(i);
            String name = obj.getString("name");
            String tileSetName = obj.optString("tileSetName", "");

            TileSet ts = tileSets.stream()
                    .filter(t -> t.name.equals(tileSetName))
                    .findFirst().orElse(this);

            TilePattern pattern = new TilePattern(name, ts);

            org.json.JSONArray coords = obj.getJSONArray("tiles");
            for (int j = 0; j < coords.length(); j++) {
                org.json.JSONObject coord = coords.getJSONObject(j);
                pattern.tileCoords.add(new int[]{coord.getInt("x"), coord.getInt("y")});
            }

            getPatterns().add(pattern);
        }
    }
}
