package XDoG.Util;

import java.awt.Color;

public class PixelConverter {
    /**
     * Breaks a packed ARGB integer into its individual components.
     *
     * @param rgba A single integer containing alpha, red, green, blue in ARGB
     * format
     * @return An int array of length four where element zero is alpha, element
     * one is red, element two is green, element three is blue
     */
    public static int[] toArray(int rgba) {
        Color c = new Color(rgba, true);
        
        int[] array = new int[4];
        
        array[0] = c.getAlpha();
        array[1] = c.getRed();
        array[2] = c.getGreen();
        array[3] = c.getBlue();
        
        return array;
    }
    
    /**
     * Converts an integer array of RGBA components into normalized floats.
     *
     * Each int component is clamped between zero and 255, then divided by 255
     * to produce a float between zero and one.
     *
     * @param rgba An array of integer components, typically length four for
     * alpha, red, green, blue
     * @return A float array of same length, each value in the range zero to one
     */
    public static float[] toNormalized(int[] rgba) {
        float[] normRgba = new float[rgba.length];
        
        for (int i = 0; i < rgba.length; i++) {
            normRgba[i] = Math.max(0, Math.min(255, rgba[i]));
            normRgba[i] /= 255.0;
        }
        
        return normRgba;
    }
    
    /**
     * Converts a single integer color component into a normalized float.
     *
     * The input is clamped between zero and 255 and then divided by 255
     * to produce a float in the range zero to one.
     *
     * @param c An integer component, for example red, green, blue or alpha
     * @return A float value between zero and one representing the normalized component
     */
    public static float toNormalized(int c) {
        float normC = Math.max(0, Math.min(255, c));
        
        return normC /= 255.0;
    }
}