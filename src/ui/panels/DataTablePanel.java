package ui.panels;

import model.CrimeRecord;
import ui.MainFrame;
import util.DataStore;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DataTablePanel extends JPanel {

    private final DataStore dataStore;
    private JTable table;
    private CrimeTableModel tableModel;
    private JTextField searchField;
    private JComboBox<String> districtFilter;
    private JComboBox<String> typeFilter;
    private JLabel countLabel;

    public DataTablePanel(DataStore dataStore) {
        this.dataStore = dataStore;
        setBackground(MainFrame.bgDark);
        setLayout(new BorderLayout());
        buildUI();
        refresh();
    }

    private void buildUI() {
        JPanel header = new JPanel(new BorderLayout(0, 12));
        header.setBackground(MainFrame.bgDark);
        header.setBorder(new EmptyBorder(28, 32, 16, 32));

        JLabel title = new JLabel("Crime Records");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(MainFrame.textPrimary);
        header.add(title, BorderLayout.NORTH);
        header.add(buildFilterBar(), BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        tableModel = new CrimeTableModel();
        table = new JTable(tableModel);
        styleTable();

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(null);
        scroll.setBackground(MainFrame.bgCard);
        scroll.getViewport().setBackground(MainFrame.bgCard);
        JPanel tableWrap = new JPanel(new BorderLayout());
        tableWrap.setBackground(MainFrame.bgDark);
        tableWrap.setBorder(new EmptyBorder(0, 32, 0, 32));
        tableWrap.add(scroll, BorderLayout.CENTER);
        add(tableWrap, BorderLayout.CENTER);

        add(buildActionBar(), BorderLayout.SOUTH);
    }

    private JPanel buildFilterBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        bar.setBackground(MainFrame.bgDark);

        searchField = new JTextField(18);
        searchField.putClientProperty("JTextField.placeholderText", "Search description or crime type...");
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        searchField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) { applyFilters(); }
        });

        districtFilter = new JComboBox<>();
        typeFilter = new JComboBox<>();
        districtFilter.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        typeFilter.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        districtFilter.addActionListener(e -> applyFilters());
        typeFilter.addActionListener(e -> applyFilters());

        countLabel = new JLabel("0 records");
        countLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        countLabel.setForeground(MainFrame.textMuted);

        bar.add(new JLabel(styledLabel("🔍 Search:")));
        bar.add(searchField);
        bar.add(Box.createHorizontalStrut(8));
        bar.add(new JLabel(styledLabel("District:")));
        bar.add(districtFilter);
        bar.add(new JLabel(styledLabel("Type:")));
        bar.add(typeFilter);
        bar.add(Box.createHorizontalStrut(16));
        bar.add(countLabel);
        return bar;
    }

    private JPanel buildActionBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        bar.setBackground(MainFrame.bgDark);
        bar.setBorder(new EmptyBorder(4, 32, 12, 32));

        JButton addBtn    = actionButton("+ Add Record",    MainFrame.accentCyan, Color.BLACK);
        JButton editBtn   = actionButton("✎ Edit",          MainFrame.accentGold, Color.BLACK);
        JButton deleteBtn = actionButton("✕ Delete",        MainFrame.accentRed,  Color.WHITE);
        JButton clearBtn  = actionButton("⚠ Clear All",     new Color(0x444455), Color.WHITE);

        addBtn.addActionListener(e -> openAddDialog());
        editBtn.addActionListener(e -> openEditDialog());
        deleteBtn.addActionListener(e -> deleteSelected());
        clearBtn.addActionListener(e -> clearAll());

        bar.add(addBtn);
        bar.add(editBtn);
        bar.add(deleteBtn);
        bar.add(Box.createHorizontalStrut(16));
        bar.add(clearBtn);
        return bar;
    }

    private JButton actionButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void styleTable() {
        table.setBackground(MainFrame.bgCard);
        table.setForeground(MainFrame.textPrimary);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setRowHeight(28);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionBackground(MainFrame.accentBlue);
        table.setSelectionForeground(MainFrame.textPrimary);
        table.setAutoCreateRowSorter(true);

        JTableHeader th = table.getTableHeader();
        th.setBackground(new Color(0x0F1928));
        th.setForeground(MainFrame.textMuted);
        th.setFont(new Font("Segoe UI", Font.BOLD, 11));
        th.setPreferredSize(new Dimension(0, 32));

        table.getColumnModel().getColumn(0).setPreferredWidth(55);
        table.getColumnModel().getColumn(1).setPreferredWidth(70);
        table.getColumnModel().getColumn(2).setPreferredWidth(95);
        table.getColumnModel().getColumn(3).setPreferredWidth(140);
        table.getColumnModel().getColumn(4).setPreferredWidth(200);
        table.getColumnModel().getColumn(5).setPreferredWidth(70);
        table.getColumnModel().getColumn(6).setPreferredWidth(55);

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object value, boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, value, sel, focus, row, col);
                setBorder(new EmptyBorder(0, 10, 0, 10));
                if (!sel) {
                    setBackground(row % 2 == 0 ? MainFrame.bgCard : new Color(0x1E1E32));
                    setForeground(MainFrame.textPrimary);
                }
                if (col == 5 && !sel) {
                    boolean arrested = "Yes".equals(value);
                    setForeground(arrested ? new Color(0x00C48C) : MainFrame.accentRed);
                }
                return this;
            }
        });
    }

    public void refresh() {
        String selDistrict = (String) districtFilter.getSelectedItem();
        String selType     = (String) typeFilter.getSelectedItem();

        districtFilter.removeAllItems();
        districtFilter.addItem("All Districts");
        dataStore.getDistricts().forEach(districtFilter::addItem);
        if (selDistrict != null) districtFilter.setSelectedItem(selDistrict);

        typeFilter.removeAllItems();
        typeFilter.addItem("All Types");
        dataStore.getCrimeTypes().forEach(typeFilter::addItem);
        if (selType != null) typeFilter.setSelectedItem(selType);

        applyFilters();
    }

    private void applyFilters() {
        String query    = searchField.getText().trim().toLowerCase();
        String district = (String) districtFilter.getSelectedItem();
        String type     = (String) typeFilter.getSelectedItem();

        List<CrimeRecord> filtered = dataStore.getAllCrimes().stream()
                .filter(c -> {
                    if (!query.isEmpty()) {
                        return c.getCrimeType().toLowerCase().contains(query)
                                || c.getDescription().toLowerCase().contains(query);
                    }
                    return true;
                })
                .filter(c -> district == null || district.equals("All Districts")
                        || c.getDistrict().equals(district))
                .filter(c -> type == null || type.equals("All Types")
                        || c.getCrimeType().equals(type))
                .collect(Collectors.toList());

        tableModel.setData(filtered);
        countLabel.setText(String.format("%,d records", filtered.size()));
    }

    private void openAddDialog() {
        CrimeRecord blank = new CrimeRecord(
                dataStore.getNextCrimeId(), "", LocalDate.now(), "", "", false, 0);
        CrimeRecord result = showEditDialog(blank, true);
        if (result != null) {
            dataStore.addCrime(result);
            refresh();
        }
    }

    private void openEditDialog() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a row to edit.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        CrimeRecord existing = tableModel.getRecord(table.convertRowIndexToModel(row));
        CrimeRecord result   = showEditDialog(existing, false);
        if (result != null) {
            dataStore.updateCrime(result);
            refresh();
        }
    }

    private CrimeRecord showEditDialog(CrimeRecord record, boolean isNew) {
        JTextField idField   = new JTextField(String.valueOf(record.getId()), 10);
        idField.setEditable(false);
        JTextField distField = new JTextField(record.getDistrict(), 10);
        JTextField dateField = new JTextField(record.getDate().toString(), 10);
        JTextField typeField = new JTextField(record.getCrimeType(), 10);
        JTextField descField = new JTextField(record.getDescription(), 20);
        JTextField hourField = new JTextField(String.valueOf(record.getHour()), 5);
        JCheckBox arrestBox  = new JCheckBox("Arrested", record.getArrested());

        Object[] fields = {
                "ID:", idField,
                "District:", distField,
                "Date (yyyy-MM-dd):", dateField,
                "Crime Type:", typeField,
                "Description:", descField,
                "Hour (0-23):", hourField,
                arrestBox
        };

        int opt = JOptionPane.showConfirmDialog(this, fields,
                isNew ? "Add Crime Record" : "Edit Crime Record",
                JOptionPane.OK_CANCEL_OPTION);
        if (opt != JOptionPane.OK_OPTION) return null;

        try {
            LocalDate date = LocalDate.parse(dateField.getText().trim());
            int hour = Integer.parseInt(hourField.getText().trim());
            return new CrimeRecord(
                    record.getId(), distField.getText().trim(), date,
                    typeField.getText().trim().toUpperCase(),
                    descField.getText().trim(),
                    arrestBox.isSelected(), hour);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid input: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    private void deleteSelected() {
        int[] rows = table.getSelectedRows();
        if (rows.length == 0) {
            JOptionPane.showMessageDialog(this, "Select rows to delete.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int opt = JOptionPane.showConfirmDialog(this,
                "Delete " + rows.length + " selected record(s)?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (opt != JOptionPane.YES_OPTION) return;

        for (int row : rows) {
            CrimeRecord r = tableModel.getRecord(table.convertRowIndexToModel(row));
            dataStore.deleteCrime(r.getId());
        }
        refresh();
    }

    private void clearAll() {
        int opt = JOptionPane.showConfirmDialog(this,
                "Delete ALL " + dataStore.getTotalCrimes() + " crime records?\nThis cannot be undone.",
                "Confirm Clear All", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opt != JOptionPane.YES_OPTION) return;
        dataStore.clearCrimes();
        refresh();
    }

    private String styledLabel(String s) { return s; }

    static class CrimeTableModel extends AbstractTableModel {
        private static final String[] COLS =
                {"ID", "District", "Date", "Crime Type", "Description", "Arrested", "Hour"};
        private List<CrimeRecord> data = new ArrayList<>();

        void setData(List<CrimeRecord> records) {
            this.data = new ArrayList<>(records);
            fireTableDataChanged();
        }

        CrimeRecord getRecord(int row) { return data.get(row); }

        @Override public int getRowCount()    { return data.size(); }
        @Override public int getColumnCount() { return COLS.length; }
        @Override public String getColumnName(int col) { return COLS[col]; }

        @Override
        public Object getValueAt(int row, int col) {
            CrimeRecord r = data.get(row);
            return switch (col) {
                case 0 -> r.getId();
                case 1 -> r.getDistrict();
                case 2 -> r.getDate().toString();
                case 3 -> r.getCrimeType();
                case 4 -> r.getDescription();
                case 5 -> r.getArrested() ? "Yes" : "No";
                case 6 -> r.getHour();
                default -> "";
            };
        }
    }
}
