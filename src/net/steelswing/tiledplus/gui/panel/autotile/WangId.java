/*
 * Ну вы же понимаете, что код здесь только мой?
 * Well, you do understand that the code here is only mine?
 */

package net.steelswing.tiledplus.gui.panel.autotile;

/**
 * Represents a Wang tile ID encoding edge and corner colors.
 * <p>
 * Layout:
 * 7|0|1
 * 6|.|2
 * 5|4|3
 * <p>
 * Where even indices (0,2,4,6) are edges and odd indices (1,3,5,7) are corners.
 */
public class WangId {

    public static final int BITS_PER_INDEX = 8;
    public static final long INDEX_MASK = 0xFFL;
    public static final long FULL_MASK = 0xFFFFFFFFFFFFFFFFL;
    public static final int MAX_COLOR_COUNT = (1 << BITS_PER_INDEX) - 2;

    // Index constants
    public static final int TOP = 0;
    public static final int TOP_RIGHT = 1;
    public static final int RIGHT = 2;
    public static final int BOTTOM_RIGHT = 3;
    public static final int BOTTOM = 4;
    public static final int BOTTOM_LEFT = 5;
    public static final int LEFT = 6;
    public static final int TOP_LEFT = 7;

    public static final int NUM_CORNERS = 4;
    public static final int NUM_EDGES = 4;
    public static final int NUM_INDEXES = 8;

    // Masks
    public static final long MASK_TOP = INDEX_MASK << (BITS_PER_INDEX * TOP);
    public static final long MASK_TOP_RIGHT = INDEX_MASK << (BITS_PER_INDEX * TOP_RIGHT);
    public static final long MASK_RIGHT = INDEX_MASK << (BITS_PER_INDEX * RIGHT);
    public static final long MASK_BOTTOM_RIGHT = INDEX_MASK << (BITS_PER_INDEX * BOTTOM_RIGHT);
    public static final long MASK_BOTTOM = INDEX_MASK << (BITS_PER_INDEX * BOTTOM);
    public static final long MASK_BOTTOM_LEFT = INDEX_MASK << (BITS_PER_INDEX * BOTTOM_LEFT);
    public static final long MASK_LEFT = INDEX_MASK << (BITS_PER_INDEX * LEFT);
    public static final long MASK_TOP_LEFT = INDEX_MASK << (BITS_PER_INDEX * TOP_LEFT);

    public static final long MASK_TOP_SIDE = MASK_TOP_LEFT | MASK_TOP | MASK_TOP_RIGHT;
    public static final long MASK_RIGHT_SIDE = MASK_TOP_RIGHT | MASK_RIGHT | MASK_BOTTOM_RIGHT;
    public static final long MASK_BOTTOM_SIDE = MASK_BOTTOM_LEFT | MASK_BOTTOM | MASK_BOTTOM_RIGHT;
    public static final long MASK_LEFT_SIDE = MASK_TOP_LEFT | MASK_LEFT | MASK_BOTTOM_LEFT;

    public static final long MASK_EDGES = MASK_TOP | MASK_RIGHT | MASK_BOTTOM | MASK_LEFT;
    public static final long MASK_CORNERS = MASK_TOP_RIGHT | MASK_BOTTOM_RIGHT | MASK_BOTTOM_LEFT | MASK_TOP_LEFT;

    private long id;

    public WangId() {
        this(0);
    }

