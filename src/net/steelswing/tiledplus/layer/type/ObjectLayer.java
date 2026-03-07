/*
 * Ну вы же понимаете, что код здесь только мой?
 * Well, you do understand that the code here is only mine?
 */

package net.steelswing.tiledplus.layer.type;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.steelswing.flopslop.util.Rect;
import net.steelswing.tiledplus.TiledPlus;
import net.steelswing.tiledplus.layer.Layer;
import net.steelswing.tiledplus.layer.LayerType;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * File: ObjectLayer.java
 * Created on 2025 Nov 9, 19:04:59
 *
 * @author LWJGL2
 */
@Getter
@Setter
@Accessors(chain = true)
public class ObjectLayer extends Layer {

    private static final String TAG = "ObjectLayer";

    public List<ObjectInfo> objects = new ArrayList<>();

    public ObjectLayer(JSONObject layer) {
        super(LayerType.OBJECTS, layer);
        final JSONArray array = layer.getJSONArray("objects");
        for (int i = 0; i < array.length(); i++) {
            JSONObject jo = array.getJSONObject(i);
            objects.add(new ObjectInfo(jo.getString("name"), jo.getBoolean("visible"), jo.getFloat("x"), jo.getFloat("y"), jo.getFloat("width"), jo.getFloat("height")));
        }
        TiledPlus.log(TAG, "Loaded ObjectLayer " + objects.size() + " (" + layer.optString("name") + ")");
//        if (layer.has("data")) {
//            JSONArray data = layer.getJSONArray("data");
//
//            final int width = layer.getInt("width");
//            final int height = layer.getInt("height");
//
//            TiledPlus.log(TAG, width + "x" + height);
//
//            for (int x = 0; x < width; x++) {
//                for (int y = 0; y < height; y++) {
//                    int index = x + y * width;
//                    if (data.getInt(index) != 0) {
//                        rects.add(new Rectangle(
//                                x * tileWidth,
//                                y * tileHeight,
//                                tileWidth,
//                                tileHeight
//                        ));
//                    }
//                }
//            }
//        }

//        TiledPlus.log(TAG, "Loaded ObjectLayer rects: " + rects.size());
    }

    @Getter
    @Setter
    @Accessors(chain = true)
    public static class ObjectInfo extends Rect {

        public String name;
        public boolean visible;

        public ObjectInfo(String name, boolean visible, float x, float y, float width, float height) {
            super(x, y, width, height);
            this.name = name;
            this.visible = visible;
        }

    }

//    public void render(WorldClient world, SpriteBatch batch) {
//        Texture2D white = world.getResourceLoader().textureWhite;
//
//        // force draw
//        batch.setColor(1f, 0, 0, 0.5f);
//        int displayWidth = KubaDrach.getWidth();
//        int displayHeight = KubaDrach.getWidth();
//
//        for (int i = 0; i < rects.size(); i++) {
//            Rectangle rect = rects.get(i);
//
//            float x = rect.x;
//            float y = rect.y;
//
//            batch.draw(white, x, y, rect.width, rect.height);
//        }
//    }
}
