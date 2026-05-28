package ui.panels;

import model.CrimeRecord;
import ui.MainFrame;
import util.DataStore;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class DashboardPanel extends JPanel {

    private final DataStore dataStore;

    private JLabel totalCrimesVal, arrestRateVal, topCrimeVal, districtCountVal, peakHourVal;
    private JPanel rankingList, crimeTypeList;
    private JLabel statusLabel;
    private JLabel lastUpdatedLabel;

    public DashboardPanel(DataStore dataStore) {
        this.dataStore = dataStore;
        setBackground(MainFrame.bgDark);
        setLayout(new BorderLayout(0, 0));
        buildUI();
        refresh();
    }

    private void buildUI() {
        add(buildHeader(), BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(20, 28, 28, 28));
        body.add(buildStatCards());
        body.add(Box.createVerticalStrut(20));
        body.add(buildBottomRow());

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(MainFrame.bgDark);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // subtle bottom gradient stripe
                GradientPaint gp = new GradientPaint(
                        0, getHeight() - 1, new Color(0x2979FF, true),
                        getWidth(), getHeight() - 1, new Color(0x00E5FF, true));
                g2.setPaint(gp);
                g2.fillRect(0, getHeight() - 1, getWidth(), 1);
                g2.dispose();
            }
        };
        header.setOpaque(false);
        header.setLayout(new BorderLayout(0, 2));
        header.setBorder(new EmptyBorder(30, 32, 18, 32));

        JPanel left = new JPanel(new GridLayout(2, 1, 0, 3));
        left.setOpaque(false);

        JLabel title = new JLabel("Dashboard Overview");
        title.setFont(new Font(MainFrame.fontDisplay(), Font.BOLD, 28));
        title.setForeground(MainFrame.textPrimary);

        statusLabel = new JLabel("Load data to begin analysis");
        statusLabel.setFont(new Font(MainFrame.fontUI(), Font.PLAIN, 12));
        statusLabel.setForeground(MainFrame.textMuted);

        left.add(title);
        left.add(statusLabel);
        header.add(left, BorderLayout.WEST);

        lastUpdatedLabel = new JLabel("—");
        lastUpdatedLabel.setFont(new Font(MainFrame.fontUI(), Font.PLAIN, 11));
        lastUpdatedLabel.setForeground(MainFrame.textDim);
        header.add(lastUpdatedLabel, BorderLayout.EAST);

        return header;
    }

    private JPanel buildStatCards() {
        JPanel row = new JPanel(new GridLayout(1, 5, 14, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));

        totalCrimesVal   = new JLabel("0");
        arrestRateVal    = new JLabel("0%");
        topCrimeVal      = new JLabel("N/A");
        districtCountVal = new JLabel("0");
        peakHourVal      = new JLabel("N/A");

        row.add(glowCard("TOTAL CRIMES",    totalCrimesVal,   MainFrame.accentCyan,   "▣"));
        row.add(glowCard("ARREST RATE",     arrestRateVal,    MainFrame.accentGold,   "⧖"));
        row.add(glowCard("TOP CRIME TYPE",  topCrimeVal,      MainFrame.accentRed,    "▲"));
        row.add(glowCard("DISTRICTS",       districtCountVal, MainFrame.accentPurple, "◈"));
        row.add(glowCard("PEAK HOUR",       peakHourVal,      MainFrame.accentGreen,  "◉"));

        return row;
    }

    private JPanel glowCard(String label, JLabel valueLabel, Color accent, String symbol) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // card background gradient
                GradientPaint bgGrad = new GradientPaint(
                        0, 0,           MainFrame.bgCard2,
                        0, getHeight(), MainFrame.bgCard);
                g2.setPaint(bgGrad);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);

                // top accent stripe with glow
                GradientPaint stripe = new GradientPaint(
                        0, 0, accent,
                        getWidth(), 0, new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0));
                g2.setPaint(stripe);
                g2.fillRoundRect(0, 0, getWidth(), 3, 3, 3);

                // subtle inner glow
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 12));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);

                // border
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 40));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(16, 18, 14, 14));

        // symbol top-right
        JLabel sym = new JLabel(symbol);
        sym.setFont(new Font(MainFrame.fontUI(), Font.BOLD, 22));
        sym.setForeground(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 60));
        sym.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(sym, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(2, 1, 0, 4));
        center.setOpaque(false);

        valueLabel.setFont(new Font(MainFrame.fontDisplay(), Font.BOLD, 28));
        valueLabel.setForeground(accent);
        center.add(valueLabel);

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font(MainFrame.fontUI(), Font.BOLD, 9));
        lbl.setForeground(MainFrame.textMuted);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
        center.add(lbl);

        card.add(center, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildBottomRow() {
        JPanel row = new JPanel(new GridLayout(1, 2, 14, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 340));

        rankingList   = new JPanel();
        rankingList.setLayout(new BoxLayout(rankingList, BoxLayout.Y_AXIS));
        rankingList.setOpaque(false);

        crimeTypeList = new JPanel();
        crimeTypeList.setLayout(new BoxLayout(crimeTypeList, BoxLayout.Y_AXIS));
        crimeTypeList.setOpaque(false);

        row.add(listCard("Top Districts by Crime Count", rankingList, MainFrame.accentCyan));
        row.add(listCard("Top Crime Types",              crimeTypeList, MainFrame.accentRed));
        return row;
    }

    private JPanel listCard(String title, JPanel inner, Color accent) {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint bg = new GradientPaint(
                        0, 0,           MainFrame.bgCard2,
                        0, getHeight(), MainFrame.bgCard);
                g2.setPaint(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(MainFrame.divider);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(18, 20, 16, 20));

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.setBorder(new EmptyBorder(0, 0, 12, 0));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font(MainFrame.fontUI(), Font.BOLD, 13));
        titleLbl.setForeground(MainFrame.textPrimary);

        // colored underline accent dot
        JPanel dot = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, accent, getWidth(), 0,
                        new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0));
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), 2, 2, 2);
                g2.dispose();
            }
        };
        dot.setOpaque(false);
        dot.setPreferredSize(new Dimension(0, 2));

        JPanel titleBlock = new JPanel(new BorderLayout(0, 6));
        titleBlock.setOpaque(false);
        titleBlock.add(titleLbl, BorderLayout.CENTER);
        titleBlock.add(dot, BorderLayout.SOUTH);

        titleRow.add(titleBlock, BorderLayout.WEST);
        card.add(titleRow, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(inner);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    public void refresh() {
        List<CrimeRecord> crimes = dataStore.getAllCrimes();
        int total = crimes.size();

        totalCrimesVal.setText(String.format("%,d", total));
        arrestRateVal.setText(String.format("%.1f%%", dataStore.getArrestRate()));
        topCrimeVal.setText(total == 0 ? "N/A" : truncate(dataStore.getTopCrimeType(), 12));
        districtCountVal.setText(String.valueOf(dataStore.getDistricts().size()));
        peakHourVal.setText(total == 0 ? "N/A" : dataStore.getPeakHour().split(" ")[0]);

        if (total == 0) {
            statusLabel.setText("No data loaded — fetch live data or load a CSV");
            statusLabel.setForeground(MainFrame.textMuted);
        } else {
            statusLabel.setText(String.format("%,d records • %d districts • %.1f%% arrest rate",
                    total, dataStore.getDistricts().size(), dataStore.getArrestRate()));
            statusLabel.setForeground(MainFrame.accentGreen);
        }

        lastUpdatedLabel.setText("Updated " + java.time.LocalTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));

        rebuildList(rankingList, buildDistrictRows(crimes, total));
        rebuildList(crimeTypeList, buildTypeRows(crimes, total));
    }

    private List<JPanel> buildDistrictRows(List<CrimeRecord> crimes, int total) {
        Map<String, Integer> byDistrict = dataStore.getCrimeCountByDistrict();
        int max = byDistrict.values().stream().mapToInt(v -> v).max().orElse(1);
        return byDistrict.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(9)
                .map(e -> rankRow("District " + e.getKey(), e.getValue(), max, MainFrame.accentCyan))
                .collect(Collectors.toList());
    }

    private List<JPanel> buildTypeRows(List<CrimeRecord> crimes, int total) {
        int max = (int) crimes.stream()
                .collect(Collectors.groupingBy(CrimeRecord::getCrimeType, Collectors.counting()))
                .values().stream().mapToLong(v -> v).max().orElse(1L);
        return crimes.stream()
                .collect(Collectors.groupingBy(CrimeRecord::getCrimeType, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(9)
                .map(e -> rankRow(e.getKey(), (int)(long)e.getValue(), max, MainFrame.accentRed))
                .collect(Collectors.toList());
    }

    private void rebuildList(JPanel list, List<JPanel> rows) {
        list.removeAll();
        if (rows.isEmpty()) {
            JLabel ph = new JLabel("No data yet");
            ph.setForeground(MainFrame.textDim);
            ph.setFont(new Font(MainFrame.fontUI(), Font.PLAIN, 12));
            ph.setBorder(new EmptyBorder(12, 4, 4, 4));
            list.add(ph);
        } else {
            rows.forEach(list::add);
        }
        list.revalidate();
        list.repaint();
    }

    private JPanel rankRow(String label, int count, int max, Color accent) {
        return new JPanel(new BorderLayout(8, 0)) {
            {
                setOpaque(false);
                setBorder(new EmptyBorder(5, 2, 5, 2));
                setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

                JLabel name = new JLabel(truncate(label, 24));
                name.setFont(new Font(MainFrame.fontUI(), Font.PLAIN, 12));
                name.setForeground(MainFrame.textPrimary);

                JLabel cnt = new JLabel(String.format("%,d", count));
                cnt.setFont(new Font(MainFrame.fontUI(), Font.BOLD, 12));
                cnt.setForeground(accent);

                add(name, BorderLayout.WEST);
                add(cnt,  BorderLayout.EAST);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                double ratio = max > 0 ? (double) count / max : 0;
                int barW = (int)(getWidth() * ratio * 0.85);
                // bar track
                g2.setColor(MainFrame.surface);
                g2.fillRoundRect(0, getHeight() - 4, getWidth(), 3, 3, 3);
                // bar fill
                GradientPaint gp = new GradientPaint(0, 0, accent,
                        barW, 0, new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 80));
                g2.setPaint(gp);
                g2.fillRoundRect(0, getHeight() - 4, barW, 3, 3, 3);
                g2.dispose();
                super.paintComponent(g);
            }
        };
    }

    private String truncate(String s, int max) {
        if (s == null) return "N/A";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
