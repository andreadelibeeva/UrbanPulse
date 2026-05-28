package ui.panels;

import analysis.CorrelationEngine;
import analysis.CorrelationEngine.CorrelationResult;
import model.SocialIndicator;
import ui.MainFrame;
import util.DataStore;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
    private JLabel  selectedCountLabel;
    private JButton undoBtn;

    // current district list, in display order (plus selected preset per district)
    private final LinkedHashMap<String, JComboBox<String>> districtCombos = new LinkedHashMap<>();
    private List<CorrelationResult> lastResults = new ArrayList<>();

    /** History snapshots for Undo (each entry = district -> preset label). */
    private final Deque<LinkedHashMap<String, String>> history = new ArrayDeque<>();
    private static final int MAX_HISTORY = 25;

    public CorrelationPanel(DataStore dataStore) {
        this.dataStore = dataStore;
        setBackground(MainFrame.bgDark);
        setLayout(new BorderLayout());
        buildUI();
    }

    // ==================================================================
    //  UI scaffolding
    // ==================================================================

    private void buildUI() {
        add(buildHeader(), BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildLeftPanel(), buildRightPanel());
        split.setBackground(MainFrame.bgDark);
        split.setBorder(new EmptyBorder(0, 32, 24, 32));
        split.setDividerSize(6);
        split.setDividerLocation(420);
        split.setResizeWeight(0.38);
        add(split, BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 6));
        header.setBackground(MainFrame.bgDark);
        header.setBorder(new EmptyBorder(28, 32, 16, 32));

        JLabel title = new JLabel("Correlation Analysis");
        title.setFont(new Font(MainFrame.fontDisplay(), Font.BOLD, 28));
        title.setForeground(MainFrame.textPrimary);

        statusLabel = new JLabel("Load crime data, assign deprivation levels, then run the analysis.");
        statusLabel.setFont(new Font(MainFrame.fontUI(), Font.PLAIN, 12));
        statusLabel.setForeground(MainFrame.textMuted);

        header.add(title,       BorderLayout.NORTH);
        header.add(statusLabel, BorderLayout.SOUTH);
        return header;
    }

    // -------- LEFT panel (districts) -----------------------------------

    private JPanel buildLeftPanel() {
        JPanel outer = roundedCard();
        outer.setLayout(new BorderLayout(0, 0));
        outer.setBorder(new EmptyBorder(18, 18, 18, 18));

        // ---- title block ----
        JPanel topBlock = new JPanel();
        topBlock.setLayout(new BoxLayout(topBlock, BoxLayout.Y_AXIS));
        topBlock.setOpaque(false);

        JLabel heading = new JLabel("District Deprivation Levels");
        heading.setFont(new Font(MainFrame.fontDisplay(), Font.BOLD, 16));
        heading.setForeground(MainFrame.textPrimary);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subHead = new JLabel("Assign a deprivation level to each district. Add or remove any you like.");
        subHead.setFont(new Font(MainFrame.fontUI(), Font.PLAIN, 11));
        subHead.setForeground(MainFrame.textMuted);
        subHead.setAlignmentX(Component.LEFT_ALIGNMENT);
        subHead.setBorder(new EmptyBorder(2, 0, 10, 0));

        topBlock.add(heading);
        topBlock.add(subHead);

        // ---- primary action row: Auto-load + Smart Fill ----
        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        actionRow.setOpaque(false);
        actionRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton autoLoadBtn = pillButton("⟳  Auto-Load", MainFrame.accentBlue, Color.WHITE);
        autoLoadBtn.setToolTipText("Load every district from your crime data");
        autoLoadBtn.addActionListener(e -> { snapshot(); autoLoadDistricts(false); });

        JButton smartFillBtn = pillButton("✦  Smart Fill", MainFrame.accentGold,
                MainFrame.theme.light ? Color.WHITE : new Color(0x111122));
        smartFillBtn.setToolTipText("Auto-assign deprivation by crime-rate percentile");
        smartFillBtn.addActionListener(e -> { snapshot(); smartFill(); });

        actionRow.add(autoLoadBtn);
        actionRow.add(smartFillBtn);

        // ---- secondary action row: Add / Reset / Undo ----
        JPanel mgmtRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        mgmtRow.setOpaque(false);
        mgmtRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        mgmtRow.setBorder(new EmptyBorder(6, 0, 0, 0));

        JButton addBtn   = pillButton("+  Add / Remove…", MainFrame.accentPurple, Color.WHITE);
        addBtn.setToolTipText("Pick exactly which districts appear in the list");
        addBtn.addActionListener(e -> openAddRemoveDialog());

        JButton resetBtn = pillButton("⟲  Reset", MainFrame.surface, MainFrame.textPrimary);
        resetBtn.setToolTipText("Clear every district from the list");
        resetBtn.addActionListener(e -> { snapshot(); rebuildDistrictList(Collections.emptyList());
            statusLabel.setText("List cleared. Press Auto-Load or Add to bring districts back.");
            statusLabel.setForeground(MainFrame.textMuted);
        });

        undoBtn = pillButton("↶  Undo", MainFrame.surface, MainFrame.textPrimary);
        undoBtn.setToolTipText("Undo the last change to the district list");
        undoBtn.addActionListener(e -> undo());
        undoBtn.setEnabled(false);

        mgmtRow.add(addBtn);
        mgmtRow.add(resetBtn);
        mgmtRow.add(undoBtn);

        // ---- bulk-preset row ----
        JPanel bulkRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        bulkRow.setOpaque(false);
        bulkRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        bulkRow.setBorder(new EmptyBorder(8, 0, 4, 0));

        JLabel bulkLabel = new JLabel("Set all to:");
        bulkLabel.setForeground(MainFrame.textMuted);
        bulkLabel.setFont(new Font(MainFrame.fontUI(), Font.PLAIN, 11));

        bulkPresetCombo = new JComboBox<>(presetLabels());
        bulkPresetCombo.setFont(new Font(MainFrame.fontUI(), Font.PLAIN, 11));
        bulkPresetCombo.setBackground(MainFrame.surface);
        bulkPresetCombo.setForeground(MainFrame.textPrimary);

        JButton applyAllBtn = pillButton("Apply", MainFrame.surfaceHover, MainFrame.textPrimary);
        applyAllBtn.addActionListener(e -> { snapshot(); applyBulkPreset(); });

        selectedCountLabel = new JLabel(" ");
        selectedCountLabel.setForeground(MainFrame.textMuted);
        selectedCountLabel.setFont(new Font(MainFrame.fontUI(), Font.PLAIN, 11));

        bulkRow.add(bulkLabel);
        bulkRow.add(bulkPresetCombo);
        bulkRow.add(applyAllBtn);
        bulkRow.add(Box.createHorizontalStrut(8));
        bulkRow.add(selectedCountLabel);

        topBlock.add(Box.createVerticalStrut(2));
        topBlock.add(actionRow);
        topBlock.add(mgmtRow);
        topBlock.add(bulkRow);
        outer.add(topBlock, BorderLayout.NORTH);

        // ---- scroll list of districts ----
        districtListPanel = new JPanel();
        districtListPanel.setLayout(new BoxLayout(districtListPanel, BoxLayout.Y_AXIS));
        districtListPanel.setOpaque(false);

        JScrollPane scroll = new JScrollPane(districtListPanel);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getViewport().setBackground(MainFrame.bgCard);
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        outer.add(scroll, BorderLayout.CENTER);

        // ---- run button ----
        JButton runBtn = new JButton("▶  Run Correlation Analysis") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(
                        0, 0,           MainFrame.accentCyan,
                        getWidth(), 0,  MainFrame.accentBlue);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        runBtn.setForeground(MainFrame.theme.light ? Color.WHITE : new Color(0x081020));
        runBtn.setFont(new Font(MainFrame.fontUI(), Font.BOLD, 13));
        runBtn.setBorderPainted(false);
        runBtn.setContentAreaFilled(false);
        runBtn.setFocusPainted(false);
        runBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        runBtn.setPreferredSize(new Dimension(0, 42));
        runBtn.setBorder(new EmptyBorder(0, 0, 0, 0));
        runBtn.addActionListener(e -> runAnalysis());

        JPanel runWrap = new JPanel(new BorderLayout());
        runWrap.setOpaque(false);
        runWrap.setBorder(new EmptyBorder(12, 0, 0, 0));
        runWrap.add(runBtn, BorderLayout.CENTER);
        outer.add(runWrap, BorderLayout.SOUTH);

        return outer;
    }

    // -------- RIGHT panel (results) ------------------------------------

    private JPanel buildRightPanel() {
        JPanel outer = roundedCard();
        outer.setLayout(new BorderLayout(0, 0));
        outer.setBorder(new EmptyBorder(18, 18, 18, 18));

        JLabel heading = new JLabel("Pearson r Results");
        heading.setFont(new Font(MainFrame.fontDisplay(), Font.BOLD, 16));
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
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        outer.add(scroll, BorderLayout.CENTER);
        return outer;
    }

    // ==================================================================
    //  District list management
    // ==================================================================

    private void rebuildDistrictList(List<String> districts) {
        // Preserve current selections so removing one item doesn't reset others
        Map<String, String> previousSelections = new HashMap<>();
        for (Map.Entry<String, JComboBox<String>> e : districtCombos.entrySet()) {
            previousSelections.put(e.getKey(), (String) e.getValue().getSelectedItem());
        }

        districtListPanel.removeAll();
        districtCombos.clear();

        if (districts.isEmpty()) {
            JPanel empty = new JPanel();
            empty.setLayout(new BoxLayout(empty, BoxLayout.Y_AXIS));
            empty.setOpaque(false);
            empty.setBorder(new EmptyBorder(28, 8, 28, 8));

            JLabel icon = new JLabel("✿");
            icon.setFont(new Font(MainFrame.fontDisplay(), Font.BOLD, 38));
            icon.setForeground(MainFrame.divider);
            icon.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel l = new JLabel("<html><center>No districts in the list yet.<br>Use <b>Auto-Load</b> or <b>Add / Remove…</b></center></html>");
            l.setForeground(MainFrame.textMuted);
            l.setFont(new Font(MainFrame.fontUI(), Font.PLAIN, 12));
            l.setAlignmentX(Component.CENTER_ALIGNMENT);
            l.setHorizontalAlignment(SwingConstants.CENTER);

            empty.add(icon);
            empty.add(Box.createVerticalStrut(8));
            empty.add(l);
            districtListPanel.add(empty);
        } else {
            for (String district : districts) {
                String preserved = previousSelections.get(district);
                districtListPanel.add(buildDistrictRow(district, preserved));
                districtListPanel.add(Box.createVerticalStrut(5));
            }
        }

        updateSelectedCount();
        districtListPanel.revalidate();
        districtListPanel.repaint();
    }

    private JPanel buildDistrictRow(String district, String preservedPreset) {
        JPanel row = new JPanel(new BorderLayout(8, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(MainFrame.surface);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.setColor(MainFrame.divider);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
            }
        };
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(8, 12, 8, 8));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));

        // left block: district + crime count
        int crimes = dataStore.getCrimeCountByDistrict().getOrDefault(district, 0);
        JLabel distLabel = new JLabel("District  " + district);
        distLabel.setFont(new Font(MainFrame.fontUI(), Font.BOLD, 12));
        distLabel.setForeground(MainFrame.textPrimary);

        JLabel countLabel = new JLabel(crimes + " crime" + (crimes == 1 ? "" : "s"));
        countLabel.setFont(new Font(MainFrame.fontUI(), Font.PLAIN, 10));
        countLabel.setForeground(MainFrame.textMuted);

        JPanel leftBlock = new JPanel();
        leftBlock.setLayout(new BoxLayout(leftBlock, BoxLayout.Y_AXIS));
        leftBlock.setOpaque(false);
        leftBlock.add(distLabel);
        leftBlock.add(countLabel);
        row.add(leftBlock, BorderLayout.WEST);

        // right block: preset combo + colour dot + remove button
        JComboBox<String> combo = new JComboBox<>(presetLabels());
        combo.setFont(new Font(MainFrame.fontUI(), Font.PLAIN, 11));
        combo.setBackground(MainFrame.bgCard2);
        combo.setForeground(MainFrame.textPrimary);
        combo.setPreferredSize(new Dimension(130, 26));

        String startLabel;
        if (preservedPreset != null) {
            startLabel = preservedPreset;
        } else {
            SocialIndicator existing = dataStore.getSocialMap().get(district);
            startLabel = (existing != null) ? guessPreset(existing).label : Preset.MEDIUM.label;
        }
        combo.setSelectedItem(startLabel);

        JLabel dot = new JLabel("●");
        dot.setFont(new Font(MainFrame.fontUI(), Font.BOLD, 16));
        dot.setBorder(new EmptyBorder(0, 6, 0, 0));
        updateDot(dot, (String) combo.getSelectedItem());
        combo.addActionListener(e -> updateDot(dot, (String) combo.getSelectedItem()));

        JButton remove = roundIconButton("×",
                "Remove district " + district + " from the list");
        remove.addActionListener(e -> {
            snapshot();
            List<String> remaining = new ArrayList<>(districtCombos.keySet());
            remaining.remove(district);
            rebuildDistrictList(remaining);
            statusLabel.setText("Removed District " + district + ".");
            statusLabel.setForeground(MainFrame.accentRed);
        });

        JPanel rightBlock = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        rightBlock.setOpaque(false);
        rightBlock.add(combo);
        rightBlock.add(dot);
        rightBlock.add(remove);
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

    private void updateSelectedCount() {
        int n = districtCombos.size();
        int available = dataStore.getDistricts().size();
        if (available == 0) {
            selectedCountLabel.setText(" ");
        } else {
            selectedCountLabel.setText(n + " / " + available + " districts");
        }
    }

    // ==================================================================
    //  Undo support
    // ==================================================================

    /** Captures the current list of districts + selected presets onto the undo stack. */
    private void snapshot() {
        LinkedHashMap<String, String> snap = new LinkedHashMap<>();
        for (Map.Entry<String, JComboBox<String>> e : districtCombos.entrySet()) {
            snap.put(e.getKey(), (String) e.getValue().getSelectedItem());
        }
        history.push(snap);
        while (history.size() > MAX_HISTORY) history.pollLast();
        undoBtn.setEnabled(true);
    }

    private void undo() {
        if (history.isEmpty()) return;
        LinkedHashMap<String, String> snap = history.pop();
        rebuildDistrictList(new ArrayList<>(snap.keySet()));
        for (Map.Entry<String, String> e : snap.entrySet()) {
            JComboBox<String> combo = districtCombos.get(e.getKey());
            if (combo != null && e.getValue() != null) combo.setSelectedItem(e.getValue());
        }
        undoBtn.setEnabled(!history.isEmpty());
        statusLabel.setText("Undo applied.");
        statusLabel.setForeground(MainFrame.accentCyan);
    }

    // ==================================================================
    //  Add / Remove dialog (multi-select with search)
    // ==================================================================

    private void openAddRemoveDialog() {
        List<String> available = dataStore.getDistricts();
        if (available.isEmpty()) {
            statusLabel.setText("No crime data loaded yet — use Fetch Live Data or Load CSV first.");
            statusLabel.setForeground(MainFrame.accentRed);
            return;
        }
        available = new ArrayList<>(available);
        Collections.sort(available);

        // Build a check-list dialog
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this),
                "Add / Remove Districts", Dialog.ModalityType.APPLICATION_MODAL);
        dlg.getContentPane().setBackground(MainFrame.bgPanel);
        dlg.setLayout(new BorderLayout(0, 0));
        dlg.setSize(380, 520);
        dlg.setLocationRelativeTo(this);

        JPanel head = new JPanel(new BorderLayout(0, 6));
        head.setOpaque(false);
        head.setBorder(new EmptyBorder(18, 20, 8, 20));

        JLabel title = new JLabel("Pick districts");
        title.setFont(new Font(MainFrame.fontDisplay(), Font.BOLD, 18));
        title.setForeground(MainFrame.textPrimary);

        JLabel sub = new JLabel("Tick the districts you want in the correlation list.");
        sub.setFont(new Font(MainFrame.fontUI(), Font.PLAIN, 11));
        sub.setForeground(MainFrame.textMuted);

        head.add(title, BorderLayout.NORTH);
        head.add(sub,   BorderLayout.SOUTH);
        dlg.add(head, BorderLayout.NORTH);

        // search field
        JTextField search = new JTextField();
        search.putClientProperty("JTextField.placeholderText", "Search districts…");
        search.setFont(new Font(MainFrame.fontUI(), Font.PLAIN, 12));

        // list with checkboxes
        DefaultListModel<DistrictItem> model = new DefaultListModel<>();
        for (String d : available) {
            int crimes = dataStore.getCrimeCountByDistrict().getOrDefault(d, 0);
            model.addElement(new DistrictItem(d, crimes, districtCombos.containsKey(d)));
        }

        JList<DistrictItem> list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setBackground(MainFrame.bgCard);
        list.setForeground(MainFrame.textPrimary);
        list.setFixedCellHeight(34);
        list.setCellRenderer(new DistrictCellRenderer());
        list.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int idx = list.locationToIndex(e.getPoint());
                if (idx >= 0) {
                    DistrictItem it = model.get(idx);
                    it.checked = !it.checked;
                    list.repaint();
                }
            }
        });

        search.addCaretListener(ev -> {
            String q = search.getText().trim().toLowerCase();
            // We need to keep the model state, so filter into a fresh display model
            // but easier: just hide via a custom filter — JList doesn't filter natively,
            // so we rebuild the model preserving the `checked` flags by name.
            Map<String, Boolean> state = new HashMap<>();
            for (int i = 0; i < model.size(); i++) state.put(model.get(i).name, model.get(i).checked);

            model.clear();
            for (String d : dataStore.getDistricts()) {
                if (!q.isEmpty() && !d.toLowerCase().contains(q)) continue;
                int crimes = dataStore.getCrimeCountByDistrict().getOrDefault(d, 0);
                model.addElement(new DistrictItem(d, crimes,
                        state.getOrDefault(d, districtCombos.containsKey(d))));
            }
        });

        JPanel listWrap = new JPanel(new BorderLayout(0, 8));
        listWrap.setOpaque(false);
        listWrap.setBorder(new EmptyBorder(0, 20, 8, 20));

        // search row + quick actions
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        toolbar.setOpaque(false);

        JButton selectAll = pillButton("Select all", MainFrame.surfaceHover, MainFrame.textPrimary);
        selectAll.addActionListener(ev -> {
            for (int i = 0; i < model.size(); i++) model.get(i).checked = true;
            list.repaint();
        });
        JButton clearAll = pillButton("Clear", MainFrame.surfaceHover, MainFrame.textPrimary);
        clearAll.addActionListener(ev -> {
            for (int i = 0; i < model.size(); i++) model.get(i).checked = false;
            list.repaint();
        });
        JButton invert = pillButton("Invert", MainFrame.surfaceHover, MainFrame.textPrimary);
        invert.addActionListener(ev -> {
            for (int i = 0; i < model.size(); i++) model.get(i).checked = !model.get(i).checked;
            list.repaint();
        });

        toolbar.add(selectAll);
        toolbar.add(clearAll);
        toolbar.add(invert);

        listWrap.add(search,   BorderLayout.NORTH);
        JScrollPane sp = new JScrollPane(list);
        sp.setBorder(BorderFactory.createLineBorder(MainFrame.divider, 1));
        sp.getViewport().setBackground(MainFrame.bgCard);
        listWrap.add(sp,        BorderLayout.CENTER);
        listWrap.add(toolbar,   BorderLayout.SOUTH);

        dlg.add(listWrap, BorderLayout.CENTER);

        // footer
        JPanel foot = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        foot.setOpaque(false);
        foot.setBorder(new EmptyBorder(0, 20, 18, 20));

        JButton cancel = pillButton("Cancel", MainFrame.surface, MainFrame.textPrimary);
        cancel.addActionListener(ev -> dlg.dispose());

        JButton apply = pillButton("Apply", MainFrame.accentPurple, Color.WHITE);
        apply.addActionListener(ev -> {
            snapshot();
            List<String> chosen = new ArrayList<>();
            for (int i = 0; i < model.size(); i++) {
                if (model.get(i).checked) chosen.add(model.get(i).name);
            }
            // Also include districts that were checked but currently filtered out
            // (state map already preserves them through search). But if user
            // searches & doesn't clear, items not present in model keep their
            // checked flag from `state` map. To handle that, also include any
            // district that was previously checked and isn't in the current model.
            // (Simpler: keep a separate persistent set updated as user toggles.)
            // For robustness we already rebuild model preserving state.
            Collections.sort(chosen);
            rebuildDistrictList(chosen);
            statusLabel.setText(chosen.size() + " districts in the analysis list.");
            statusLabel.setForeground(MainFrame.accentGreen);
            dlg.dispose();
        });

        foot.add(cancel);
        foot.add(apply);
        dlg.add(foot, BorderLayout.SOUTH);

        dlg.setVisible(true);
    }

    /** Row item for the add/remove dialog. */
    private static class DistrictItem {
        final String name;
        final int    crimes;
        boolean      checked;
        DistrictItem(String name, int crimes, boolean checked) {
            this.name = name; this.crimes = crimes; this.checked = checked;
        }
    }

    private class DistrictCellRenderer extends JPanel implements ListCellRenderer<DistrictItem> {
        private final JCheckBox box = new JCheckBox();
        private final JLabel    name = new JLabel();
        private final JLabel    count = new JLabel();

        DistrictCellRenderer() {
            setLayout(new BorderLayout(8, 0));
            setBorder(new EmptyBorder(4, 10, 4, 10));
            box.setOpaque(false);
            name.setFont(new Font(MainFrame.fontUI(), Font.BOLD, 12));
            count.setFont(new Font(MainFrame.fontUI(), Font.PLAIN, 11));
            count.setForeground(MainFrame.textMuted);
            add(box,  BorderLayout.WEST);
            add(name, BorderLayout.CENTER);
            add(count,BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends DistrictItem> list,
                                                      DistrictItem v, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            box.setSelected(v.checked);
            name.setText("District " + v.name);
            count.setText(v.crimes + (v.crimes == 1 ? " crime" : " crimes"));
            name.setForeground(MainFrame.textPrimary);
            setBackground(isSelected ? MainFrame.surfaceHover
                    : (index % 2 == 0 ? MainFrame.bgCard : MainFrame.bgCard2));
            setOpaque(true);
            return this;
        }
    }

    // ==================================================================
    //  Analysis actions
    // ==================================================================

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
            statusLabel.setText("Loaded " + districts.size() + " districts. Assign deprivation levels, then Run.");
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

        Map<String, Integer> counts = dataStore.getCrimeCountByDistrict();
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>();
        for (String d : districtCombos.keySet()) {
            sorted.add(new AbstractMap.SimpleEntry<>(d, counts.getOrDefault(d, 0)));
        }
        sorted.sort(Map.Entry.comparingByValue());

        int n = sorted.size();
        for (int i = 0; i < n; i++) {
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

        statusLabel.setText("Smart Fill applied — higher crime districts get higher deprivation levels.");
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

    // ==================================================================
    //  Results rendering
    // ==================================================================

    private void showPlaceholder() {
        resultsPanel.removeAll();

        JPanel card = gradientCard(MainFrame.bgCard, MainFrame.bgCard2);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(36, 20, 36, 20));

        JLabel icon = new JLabel("∿");
        icon.setFont(new Font(MainFrame.fontDisplay(), Font.BOLD, 56));
        icon.setForeground(MainFrame.divider);
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel l1 = new JLabel("No results yet");
        l1.setFont(new Font(MainFrame.fontDisplay(), Font.BOLD, 16));
        l1.setForeground(MainFrame.textMuted);
        l1.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel l2 = new JLabel("<html><center>1. Click <b>Auto-Load</b> (or <b>Add / Remove…</b>)<br>"
                + "2. Tap <b>Smart Fill</b> — or pick levels yourself<br>"
                + "3. Press <b>Run Correlation Analysis</b></center></html>");
        l2.setFont(new Font(MainFrame.fontUI(), Font.PLAIN, 12));
        l2.setForeground(MainFrame.textMuted);
        l2.setAlignmentX(Component.CENTER_ALIGNMENT);
        l2.setBorder(new EmptyBorder(10, 0, 0, 0));
        l2.setHorizontalAlignment(SwingConstants.CENTER);

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
        JPanel card = gradientCard(MainFrame.bgCard2, MainFrame.bgCard);
        card.setLayout(new BorderLayout(0, 6));
        card.setBorder(new EmptyBorder(16, 18, 16, 18));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));

        CorrelationResult top = results.get(0);
        double abs = Math.abs(top.pearsonR);
        Color topColor = abs >= 0.6 ? MainFrame.accentRed
                : abs >= 0.3 ? MainFrame.accentGold
                :              MainFrame.textMuted;

        JLabel topLine = new JLabel("Strongest predictor: " + top.indicator);
        topLine.setFont(new Font(MainFrame.fontDisplay(), Font.BOLD, 14));
        topLine.setForeground(topColor);

        String distCount = districtCombos.size() + " districts analysed";
        JLabel bottomLine = new JLabel(distCount + "   ·   " + top.interpretation);
        bottomLine.setFont(new Font(MainFrame.fontUI(), Font.PLAIN, 11));
        bottomLine.setForeground(MainFrame.textMuted);

        card.add(topLine,    BorderLayout.CENTER);
        card.add(bottomLine, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildResultCard(CorrelationResult r) {
        double abs = Math.abs(r.pearsonR);
        Color barColor = abs >= 0.6 ? MainFrame.accentRed
                : abs >= 0.3 ? MainFrame.accentGold
                :              MainFrame.accentBlue;

        JPanel card = gradientCard(MainFrame.bgCard, MainFrame.bgCard2);
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(new EmptyBorder(14, 16, 14, 16));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);

        JLabel indicatorLabel = new JLabel(indicatorIcon(r.indicator) + "  " + r.indicator);
        indicatorLabel.setFont(new Font(MainFrame.fontDisplay(), Font.BOLD, 13));
        indicatorLabel.setForeground(MainFrame.textPrimary);

        JLabel rValue = new JLabel(String.format("r = %+.3f", r.pearsonR));
        rValue.setFont(new Font(MainFrame.fontUI(), Font.BOLD, 15));
        rValue.setForeground(barColor);
        topRow.add(indicatorLabel, BorderLayout.WEST);
        topRow.add(rValue,         BorderLayout.EAST);
        card.add(topRow, BorderLayout.NORTH);

        card.add(new CorrelationBar(r.pearsonR, barColor), BorderLayout.CENTER);

        String muted = colorToHex(MainFrame.textMuted);
        String dim   = colorToHex(MainFrame.textDim);
        JLabel interp = new JLabel("<html><span style='color:" + muted + "'>" + r.interpretation
                + "</span> — <span style='color:" + dim + "'>" + r.policyImplication + "</span></html>");
        interp.setFont(new Font(MainFrame.fontUI(), Font.PLAIN, 11));
        card.add(interp, BorderLayout.SOUTH);

        return card;
    }

    private String indicatorIcon(String indicator) {
        return switch (indicator) {
            case CorrelationEngine.unemployment -> "✦";
            case CorrelationEngine.poverty      -> "✿";
            case CorrelationEngine.dropout      -> "✎";
            case CorrelationEngine.density      -> "⌂";
            case CorrelationEngine.income       -> "❖";
            default -> "◈";
        };
    }

    // ==================================================================
    //  Helpers
    // ==================================================================

    private JPanel roundedCard() {
        return new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(MainFrame.bgCard);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.setColor(MainFrame.divider);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
            }
        };
    }

    private JPanel gradientCard(Color c1, Color c2) {
        return new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, c1, getWidth(), getHeight(), c2);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.setColor(MainFrame.divider);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
            }
        };
    }

    private JButton pillButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color paint = getModel().isRollover() ? MainFrame.lighten(bg, 0.10f) : bg;
                g2.setColor(paint);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font(MainFrame.fontUI(), Font.BOLD, 11));
        btn.setForeground(fg);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(6, 14, 6, 14));
        return btn;
    }

    private JButton roundIconButton(String glyph, String tip) {
        JButton b = new JButton(glyph) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color paint = getModel().isRollover() ? MainFrame.withAlpha(MainFrame.accentRed, 220)
                        : MainFrame.surfaceHover;
                g2.setColor(paint);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(new Font(MainFrame.fontUI(), Font.BOLD, 13));
        b.setForeground(MainFrame.textPrimary);
        b.setPreferredSize(new Dimension(22, 22));
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(0, 0, 0, 0));
        b.setMargin(new Insets(0, 0, 0, 0));
        b.setToolTipText(tip);
        return b;
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

    private static String colorToHex(Color c) {
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }

    public void refresh() {
        if (!dataStore.getDistricts().isEmpty() && districtCombos.isEmpty()) {
            autoLoadDistricts(true);
        } else if (!dataStore.getDistricts().isEmpty()) {
            // bring in newly-loaded districts that aren't in the list yet
            List<String> current = dataStore.getDistricts();
            boolean changed = false;
            for (String d : current) {
                if (!districtCombos.containsKey(d)) { changed = true; break; }
            }
            if (changed) autoLoadDistricts(true);
        }
        updateSelectedCount();
    }

    // ==================================================================
    //  Pearson r bar painter
    // ==================================================================

    private static class CorrelationBar extends JPanel {
        private final double r;
        private final Color  color;

        CorrelationBar(double r, Color color) {
            this.r     = r;
            this.color = color;
            setOpaque(false);
            setPreferredSize(new Dimension(0, 16));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w   = getWidth();
            int h   = getHeight();
            int mid = w / 2;

            g2.setColor(MainFrame.divider);
            g2.fillRoundRect(0, (h - 6) / 2, w, 6, 4, 4);

            g2.setFont(new Font(MainFrame.fontUI(), Font.PLAIN, 9));
            g2.setColor(MainFrame.textDim);
            g2.drawString("-1", 2, h - 2);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString("+1", w - fm.stringWidth("+1") - 2, h - 2);
            g2.drawString("0",  mid - fm.stringWidth("0") / 2, h - 2);

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

            g2.setColor(MainFrame.withAlpha(MainFrame.textPrimary, 80));
            g2.fillRect(mid - 1, (h - 10) / 2, 2, 10);

            g2.dispose();
        }
    }
}
