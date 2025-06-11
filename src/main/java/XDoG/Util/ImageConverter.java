package XDoG.Util;

import java.awt.image.BufferedImage;

public class ImageConverter {
    public static class Result {
        public final float[][][] lab;
        public final float epsilon;
        
        /**
         * Holds the result of converting an image to OKLab floats.
         * 
         * @param lab 3D float matrix containing the image in OkLab.
         * @param epsilon Calculated epsilon value based on image range.
         */
        public Result(float[][][] lab, float epsilon) {
            this.lab = lab;
            this.epsilon = epsilon;
        }
    }
    
    /**
     * Convert a BufferedImage into a normalized OKLab array, keep alpha, and
     * update epsilon if requested.
     *
     * @param image Source image in ARGB format
     * @param epsilon Initial epsilon in range 0 to 100 for thresholding
     * @param useRange If true, compute a new epsilon based on the lightness
     * range in the image
     * @return a Result object containing:
     * - array lab: an array where element [y][x][0] is normalized alpha element 
     * [y][x][1] is lightness divided by 100 element [y][x][2] is normalized a 
     * channel element [y][x][3] is normalized b channel.
     * - float epsilon: the possibly adjusted threshold value.
     */
    public static Result imageToLabMatrix(BufferedImage image, float epsilon, boolean useRange) {
        int height = image.getHeight();
        int width = image.getWidth();
        float[][][] labImage = new float[height][width][4];
        
        float min = Float.POSITIVE_INFINITY, max = Float.NEGATIVE_INFINITY, avg = 0.0f;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int[] rgba = PixelConverter.toArray(image.getRGB(x, y));
                float[] normRgba = PixelConverter.toNormalized(rgba);
                
                // Preserve alpha
                labImage[y][x][0] = normRgba[0];

                // sRGB -> linear
                float lr = sRgbToLinear(normRgba[1]);
                float lg = sRgbToLinear(normRgba[2]);
                float lb = sRgbToLinear(normRgba[3]);

                // linear RGB -> XYZ
                float[] xyz = rgbToXyz(lr, lg, lb);

                // XYZ -> Lab
                float[] lab = xyzToLab(xyz[0], xyz[1], xyz[2]);

                // Normalize Lab: L/100, a/b from [-127,127] to [0,1]
                labImage[y][x][1] = lab[0] / 100.0f;
                labImage[y][x][2] = 0.5f + (lab[1] / 127.0f) * 0.5f;
                labImage[y][x][3] = 0.5f + (lab[2] / 127.0f) * 0.5f;
                
                min = Math.min(labImage[y][x][1] * 100, min);
                max = Math.max(labImage[y][x][1] * 100, max);
                avg += labImage[y][x][1] * 100;
            }
        }
        
        // Compute range based on image values
        avg /= (height * width);
        float d1 = avg - min, d2 = max - avg;
        float d = Math.min(d1, d2);
        
        min = avg - d;
        max = avg + d;
        
        float range = max - min;
        float normEps = epsilon / 100.0f;
        
        if (useRange) {
            epsilon = min + normEps * range;
        }
        
        return new Result(labImage, epsilon);
    }
    
    private static float sRgbToLinear(float c) {
        return (c <= 0.04045f) ? c / 12.92f : (float) Math.pow((c + 0.055f) / 1.055f, 2.4f);
    }
    
    private static float[] rgbToXyz(float r, float g, float b) {
        float x = r * 0.4124f + g * 0.3576f + b * 0.1805f;
        float y = r * 0.2126f + g * 0.7152f + b * 0.0722f;
        float z = r * 0.0193f + g * 0.1192f + b * 0.9505f;
        
        return new float[]{ x * 100.0f, y * 100.0f, z * 100.0f };
    }
    
    private static float[] xyzToLab(float x, float y, float z) {
        float xr = x / 95.047f;
        float yr = y / 100.000f;
        float zr = z / 108.883f;
        
        return new float[]{
            116.0f * fLab(yr) - 16.0f,
            500.0f * (fLab(xr) - fLab(yr)),
            200.0f * (fLab(yr) - fLab(zr))
        };
    }
    
    private static float fLab(float t) {
        return (t > 0.008856f) ? (float) Math.cbrt(t) : (7.787f * t + 16.0f/116.0f);
    }
    
    /**
     * Convert a two‑channel float array back into a grayscale BufferedImage.
     * Uses channel zero for alpha and channel one for gray value.
     *
     * @param image A float array with dimensions height, width, channels where
     * channel zero is alpha and channel one is gray level
     * @return A new BufferedImage in ARGB format where RGB channels all equal
     * the gray level
     */
    public static BufferedImage toBufferedImage(float[][][] image) {
        int height = image.length;
        int width = image[0].length;

        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float alphaNorm = image[y][x][0];
                float grayNorm = image[y][x][1];

                int a = Helper.clamp((int) Math.round(alphaNorm * 255.0), 0, 255);
                int g = Helper.clamp((int) Math.round(grayNorm * 255.0), 0, 255);

                int argb = (a << 24) | (g << 16) | (g << 8) | g;
                output.setRGB(x, y, argb);
            }
        }

        return output;
    }
}