    public WangId(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public boolean isEmpty() {
        return id == 0;
    }

    /**
     * Returns the color of the edge at the given index.
     * Index 0 = top, 1 = right, 2 = bottom, 3 = left
     * <p>
     * |0|
     * 3|.|1
     * |2|
     */
    public int edgeColor(int index) {
        assert index >= 0 && index < NUM_EDGES;
        return indexColor(index * 2);
    }

    /**
     * Returns the color of the corner at the given index.
     * Index 0 = top right, 1 = bottom right, 2 = bottom left, 3 = top left
     * <p>
     * 3| |0
     * |.|
     * 2| |1
     */
    public int cornerColor(int index) {
        assert index >= 0 && index < NUM_CORNERS;
        return indexColor(index * 2 + 1);
    }

    /**
     * Returns the color at a certain index 0-7.
     * <p>
     * 7|0|1
     * 6|.|2
     * 5|4|3
     */
    public int indexColor(int index) {
        assert index >= 0 && index < NUM_INDEXES;
        return (int) ((id >> (index * BITS_PER_INDEX)) & INDEX_MASK);
    }

    public void setEdgeColor(int index, int value) {
        assert index >= 0 && index < NUM_EDGES;
        setIndexColor(index * 2, value);
    }

    public void setCornerColor(int index, int value) {
        assert index >= 0 && index < NUM_CORNERS;
        setIndexColor(index * 2 + 1, value);
    }

    /**
     * Sets the color of a certain grid index:
     * <p>
     * y
     * x 0|1|2
     * 1|.|.
     * 2|.|.
     */
    public void setGridColor(int x, int y, int value) {
        int index = indexByGrid(x, y);
        if (index < NUM_INDEXES) {
            setIndexColor(index, value);
        }
    }

    /**
     * Sets the color of a certain index 0-7.
     * <p>
     * 7|0|1
     * 6|.|2
     * 5|4|3
     */
    public void setIndexColor(int index, int value) {
        assert index >= 0 && index < NUM_INDEXES;
        id &= ~(INDEX_MASK << (index * BITS_PER_INDEX));
        id |= (long) (value & INDEX_MASK) << (index * BITS_PER_INDEX);
    }

    /**
     * Matches this WangId's edges/corners with an adjacent one.
     * Position 0-7 with 0 being top, and 7 being top left:
     * <p>
     * 7|0|1
     * 6|.|2
     * 5|4|3
     */
    public void updateToAdjacent(WangId adjacent, int position) {
        setIndexColor(position, adjacent.indexColor(oppositeIndex(position)));

        if (!isCorner(position)) {
            int cornerIndex = position / 2;
            setCornerColor(cornerIndex, adjacent.cornerColor((cornerIndex + 1) % NUM_CORNERS));
            setCornerColor((cornerIndex + 3) % NUM_CORNERS, adjacent.cornerColor((cornerIndex + 2) % NUM_CORNERS));
        }
    }

    public void mergeWith(WangId wangId, WangId mask) {
        this.id = (this.id & ~mask.id) | (wangId.id & mask.id);
    }

    /**
     * Returns true if one or more indexes have no color.
     */
    public boolean hasWildCards() {
        for (int i = 0; i < NUM_INDEXES; i++) {
            if (indexColor(i) == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if one or more corners have no color.
     */
    public boolean hasCornerWildCards() {
        for (int i = 0; i < NUM_CORNERS; i++) {
            if (cornerColor(i) == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if one or more edges have no color.
     */
    public boolean hasEdgeWildCards() {
        for (int i = 0; i < NUM_EDGES; i++) {
            if (edgeColor(i) == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a mask that is 0 for any indexes that have no color defined.
     */
    public WangId mask() {
        long mask = 0;
        for (int i = 0; i < NUM_INDEXES; i++) {
            if (indexColor(i) != 0) {
                mask |= INDEX_MASK << (i * BITS_PER_INDEX);
            }
        }
        return new WangId(mask);
    }

    /**
     * Returns a mask that is 0 for any indexes that don't match the given color.
     */
    public WangId mask(int value) {
        long mask = 0;
        for (int i = 0; i < NUM_INDEXES; i++) {
            if (indexColor(i) == value) {
                mask |= INDEX_MASK << (i * BITS_PER_INDEX);
            }
        }
        return new WangId(mask);
    }

    public boolean hasCornerWithColor(int value) {
        for (int i = 0; i < NUM_CORNERS; i++) {
            if (cornerColor(i) == value) {
                return true;
            }
        }
        return false;
    }

    public boolean hasEdgeWithColor(int value) {
        for (int i = 0; i < NUM_EDGES; i++) {
            if (edgeColor(i) == value) {
                return true;
            }
        }
        return false;
    }

    /**
     * Rotates the Wang ID clockwise by (90 * rotations) degrees.
     */
    public void rotate(int rotations) {
        this.id = rotated(rotations).id;
    }

    public WangId rotated(int rotations) {
        if (rotations < 0) {
            rotations = 4 + (rotations % 4);
        } else {
            rotations %= 4;
        }

        long rotated = id << (rotations * BITS_PER_INDEX * 2);
        rotated = rotated | (id >>> ((4 - rotations) * BITS_PER_INDEX * 2));

        return new WangId(rotated);
    }

    public void flipHorizontally() {
        this.id = flippedHorizontally().id;
    }

    public void flipVertically() {
        flipHorizontally();
        rotate(2);
    }

    public WangId flippedHorizontally() {
        WangId newWangId = new WangId(id);

        newWangId.setIndexColor(RIGHT, indexColor(LEFT));
        newWangId.setIndexColor(LEFT, indexColor(RIGHT));

        for (int i = 0; i < NUM_CORNERS; i++) {
            newWangId.setCornerColor(i, cornerColor(NUM_CORNERS - 1 - i));
        }

        return newWangId;
    }

    public WangId flippedVertically() {
        WangId newWangId = new WangId(id);
        newWangId.flipVertically();
        return newWangId;
    }

    public static int indexByGrid(int x, int y) {
        assert x >= 0 && x < 3;
        assert y >= 0 && y < 3;

        int[][] map = {
            {TOP_LEFT, TOP, TOP_RIGHT},
            {LEFT, NUM_INDEXES, RIGHT},
            {BOTTOM_LEFT, BOTTOM, BOTTOM_RIGHT}
        };

        return map[y][x];
    }

    public static int oppositeIndex(int index) {
        return (index + 4) % NUM_INDEXES;
    }

    public static int nextIndex(int index) {
        return (index + 1) % NUM_INDEXES;
    }

    public static int previousIndex(int index) {
        return (index + NUM_INDEXES - 1) % NUM_INDEXES;
    }

    public static boolean isCorner(int index) {
        return (index & 1) != 0;
    }

    /**
     * Creates a WangId based on a 32-bit value, which uses 4 bits per index.
     * Provided for compatibility.
     */
    public static WangId fromUint(int id) {
        long id64 = 0;
        for (int i = 0; i < NUM_INDEXES; i++) {
            long color = (id >> (i * 4)) & 0xF;
            id64 |= color << (i * BITS_PER_INDEX);
        }
        return new WangId(id64);
    }

    /**
     * Converts the WangId to a 32-bit value, using 4 bits per index.
     * Provided for compatibility.
     */
    public int toUint() {
        int result = 0;
        for (int i = 0; i < NUM_INDEXES; i++) {
            int color = (int) ((id >> (i * BITS_PER_INDEX)) & INDEX_MASK);
            result |= color << (i * 4);
        }
        return result;
    }

    public static WangId fromString(String string) {
        WangId id = new WangId();
        String[] parts = string.split(",");

        if (parts.length == NUM_INDEXES) {
            for (int i = 0; i < NUM_INDEXES; i++) {
                try {
                    int color = Integer.parseInt(parts[i].trim());

                    if (color > MAX_COLOR_COUNT) {
                        return new WangId();
                    }

                    id.setIndexColor(i, color);
                } catch (NumberFormatException e) {
                    return new WangId();
                }
            }
        }

        return id;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < NUM_INDEXES; i++) {
            if (i > 0) {
                result.append(',');
            }
            result.append(indexColor(i));
        }
        return result.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        WangId other = (WangId) obj;
        return id == other.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    public WangId and(long mask) {
        return new WangId(id & mask);
    }

    public void andEquals(long mask) {
        id &= mask;
    }
}
