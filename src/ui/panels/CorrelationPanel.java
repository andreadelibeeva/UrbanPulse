package ui.panels;

import analysis.CorrelationEngine;
import analysis.CorrelationEngine.CorrelationResult;
import model.SocialIndicator;
import ui.MainFrame;
import util.DataStore;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;
import java.util.List;

public class CorrelationPanel extends JPanel {

    private final DataStore dataStore;
    private JPanel resultsPanel;
    private JPanel socialEditor;
    private JLabel statusLabel;
    private List<CorrelationResult> lastResults = new ArrayList<>();

    public CorrelationPanel(DataStore dataStore) {
        this.dataStore = dataStore;
        setBackground(MainFrame.bgDark);
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {
        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.setBackground(MainFrame.bgDark);
        header.setBorder(new EmptyBorder(28, 32, 16, 32));

        JLabel title = new JLabel("Correlation Analysis");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(MainFrame.textPrimary);

        statusLabel = new JLabel("Enter social indicator data below, then run the analysis.");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(MainFrame.textMuted);

        header.add(title, BorderLayout.NORTH);
        header.add(statusLabel, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setBackground(MainFrame.bgDark);
        split.setBorder(new EmptyBorder(0, 32, 24, 32));
        split.setDividerSize(6);
        split.setDividerLocation(340);

        split.setLeftComponent(buildSocialEditorPanel());
        split.setRightComponent(buildResultsPanel());
        add(split, BorderLayout.CENTER);
    }

    private JPanel buildSocialEditorPanel() {
        JPanel outer = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(MainFrame.bgCard);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
            }
        };
        outer.setOpaque(false);
        outer.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel heading = new JLabel("Social Indicators per District");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 13));
        heading.setForeground(MainFrame.textPrimary);
        heading.setBorder(new EmptyBorder(0, 0, 10, 0));
        outer.add(heading, BorderLayout.NORTH);

        socialEditor = new JPanel();
        socialEditor.setLayout(new BoxLayout(socialEditor, BoxLayout.Y_AXIS));
        socialEditor.setBackground(MainFrame.bgCard);

        JScrollPane scroll = new JScrollPane(socialEditor);
        scroll.setBorder(null);
        scroll.setBackground(MainFrame.bgCard);
        scroll.getViewport().setBackground(MainFrame.bgCard);
        outer.add(scroll, BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new GridLayout(1, 2, 8, 0));
        btnRow.setBackground(MainFrame.bgCard);
        btnRow.setBorder(new EmptyBorder(10, 0, 0, 0));

        JButton addDistrictBtn = new JButton("+ Add District");
        addDistrictBtn.setBackground(MainFrame.accentBlue);
        addDistrictBtn.setForeground(MainFrame.textPrimary);
        addDistrictBtn.setBorderPainted(false);
        addDistrictBtn.setFocusPainted(false);
        addDistrictBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        addDistrictBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addDistrictBtn.addActionListener(e -> addDistrictEditor(null));

        JButton runBtn = new JButton("▶ Run Analysis");
        runBtn.setBackground(MainFrame.accentCyan);
        runBtn.setForeground(Color.BLACK);
        runBtn.setBorderPainted(false);
        runBtn.setFocusPainted(false);
        runBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        runBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        runBtn.addActionListener(e -> runAnalysis());

        btnRow.add(addDistrictBtn);
        btnRow.add(runBtn);
        outer.add(btnRow, BorderLayout.SOUTH);

        populateSocialEditors();
        return outer;
    }

    private void populateSocialEditors() {
        socialEditor.removeAll();
        List<SocialIndicator> socials = dataStore.getAllSocials();
        if (socials.isEmpty()) {
            addDistrictEditor(null);
        } else {
            for (SocialIndicator si : socials) {
                addDistrictEditor(si);
            }
        }
        socialEditor.revalidate();
        socialEditor.repaint();
    }

    private void addDistrictEditor(SocialIndicator si) {
        JPanel row = new JPanel(new GridLayout(0, 1, 2, 2));
        row.setBackground(new Color(0x1E1E32));
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0x2A2A3E)),
                new EmptyBorder(8, 8, 8, 8)));

        JTextField districtFld   = new JTextField(si != null ? si.getDistrict() : "", 8);
        JTextField unemployFld   = new JTextField(si != null ? String.valueOf(si.getUnemploymentRate()) : "0.0", 6);
        JTextField povertyFld    = new JTextField(si != null ? String.valueOf(si.getPovertyRate()) : "0.0", 6);
        JTextField dropoutFld    = new JTextField(si != null ? String.valueOf(si.getSchoolDropoutRate()) : "0.0", 6);
        JTextField densityFld    = new JTextField(si != null ? String.valueOf(si.getPopulationDensity()) : "0.0", 6);
        JTextField incomeFld     = new JTextField(si != null ? String.valueOf(si.getMedianIncome()) : "0.0", 8);

        styleField(districtFld); styleField(unemployFld); styleField(povertyFld);
        styleField(dropoutFld);  styleField(densityFld);  styleField(incomeFld);

        JButton saveRowBtn = new JButton("Save");
        saveRowBtn.setBackground(new Color(0x1A4A3A));
        saveRowBtn.setForeground(new Color(0x00C48C));
        saveRowBtn.setBorderPainted(false);
        saveRowBtn.setFocusPainted(false);
        saveRowBtn.setFont(new Font("Segoe UI", Font.BOLD, 10));

        JButton removeBtn = new JButton("✕");
        removeBtn.setBackground(new Color(0x3A1A1A));
        removeBtn.setForeground(MainFrame.accentRed);
        removeBtn.setBorderPainted(false);
        removeBtn.setFocusPainted(false);
        removeBtn.setFont(new Font("Segoe UI", Font.BOLD, 10));

        saveRowBtn.addActionListener(e -> {
            try {
                String district = districtFld.getText().trim();
                if (district.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "District cannot be empty.");
                    return;
                }
                dataStore.addSocial(new SocialIndicator(
                        dataStore.getNextSocialId(), district,
                        parseDouble(unemployFld), parseDouble(povertyFld),
                        parseDouble(dropoutFld),  parseDouble(densityFld),
                        parseDouble(incomeFld)));
                statusLabel.setText("Saved district: " + district);
                row.setBackground(new Color(0x1A3A2A));
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter valid numbers.");
            }
        });

        removeBtn.addActionListener(e -> {
            dataStore.deleteSocials(districtFld.getText().trim());
            socialEditor.remove(row);
            socialEditor.revalidate();
            socialEditor.repaint();
        });

        row.add(labeledField("District:", districtFld));
        row.add(labeledField("Unemployment %:", unemployFld));
        row.add(labeledField("Poverty %:", povertyFld));
        row.add(labeledField("School Dropout %:", dropoutFld));
        row.add(labeledField("Pop. Density:", densityFld));
        row.add(labeledField("Median Income:", incomeFld));

        JPanel btnRow = new JPanel(new GridLayout(1, 2, 4, 0));
        btnRow.setBackground(new Color(0x1E1E32));
        btnRow.add(saveRowBtn);
        btnRow.add(removeBtn);
        row.add(btnRow);

        socialEditor.add(row);
        socialEditor.revalidate();
        socialEditor.repaint();
    }

    private JPanel labeledField(String label, JTextField field) {
        JPanel p = new JPanel(new BorderLayout(4, 0));
        p.setBackground(new Color(0x1E1E32));
        JLabel lbl = new JLabel(label);
        lbl.setForeground(MainFrame.textMuted);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lbl.setPreferredSize(new Dimension(110, 20));
        p.add(lbl, BorderLayout.WEST);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private void styleField(JTextField f) {
        f.setBackground(new Color(0x252540));
        f.setForeground(MainFrame.textPrimary);
        f.setCaretColor(MainFrame.textPrimary);
        f.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x3A3A5A)),
                new EmptyBorder(2, 4, 2, 4)));
    }

    private double parseDouble(JTextField f) {
        return Double.parseDouble(f.getText().trim());
    }

    private JPanel buildResultsPanel() {
        JPanel outer = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(MainFrame.bgCard);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
            }
        };
        outer.setOpaque(false);
        outer.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel heading = new JLabel("Pearson Correlation Results");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 13));
        heading.setForeground(MainFrame.textPrimary);
        heading.setBorder(new EmptyBorder(0, 0, 10, 0));
        outer.add(heading, BorderLayout.NORTH);

        resultsPanel = new JPanel();
        resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));
        resultsPanel.setBackground(MainFrame.bgCard);

        showPlaceholder();

        JScrollPane scroll = new JScrollPane(resultsPanel);
        scroll.setBorder(null);
        scroll.setBackground(MainFrame.bgCard);
        scroll.getViewport().setBackground(MainFrame.bgCard);
        outer.add(scroll, BorderLayout.CENTER);
        return outer;
    }

    private void showPlaceholder() {
        resultsPanel.removeAll();
        JLabel ph = new JLabel("<html><center>Add social indicator data for each<br>district, then click 'Run Analysis'.</center></html>");
        ph.setForeground(MainFrame.textMuted);
        ph.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        ph.setAlignmentX(Component.CENTER_ALIGNMENT);
        ph.setBorder(new EmptyBorder(40, 20, 20, 20));
        resultsPanel.add(Box.createVerticalGlue());
        resultsPanel.add(ph);
        resultsPanel.add(Box.createVerticalGlue());
        resultsPanel.revalidate();
        resultsPanel.repaint();
    }

    private void runAnalysis() {
        Map<String, Integer> crimeCounts = dataStore.getCrimeCountByDistrict();
        Map<String, SocialIndicator> socialMap = dataStore.getSocialMap();

        if (crimeCounts.isEmpty()) {
            statusLabel.setText("No crime data loaded. Use 'Load Crime CSV' first.");
            return;
        }
        if (socialMap.isEmpty()) {
            statusLabel.setText("No social indicators saved. Add and save district data first.");
            return;
        }

        CorrelationEngine engine = new CorrelationEngine();
        lastResults = engine.computeAll(crimeCounts, socialMap);

        if (lastResults.isEmpty()) {
            statusLabel.setText("Need at least 3 districts with both crime and social data.");
            showPlaceholder();
            return;
        }

        statusLabel.setText("Strongest predictor: " + engine.strongestPredictor(lastResults));
        showResults(lastResults);
    }

    private void showResults(List<CorrelationResult> results) {
        resultsPanel.removeAll();
        resultsPanel.add(Box.createVerticalStrut(8));

        for (CorrelationResult r : results) {
            resultsPanel.add(buildResultRow(r));
            resultsPanel.add(Box.createVerticalStrut(8));
        }

        resultsPanel.revalidate();
        resultsPanel.repaint();
    }

    private JPanel buildResultRow(CorrelationResult r) {
        JPanel card = new JPanel(new BorderLayout(0, 6)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0x1E1E32));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(12, 14, 12, 14));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        JLabel nameLabel = new JLabel(r.indicator);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        nameLabel.setForeground(MainFrame.textPrimary);

        double abs = Math.abs(r.pearsonR);
        Color barColor;
        if (abs >= 0.6)       barColor = MainFrame.accentRed;
        else if (abs >= 0.3)  barColor = MainFrame.accentGold;
        else                  barColor = MainFrame.textMuted;

        JLabel rLabel = new JLabel(String.format("r = %+.3f", r.pearsonR));
        rLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        rLabel.setForeground(barColor);

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        topRow.add(nameLabel, BorderLayout.WEST);
        topRow.add(rLabel, BorderLayout.EAST);
        card.add(topRow, BorderLayout.NORTH);

        BarWidget bar = new BarWidget(r.pearsonR, barColor);
        bar.setPreferredSize(new Dimension(0, 10));
        card.add(bar, BorderLayout.CENTER);

        JLabel interp = new JLabel("<html>" + r.interpretation + " — " + r.policyImplication + "</html>");
        interp.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        interp.setForeground(MainFrame.textMuted);
        card.add(interp, BorderLayout.SOUTH);

        return card;
    }

    private static class BarWidget extends JPanel {
        private final double r;
        private final Color color;

        BarWidget(double r, Color color) {
            this.r = r;
            this.color = color;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int mid = getWidth() / 2;
            g2.setColor(new Color(0x2A2A3E));
            g2.fillRoundRect(0, 2, getWidth(), getHeight() - 4, 4, 4);
            int barLen = (int)(Math.abs(r) * mid);
            if (r >= 0) {
                g2.setColor(color);
                g2.fillRoundRect(mid, 2, barLen, getHeight() - 4, 4, 4);
            } else {
                g2.setColor(color);
                g2.fillRoundRect(mid - barLen, 2, barLen, getHeight() - 4, 4, 4);
            }
            g2.setColor(new Color(0xFFFFFF, true));
            g2.drawLine(mid, 0, mid, getHeight());
            g2.dispose();
        }
    }

    public void refresh() {
        populateSocialEditors();
    }
}
