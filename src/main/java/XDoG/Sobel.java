package XDoG;

import XDoG.Util.Helper;

public class Sobel {
    // Horizontal Sobel kernel
    private static final float[][] kx = {
        {-1, 0, 1},
        {-2, 0, 2},
        {-1, 0, 1}
    };
    
    // Vertical Sobel kernel
    private static final float[][] ky = {
        {-1, -2, -1},
        {0, 0, 0},
        {1, 2, 1}
    };
    
    /**
     * Compute the structure tensor for each pixel in a lab image.
     *
     * The structure tensor components are:
     * E = sum over color channels of (gradient x)^2
     * F = sum over color channels of (gradient x times gradient y)
     * G = sum over color channels of (gradient y)^2
     *
     * @param labImage a height by width by channel array containing normalized
     * lab data. Channel zero is preserved alpha and channels one, two, three
     * are L, a, b.
     * @return a height by width by three array where the third dimension holds
     * E, F, and G.
     */
    public static float[][][] computeStructureTensor(float[][][] labImage) {
        int height = labImage.length;
        int width = labImage[0].length;
        float[][][] tensor = new float[height][width][3];

        float[][][] gx = new float[3][height][width];
        float[][][] gy = new float[3][height][width];
        
        // Compute horizontal and vertical gradients for each of the three color channels
        for (int c = 0; c < 3; c++) {
            gx[c] = applySobelKernel(labImage, c + 1, kx);
            gy[c] = applySobelKernel(labImage, c + 1, ky);
        }

        // Combine gradients into structure tensor components
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float E = 0, F = 0, G = 0;
                
                for (int c = 0; c < 3; c++) {
                    float gxc = gx[c][y][x];
                    float gyc = gy[c][y][x];
                    E += gxc * gxc;
                    F += gxc * gyc;
                    G += gyc * gyc;
                }
                
                tensor[y][x][0] = E;
                tensor[y][x][1] = F;
                tensor[y][x][2] = G;
            }
        }
        
        return tensor;
    }
    
    private static float[][] applySobelKernel(float[][][] labImage, int channel, float[][] kernel) {
        int height = labImage.length;
        int width = labImage[0].length;
        float[][] out = new float[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float sum = 0;

                for (int j = -1; j <= 1; j++) {
                    for (int i = -1; i <= 1; i++) {
                        int yy = Helper.clamp(y + j, 0, height - 1);
                        int xx = Helper.clamp(x + i, 0, width - 1);

                        sum += kernel[j + 1][i + 1] * labImage[yy][xx][channel];
                    }
                }

                out[y][x] = sum;
            }
        }

        return out;
    }
}