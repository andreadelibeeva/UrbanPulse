package ui;

import com.formdev.flatlaf.FlatDarkLaf;
import util.DataStore;
import ui.panels.*;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    //Colors
    public static final Color bgDark = new Color(0x0F0F1A);
    public static final Color bgPanel     = new Color(0x1A1A2E);
    public static final Color bgCard      = new Color(0x16213E);
    public static final Color accentBlue  = new Color(0x0F3460);
    public static final Color accentCyan  = new Color(0x00D4FF);
    public static final Color accentRed   = new Color(0xE94560);
    public static final Color accentGold  = new Color(0xFFBE0B);
    public static final Color textPrimary = new Color(0xEEEEEE);
    public static final Color textMuted   = new Color(0x888899);

    private final DataStore dataStore;
    private JPanel contentArea;
    private CardLayout cardLayout;

    //Panel names for CardLayout
    public static final String panelDashboard = "Dashboard";
    public static final String panelHeatmap = "Heatmap";
    public static final String panelTable = "Data Table";
    public static final String panelCorrelation = "Correlation";
    public static final String panelBrief = "Policy Brief";

    public MainFrame() {
        this.dataStore = new DataStore();
        dataStore.loadAll();
        initUI();
    }

    private void initUI() {
        setTitle("Urban Pulse - Crime & Social Deprivation Correlator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setMinimumSize(new Dimension(1024, 680));
        setLocationRelativeTo(null);
        getContentPane().setBackground(bgDark);
        setLayout(new BorderLayout());

        //Sidebar
        add(buildSiebar(), BorderLayout.WEST);

        //Content Area
        cardLayout = new CardLayout();
        contentArea = new JPanel(cardLayout);
        contentArea.setBackground(bgDark);

        contentArea.add(new DashboardPanel(dataStore), panelDashboard);
        contentArea.add(new HeatmapPanel(dataStore), panelHeatmap);
        contentArea.add(new DataTablePanel(dataStore), panelTable);
        contentArea.add(new CorrelationPanel(dataStore), panelCorrelation);
        contentArea.add(new PolicyBriefPanel(dataStore), panelBrief);

        add(contentArea, BorderLayout.CENTER);

        //Show dashboard on start
        cardLayout.show(contentArea, panelDashboard);

        //save on close
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) {
                try {
                    dataStore.saveAll();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    private JPanel buildSiebar() {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(bgPanel);
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));

        //Logo
        JPanel logoPanel = new JPanel(new GridLayout(2, 1));
        logoPanel.setBackground(bgPanel);
        logoPanel.setBorder(BorderFactory.createEmptyBorder(24, 20, 24, 20));
        logoPanel.setMaximumSize(new Dimension(220, 90));

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titleRow.setBackground(bgPanel);
        JLabel t1 = new JLabel("URBAN");
        t1.setFont(new Font("Segoe UI", Font.BOLD, 22));
        t1.setForeground(accentCyan);
        JLabel t2 = new JLabel("PULSE");
        t2.setFont(new Font("Segoe UI", Font.BOLD, 22));
        t2.setForeground(accentRed);
        titleRow.add(t1);
        titleRow.add(t2);

        JLabel tagline = new JLabel("Crime & Society Analyzer");
        tagline.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        tagline.setForeground(textMuted);

        logoPanel.add(titleRow);
        logoPanel.add(tagline);
        sidebar.add(logoPanel);
        sidebar.add(makeSep());

        //Navigation buttons
        String[][] nav = {
                {"⬛", panelDashboard, "Overview & key stats"},
                {"\uD83D\uDD34", panelHeatmap, "Crime density map"},
                {"\uD83D\uDCCB", panelTable, "Browse crime data"},
                {"\uD83D\uDCC8", panelCorrelation, "Social indicator links"},
                {"\uD83D\uDCC4",panelBrief, "Policy brief report" }
        };

        for (String[] item: nav) {
            sidebar.add(navBtn(item[0], item[1], item[2]));
        }

        sidebar.add(Box.createVerticalGlue());
        sidebar.add(makeSep());

        //Load CSV button
        JButton loadBtn = new JButton("⬆  Load Crime CSV");
        loadBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        loadBtn.setMaximumSize(new Dimension(190, 40));
        loadBtn.setBackground(accentRed);
        loadBtn.setForeground(Color.WHITE);
        loadBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        loadBtn.setBorderPainted(false);
        loadBtn.setFocusPainted(false);
        loadBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        loadBtn.addActionListener(e -> openCSVChooser());
        sidebar.add(loadBtn);
        sidebar.add(Box.createVerticalStrut(16));

        return sidebar;
    }

    private JButton navBtn(String icon, String panel, String tip) {
        JButton btn = new JButton(icon + " " + panel);
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(220, 48));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 8));
        btn.setBackground(bgPanel);
        btn.setForeground(textPrimary);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(accentBlue); btn.setForeground(accentCyan);
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(bgPanel); btn.setForeground(textPrimary);
            }
        });
        btn.addActionListener(e -> cardLayout.show(contentArea, panel));
        return btn;
    }

    private void openCSVChooser() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Crime CSV File");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "CSV Files (*.csv)", "csv"
        ));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            new LoadDataWorker(chooser.getSelectedFile().getAbsolutePath(),
                    dataStore, this).execute();
        }
    }

    private JSeparator makeSep() {
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(0x2A2A3E));
        sep.setMaximumSize(new Dimension(220, 1));
        return sep;
    }

    public DataStore getDataStore() {
        return dataStore;
    }
}
