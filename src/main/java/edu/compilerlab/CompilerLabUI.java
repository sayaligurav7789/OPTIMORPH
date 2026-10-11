
package edu.compilerlab;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

public final class CompilerLabUI extends JFrame {

    private final JTextArea sourceArea = new JTextArea();
    private final JTextArea parsedArea = outputArea();
    private final JTextArea tacArea = outputArea();
    private final JTextArea foldedArea = outputArea();
    private final JTextArea propagatedArea = outputArea();
    private final JTextArea optimizedArea = outputArea();
    private final JTextArea diagnosticsArea = outputArea();

    private final JLabel statusLabel = new JLabel(
            "Ready — enter assignments and click Run Optimization."
    );

    private static final String EXAMPLE =
            "a = 10 + 20;\\n" +
            "b = a * 2;\\n" +
            "unused = 5 * 0;\\n" +
            "result = b + 1;";

    public CompilerLabUI() {
        super("MiniJava Optimization Lab");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 650));
        setSize(1080, 740);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(16, 16, 16, 16));
        root.setBackground(new Color(245, 247, 251));
        setContentPane(root);

        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildMainContent(), BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(8, 4));
        header.setOpaque(false);

        JLabel title = new JLabel("MiniJava Optimization Lab");
        title.setFont(new Font("SansSerif", Font.BOLD, 25));
        title.setForeground(new Color(28, 39, 58));

        JLabel subtitle = new JLabel(
                "Explore a tiny compiler pipeline: parse → fold → propagate → eliminate dead code"
        );
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setForeground(new Color(92, 104, 124));

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.add(title);
        titles.add(Box.createVerticalStrut(5));
        titles.add(subtitle);

        header.add(titles, BorderLayout.CENTER);
        return header;
    }

    private JSplitPane buildMainContent() {
        JPanel inputPanel = new JPanel(new BorderLayout(8, 8));
        inputPanel.setBackground(Color.WHITE);
        inputPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(222, 227, 236)),
                new EmptyBorder(12, 12, 12, 12)
        ));

        JLabel inputTitle = new JLabel("Source program");
        inputTitle.setFont(new Font("SansSerif", Font.BOLD, 15));

        sourceArea.setText(EXAMPLE.replace("\\n", "\n"));
        sourceArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        sourceArea.setTabSize(2);
        sourceArea.setCaretPosition(0);
        sourceArea.setBorder(new EmptyBorder(8, 8, 8, 8));
        sourceArea.setBackground(new Color(250, 251, 254));

        JPanel inputTop = new JPanel(new BorderLayout());
        inputTop.setOpaque(false);
        inputTop.add(inputTitle, BorderLayout.WEST);

        JButton exampleButton = new JButton("Load example");
        exampleButton.addActionListener(e -> {
            sourceArea.setText(EXAMPLE.replace("\\n", "\n"));
            diagnosticsArea.setText("Example loaded. Click Run Optimization.");
            diagnosticsArea.setForeground(new Color(83, 96, 116));
            clearOutputs();
            statusLabel.setText("Example loaded.");
        });
        inputTop.add(exampleButton, BorderLayout.EAST);

        JButton runButton = new JButton("▶  Run Optimization");
        runButton.setFocusPainted(false);
        runButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        runButton.setBackground(new Color(53, 94, 220));
        runButton.setForeground(Color.WHITE);
        runButton.setOpaque(true);
        runButton.setBorderPainted(false);
        runButton.setPreferredSize(new Dimension(190, 40));
        runButton.addActionListener(e -> runPipeline());

        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> {
            sourceArea.setText("");
            clearOutputs();
            diagnosticsArea.setText("Diagnostics will appear here after you run the program.");
            diagnosticsArea.setForeground(new Color(83, 96, 116));
            statusLabel.setText("Cleared.");
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(runButton);
        buttons.add(clearButton);

        JPanel bottomInput = new JPanel(new BorderLayout(0, 8));
        bottomInput.setOpaque(false);
        bottomInput.add(buttons, BorderLayout.NORTH);

        JLabel hint = new JLabel(
                "<html><font color='#68758a'>Syntax: variable = arithmetic expression;</font></html>"
        );
        bottomInput.add(hint, BorderLayout.SOUTH);

        inputPanel.add(inputTop, BorderLayout.NORTH);

        diagnosticsArea.setRows(4);
        diagnosticsArea.setText("Diagnostics will appear here after you run the program.");
        diagnosticsArea.setForeground(new Color(83, 96, 116));

        JSplitPane editorSplit = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(sourceArea),
                new JScrollPane(diagnosticsArea)
        );
        editorSplit.setResizeWeight(0.72);
        editorSplit.setDividerSize(6);
        editorSplit.setBorder(null);

        inputPanel.add(editorSplit, BorderLayout.CENTER);
        inputPanel.add(bottomInput, BorderLayout.SOUTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tabs.addTab("Parsed", wrapOutput(parsedArea));
        tabs.addTab("TAC Generation", wrapOutput(tacArea));
        tabs.addTab("Constant Folding", wrapOutput(foldedArea));
        tabs.addTab("Constant Propagation", wrapOutput(propagatedArea));
        tabs.addTab("Dead Code Elimination", wrapOutput(optimizedArea));

        JPanel resultsPanel = new JPanel(new BorderLayout(0, 8));
        resultsPanel.setBackground(Color.WHITE);
        resultsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(222, 227, 236)),
                new EmptyBorder(10, 10, 10, 10)
        ));

        JLabel resultsTitle = new JLabel("Optimization stages");
        resultsTitle.setFont(new Font("SansSerif", Font.BOLD, 15));

        resultsPanel.add(resultsTitle, BorderLayout.NORTH);
        resultsPanel.add(tabs, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                inputPanel,
                resultsPanel
        );
        split.setResizeWeight(0.43);
        split.setBorder(null);
        split.setDividerSize(8);

        return split;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);

        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(83, 96, 116));
        footer.add(statusLabel, BorderLayout.CENTER);

        JLabel badge = new JLabel("EDUCATIONAL DEMO");
        badge.setFont(new Font("SansSerif", Font.BOLD, 10));
        badge.setForeground(new Color(53, 94, 220));
        footer.add(badge, BorderLayout.EAST);

        return footer;
    }

    private static JTextArea outputArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        area.setBackground(new Color(250, 251, 254));
        area.setBorder(new EmptyBorder(12, 12, 12, 12));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        return area;
    }

    private static JPanel wrapOutput(JTextArea area) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(area), BorderLayout.CENTER);
        return panel;
    }

    private void runPipeline() {
        String source = sourceArea.getText().trim();

        if (source.isEmpty()) {
            diagnosticsArea.setText("ERROR | No source code entered.");
            diagnosticsArea.setForeground(new Color(190, 40, 40));
            clearOutputs();
            statusLabel.setText("Enter a program before running optimization.");
            return;
        }

        Pipeline pipeline = new Pipeline();

        // Display diagnostics before attempting optimization.
        List<String> diagnostics = pipeline.diagnose(source);
        diagnosticsArea.setText(String.join("\n", diagnostics));

        boolean hasErrors = diagnostics.stream()
                .anyMatch(message -> message.startsWith("ERROR"));

        boolean hasWarnings = diagnostics.stream()
                .anyMatch(message -> message.startsWith("WARNING"));

        if (hasErrors) {
            diagnosticsArea.setForeground(new Color(190, 40, 40));
            clearOutputs();
            statusLabel.setText("Input contains errors. Check the Diagnostics panel.");
            return;
        }

        if (hasWarnings) {
            diagnosticsArea.setForeground(new Color(170, 115, 0));
        } else {
            diagnosticsArea.setForeground(new Color(30, 125, 70));
        }

        try {
            var parsed = pipeline.parse(source);
            var tac = pipeline.generateTac(parsed);
            tacArea.setText(String.join("\n", tac));
            var folded = pipeline.foldAll(parsed);
            var propagated = pipeline.propagate(folded);
            var optimized = pipeline.eliminateDead(propagated);

            parsedArea.setText(pipeline.show(parsed));
            foldedArea.setText(pipeline.show(folded));
            propagatedArea.setText(pipeline.show(propagated));
            optimizedArea.setText(pipeline.show(optimized));

            statusLabel.setText(
                    hasWarnings
                            ? "Optimization complete with warnings. Review Diagnostics."
                            : "Optimization complete. Review each tab to compare the stages."
            );

        } catch (RuntimeException ex) {
            clearOutputs();
            diagnosticsArea.setForeground(new Color(190, 40, 40));
            diagnosticsArea.setText(
                    "ERROR | " + (ex.getMessage() == null
                            ? "An unexpected processing error occurred."
                            : ex.getMessage())
            );
            statusLabel.setText("Could not process the program.");
        }
    }

    private void clearOutputs() {
        parsedArea.setText("");
        tacArea.setText("");
        foldedArea.setText("");
        propagatedArea.setText("");
        optimizedArea.setText("");
    }

    public static void launch() {
        SwingUtilities.invokeLater(() -> new CompilerLabUI().setVisible(true));
    }
}
