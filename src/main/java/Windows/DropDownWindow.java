package Windows;

import XDoG.Operations;
import XDoG.Util.PostProcessingOptions.Mode;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.io.File;
import java.io.IOException;
import java.text.*;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.border.*;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.text.*;

public class DropDownWindow {
    // Interface components
    private JFrame frame;
    private JLabel dropLabel;
    private JPanel setsPanel;
    private JPanel rightSide;

    // Parameter input fields panel
    private JComboBox<Mode> typeComboBox;
    private JPanel parametersPanel;
    private JPanel bottomPanel;
    private boolean processAllSets = false;
    private JToggleButton processAllButton;
    private final JFormattedTextField[] parameterFields = new JFormattedTextField[8];

    // Parameter sets
    private AbstractButton selectedButton;
    private int selectedSet = 2;
    private final float[][] defaultValues = new float[][]{
        {2.28f, 1.4f, 4.4f, 21.7f, 0.017f, 79.5f, 1.0f},
        {2.45f, 1.0f, 6.0f, 18.0f, 0.60f, 82.2f, 1.0f},
        {2.97f, 1.4f, 13.2f, 18.2f, 10.3f, 73.1f, 1.95f},
        {3.76f, 1.4f, 2.2f, 15.7f, 0.49f, 78.3f, 2.4f},
        {5.84f, 0.8f, 3.2f, 120.0f, 0.083f, 72.6f, 0.75f},
        {0.10f, 2.0f, 20.0f, 40.0f, 0.01f, 100.0f, 7.2f},
        {0.10f, 6.8f, 20.0f, 70.0f, 0.01f, 80.0f, 0.6f},
        {4.16f, 1.4f, 12.0f, 22.0f, 0.09f, 88.0f, 4.0f},
        {4.16f, 1.4f, 12.0f, 22.0f, 3.42f, 79.0f, 4.0f}
    };
    private float[] values;

    // Other states
    private int levels = 3;
    private boolean useRange = false;
    private Mode type = Mode.Threshold;
    
    private boolean loading = false;
    private final Font defaultFont = UIManager.getDefaults().getFont("Label.font");

    public DropDownWindow() {
        // load initial values from set 3
        values = defaultValues[selectedSet].clone();

        initFrame();
        initDropLabel();
        initTypeComboBox();
        initBottomPanel();
        initRightSidePanel();
        finalizeFrame();
    }

    private void initFrame() {
        frame = new JFrame("XDoG");
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
    }

    private void initDropLabel() {
        dropLabel = new JLabel("Drop IMAGE files here", SwingConstants.CENTER);
        dropLabel.setPreferredSize(new Dimension(300, 200));
        dropLabel.setBorder(BorderFactory.createLineBorder(Color.WHITE));
        dropLabel.setForeground(Color.WHITE);
        dropLabel.setOpaque(true);
        dropLabel.setBackground(Color.BLACK);
        dropLabel.setTransferHandler(createTransferHandler());
        frame.add(dropLabel, BorderLayout.CENTER);
    }

