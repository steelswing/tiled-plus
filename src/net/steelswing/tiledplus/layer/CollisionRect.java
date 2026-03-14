/*
 * Ну вы же понимаете, что код здесь только мой?
 * Well, you do understand that the code here is only mine?
 */
package net.steelswing.tiledplus.layer;

/**
 * File: CollisionRect.java
 * Collision rectangle for a TilePattern, stored in pixel coords relative to pattern origin.
 * x, y, w, h are in pixels (snapped to grid).
 *
 * @author LWJGL2
 */
public class CollisionRect {

    /** Position and size in pixels, relative to pattern top-left */
    public float x, y, w, h;

    public CollisionRect(float x, float y, float w, float h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public CollisionRect copy() {
        return new CollisionRect(x, y, w, h);
    }

    public org.json.JSONObject toJSON() {
        org.json.JSONObject o = new org.json.JSONObject();
        o.put("x", x);
        o.put("y", y);
        o.put("w", w);
        o.put("h", h);
        return o;
    }

    public static CollisionRect fromJSON(org.json.JSONObject o) {
        return new CollisionRect(
                (float) o.getDouble("x"),
                (float) o.getDouble("y"),
                (float) o.getDouble("w"),
                (float) o.getDouble("h")
        );
    }
}