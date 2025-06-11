package XDoG.Util;

public abstract class PostProcessingOptions {
    public enum Mode { None, Threshold, Quantization }
    
    /**
     * Factory method to select the appropriate options subclass based on the
     * chosen mode and parameters.
     *
     * @param mode The post‑processing mode to use
     * @param epsilon Threshold cutoff in percent for threshold mode
     * @param phi Sharpness factor for threshold mode
     * @param levels Number of discrete levels for quantization mode
     * @return An instance of the corresponding PostProcessingOptions subclass
     * @throws IllegalArgumentException if an unknown mode is provided
     */
    public static PostProcessingOptions fromMode(
            Mode mode, float epsilon, float phi, int levels
    ) {
        return switch (mode) {
            case None -> new NoneOptions();
            case Threshold -> new ThresholdOptions(epsilon, phi);
            case Quantization -> new QuantizationOptions(levels);
            default -> throw new IllegalArgumentException("Unknown Mode: " + mode);
        };
    }
    
    /**
     * Apply the post‑processing function to a normalized input value.
     *
     * @param value A float in the zero to one range (typically lightness or
     * filter output)
     * @return The adjusted value after applying the chosen post‑processing
     */
    public abstract float apply(float value);

    // --- Private subclasses implementing each mode ---------------------------

    private static class NoneOptions extends PostProcessingOptions {
        @Override
        public float apply(float value) {
            return value;
        }
    }
    
    private static class ThresholdOptions extends PostProcessingOptions {
        private final float epsilon, phi;
        
        ThresholdOptions(float epsilon, float phi) {
            this.epsilon = epsilon;
            this.phi = phi;
        }
        
        @Override
        public float apply(float v) {
            float pct = v * 100f;
            
            if (pct >= epsilon) return 1.0f;
            
            return 1.0f + (float)Math.tanh(phi * (pct - epsilon));
        }
    }

    private static class QuantizationOptions extends PostProcessingOptions {
        private final int levels;
        
        QuantizationOptions(int levels) {
            this.levels = levels;
        }
        
        @Override
        public float apply(float v) {
            if (levels <= 1) {
                return 0f;
            }
            
            float c = Math.max(0f, Math.min(1f, v));
            int s = Math.round(c * (levels - 1));

            return s / (float) (levels - 1);
        }
    }
}