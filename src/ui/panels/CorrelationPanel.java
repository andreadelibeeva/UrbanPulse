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

    private enum Preset {
        VERY_HIGH ("Very High",  28, 40, 22, 15000, 22000, new Color(0xC0392B)),
        HIGH      ("High",       20, 28, 16, 11000, 32000, new Color(0xE67E22)),
        MEDIUM    ("Medium",     12, 16, 10,  8000, 48000, new Color(0xF39C12)),
        LOW       ("Low",         7,  9,  5,  6000, 68000, new Color(0x27AE60)),
        VERY_LOW  ("Very Low",    3,  4,  2,  3500, 95000, new Color(0x2ECC71));

        final String label;
        final double unemployment, poverty, dropout, density, income;
        final Color color;

        Preset(String label, double u, double p, double d,
               double den, double inc, Color c) {
            this.label = label; this.unemployment = u; this.poverty = p;
            this.dropout = d;   this.density = den;   this.income = inc;
            this.color = c;
        }

        SocialIndicator toIndicator(int id, String district) {
            return new SocialIndicator(id, district,
                    unemployment, poverty, dropout, density, income);
        }
    }

    private final DataStore dataStore;
    private JPanel  districtListPanel;
    private JPanel  resultsPanel;
    private JLabel  statusLabel;
    private JComboBox<String> bulkPresetCombo;

    // One combo per district, keyed by district string
    private final Map<String, JComboBox<String>> districtCombos = new LinkedHashMap<>();
    private List<CorrelationResult> lastResults = new ArrayList<>();

    public CorrelationPanel(DataStore dataStore) {
        this.dataStore = dataStore;
        setBackground(MainFrame.bgDark);
        setLayout(new BorderLayout());
        buildUI();
    }


    private void buildUI() {
        add(buildHeader(), BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildLeftPanel(), buildRightPanel());
        split.setBackground(MainFrame.bgDark);
        split.setBorder(new EmptyBorder(0, 32, 24, 32));
        split.setDividerSize(5);
        split.setDividerLocation(380);
        split.setResizeWeight(0.35);
        add(split, BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 6));
        header.setBackground(MainFrame.bgDark);
        header.setBorder(new EmptyBorder(28, 32, 16, 32));

        JLabel title = new JLabel("Correlation Analysis");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(MainFrame.textPrimary);

        statusLabel = new JLabel("Load crime data, assign deprivation levels, then run the analysis.");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(MainFrame.textMuted);

        header.add(title,       BorderLayout.NORTH);
        header.add(statusLabel, BorderLayout.SOUTH);
        return header;
    }


    private JPanel buildLeftPanel() {
        JPanel outer = new JPanel(new BorderLayout(0, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(MainFrame.bgCard);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
            }
        };
        outer.setOpaque(false);
        outer.setBorder(new EmptyBorder(18, 18, 18, 18));

        JPanel topBlock = new JPanel();
        topBlock.setLayout(new BoxLayout(topBlock, BoxLayout.Y_AXIS));
        topBlock.setOpaque(false);

        JLabel heading = new JLabel("District Deprivation Levels");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 14));
        heading.setForeground(MainFrame.textPrimary);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subHead = new JLabel("Assign a deprivation level to each district from your crime data.");
        subHead.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        subHead.setForeground(MainFrame.textMuted);
        subHead.setAlignmentX(Component.LEFT_ALIGNMENT);
        subHead.setBorder(new EmptyBorder(2, 0, 10, 0));

        topBlock.add(heading);
        topBlock.add(subHead);

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        actionRow.setOpaque(false);
        actionRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton autoLoadBtn = pillButton("⟳  Auto-Load Districts", MainFrame.accentBlue);
        autoLoadBtn.setToolTipText("Load all districts from your crime data automatically");
        autoLoadBtn.addActionListener(e -> autoLoadDistricts(false));

        JButton smartFillBtn = pillButton("★  Smart Fill", MainFrame.accentGold);
        smartFillBtn.setToolTipText("Automatically assign deprivation levels based on crime rates");
        smartFillBtn.addActionListener(e -> smartFill());

        actionRow.add(autoLoadBtn);
        actionRow.add(smartFillBtn);

        JPanel bulkRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        bulkRow.setOpaque(false);
        bulkRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        bulkRow.setBorder(new EmptyBorder(6, 0, 8, 0));

        JLabel bulkLabel = new JLabel("Set all to:");
        bulkLabel.setForeground(MainFrame.textMuted);
        bulkLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        bulkPresetCombo = new JComboBox<>(presetLabels());
        bulkPresetCombo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        bulkPresetCombo.setBackground(new Color(0x1A1A38));
        bulkPresetCombo.setForeground(MainFrame.textPrimary);

        JButton applyAllBtn = pillButton("Apply", new Color(0x2A2A4A));
        applyAllBtn.addActionListener(e -> applyBulkPreset());

        bulkRow.add(bulkLabel);
        bulkRow.add(bulkPresetCombo);
        bulkRow.add(applyAllBtn);

        topBlock.add(Box.createVerticalStrut(4));
        topBlock.add(actionRow);
        topBlock.add(bulkRow);
        outer.add(topBlock, BorderLayout.NORTH);

        districtListPanel = new JPanel();
        districtListPanel.setLayout(new BoxLayout(districtListPanel, BoxLayout.Y_AXIS));
        districtListPanel.setOpaque(false);

        JScrollPane scroll = new JScrollPane(districtListPanel);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getViewport().setBackground(MainFrame.bgCard);
        outer.add(scroll, BorderLayout.CENTER);

        JButton runBtn = new JButton("▶  Run Correlation Analysis") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(
                        0, 0,           new Color(0x00C8FF),
                        getWidth(), 0,  new Color(0x0057FF));
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        runBtn.setForeground(Color.WHITE);
        runBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        runBtn.setBorderPainted(false);
        runBtn.setContentAreaFilled(false);
        runBtn.setFocusPainted(false);
        runBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        runBtn.setPreferredSize(new Dimension(0, 40));
        runBtn.setBorder(new EmptyBorder(0, 0, 0, 0));
        runBtn.addActionListener(e -> runAnalysis());

        JPanel runWrap = new JPanel(new BorderLayout());
        runWrap.setOpaque(false);
        runWrap.setBorder(new EmptyBorder(12, 0, 0, 0));
        runWrap.add(runBtn, BorderLayout.CENTER);
        outer.add(runWrap, BorderLayout.SOUTH);

        return outer;
    }

    private JPanel buildRightPanel() {
        JPanel outer = new JPanel(new BorderLayout(0, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(MainFrame.bgCard);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
            }
        };
        outer.setOpaque(false);
        outer.setBorder(new EmptyBorder(18, 18, 18, 18));

        JLabel heading = new JLabel("Pearson r Results");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 14));
        heading.setForeground(MainFrame.textPrimary);
        heading.setBorder(new EmptyBorder(0, 0, 14, 0));
        outer.add(heading, BorderLayout.NORTH);

        resultsPanel = new JPanel();
        resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));
        resultsPanel.setOpaque(false);
        showPlaceholder();

        JScrollPane scroll = new JScrollPane(resultsPanel);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getViewport().setBackground(MainFrame.bgCard);
        outer.add(scroll, BorderLayout.CENTER);
        return outer;
    }

    private void rebuildDistrictList(List<String> districts) {
        districtListPanel.removeAll();
        districtCombos.clear();

        if (districts.isEmpty()) {
            JLabel empty = new JLabel("<html><center><br>No crime data loaded.<br>Use the buttons above<br>to load crime data first.</center></html>");
            empty.setForeground(MainFrame.textMuted);
            empty.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            districtListPanel.add(Box.createVerticalStrut(20));
            districtListPanel.add(empty);
        } else {
            for (String district : districts) {
                districtListPanel.add(buildDistrictRow(district));
                districtListPanel.add(Box.createVerticalStrut(4));
            }
        }

        districtListPanel.revalidate();
        districtListPanel.repaint();
    }

    private JPanel buildDistrictRow(String district) {
        JPanel row = new JPanel(new BorderLayout(8, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0x1A1A38));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
            }
        };
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(8, 10, 8, 10));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        // Crime count badge
        int crimes = dataStore.getCrimeCountByDistrict().getOrDefault(district, 0);
        JLabel distLabel = new JLabel("District  " + district);
        distLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        distLabel.setForeground(MainFrame.textPrimary);

        JLabel countLabel = new JLabel(crimes + " crimes");
        countLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        countLabel.setForeground(MainFrame.textMuted);

        JPanel leftBlock = new JPanel();
        leftBlock.setLayout(new BoxLayout(leftBlock, BoxLayout.Y_AXIS));
        leftBlock.setOpaque(false);
        leftBlock.add(distLabel);
        leftBlock.add(countLabel);
        row.add(leftBlock, BorderLayout.WEST);

        // Preset combo
        JComboBox<String> combo = new JComboBox<>(presetLabels());
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        combo.setBackground(new Color(0x12122A));
        combo.setForeground(MainFrame.textPrimary);
        combo.setPreferredSize(new Dimension(130, 26));

        // Restore previously saved preset if one exists
        SocialIndicator existing = dataStore.getSocialMap().get(district);
        if (existing != null) {
            combo.setSelectedItem(guessPreset(existing).label);
        } else {
            combo.setSelectedItem(Preset.MEDIUM.label);
        }

        // Color dot to the right of combo
        JLabel dot = new JLabel("●");
        dot.setFont(new Font("Segoe UI", Font.BOLD, 16));
        dot.setBorder(new EmptyBorder(0, 6, 0, 0));
        updateDot(dot, (String) combo.getSelectedItem());
        combo.addActionListener(e -> updateDot(dot, (String) combo.getSelectedItem()));

        JPanel rightBlock = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        rightBlock.setOpaque(false);
        rightBlock.add(combo);
        rightBlock.add(dot);
        row.add(rightBlock, BorderLayout.EAST);

        districtCombos.put(district, combo);
        return row;
    }

    private void updateDot(JLabel dot, String presetLabel) {
        for (Preset p : Preset.values()) {
            if (p.label.equals(presetLabel)) {
                dot.setForeground(p.color);
                return;
            }
        }
        dot.setForeground(MainFrame.textMuted);
    }


    private void autoLoadDistricts(boolean silent) {
        List<String> districts = dataStore.getDistricts();
        if (districts.isEmpty()) {
            if (!silent) {
                statusLabel.setText("No crime data loaded yet — use Fetch Live Data or Load CSV first.");
                statusLabel.setForeground(MainFrame.accentRed);
            }
            rebuildDistrictList(Collections.emptyList());
            return;
        }
        districts.sort(Comparator.naturalOrder());
        rebuildDistrictList(districts);
        if (!silent) {
            statusLabel.setText("Loaded " + districts.size() + " districts. Assign deprivation levels, then Run Analysis.");
            statusLabel.setForeground(MainFrame.accentGreen);
        }
    }

    private void smartFill() {
        if (districtCombos.isEmpty()) {
            autoLoadDistricts(true);
            if (districtCombos.isEmpty()) {
                statusLabel.setText("Load crime data first before using Smart Fill.");
                statusLabel.setForeground(MainFrame.accentRed);
                return;
            }
        }

        // Sort districts by crime count ascending → assign presets proportionally
        Map<String, Integer> counts = dataStore.getCrimeCountByDistrict();
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(counts.entrySet());
        sorted.sort(Map.Entry.comparingByValue());

        int n = sorted.size();
        for (int i = 0; i < sorted.size(); i++) {
            String district = sorted.get(i).getKey();
            JComboBox<String> combo = districtCombos.get(district);
            if (combo == null) continue;
            double percentile = n == 1 ? 0.5 : (double) i / (n - 1);
            Preset assigned;
            if      (percentile >= 0.80) assigned = Preset.VERY_HIGH;
            else if (percentile >= 0.60) assigned = Preset.HIGH;
            else if (percentile >= 0.40) assigned = Preset.MEDIUM;
            else if (percentile >= 0.20) assigned = Preset.LOW;
            else                         assigned = Preset.VERY_LOW;
            combo.setSelectedItem(assigned.label);
        }

        statusLabel.setText("Smart fill applied — higher crime districts assigned higher deprivation levels.");
        statusLabel.setForeground(MainFrame.accentCyan);
    }

    private void applyBulkPreset() {
        String selected = (String) bulkPresetCombo.getSelectedItem();
        for (JComboBox<String> combo : districtCombos.values()) {
            combo.setSelectedItem(selected);
        }
    }

    private void runAnalysis() {
        if (districtCombos.isEmpty()) {
            autoLoadDistricts(true);
        }
        if (districtCombos.isEmpty()) {
            statusLabel.setText("No crime data loaded. Use Fetch Live Data or Load CSV first.");
            statusLabel.setForeground(MainFrame.accentRed);
            return;
        }

        // Commit current combo selections into the dataStore
        for (Map.Entry<String, JComboBox<String>> entry : districtCombos.entrySet()) {
            String district    = entry.getKey();
            String presetLabel = (String) entry.getValue().getSelectedItem();
            Preset p = presetFromLabel(presetLabel);
            if (p != null) {
                dataStore.addSocial(p.toIndicator(dataStore.getNextSocialId(), district));
            }
        }
        dataStore.saveSocials();

        Map<String, Integer> crimeCounts = dataStore.getCrimeCountByDistrict();
        Map<String, SocialIndicator> socialMap = dataStore.getSocialMap();

        CorrelationEngine engine = new CorrelationEngine();
        lastResults = engine.computeAll(crimeCounts, socialMap);

        if (lastResults.isEmpty()) {
            statusLabel.setText("Need at least 3 matching districts. Make sure crime data is loaded.");
            statusLabel.setForeground(MainFrame.accentRed);
            showPlaceholder();
            return;
        }

        String top = engine.strongestPredictor(lastResults);
        statusLabel.setText("Strongest predictor of crime: " + top);
        statusLabel.setForeground(MainFrame.accentCyan);
        showResults(lastResults);
    }

    private void showPlaceholder() {
        resultsPanel.removeAll();

        JPanel card = gradientCard(new Color(0x12122A), new Color(0x1A1A38));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(28, 20, 28, 20));

        JLabel icon = new JLabel("∿");
        icon.setFont(new Font("Segoe UI", Font.BOLD, 48));
        icon.setForeground(new Color(0x2A2A5A));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel l1 = new JLabel("No results yet");
        l1.setFont(new Font("Segoe UI", Font.BOLD, 14));
        l1.setForeground(MainFrame.textMuted);
        l1.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel l2 = new JLabel("<html><center style='color:#3A3A6A'>1. Click Auto-Load Districts<br>"
                + "2. Click Smart Fill (or set levels manually)<br>"
                + "3. Click Run Correlation Analysis</center></html>");
        l2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        l2.setForeground(MainFrame.textMuted);
        l2.setAlignmentX(Component.CENTER_ALIGNMENT);
        l2.setBorder(new EmptyBorder(8, 0, 0, 0));

        card.add(icon);
        card.add(Box.createVerticalStrut(8));
        card.add(l1);
        card.add(l2);

        resultsPanel.add(card);
        resultsPanel.revalidate();
        resultsPanel.repaint();
    }

    private void showResults(List<CorrelationResult> results) {
        resultsPanel.removeAll();
        resultsPanel.add(Box.createVerticalStrut(4));

        // Summary card
        resultsPanel.add(buildSummaryCard(results));
        resultsPanel.add(Box.createVerticalStrut(10));

        for (CorrelationResult r : results) {
            resultsPanel.add(buildResultCard(r));
            resultsPanel.add(Box.createVerticalStrut(8));
        }

        resultsPanel.revalidate();
        resultsPanel.repaint();
    }

    private JPanel buildSummaryCard(List<CorrelationResult> results) {
        JPanel card = gradientCard(new Color(0x0D1A38), new Color(0x12122A));
        card.setLayout(new BorderLayout(0, 6));
        card.setBorder(new EmptyBorder(14, 16, 14, 16));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        CorrelationResult top = results.get(0);
        double abs = Math.abs(top.pearsonR);
        Color topColor = abs >= 0.6 ? MainFrame.accentRed
                : abs >= 0.3 ? MainFrame.accentGold
                :              MainFrame.textMuted;

        JLabel topLine = new JLabel("Strongest predictor: " + top.indicator);
        topLine.setFont(new Font("Segoe UI", Font.BOLD, 13));
        topLine.setForeground(topColor);

        String distCount = districtCombos.size() + " districts analysed";
        JLabel bottomLine = new JLabel(distCount + "   ·   " + top.interpretation);
        bottomLine.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        bottomLine.setForeground(MainFrame.textMuted);

        card.add(topLine,    BorderLayout.CENTER);
        card.add(bottomLine, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildResultCard(CorrelationResult r) {
        double abs = Math.abs(r.pearsonR);
        Color barColor = abs >= 0.6 ? MainFrame.accentRed
                : abs >= 0.3 ? MainFrame.accentGold
                :              new Color(0x4A6080);

        JPanel card = gradientCard(new Color(0x14142A), new Color(0x1A1A36));
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(new EmptyBorder(14, 16, 14, 16));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));

        // Top row: indicator name + r value
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);

        JLabel indicatorLabel = new JLabel(indicatorIcon(r.indicator) + "  " + r.indicator);
        indicatorLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        indicatorLabel.setForeground(MainFrame.textPrimary);

        JLabel rValue = new JLabel(String.format("r = %+.3f", r.pearsonR));
        rValue.setFont(new Font("Segoe UI", Font.BOLD, 15));
        rValue.setForeground(barColor);
        topRow.add(indicatorLabel, BorderLayout.WEST);
        topRow.add(rValue,         BorderLayout.EAST);
        card.add(topRow, BorderLayout.NORTH);

        // Bar
        card.add(new CorrelationBar(r.pearsonR, barColor), BorderLayout.CENTER);

        // Bottom text
        JLabel interp = new JLabel("<html><span style='color:#5A6080'>" + r.interpretation
                + "</span> — <span style='color:#3A4060'>" + r.policyImplication + "</span></html>");
        interp.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        card.add(interp, BorderLayout.SOUTH);

        return card;
    }

    private String indicatorIcon(String indicator) {
        return switch (indicator) {
            case CorrelationEngine.unemployment -> "💼";
            case CorrelationEngine.poverty      -> "🏚";
            case CorrelationEngine.dropout      -> "📚";
            case CorrelationEngine.density      -> "🏙";
            case CorrelationEngine.income       -> "💰";
            default -> "◈";
        };
    }

    private JPanel gradientCard(Color c1, Color c2) {
        return new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, c1, getWidth(), getHeight(), c2);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
            }
        };
    }

    private JButton pillButton(String text, Color bg) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.setForeground(Color.WHITE);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(5, 12, 5, 12));
        return btn;
    }

    private String[] presetLabels() {
        return Arrays.stream(Preset.values())
                .map(p -> p.label)
                .toArray(String[]::new);
    }

    private Preset presetFromLabel(String label) {
        for (Preset p : Preset.values()) if (p.label.equals(label)) return p;
        return Preset.MEDIUM;
    }

    private Preset guessPreset(SocialIndicator si) {
        double score = si.getUnemploymentRate() + si.getPovertyRate();
        if (score >= 50) return Preset.VERY_HIGH;
        if (score >= 35) return Preset.HIGH;
        if (score >= 20) return Preset.MEDIUM;
        if (score >= 10) return Preset.LOW;
        return Preset.VERY_LOW;
    }

    public void refresh() {
        // Only re-load the district list if crime data has changed
        if (!dataStore.getDistricts().isEmpty() && districtCombos.isEmpty()) {
            autoLoadDistricts(true);
        } else if (!dataStore.getDistricts().isEmpty()) {
            // Add any new districts that appeared after a data load
            List<String> current = dataStore.getDistricts();
            boolean changed = false;
            for (String d : current) {
                if (!districtCombos.containsKey(d)) { changed = true; break; }
            }
            if (changed) autoLoadDistricts(true);
        }
    }

    //Bar

    private static class CorrelationBar extends JPanel {
        private final double r;
        private final Color  color;

        CorrelationBar(double r, Color color) {
            this.r     = r;
            this.color = color;
            setOpaque(false);
            setPreferredSize(new Dimension(0, 14));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 14));
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w   = getWidth();
            int h   = getHeight();
            int mid = w / 2;

            // Track
            g2.setColor(new Color(0x22223A));
            g2.fillRoundRect(0, (h - 6) / 2, w, 6, 4, 4);

            // Negative / Positive labels
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            g2.setColor(new Color(0x3A3A5A));
            g2.drawString("-1", 2, h - 2);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString("+1", w - fm.stringWidth("+1") - 2, h - 2);
            g2.drawString("0",  mid - fm.stringWidth("0") / 2, h - 2);

            // Fill bar
            int barLen = (int) (Math.abs(r) * (mid - 4));
            GradientPaint gp;
            if (r >= 0) {
                gp = new GradientPaint(mid, 0, color.darker(), mid + barLen, 0, color);
                g2.setPaint(gp);
                g2.fillRoundRect(mid, (h - 8) / 2, barLen, 8, 4, 4);
            } else {
                gp = new GradientPaint(mid - barLen, 0, color, mid, 0, color.darker());
                g2.setPaint(gp);
                g2.fillRoundRect(mid - barLen, (h - 8) / 2, barLen, 8, 4, 4);
            }

            // Centre tick
            g2.setColor(new Color(0xFFFFFF, true));
            g2.setColor(new Color(255, 255, 255, 80));
            g2.fillRect(mid - 1, (h - 10) / 2, 2, 10);

            g2.dispose();
        }
    }
}