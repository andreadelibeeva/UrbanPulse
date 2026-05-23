package ui.panels;

import analysis.CorrelationEngine;
import analysis.CorrelationEngine.CorrelationResult;
import analysis.PolicyBriefGenerator;
import model.SocialIndicator;
import ui.MainFrame;
import util.DataStore;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

public class PolicyBriefPanel extends JPanel {

    private final DataStore dataStore;
    private final CorrelationEngine engine = new CorrelationEngine();
    private final PolicyBriefGenerator generator = new PolicyBriefGenerator();
    private JTextArea briefArea;
    private JLabel statusLabel;

    public PolicyBriefPanel(DataStore dataStore) {
        this.dataStore = dataStore;
        setBackground(MainFrame.bgDark);
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {
        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.setBackground(MainFrame.bgDark);
        header.setBorder(new EmptyBorder(28, 32, 16, 32));

        JLabel title = new JLabel("Policy Brief");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(MainFrame.textPrimary);

        statusLabel = new JLabel("Generate a report from your loaded data.");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(MainFrame.textMuted);

        header.add(title, BorderLayout.NORTH);
        header.add(statusLabel, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        briefArea = new JTextArea();
        briefArea.setEditable(false);
        briefArea.setBackground(MainFrame.bgCard);
        briefArea.setForeground(MainFrame.textPrimary);
        briefArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        briefArea.setBorder(new EmptyBorder(16, 20, 16, 20));
        briefArea.setLineWrap(true);
        briefArea.setWrapStyleWord(false);
        briefArea.setText("No report generated yet.\n\nClick 'Generate Report' to produce a policy brief based on your loaded crime data and social indicators.");

        JScrollPane scroll = new JScrollPane(briefArea);
        scroll.setBorder(null);
        scroll.setBackground(MainFrame.bgCard);
        scroll.getViewport().setBackground(MainFrame.bgCard);

        JPanel scrollWrap = new JPanel(new BorderLayout());
        scrollWrap.setBackground(MainFrame.bgDark);
        scrollWrap.setBorder(new EmptyBorder(0, 32, 0, 32));
        scrollWrap.add(scroll, BorderLayout.CENTER);
        add(scrollWrap, BorderLayout.CENTER);

        add(buildActionBar(), BorderLayout.SOUTH);
    }

    private JPanel buildActionBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        bar.setBackground(MainFrame.bgDark);
        bar.setBorder(new EmptyBorder(4, 32, 12, 32));

        JButton generateBtn = new JButton("▶ Generate Report");
        generateBtn.setBackground(MainFrame.accentCyan);
        generateBtn.setForeground(Color.BLACK);
        generateBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        generateBtn.setBorderPainted(false);
        generateBtn.setFocusPainted(false);
        generateBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        generateBtn.addActionListener(e -> generateBrief());

        JButton exportBtn = new JButton("⬇ Export .txt");
        exportBtn.setBackground(MainFrame.accentGold);
        exportBtn.setForeground(Color.BLACK);
        exportBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        exportBtn.setBorderPainted(false);
        exportBtn.setFocusPainted(false);
        exportBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        exportBtn.addActionListener(e -> exportBrief());

        bar.add(generateBtn);
        bar.add(exportBtn);
        return bar;
    }

    private void generateBrief() {
        int totalCrimes = dataStore.getTotalCrimes();

        if (totalCrimes == 0) {
            statusLabel.setText("No crime data loaded. Load a CSV file first.");
            briefArea.setText("No crime data loaded.\n\nUse the 'Load Crime CSV' button in the sidebar to load data first.");
            return;
        }

        Map<String, Integer> crimeCounts = dataStore.getCrimeCountByDistrict();
        Map<String, SocialIndicator> socialMap = dataStore.getSocialMap();
        List<CorrelationResult> correlations = engine.computeAll(crimeCounts, socialMap);
        String strongestPredictor = engine.strongestPredictor(correlations);

        String brief = generator.generate(
                "Chicago",
                totalCrimes,
                dataStore.getArrestRate(),
                dataStore.getTopCrimeType(),
                dataStore.getPeakHour(),
                correlations,
                strongestPredictor
        );

        briefArea.setText(brief);
        briefArea.setCaretPosition(0);

        if (correlations.isEmpty()) {
            statusLabel.setText("Report generated — no correlation data (add social indicators in the Correlation tab).");
        } else {
            statusLabel.setText("Report generated. Strongest predictor: " + strongestPredictor);
        }
    }

    private void exportBrief() {
        String content = briefArea.getText();
        if (content.isEmpty() || content.startsWith("No report")) {
            JOptionPane.showMessageDialog(this,
                    "Generate a report first before exporting.",
                    "Nothing to Export", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Policy Brief");
        chooser.setFileFilter(new FileNameExtensionFilter("Text Files (*.txt)", "txt"));
        chooser.setSelectedFile(new File("urban_pulse_policy_brief.txt"));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            String path = chooser.getSelectedFile().getAbsolutePath();
            if (!path.endsWith(".txt")) path += ".txt";
            try {
                generator.export(path);
                statusLabel.setText("Exported to: " + path);
                JOptionPane.showMessageDialog(this,
                        "Policy brief exported successfully!",
                        "Exported", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Export failed: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
