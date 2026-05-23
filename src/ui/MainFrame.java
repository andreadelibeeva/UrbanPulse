package ui;

import util.DataStore;
import ui.panels.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MainFrame extends JFrame {
    //colorsss
    public static final Color bgDark      = new Color(0x08081A);
    public static final Color bgPanel     = new Color(0x0C0C22);
    public static final Color bgCard      = new Color(0x10102E);
    public static final Color bgCard2     = new Color(0x0D1A38);
    public static final Color accentBlue  = new Color(0x2979FF);
    public static final Color accentCyan  = new Color(0x00E5FF);
    public static final Color accentRed   = new Color(0xFF3366);
    public static final Color accentGold  = new Color(0xFFC300);
    public static final Color accentGreen = new Color(0x00E676);
    public static final Color accentPurple= new Color(0x8B5CF6);
    public static final Color textPrimary = new Color(0xEEEEFF);
    public static final Color textMuted   = new Color(0x6B7090);
    public static final Color textDim     = new Color(0x3A3A5E);

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

    private JLabel recordCountLabel;
    private String activePanel = panelDashboard;
    private final JButton[] navButtons = new JButton[5];

    public MainFrame() {
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
        setSize(1300, 820);
        setMinimumSize(new Dimension(1100, 700));
        setLocationRelativeTo(null);
        getContentPane().setBackground(bgDark);
        setLayout(new BorderLayout());

        add(buildSidebar(), BorderLayout.WEST);

        cardLayout  = new CardLayout();
        contentArea = new JPanel(cardLayout);
        contentArea.setBackground(bgDark);

        dashboardPanel   = new DashboardPanel(dataStore);
        heatmapPanel     = new HeatmapPanel(dataStore);
        dataTablePanel   = new DataTablePanel(dataStore);
        correlationPanel = new CorrelationPanel(dataStore);

        contentArea.add(dashboardPanel,            panelDashboard);
        contentArea.add(heatmapPanel,              panelHeatmap);
        contentArea.add(dataTablePanel,            panelTable);
        contentArea.add(correlationPanel,          panelCorrelation);
        contentArea.add(new PolicyBriefPanel(dataStore), panelBrief);

        add(contentArea, BorderLayout.CENTER);
        cardLayout.show(contentArea, panelDashboard);

        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) {
                try { dataStore.saveAll(); } catch (Exception ignored) {}
            }
        });
    }

    //sidebar

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(
                        0, 0,             new Color(0x0F0F28),
                        0, getHeight(),   new Color(0x07071A));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // right border line
                g2.setColor(new Color(0x1E1E42));
                g2.fillRect(getWidth() - 1, 0, 1, getHeight());
                g2.dispose();
            }
        };
        sidebar.setOpaque(false);
        sidebar.setPreferredSize(new Dimension(240, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));

        sidebar.add(buildLogo());
        sidebar.add(makeSep());

        String[][] nav = {
                {"◈", panelDashboard,   "Overview & key stats"},
                {"◉", panelHeatmap,     "Crime density map"},
                {"≡", panelTable,       "Browse & edit records"},
                {"∿", panelCorrelation, "Social indicator links"},
                {"§", panelBrief,       "Generate policy report"}
        };

        for (int i = 0; i < nav.length; i++) {
            navButtons[i] = navBtn(nav[i][0], nav[i][1], nav[i][2], i);
            sidebar.add(navButtons[i]);
        }

        sidebar.add(Box.createVerticalGlue());
        sidebar.add(makeSep());
        sidebar.add(buildDataSection());

        return sidebar;
    }

    private JPanel buildLogo() {
        JPanel logo = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // glow behind circle
                g2.setColor(new Color(0x00E5FF, true));
                for (int r = 18; r > 0; r -= 3) {
                    g2.setColor(new Color(0, 229, 255, 8));
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
        logo.setMaximumSize(new Dimension(240, 100));

        JPanel nameRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        nameRow.setOpaque(false);

        JLabel t1 = new JLabel("URBAN");
        t1.setFont(new Font("Segoe UI", Font.BOLD, 20));
        t1.setForeground(accentCyan);

        JLabel t2 = new JLabel("PULSE");
        t2.setFont(new Font("Segoe UI", Font.BOLD, 20));
        t2.setForeground(accentRed);

        nameRow.add(t1);
        nameRow.add(t2);

        JLabel tagline = new JLabel("Crime & Society Analyzer");
        tagline.setFont(new Font("Segoe UI", Font.PLAIN, 10));
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
                    GradientPaint gp = new GradientPaint(
                            0, 0, new Color(0x2979FF, true),
                            getWidth(), 0, new Color(0x08081A, true));
                    // subtle glow bg
                    g2.setPaint(new GradientPaint(0, 0, new Color(0x1A2A50), getWidth(), 0, new Color(0x0C0C22)));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    // left accent bar
                    g2.setColor(accentCyan);
                    g2.fillRoundRect(0, 6, 3, getHeight() - 12, 3, 3);
                } else if (hover) {
                    g2.setColor(new Color(0x15153A));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };

        btn.setText("  " + icon + "   " + panel);
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(240, 46));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(new EmptyBorder(0, 14, 0, 8));
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setForeground(panel.equals(activePanel) ? accentCyan : textMuted);
        btn.setFont(new Font("Segoe UI", panel.equals(activePanel) ? Font.BOLD : Font.PLAIN, 13));
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
            for (JButton b : navButtons) b.repaint();
            updateNavButtonStyles();
        });

        return btn;
    }

    private void updateNavButtonStyles() {
        String[] panels = {panelDashboard, panelHeatmap, panelTable, panelCorrelation, panelBrief};
        for (int i = 0; i < navButtons.length; i++) {
            boolean active = activePanel.equals(panels[i]);
            navButtons[i].setForeground(active ? accentCyan : textMuted);
            navButtons[i].setFont(new Font("Segoe UI", active ? Font.BOLD : Font.PLAIN, 13));
        }
    }

    private JPanel buildDataSection() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(12, 16, 20, 16));
        panel.setMaximumSize(new Dimension(240, 160));

        // Record count badge
        recordCountLabel = new JLabel("0 records loaded");
        recordCountLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        recordCountLabel.setForeground(textMuted);
        recordCountLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(recordCountLabel);
        panel.add(Box.createVerticalStrut(10));

        // Fetch Live Data button
        JButton apiBtn = makeDataButton("🌐  Fetch Live Data", accentCyan, bgDark, true);
        apiBtn.addActionListener(e -> FetchAPIWorker.showAndRun(this, dataStore));
        panel.add(apiBtn);
        panel.add(Box.createVerticalStrut(8));

        // Load CSV button
        JButton csvBtn = makeDataButton("📂  Load CSV File", new Color(0x2A2A4A), textPrimary, false);
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
                    GradientPaint gp = new GradientPaint(0, 0, bg.brighter(), getWidth(), getHeight(), bg);
                    g2.setPaint(gp);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                } else {
                    g2.setColor(bg);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                    g2.setColor(new Color(0x2A2A5A));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setText(text);
        btn.setForeground(fg);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(210, 36));
        btn.setPreferredSize(new Dimension(210, 36));
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
        sep.setForeground(new Color(0x1A1A3A));
        sep.setMaximumSize(new Dimension(240, 1));
        return sep;
    }
}
