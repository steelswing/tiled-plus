/*
 * Ну вы же понимаете, что код здесь только мой?
 * Well, you do understand that the code here is only mine?
 */

package net.steelswing.tiledplus.layer;

import java.io.File;
import java.io.FileInputStream;
import net.steelswing.tiledplus.layer.type.TileLayer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.steelswing.flopslop.render.texture.TextureFilter;
import net.steelswing.flopslop.render.texture.type.ImageData;
import net.steelswing.flopslop.render.texture.type.Texture2D;
import net.steelswing.tiledplus.TiledPlus;
import net.steelswing.tiledplus.layer.type.ObjectLayer;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * File: EditorSession.java
 * Created on 2026 Feb 10, 03:36:14
 *
 * @author LWJGL2
 */
@Getter
@Setter
@Accessors(chain = true)
public class EditorSession {

    public static String TAG = "EditorSession";

    public List<Layer> layers = new ArrayList<>();
    public List<TileSet> tileSets = new ArrayList<>();

    public int tileWidth = 16, tileHeight = 16;
    public int levelWidth = 64, levelHeight = 64;

    public Tile[] allTiles;
    protected Map<String, Texture2D> textures = new HashMap<>();

    public File baseDir;

    public void load(File baseDir, JSONObject rawMap) {
        layers.clear();
        tileSets.clear();
        for (Texture2D value : textures.values()) {
            value.destruct();
        }
        textures.clear();

        this.baseDir = baseDir;
        JSONArray layersRaw = rawMap.getJSONArray("layers");
        JSONArray tilesetsRaw = rawMap.getJSONArray("tilesets");

        levelWidth = rawMap.getInt("width");
        levelHeight = rawMap.getInt("height");

        tileWidth = rawMap.getInt("tilewidth");
        tileHeight = rawMap.getInt("tileheight");

        for (int i = 0; i < tilesetsRaw.length(); i++) {
            JSONObject tilesetInfo = tilesetsRaw.getJSONObject(i);
            TileSet tileSet = new TileSet();
            tileSet.name = tilesetInfo.optString("name");
            tileSet.firstgid = tilesetInfo.getInt("firstgid");
            tileSet.tileWidth = tilesetInfo.getInt("tilewidth");
            tileSet.tileHeight = tilesetInfo.getInt("tileheight");
            tileSet.imagePath = tilesetInfo.getString("image");
            tileSet.columns = tilesetInfo.getInt("columns");
            tileSet.tilesCount = tilesetInfo.getInt("tilecount");
            tileSet.tiles = new Tile[tileSet.tilesCount];
            for (int index = 0; index < tileSet.tiles.length; index++) {
                tileSet.tiles[index] = new Tile(index + 1);
            }

            tileSets.add(tileSet);
        }

        for (int i = 0; i < layersRaw.length(); i++) {
            JSONObject layerInfo = (JSONObject) layersRaw.get(i);
            String type = layerInfo.getString("type");
            String name = layerInfo.getString("name");

            if (type.equals("tilelayer")) {
                final TileLayer tileLayer = new TileLayer(layerInfo);
                this.layers.add(tileLayer);
            } else if (type.equals("objectgroup")) {
//                if (name.contains("collision")) {
//                    this.layers.add(new CollisionLayer(layerInfo, atlas));
//                }
//                if (name.contains("objects")) {
                this.layers.add(new ObjectLayer(layerInfo));
//                }
            } else {
                System.out.println("Unknown layer type: " + type + " name: " + name);
            }
//            if (type.equals("tilelayer")) {
//                this.layers.add(new TileLayer(layerInfo));
//            }
        }
        Collections.reverse(layers);

        System.out.println("Loaded layers: " + this.layers.size());

        prepareForRender();
    }

    public static float padding = 0.0f;

    public void prepareForRender() {
        int sumTileCount = 0;
        for (TileSet tileSet : tileSets) {
            TiledPlus.log(TAG, "Compute tile set " + tileSet.name);
            tileSet.file = new File(baseDir, tileSet.imagePath);

            try {
                tileSet.atlasTexture = new Texture2D(ImageData.ofAwt(new FileInputStream(tileSet.file)), TextureFilter.NEAREST_NO_MIP_MAPS);
                tileSet.atlasTexture.loadTexture();
                tileSet.atlasTexture.uploadTextureData();
                textures.put(tileSet.imagePath, tileSet.atlasTexture);

                tileSet.init(tileSet.atlasTexture);
            } catch (Exception e) {
                e.printStackTrace();
            }

            Texture2D atlas = tileSet.atlasTexture;

            final int tileW = atlas.getWidth() / tileSet.tileWidth;
            final int tileH = atlas.getHeight() / tileSet.tileHeight;

            TiledPlus.log(TAG, "\tAtlas size: " + atlas.getWidth() + "x" + atlas.getHeight());
            TiledPlus.log(TAG, "\tTiles count: " + tileSet.tilesCount);

            final int cols = tileW;
            final int rows = tileH;
            final int tileWidth = tileSet.tileWidth;
            final int tileHeight = tileSet.tileHeight;

            int counter = 0;

            final Tile[] tiles = tileSet.tiles;
            for (int y = 0; y < rows; y++) {
                for (int x = 0; x < cols; x++) {
                    int id = counter++;
                    tiles[id].icon = atlas.subImage(x * tileWidth + padding, y * tileHeight + padding, tileWidth - padding * 2, tileHeight - padding * 2);
                }
            }

            sumTileCount += tileSet.tilesCount;
        }
        TiledPlus.log(TAG, "Summary " + sumTileCount);

        // Найти максимальный GID
        int maxGid = 0;
        for (TileSet tileSet : tileSets) {
            int lastGid = tileSet.firstgid + tileSet.tilesCount - 1;
            if (lastGid > maxGid) {
                maxGid = lastGid;
            }
        }

        allTiles = new Tile[maxGid + 1];
        for (TileSet tileSet : tileSets) {
            for (int i = 0; i < tileSet.tiles.length; i++) {
                int gid = tileSet.firstgid + i;
                allTiles[gid] = tileSet.tiles[i];
                tileSet.tiles[i].id = gid; // <-- tile.id должен быть GID!
            }
        }

//        allTiles = new Tile[sumTileCount + 1];
//        int nextId = 0;
//        for (TileSet tileSet : tileSets) {
//            for (int i = 0; i < tileSet.tiles.length; i++) {
//                allTiles[++nextId] = tileSet.tiles[i];
//            }
//        }
    }


}
