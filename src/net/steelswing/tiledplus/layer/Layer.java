/*
 * Ну вы же понимаете, что код здесь только мой?
 * Well, you do understand that the code here is only mine?
 */

package net.steelswing.tiledplus.layer;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.json.JSONObject;

/**
 * File: Layer.java
 * Created on 2026 Feb 10, 03:38:28
 *
 * @author LWJGL2
 */
@Getter
@Setter
@Accessors(chain = true)
public abstract class Layer {

    public LayerType layerType;
    public String name;
    public boolean visible = true;
    public boolean locked = false;

    public Object userObject;

    public Layer(LayerType layerType, JSONObject layer) {
        this.layerType = layerType;
        this.name = layer.getString("name");
    }
}
