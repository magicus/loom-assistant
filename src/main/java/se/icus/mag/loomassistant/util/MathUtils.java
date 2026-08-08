/*
 * Copyright © Magnus Ihse Bursie 2026.
 * This file is released under MIT. See LICENSE for full license details.
 */
package se.icus.mag.loomassistant.util;

public class MathUtils {
    public static boolean isIn(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    public static <T> int indexOf(T[] array, T object) {
        for (int i = 0; i < array.length; i++) if (array[i] == object) return i;
        return 0;
    }
}
