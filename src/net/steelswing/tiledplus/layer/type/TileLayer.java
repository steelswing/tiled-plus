/*
 * Ну вы же понимаете, что код здесь только мой?
 * Well, you do understand that the code here is only mine?
 */

package net.steelswing.tiledplus.layer.type;

import net.steelswing.tiledplus.TiledPlus;
import net.steelswing.tiledplus.layer.Layer;
import net.steelswing.tiledplus.layer.LayerType;
import net.steelswing.tiledplus.layer.Tile;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * File: TileLayer.java
 * Created on 2026 Mar 7, 18:31:17
 *
 * @author LWJGL2
 */
public class TileLayer extends Layer {

    private static final String TAG = "TileLayer";

    public int[] buffer;

    public final int width, height;

    public TileLayer(int mapWidth, int mapHeight) {
        super(LayerType.TILESET, new JSONObject().put("name", ""));
        this.width = mapWidth;
        this.height = mapHeight;

        buffer = new int[width * height];
    }

    public TileLayer(JSONObject layer) {
        super(LayerType.TILESET, layer);
        JSONArray data = layer.getJSONArray("data");
        width = layer.getInt("width");
        height = layer.getInt("height");

        visible = layer.getBoolean("visible");

        buffer = new int[data.length()];
        for (int i = 0; i < data.length(); i++) {
            buffer[i] = data.getInt(i);
        }
//
//        for (int x = 0; x < width; x++) {
//            for (int y = 0; y < height; y++) {
//                final int id = data.getInt(index(x, y));
//                buffer[index(x, y)] = worldAtlas.get(id);
//            }
//        }

        System.out.println(TAG + " Loaded TileLayer " + width + "x" + height + " (" + layer.optString("name") + ")");
    }

//    @Override
//    public void render(WorldClient world, SpriteBatch batch) {
    ////        batch.setTranslation(0, 0, 0);
//        batch.setColor(1f, 1f, 1f, 1f);
////        batch.setTranslation(-world.renderOffsetX * world.scale, -world.renderOffsetY * world.scale, world.renderOffsetZ);
//        int displayWidth = KubaDrach.getWidth();
//        int displayHeight = KubaDrach.getWidth();
//
//        for (int x = 0; x < width; x++) {
//            for (int y = 0; y < height; y++) {
//                int tileId = buffer[index(x, y)];
//                if (tileId == 0) {
//                    continue;
//                }
////                batch.draw(tile.icon, (x * tileWidth), (y * tileHeight), tileWidth, tileHeight);
//            }
//        }
//    }
    
    public Tile get(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return null;
        }

        Tile[] allTiles = TiledPlus.getInstance().editorMain.editorSession.allTiles;

        int tileId = buffer[index(x, y)];
        if (tileId == 0) {
            return null;
        }
        if (tileId <= 0 || tileId >= allTiles.length) {
            return null;
        }
        Tile tile = allTiles[tileId];
        if (tile == null) {
            return null;
        }
        return tile;
    }

    public void set(int x, int y, Tile val) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return;
        }
        buffer[index(x, y)] = val == null ? 0 : val.id;
    }

    public int index(int x, int y) {
        return x + y * width;
    }
}
