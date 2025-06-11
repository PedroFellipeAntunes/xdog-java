package XDoG.Util;

public class Helper {
    /**
     * Create a normalized one dimensional Gaussian kernel.
     *
     * The kernel radius is computed as the ceiling of 2.45 times the sigma
     * value, which captures over 98 percent of the Gaussian area. The resulting
     * kernel has a length of radius times two plus one, and is normalized so
     * that the sum of all entries equals one.
     *
     * @param sigmaC The standard deviation of the Gaussian distribution;
     * controls the spread of the blur.
     * @return A float array containing the normalized kernel weights.
     */
    public static float[] createGaussianKernel1D(float sigmaC) {
        int radius = (int) Math.ceil(2.45 * sigmaC);
        int size = radius * 2 + 1;
        float[] kernel = new float[size];
        
        // Precompute factors for the Gaussian formula
        float sigma2 = sigmaC * sigmaC;
        float norm = (float) (1.0 / (Math.sqrt(2 * Math.PI) * sigmaC));

        float sum = 0.0f;
        
        for (int i = -radius; i <= radius; i++) {
            float x = i;
            // Gaussian formula: norm * exp(-x^2 / (2 sigma^2))
            float value = (float) (norm * Math.exp(-(x * x) / (2 * sigma2)));
            
            kernel[i + radius] = value;
            sum += value;
        }

        // Normalize so all weights add up to one
        for (int i = 0; i < size; i++) {
            kernel[i] /= sum;
        }

        return kernel;
    }
    
    /**
     * Clamp an integer value to lie within a specified range.
     *
     * If the value is below the minimum, returns the minimum. If the value is
     * above the maximum, returns the maximum. Otherwise returns the original
     * value.
     *
     * @param value The input value to clamp.
     * @param min The lower bound.
     * @param max The upper bound.
     * @return The clamped value.
     */
    public static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }

        if (value > max) {
            return max;
        }

        return value;
    }
}