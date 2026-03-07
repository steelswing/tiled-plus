/*
 * Ну вы же понимаете, что код здесь только мой?
 * Well, you do understand that the code here is only mine?
 */

package net.steelswing.tiledplus.layer;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import net.steelswing.flopslop.render.texture.type.Texture2D;

/**
 * File: Tile.java
 * Created on 2026 Feb 10, 22:49:02
 *
 * @author LWJGL2
 */
@Getter
@Setter
@Accessors(chain = true)
@ToString
public class Tile {

    public int id;
    public Texture2D icon;

    public Tile(int id) {
        this.id = id;
    }
}
