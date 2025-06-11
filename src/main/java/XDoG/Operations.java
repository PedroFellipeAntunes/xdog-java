package XDoG;

import XDoG.Util.ImageConverter;
import FileManager.PngReader;
import FileManager.PngSaver;
import Windows.ImageViewer;
import XDoG.Util.PostProcessingOptions;
import XDoG.Util.PostProcessingOptions.Mode;

import java.awt.image.BufferedImage;
import java.io.IOException;

public class Operations {
    // Default
    private final float k = 1.6f;
    
    // Configuration parameters
    public float sigmaC; // Tensor-blur sigma (controls scale of edge detection)
    public float sigmaE; // Edge-blur sigma (controls edge orthogonal blur)
    public float sigmaM; // Sigma for first LIC pass
    public float tau; // Scaler for blurred images
    public float phi; // Sharpening exponent
    public float epsilon; // Dynamic threshold factor computed from image
    public float sigmaA; // Sigma for anti-aliasing LIC pass
    private final int levels; // Number of quantization levels
    
    private final boolean useRange;
    
    private final PostProcessingOptions.Mode type;
    
    // Controls for batch processing
    public boolean skip = false;
    public boolean save = true;
    
    /**
     * Constructs an Operations instance with all configurable parameters.
     *
     * @param sigmaC Gaussian blur sigma for structure tensor smoothing
     * @param sigmaE Sigma for directional (edge-aligned) blur
     * @param sigmaM Sigma for first Line Integral Convolution pass
     * @param tau Scaler for DoG negative
     * @param phi Exponent for quantization/sharpening
     * @param sigmaA Sigma for anti-aliasing pass in LIC
     * @param levels Number of quantization levels in post-processing
     * @param useRange If true, compute epsilon dynamically based on image range
     * @param type Post-processing mode (e.g., Threshold, Quant, None)
     */
    public Operations(
            float sigmaC, 
            float sigmaE, 
            float sigmaM, 
            float tau, 
            float phi, 
            float sigmaA, 
            int levels, 
            boolean useRange, 
            PostProcessingOptions.Mode type) {
        this.sigmaC = sigmaC;
        this.sigmaE = sigmaE;
        this.sigmaM = sigmaM;
        this.tau = tau;
        this.phi = phi;
        this.sigmaA = sigmaA;
        this.levels = levels;
        
        this.useRange = useRange;
        this.type = type;
    }
    
    /**
     * Starts the full XDoG processing pipeline on the input image at filePath.
     * Depending on the 'skip' and 'save' flags, it may display in a viewer or
     * directly save the output.
     *
     * @param filePath Path to the PNG file to read and process.
     * @throws IOException If reading or saving the image fails.
     */
    public void startProcess(String filePath) throws IOException {
        // 1) Read the original image from disk
        final BufferedImage original = measure("Reading image", () -> readImage(filePath));
        
        // 2) Convert to OkLab color space and optionally compute dynamic epsilon
        ImageConverter.Result resultImageToLab = measure("Converting to OkLab matrix", () -> 
                ImageConverter.imageToLabMatrix(original, epsilon, useRange)
        );
        
        final float[][][] labNormalImage = resultImageToLab.lab;
        epsilon = resultImageToLab.epsilon; // Update epsilon if dynamically computed
        
        // 3) Compute the structure tensor (edge orientation info) from Lab image
        final float[][][] tensor = measure("Computing tensor", () -> 
                Sobel.computeStructureTensor(labNormalImage)
        );
        
        // 4) Smooth the tensor with a Gaussian blur to reduce noise
        final float[][][] blurTensor = measure("Blurring tensor", () -> 
                Gaussian.gaussianBlur(tensor, sigmaC)
        );
        
        // 5) Apply directional (orthogonal) blur along edge orientations, then scale by tau and subtract to enhance edges.
        DirectionalBlur.Result resultDirectionBlur = measure("Applying ortogonal blur along edges", () -> 
                DirectionalBlur.applyFilter(labNormalImage, blurTensor, sigmaE, k, tau)
        );
        final float[][][] negative = resultDirectionBlur.output;
        final float[][][] vectorMap = resultDirectionBlur.vectorMap;

        // 6) First Line Integral Convolution pass with threshold/quantization
        PostProcessingOptions opts = PostProcessingOptions.fromMode(type, epsilon, phi, levels);
        
        final float[][][] LIC_1 = measure("LIC + Threshold/Quant", ()
                -> LIC.apply(negative, vectorMap, sigmaM, opts)
        );
        
        // 7) Second LIC pass for anti-aliasing (no thresholding here)
        PostProcessingOptions opts_none = PostProcessingOptions.fromMode(Mode.None, epsilon, phi, levels);
        
        final float[][][] LIC_2 = measure("LIC Anti-Alias", ()
                -> LIC.apply(LIC_1, vectorMap, sigmaA, opts_none)
        );
        
        // 8) Convert the final float matrix back to a BufferedImage
        final BufferedImage finalImage = measure("Converting back to image", () -> 
                ImageConverter.toBufferedImage(LIC_2)
        );
        
        // If skipping display, optionally save and exit
        if (skip) {
            System.out.println("- Display skip");
            
            if (save) {
                saveImage(finalImage, filePath);
            }
            
            return;
        }
        
        // Otherwise, open a window to show the result and then save if configured
        System.out.println("- Displaying result");
        new ImageViewer(finalImage, filePath, this);
        
        System.out.println("FINISHED PROCESS\n");
    }
    
    private BufferedImage readImage(String path) {
        return new PngReader().readPNG(path, true);
    }
    
    /**
     * Saves the processed image to disk with an informative filename.
     *
     * @param image The BufferedImage to save.
     * @param filePath The original file path used as a base for naming.
     */
    public void saveImage(BufferedImage image, String filePath) {
        PngSaver saver = new PngSaver();
        
        String prefix;
        
        if (type == Mode.Threshold) {
            prefix = String.format("XDoG[" + type + "," + useRange + "]");
        } else {
            prefix = String.format("XDoG[" + type + "]");
        }
        
        saver.saveToFile(prefix, filePath, image);
    }

    // ——— Timing helper methods ——————————————————————————————

    private <T> T measure(String label, Timeable<T> action) {
        Runtime rt = Runtime.getRuntime();
        System.gc();

        long startTime = System.currentTimeMillis();
        long beforeMem = rt.totalMemory() - rt.freeMemory();
        System.out.println("- " + label + " START");

        T result = action.execute();

        long afterMem = rt.totalMemory() - rt.freeMemory();
        long duration = System.currentTimeMillis() - startTime;
        double deltaMB = (afterMem - beforeMem) / 1024.0 / 1024.0;
        double totalMB = afterMem / 1024.0 / 1024.0;

        System.out.printf("- %s time: %dms | Δmem: %.2f MB | total: %.2f MB%n",
                label, duration, deltaMB, totalMB);

        return result;
    }

    @FunctionalInterface
    private interface Timeable<T> {
        T execute();
    }
}