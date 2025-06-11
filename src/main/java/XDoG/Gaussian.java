package XDoG;

import XDoG.Util.Helper;

public class Gaussian {
    /**
     * Applies a two pass Gaussian blur to the input image.
     *
     * First pass performs horizontal convolution, second pass performs vertical
     * convolution. If the blur sigma is zero or negative, the original image is
     * returned without modification.
     *
     * @param image A three dimensional float array with dimensions
     * [height][width][channels] containing the image data to blur
     * @param sigmaC The standard deviation of the Gaussian kernel; controls
     * blur radius
     * @return A new three dimensional float array of the same shape containing
     * the blurred image data
     */
    public static float[][][] gaussianBlur(float[][][] image, float sigmaC) {
        if (sigmaC <= 0) {
            return image;
        }

        int height = image.length;
        int width = image[0].length;
        int channels = image[0][0].length;

        float[] kernel = Helper.createGaussianKernel1D(sigmaC);
        int radius = kernel.length / 2;

        float[][][] temp = new float[height][width][channels];
        
        // First pass: horizontal blur into a temporary buffer
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                for (int c = 0; c < channels; c++) {
                    float sum = 0;
                    
                    for (int k = -radius; k <= radius; k++) {
                        int xx = Helper.clamp(x + k, 0, width - 1);
                        sum += image[y][xx][c] * kernel[k + radius];
                    }
                    
                    temp[y][x][c] = sum;
                }
            }
        }

        float[][][] output = new float[height][width][channels];
        
        // Second pass: vertical blur from the temporary buffer into the output array
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                for (int c = 0; c < channels; c++) {
                    float sum = 0;
                    
                    for (int k = -radius; k <= radius; k++) {
                        int yy = Helper.clamp(y + k, 0, height - 1);
                        sum += temp[yy][x][c] * kernel[k + radius];
                    }
                    
                    output[y][x][c] = sum;
                }
            }
        }

        return output;
    }
}