    private void initBottomPanel() {
        bottomPanel = new JPanel();
        bottomPanel.setBackground(Color.BLACK);
        bottomPanel.setBorder(BorderFactory.createLineBorder(Color.WHITE));
        bottomPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 10));

        JButton toggleRange = new JButton("Dynamic Range");
        setButtonVisuals(toggleRange, Color.BLACK, Color.WHITE, 140, 30);
        
        toggleRange.addActionListener(e -> {
            useRange = !useRange;
            updateToggleButtonVisual(toggleRange);
        });

        bottomPanel.add(toggleRange);
        frame.add(bottomPanel, BorderLayout.SOUTH);
    }

    private void initTypeComboBox() {
        typeComboBox = new JComboBox<>(Mode.values());
        typeComboBox.setSelectedItem(type);
        typeComboBox.setBackground(Color.BLACK);
        typeComboBox.setForeground(Color.WHITE);
        typeComboBox.setBorder(BorderFactory.createLineBorder(Color.WHITE));
        
        typeComboBox.addActionListener(e -> {
            if (!loading) {
                type = (Mode) typeComboBox.getSelectedItem();
                SwingUtilities.invokeLater(() -> typeComboBox.requestFocusInWindow());
            }
        });
        
        customizeComboBoxUI(typeComboBox);

        JPanel comboPanel = new JPanel(new BorderLayout());
        comboPanel.setBackground(Color.BLACK);
        comboPanel.add(typeComboBox, BorderLayout.CENTER);
        
        frame.add(comboPanel, BorderLayout.NORTH);
    }
    
    private void initRightSidePanel() {
        JPanel topButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        topButtons.setBackground(Color.BLACK);

        processAllButton = new JToggleButton("Process All Defaults");
        setButtonVisuals(processAllButton, Color.BLACK, Color.WHITE, 160, 25);
        
        processAllButton.addActionListener(e -> {
            processAllSets = !processAllSets;
            updateToggleButtonVisual(processAllButton);
        });
        
        topButtons.add(processAllButton);

        rightSide = new JPanel();
        rightSide.setLayout(new BoxLayout(rightSide, BoxLayout.Y_AXIS));
        rightSide.setBackground(Color.BLACK);
        rightSide.setBorder(BorderFactory.createLineBorder(Color.WHITE));

        rightSide.add(topButtons);
        initSetsPanel();
        rightSide.add(setsPanel);
        initParametersPanel();
        rightSide.add(parametersPanel);

        frame.add(rightSide, BorderLayout.EAST);
    }
    
    private void initSetsPanel() {
        setsPanel = new JPanel(new GridLayout(3, 3, 3, 3));
        Border margin = new EmptyBorder(10, 10, 10, 10);
        
        TitledBorder titled = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.WHITE),
                "Defaults",
                TitledBorder.CENTER,
                TitledBorder.TOP,
                defaultFont,
                Color.WHITE
        );

        setsPanel.setBorder(BorderFactory.createCompoundBorder(titled, margin));
        setsPanel.setOpaque(false);

        ButtonGroup bg = new ButtonGroup();
        
        for (int i = 0; i < defaultValues.length; i++) {
            final int idx = i;
            JToggleButton btn = new JToggleButton("" + (i + 1));
            
            setButtonVisuals(btn, Color.BLACK, Color.WHITE, 20, 20);
            
            if (i == selectedSet) {
                updateToggleButtonVisual(btn);
                selectedButton = btn;
            }
            
            btn.addActionListener(e -> {
                updateToggleButtonVisual(selectedButton); // Update the previous
                
                selectedButton = btn;
                switchParamSet(idx);
                updateToggleButtonVisual(selectedButton); // Update the new one
            });
            
            bg.add(btn);
            
            setsPanel.add(btn);
        }
    }

    private void initParametersPanel() {
        // formatter setup
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator(',');
        symbols.setGroupingSeparator('.');
        
        DecimalFormat fmt = new DecimalFormat();
        fmt.setDecimalFormatSymbols(symbols);
        fmt.setGroupingUsed(false);
        
        NumberFormatter floatFmt = new NumberFormatter(fmt);
        floatFmt.setValueClass(Float.class);
        floatFmt.setAllowsInvalid(true);
        floatFmt.setOverwriteMode(true);
        floatFmt.setCommitsOnValidEdit(true);
        DefaultFormatterFactory floatFactory = new DefaultFormatterFactory(floatFmt);

        NumberFormatter intFmt = new NumberFormatter(NumberFormat.getIntegerInstance());
        intFmt.setValueClass(Integer.class);
        intFmt.setAllowsInvalid(false);
        intFmt.setCommitsOnValidEdit(true);
        DefaultFormatterFactory intFactory = new DefaultFormatterFactory(intFmt);

        parametersPanel = new JPanel();
        parametersPanel.setBackground(Color.BLACK);
        Border margin = new EmptyBorder(10, 10, 10, 10);
        
        TitledBorder titled = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.WHITE),
                "Parameters",
                TitledBorder.CENTER,
                TitledBorder.TOP,
                defaultFont,
                Color.WHITE
        );

        parametersPanel.setBorder(BorderFactory.createCompoundBorder(titled, margin));
        parametersPanel.setLayout(new BoxLayout(parametersPanel, BoxLayout.Y_AXIS));

        // parameter fields
        parameterFields[0] = createParamPanel("sigmaC:", values[0], floatFactory, v -> values[0] = v.floatValue());
        parameterFields[1] = createParamPanel("sigmaE:", values[1], floatFactory, v -> values[1] = v.floatValue());
        parameterFields[2] = createParamPanel("sigmaM:", values[2], floatFactory, v -> values[2] = v.floatValue());
        parameterFields[3] = createParamPanel("tau:", values[3], floatFactory, v -> values[3] = v.floatValue());
        parameterFields[4] = createParamPanel("phi:", values[4], floatFactory, v -> values[4] = v.floatValue());
        parameterFields[5] = createParamPanel("epsilon:", values[5], floatFactory, v -> values[5] = v.floatValue());
        parameterFields[6] = createParamPanel("sigmaA:", values[6], floatFactory, v -> values[6] = v.floatValue());
        parameterFields[7] = createParamPanel("levels:", (Number) levels, intFactory, v -> levels = v.intValue());

        // reset button
        JButton resetBtn = new JButton("Reset");
        setButtonVisuals(resetBtn, Color.BLACK, Color.WHITE, 60, 25);
        resetBtn.addActionListener(e -> resetParameters());
        JPanel wrap = new JPanel();
        wrap.setOpaque(false);
        wrap.add(resetBtn);
        parametersPanel.add(wrap);

        frame.add(parametersPanel, BorderLayout.EAST);
    }

    private void switchParamSet(int newSet) {
        // load new values
        selectedSet = newSet;
        values = defaultValues[selectedSet].clone();
        
        for (int i = 0; i < values.length; i++) {
            parameterFields[i].setValue(values[i]);
        }
    }

    private JFormattedTextField createParamPanel(
            String labelText,
            Number initValue,
            DefaultFormatterFactory factory,
            Consumer<Number> setter
    ) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        p.add(Box.createHorizontalGlue());

        JLabel lbl = new JLabel(labelText);
        lbl.setForeground(Color.WHITE);
        lbl.setHorizontalAlignment(SwingConstants.RIGHT);
        lbl.setAlignmentX(Component.RIGHT_ALIGNMENT);
        p.add(lbl);

        p.add(Box.createRigidArea(new Dimension(5, 0)));

        JFormattedTextField fld = new JFormattedTextField();
        fld.setFormatterFactory(factory);
        fld.setFocusLostBehavior(JFormattedTextField.COMMIT_OR_REVERT);
        fld.setValue(initValue);
        fld.setBackground(Color.WHITE);
        fld.setForeground(Color.BLACK);
        fld.setHorizontalAlignment(JFormattedTextField.RIGHT);
        fld.setPreferredSize(new Dimension(80, 25));
        fld.setMaximumSize(new Dimension(80, 25));

        fld.addPropertyChangeListener("value", e -> {
            Object v = fld.getValue();
            
            if (v instanceof Number num) {
                setter.accept(num);
            }
        });

        p.add(fld);
        parametersPanel.add(p);
        
        return fld;
    }

    private void resetParameters() {
        // reset only float parameters, keep levels untouched
        for (int i = 0; i < defaultValues[selectedSet].length; i++) {
            values[i] = defaultValues[selectedSet][i];
            parameterFields[i].setValue(values[i]);
        }
    }
    
    private TransferHandler createTransferHandler() {
        return new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                return !loading && support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            @Override
            public boolean importData(TransferSupport support) {
                if (!canImport(support)) {
                    return false;
                }

                try {
                    List<File> files = (List<File>) support.getTransferable()
                            .getTransferData(DataFlavor.javaFileListFlavor);

                    for (File file : files) {
                        String name = file.getName().toLowerCase();
                        
                        if (!name.endsWith(".png") && !name.endsWith(".jpg") && !name.endsWith(".jpeg")) {
                            showError("Incorrect image format, use: png, jpg or jpeg");
                            
                            return false;
                        }
                    }
                    
                    processFiles(files);
                    
                    return true;
                } catch (UnsupportedFlavorException | IOException e) {
                    e.printStackTrace();
                    
                    return false;
                }
            }
        };
    }
    
    private void setLoadingState(boolean state) {
        loading = state;

        toggleControls(!state);

        frame.repaint();
    }
    
    private void toggleControls(boolean enabled) {
        typeComboBox.setEnabled(enabled);
        setContainerEnabled(bottomPanel, enabled);
        setContainerEnabled(rightSide, enabled);
        setContainerEnabled(parametersPanel, enabled);
        setContainerEnabled(setsPanel, enabled);
    }
    
    private void setContainerEnabled(Container container, boolean enabled) {
        container.setEnabled(enabled);
        
        for (Component comp : container.getComponents()) {
            comp.setEnabled(enabled);
            
            if (comp instanceof Container container1) {
                setContainerEnabled(container1, enabled);
            }
        }
    }

    private void processFiles(List<File> files) {
        final int totalSteps = processAllSets ? files.size() * defaultValues.length : files.size();
        dropLabel.setText("LOADING (1/" + totalSteps + ")");
        
        setLoadingState(true);

        new SimpleSwingWorker() {
            @Override
            protected Void doInBackground() throws Exception {
                Operations op = new Operations(
                        values[0], 
                        values[1], 
                        values[2], 
                        values[3], 
                        values[4], 
                        values[6], 
                        levels, 
                        useRange, 
                        type
                );
                
                int step = 0;
                
                for (File file : files) {
                    if (processAllSets) {
                        for (float[] originalValues : defaultValues) {
                            op.sigmaC = originalValues[0];
                            op.sigmaE = originalValues[1];
                            op.sigmaM = originalValues[2];
                            op.tau = originalValues[3];
                            op.phi = originalValues[4];
                            op.epsilon = originalValues[5];
                            op.sigmaA = originalValues[6];
                            
                            op.startProcess(file.getPath());
                            
                            step++;
                            final int next = step + 1;
                            
                            SwingUtilities.invokeLater(()
                                    -> dropLabel.setText("LOADING (" + next + "/" + totalSteps + ")")
                            );
                            
                            if (op.save == false && op.skip == true) {
                                break;
                            }
                        }
                    } else {
                        op.epsilon = values[5];
                        
                        op.startProcess(file.getPath());
                        
                        step++;
                        final int next = step + 1;
                        
                        SwingUtilities.invokeLater(()
                                -> dropLabel.setText("LOADING (" + next + "/" + totalSteps + ")")
                        );
                    }
                    
                    if (op.save == false && op.skip == true) {
                        break;
                    }
                }
                
                return null;
            }

            @Override
            protected void done() {
                onProcessingComplete();
            }
        }.execute();
    }

    private void customizeComboBoxUI(JComboBox<Mode> comboBox) {
        comboBox.setUI(new BasicComboBoxUI() {
            @Override
            protected ComboBoxEditor createEditor() {
                ComboBoxEditor editor = super.createEditor();
                Component editorComponent = editor.getEditorComponent();
                editorComponent.setBackground(Color.BLACK);
                editorComponent.setForeground(Color.WHITE);

                return editor;
            }

            @Override
            protected ComboPopup createPopup() {
                BasicComboPopup popup = (BasicComboPopup) super.createPopup();
                popup.getList().setBackground(Color.BLACK);
                popup.getList().setForeground(Color.WHITE);
                popup.getList().setSelectionBackground(Color.WHITE);
                popup.getList().setSelectionForeground(Color.BLACK);
                popup.setBorder(BorderFactory.createLineBorder(Color.WHITE));

                JScrollPane scrollPane = (JScrollPane) popup.getComponent(0);
                JScrollBar bar = scrollPane.getVerticalScrollBar();
                bar.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Color.WHITE));

                bar.setUI(new BasicScrollBarUI() {
                    @Override
                    protected JButton createDecreaseButton(int orientation) {
                        return createArrowButton(SwingConstants.NORTH);
                    }

                    @Override
                    protected JButton createIncreaseButton(int orientation) {
                        return createArrowButton(SwingConstants.SOUTH);
                    }

                    @Override
                    protected void configureScrollBarColors() {
                        this.thumbColor = Color.WHITE;
                        this.trackColor = Color.BLACK;
                    }
                });

                return popup;
            }

            @Override
            protected JButton createArrowButton() {
                BasicArrowButton arrow = new BasicArrowButton(
                        SwingConstants.SOUTH,
                        Color.BLACK,
                        Color.WHITE,
                        Color.WHITE,
                        Color.BLACK
                );

                arrow.setBorder(BorderFactory.createEmptyBorder());

                return arrow;
            }

            protected JButton createArrowButton(int direction) {
                BasicArrowButton arrow = new BasicArrowButton(
                        direction,
                        Color.BLACK,
                        Color.WHITE,
                        Color.WHITE,
                        Color.BLACK
                );

                arrow.setBorder(BorderFactory.createEmptyBorder());

                return arrow;
            }
        });
    }

    private void onProcessingComplete() {
        dropLabel.setText("Images Generated");

        Timer resetTimer = new Timer(1000, e -> {
            dropLabel.setText("Drop IMAGE files here");
            setLoadingState(false);
        });

        resetTimer.setRepeats(false);
        resetTimer.start();
    }
    
    private void updateToggleButtonVisual(AbstractButton button) {
        Color temp = button.getBackground();

        button.setBackground(button.getForeground());
        button.setForeground(temp);
        button.setBorder(BorderFactory.createLineBorder(temp));
    }
    
    private void setButtonVisuals(AbstractButton button, Color bg, Color fg, int width, int height) {
        button.setBackground(bg);
        button.setForeground(fg);
        button.setBorder(BorderFactory.createLineBorder(fg));
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(true);   
        button.setPreferredSize(new Dimension(width, height));
        button.setMaximumSize(new Dimension(width, height));
    }
    
    private void showError(String message) {
        JOptionPane.showMessageDialog(frame, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void finalizeFrame() {
        frame.pack();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int xPos = (screenSize.width - frame.getWidth()) / 2;
        int yPos = (screenSize.height - frame.getHeight()) / 2;
        frame.setLocation(xPos, yPos);
        frame.setVisible(true);
    }
}