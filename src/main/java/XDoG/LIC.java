package XDoG;

import XDoG.Util.Helper;
import XDoG.Util.PostProcessingOptions;

public class LIC {
    /**
     * Apply the LIC filter to an image.
     *
     * @param image [height][width][2] array: [0] = alpha, [1] = L of OkLab
     * @param vectorMap [height][width][2] array of direction vectors
     * @param sigma Gaussian blur σ
     * @param opts Post‑processing mode (NONE, THRESHOLD, or QUANTIZATION)
     * @return [height][width][2] output image
     */
    public static float[][][] apply(
            float[][][] image,
            float[][][] vectorMap,
            float sigma,
            PostProcessingOptions opts
    ) {
        int height = image.length, width = image[0].length;
        float[][][] out = new float[height][width][2];

        float[] kernel = Helper.createGaussianKernel1D(sigma);
        int r = kernel.length / 2;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                out[y][x][0] = image[y][x][0];
                
                float val = computeLicAtPixel(image, vectorMap, x, y, kernel, r);
                
                out[y][x][1] = opts.apply(val);
            }
        }
        
        return out;
    }

    // ——— Core LIC kernel sweep —————————————————————————

    private static float computeLicAtPixel(
            float[][][] img,
            float[][][] vmap,
            int sx, int sy,
            float[] kernel,
            int r
    ) {
        int height = img.length, width = img[0].length;
        float sum = kernel[r] * img[sy][sx][1];
        float step = 1.0f;

        for (int dir : new int[]{-1, +1}) {
            float px = sx, py = sy;
            
            for (int k = 1; k <= r; k++) {
                int cx = Helper.clamp(Math.round(px), 0, width - 1);
                int cy = Helper.clamp(Math.round(py), 0, height - 1);

                float vx = vmap[cy][cx][0];
                float vy = vmap[cy][cx][1];
                
                px += dir * vx * step;
                py += dir * vy * step;

                int sx2 = Helper.clamp(Math.round(px), 0, width - 1);
                int sy2 = Helper.clamp(Math.round(py), 0, height - 1);
                
                sum += kernel[r + dir * k] * img[sy2][sx2][1];
            }
        }
        
        return sum;
    }
}