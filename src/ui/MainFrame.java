package ui;

import util.DataStore;
import ui.panels.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MainFrame extends JFrame {
    public static Color bgDark      = Theme.cyberNoir.bgDark;
    public static Color bgPanel     = Theme.cyberNoir.bgPanel;
    public static Color bgCard      = Theme.cyberNoir.bgCard;
    public static Color bgCard2     = Theme.cyberNoir.bgCard2;
    public static Color surface     = Theme.cyberNoir.surface;
    public static Color surfaceHover= Theme.cyberNoir.surfaceHover;
    public static Color divider     = Theme.cyberNoir.divider;
    public static Color accentBlue  = Theme.cyberNoir.accentBlue;
    public static Color accentCyan  = Theme.cyberNoir.accentCyan;
    public static Color accentRed   = Theme.cyberNoir.accentRed;
    public static Color accentGold  = Theme.cyberNoir.accentGold;
    public static Color accentGreen = Theme.cyberNoir.accentGreen;
    public static Color accentPurple= Theme.cyberNoir.accentPurple;
    public static Color textPrimary = Theme.cyberNoir.textPrimary;
    public static Color textMuted   = Theme.cyberNoir.textMuted;
    public static Color textDim     = Theme.cyberNoir.textDim;

    public static Theme theme = Theme.cyberNoir;

    public static String fontDisplay() { return theme.display(); }
    public static String fontUI()      { return Theme.fontUI; }
    public static String fontMono()    { return Theme.fontMono; }

    public static final String panelDashboard   = "Dashboard";
    public static final String panelHeatmap     = "Heatmap";
    public static final String panelTable       = "Data Table";
    public static final String panelCorrelation = "Correlation";
    public static final String panelBrief       = "Policy Brief";

    private final DataStore dataStore;
    private JPanel contentArea;
    private CardLayout cardLayout;

    private DashboardPanel   dashboardPanel;
    private HeatmapPanel     heatmapPanel;
    private DataTablePanel   dataTablePanel;
    private CorrelationPanel correlationPanel;
    private PolicyBriefPanel briefPanel;

    private JPanel  sidebar;
    private JLabel  recordCountLabel;
    private JButton themeBtn;
    private String  activePanel = panelDashboard;
    private final JButton[] navButtons = new JButton[5];

    public MainFrame() {
        Theme.registerFonts();
        this.dataStore = new DataStore();
        dataStore.loadAll();
        initUI();
    }

    public DataStore getDataStore() { return dataStore; }

    public void refreshAll() {
        SwingUtilities.invokeLater(() -> {
            dashboardPanel.refresh();
            heatmapPanel.refresh();
            dataTablePanel.refresh();
            correlationPanel.refresh();
            updateRecordCount();
        });
    }

    private void initUI() {
        try { com.formdev.flatlaf.FlatDarkLaf.setup(); } catch (Exception ignored) {}

        setTitle("Urban Pulse — Crime & Social Deprivation Correlator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1340, 840);
        setMinimumSize(new Dimension(1100, 700));
        setLocationRelativeTo(null);
        getContentPane().setBackground(bgDark);
        setLayout(new BorderLayout());

        sidebar = buildSidebar();
        add(sidebar, BorderLayout.WEST);

        cardLayout  = new CardLayout();
        contentArea = new JPanel(cardLayout);
        contentArea.setBackground(bgDark);

        dashboardPanel   = new DashboardPanel(dataStore);
        heatmapPanel     = new HeatmapPanel(dataStore);
        dataTablePanel   = new DataTablePanel(dataStore);
        correlationPanel = new CorrelationPanel(dataStore);
        briefPanel       = new PolicyBriefPanel(dataStore);

        contentArea.add(dashboardPanel,   panelDashboard);
        contentArea.add(heatmapPanel,     panelHeatmap);
        contentArea.add(dataTablePanel,   panelTable);
        contentArea.add(correlationPanel, panelCorrelation);
        contentArea.add(briefPanel,       panelBrief);

        add(contentArea, BorderLayout.CENTER);
        cardLayout.show(contentArea, panelDashboard);

        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) {
                try { dataStore.saveAll(); } catch (Exception ignored) {}
            }
        });
    }

    private JPanel buildSidebar() {
        JPanel sb = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(
                        0, 0,             lighten(bgPanel, 0.05f),
                        0, getHeight(),   darken(bgPanel,  0.20f));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(divider);
                g2.fillRect(getWidth() - 1, 0, 1, getHeight());
                g2.dispose();
            }
        };
        sb.setOpaque(false);
        sb.setPreferredSize(new Dimension(250, 0));
        sb.setLayout(new BoxLayout(sb, BoxLayout.Y_AXIS));

        sb.add(buildLogo());
        sb.add(makeSep());

        String[][] nav = {
                {"◈", panelDashboard,   "Overview & key stats"},
                {"◉", panelHeatmap,     "Crime density map"},
                {"≡", panelTable,       "Browse & edit records"},
                {"∿", panelCorrelation, "Social indicator links"},
                {"§", panelBrief,       "Generate policy report"}
        };
        for (int i = 0; i < nav.length; i++) {
            navButtons[i] = navBtn(nav[i][0], nav[i][1], nav[i][2], i);
            sb.add(navButtons[i]);
        }

        sb.add(Box.createVerticalGlue());
        sb.add(makeSep());
        sb.add(buildThemeRow());
        sb.add(buildDataSection());

        return sb;
    }

    private JPanel buildLogo() {
        JPanel logo = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                for (int r = 18; r > 0; r -= 3) {
                    g2.setColor(new Color(accentCyan.getRed(), accentCyan.getGreen(), accentCyan.getBlue(), 12));
                    g2.fillOval(20 - r, 38 - r, 20 + r * 2, 20 + r * 2);
                }
                g2.setColor(accentCyan);
                g2.fillOval(22, 40, 16, 16);
                g2.setColor(bgDark);
                g2.fillOval(26, 44, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        logo.setOpaque(false);
        logo.setLayout(new BoxLayout(logo, BoxLayout.Y_AXIS));
        logo.setBorder(new EmptyBorder(28, 56, 24, 20));
        logo.setMaximumSize(new Dimension(250, 110));

        JPanel nameRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        nameRow.setOpaque(false);

        JLabel t1 = new JLabel("Urban");
        t1.setFont(new Font(fontDisplay(), Font.BOLD, 24));
        t1.setForeground(accentCyan);

        JLabel t2 = new JLabel("Pulse");
        t2.setFont(new Font(fontDisplay(), Font.BOLD | Font.ITALIC, 24));
        t2.setForeground(accentRed);

        nameRow.add(t1);
        nameRow.add(Box.createHorizontalStrut(4));
        nameRow.add(t2);

        JLabel tagline = new JLabel("Crime & Society Analyzer");
        tagline.setFont(new Font(fontUI(), Font.PLAIN, 10));
        tagline.setForeground(textMuted);
        tagline.setAlignmentX(Component.LEFT_ALIGNMENT);

        logo.add(nameRow);
        logo.add(Box.createVerticalStrut(3));
        logo.add(tagline);
        return logo;
    }

    private JButton navBtn(String icon, String panel, String tip, int idx) {
        JButton btn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                boolean active = activePanel.equals(panel);
                boolean hover  = getModel().isRollover();

                if (active) {
                    g2.setPaint(new GradientPaint(0, 0, lighten(bgPanel, 0.10f),
                            getWidth(), 0, bgPanel));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.setColor(accentCyan);
                    g2.fillRoundRect(0, 6, 3, getHeight() - 12, 3, 3);
                } else if (hover) {
                    g2.setColor(surfaceHover);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };

        btn.setText("  " + icon + "   " + panel);
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(250, 46));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(new EmptyBorder(0, 18, 0, 8));
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setForeground(panel.equals(activePanel) ? accentCyan : textMuted);
        btn.setFont(new Font(fontUI(), panel.equals(activePanel) ? Font.BOLD : Font.PLAIN, 13));
        btn.setToolTipText(tip);

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                if (!activePanel.equals(panel)) btn.setForeground(textPrimary);
            }
            public void mouseExited(MouseEvent e) {
                if (!activePanel.equals(panel)) btn.setForeground(textMuted);
            }
        });

        btn.addActionListener(e -> {
            activePanel = panel;
            cardLayout.show(contentArea, panel);
            updateNavButtonStyles();
        });

        return btn;
    }

    private void updateNavButtonStyles() {
        String[] panels = {panelDashboard, panelHeatmap, panelTable, panelCorrelation, panelBrief};
        for (int i = 0; i < navButtons.length; i++) {
            boolean active = activePanel.equals(panels[i]);
            navButtons[i].setForeground(active ? accentCyan : textMuted);
            navButtons[i].setFont(new Font(fontUI(), active ? Font.BOLD : Font.PLAIN, 13));
            navButtons[i].repaint();
        }
    }

    private JPanel buildThemeRow() {
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setBorder(new EmptyBorder(10, 18, 4, 18));
        row.setMaximumSize(new Dimension(250, 60));

        JLabel label = new JLabel("THEME");
        label.setFont(new Font(fontUI(), Font.BOLD, 9));
        label.setForeground(textDim);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        themeBtn = new JButton(themeLabel()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(
                        0, 0,             withAlpha(accentPurple, 220),
                        getWidth(), 0,    withAlpha(accentRed,    220));
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        themeBtn.setFont(new Font(fontUI(), Font.BOLD, 11));
        themeBtn.setForeground(Color.WHITE);
        themeBtn.setOpaque(false);
        themeBtn.setContentAreaFilled(false);
        themeBtn.setBorderPainted(false);
        themeBtn.setFocusPainted(false);
        themeBtn.setMaximumSize(new Dimension(214, 32));
        themeBtn.setPreferredSize(new Dimension(214, 32));
        themeBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        themeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        themeBtn.setBorder(new EmptyBorder(0, 14, 0, 14));
        themeBtn.setToolTipText("Click to cycle through colour modes");
        themeBtn.addActionListener(e -> cycleTheme());

        row.add(label);
        row.add(Box.createVerticalStrut(4));
        row.add(themeBtn);
        return row;
    }

    private String themeLabel() {
        String glyph = switch (theme.mode) {
            case cyberNoir   -> "◐";
            case roseQuartz  -> "✿";
            case sakuraBloom -> "❀";
        };
        return glyph + "  " + theme.displayName;
    }

    private void cycleTheme() {
        Theme next = theme.next();
        applyTheme(next);
    }

    public void applyTheme(Theme t) {
        theme        = t;
        bgDark       = t.bgDark;
        bgPanel      = t.bgPanel;
        bgCard       = t.bgCard;
        bgCard2      = t.bgCard2;
        surface      = t.surface;
        surfaceHover = t.surfaceHover;
        divider      = t.divider;
        textPrimary  = t.textPrimary;
        textMuted    = t.textMuted;
        textDim      = t.textDim;
        accentBlue   = t.accentBlue;
        accentCyan   = t.accentCyan;
        accentRed    = t.accentRed;
        accentGold   = t.accentGold;
        accentGreen  = t.accentGreen;
        accentPurple = t.accentPurple;

        getContentPane().setBackground(bgDark);

        remove(sidebar);
        sidebar = buildSidebar();
        add(sidebar, BorderLayout.WEST);

        contentArea.removeAll();
        dashboardPanel   = new DashboardPanel(dataStore);
        heatmapPanel     = new HeatmapPanel(dataStore);
        dataTablePanel   = new DataTablePanel(dataStore);
        correlationPanel = new CorrelationPanel(dataStore);
        briefPanel       = new PolicyBriefPanel(dataStore);

        contentArea.add(dashboardPanel,   panelDashboard);
        contentArea.add(heatmapPanel,     panelHeatmap);
        contentArea.add(dataTablePanel,   panelTable);
        contentArea.add(correlationPanel, panelCorrelation);
        contentArea.add(briefPanel,       panelBrief);
        cardLayout.show(contentArea, activePanel);

        revalidate();
        repaint();
    }

    private JPanel buildDataSection() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(12, 18, 22, 18));
        panel.setMaximumSize(new Dimension(250, 160));

        recordCountLabel = new JLabel("0 records loaded");
        recordCountLabel.setFont(new Font(fontUI(), Font.PLAIN, 11));
        recordCountLabel.setForeground(textMuted);
        recordCountLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(recordCountLabel);
        panel.add(Box.createVerticalStrut(10));

        JButton apiBtn = makeDataButton("Fetch Live Data", accentCyan,
                theme.light ? Color.WHITE : bgDark, true);
        apiBtn.addActionListener(e -> FetchAPIWorker.showAndRun(this, dataStore));
        panel.add(apiBtn);
        panel.add(Box.createVerticalStrut(8));

        JButton csvBtn = makeDataButton("Load CSV File", surface, textPrimary, false);
        csvBtn.addActionListener(e -> openCSVChooser());
        panel.add(csvBtn);
        panel.add(Box.createVerticalStrut(4));

        updateRecordCount();
        return panel;
    }

    private JButton makeDataButton(String text, Color bg, Color fg, boolean filled) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (filled) {
                    GradientPaint gp = new GradientPaint(0, 0, lighten(bg, 0.10f),
                            getWidth(), getHeight(), bg);
                    g2.setPaint(gp);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                } else {
                    g2.setColor(bg);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                    g2.setColor(divider);
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setText(text);
        btn.setForeground(fg);
        btn.setFont(new Font(fontUI(), Font.BOLD, 12));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(214, 36));
        btn.setPreferredSize(new Dimension(214, 36));
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void updateRecordCount() {
        int n = dataStore.getTotalCrimes();
        if (n == 0) {
            recordCountLabel.setText("No data loaded");
            recordCountLabel.setForeground(textMuted);
        } else {
            recordCountLabel.setText(String.format("%,d records loaded", n));
            recordCountLabel.setForeground(accentGreen);
        }
    }

    private void openCSVChooser() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Crime CSV File");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV Files (*.csv)", "csv"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            new LoadDataWorker(chooser.getSelectedFile().getAbsolutePath(), dataStore, this).execute();
        }
    }

    private JSeparator makeSep() {
        JSeparator sep = new JSeparator();
        sep.setForeground(divider);
        sep.setMaximumSize(new Dimension(250, 1));
        return sep;
    }

    public static Color withAlpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    public static Color lighten(Color c, float frac) {
        int r = clamp((int)(c.getRed()   + (255 - c.getRed())   * frac));
        int g = clamp((int)(c.getGreen() + (255 - c.getGreen()) * frac));
        int b = clamp((int)(c.getBlue()  + (255 - c.getBlue())  * frac));
        return new Color(r, g, b);
    }

    public static Color darken(Color c, float frac) {
        int r = clamp((int)(c.getRed()   * (1 - frac)));
        int g = clamp((int)(c.getGreen() * (1 - frac)));
        int b = clamp((int)(c.getBlue()  * (1 - frac)));
        return new Color(r, g, b);
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }
}
