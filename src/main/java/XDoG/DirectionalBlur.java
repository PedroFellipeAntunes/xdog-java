package XDoG;

import XDoG.Util.Helper;

public class DirectionalBlur {
    /**
     * Calculate a two dimensional vector map from the structure tensor.
     *
     * For each pixel, solve for the second eigenvector of the 2 by 2 tensor
     * matrix:
     * [ E F ]
     * [ F G ]
     * 
     * Uses the smaller eigenvalue to find the direction of least change.
     *
     * @param tensor A height by width by three array holding tensor components:
     * index zero is E, index one is F, index two is G
     * @return A height by width by two array where each element is a unit
     * vector [vx, vy]
     */
    public static float[][][] computeVectorMap(float[][][] tensor) {
        int height = tensor.length;
        int width = tensor[0].length;
        float[][][] vectorMap = new float[height][width][2];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float E = tensor[y][x][0];
                float F = tensor[y][x][1];
                float G = tensor[y][x][3];

                // Compute trace and determinant for the tensor matrix
                float trace = E + G;
                float det = E * G - F * F;
                // Discriminant of the characteristic polynomial
                float disc = (float) Math.sqrt(Math.max(0.0f, trace * trace - 4 * det));

                float lambda1 = (trace + disc) * 0.5f;
                float lambda2 = (trace - disc) * 0.5f;

                // Use the smaller eigenvalue
                float lambda = lambda2;

                float vx = F;
                float vy = lambda - E;

                // Handle the degenerate case where both components are effectively zero
                if (Math.abs(F) < 1e-12 && Math.abs(lambda - E) < 1e-12) {
                    vx = lambda - G;
                    vy = F;
                }

                // Normalize to unit length
                float norm = (float) Math.hypot(vx, vy);
                
                if (norm > 1e-12) {
                    vx /= norm;
                    vy /= norm;
                } else {
                    // Fallback direction if normalization fails
                    vx = 1.0f;
                    vy = 0.0f;
                }

                vectorMap[y][x][0] = vx;
                vectorMap[y][x][1] = vy;
            }
        }

        return vectorMap;
    }
    
    public static class Result {
        public final float[][][] output;
        public final float[][][] vectorMap;
        
        /**
         * Holds the output of the directional blur filter.
         * 
         * @param output Output image array with two channels: alpha and blurred 
         * lightness
         * @param vectorMap The vector map used for blur directions
         */
        public Result(float[][][] output, float[][][] vectorMap) {
            this.output = output;
            this.vectorMap = vectorMap;
        }
    }
    
    /**
     * Apply the full directional blur filter to an image.
     *
     * For each pixel, blur along lines orthogonal to the edge vector at two
     * scales, then subtract the scaled responses to produce an edge enhanced
     * result.
     *
     * @param image A height by width by two array where channel zero is alpha
     * and channel one is normalized lightness
     * @param tensor Height by width by three tensor array (E, F, G)
     * @param sigmaE Blur sigma for the first scale (edge scale)
     * @param k Scaling factor between the two blur scales
     * @param tau Weighting factor for combining the two blur results
     * @return A Result object containing the filtered image and the vector map
     */
    public static Result applyFilter(
            float[][][] image,
            float[][][] tensor,
            float sigmaE,
            float k,
            float tau
    ) {
        int height = image.length;
        int width = image[0].length;
        
        float[][][] output = new float[height][width][2];
        float[][][] vectorMap = new float[height][width][2];

        float[] kernel1 = Helper.createGaussianKernel1D(sigmaE);
        float[] kernel2 = Helper.createGaussianKernel1D(sigmaE * k);
        
        int r1 = kernel1.length / 2;
        int r2 = kernel2.length / 2;
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                output[y][x][0] = image[y][x][0];
                
                vectorMap[y][x] = computeEigenvectorLambda2(tensor[y][x]);
                
                // Compute orthogonal direction
                float dx = vectorMap[y][x][0], dy = vectorMap[y][x][1];
                float vx = -dy, vy = dx;

                // Apply directional blur at both scales
                float blur1 = directionalBlurAt(image, x, y, vx, vy, kernel1, r1);
                float blur2 = directionalBlurAt(image, x, y, vx, vy, kernel2, r2);

                // Scale values
                float b1 = blur1 * (1.0f + tau);
                float b2 = blur2 * tau;

                // Subtract to enhance edges
                float Lout = b1 - b2;
                
                output[y][x][1] = Lout;
            }
        }

        return new Result(output, vectorMap);
    }
    
    private static float[] computeEigenvectorLambda2(float[] t) {
        float E = t[0], F = t[1], G = t[2];
        float trace = E + G;
        float det = E * G - F * F;
        float disc = (float) Math.sqrt(Math.max(0.0f, trace * trace - 4 * det));
        float lambda2 = 0.5f * (trace - disc);

        float vx = lambda2 - G;
        float vy = F;

        float norm = (float) Math.hypot(vx, vy);
        
        if (norm > 1e-12) {
            vx /= norm;
            vy /= norm;
        } else {
            vx = 1.0f;
            vy = 0.0f;
        }
        
        return new float[]{vx, vy};
    }

    private static float directionalBlurAt(
            float[][][] img,
            int x, int y,
            float vx, float vy,
            float[] kernel,
            int r
    ) {
        int height = img.length, width = img[0].length;
        float sum = 0;
        
        // Iterate along the line centered at the pixel
        for (int i = -r; i <= r; i++) {
            float sx = x + i * vx;
            float sy = y + i * vy;
            
            int ix = Helper.clamp((int) Math.round(sx), 0, width - 1);
            int iy = Helper.clamp((int) Math.round(sy), 0, height - 1);
            
            // Accumulate weighted lightness channel
            sum += img[iy][ix][1] * kernel[i + r];
        }
        
        return sum;
    }
}