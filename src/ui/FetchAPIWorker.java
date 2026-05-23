package ui;

import model.CrimeRecord;
import util.ChicagoDataAPI;
import util.DataStore;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class FetchAPIWorker extends SwingWorker<Integer, String> {

    private final int limit;
    private final boolean replaceExisting;
    private final DataStore dataStore;
    private final MainFrame parent;
    private JDialog dialog;
    private JLabel statusLabel;
    private JProgressBar bar;

    public FetchAPIWorker(int limit, boolean replaceExisting, DataStore dataStore, MainFrame parent) {
        this.limit = limit;
        this.replaceExisting = replaceExisting;
        this.dataStore = dataStore;
        this.parent = parent;
        buildDialog();
    }

    public static void showAndRun(MainFrame parent, DataStore dataStore) {
        String[] options = {"500 records", "1,000 records", "5,000 records", "10,000 records"};
        int[] counts     = {500, 1000, 5000, 10000};

        JComboBox<String> countCombo = new JComboBox<>(options);
        countCombo.setSelectedIndex(1);
        countCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JCheckBox replaceBox = new JCheckBox("Replace existing data", true);
        replaceBox.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        replaceBox.setForeground(MainFrame.textPrimary);
        replaceBox.setBackground(MainFrame.bgCard);
        replaceBox.setOpaque(false);

        JLabel info = new JLabel("<html><p style='width:280px;color:#6B7090;font-size:11px'>"
                + "Fetches live crime records directly from the City of Chicago's "
                + "public data portal (data.cityofchicago.org).</p></html>");

        Object[] msg = {info, " ", "Records to fetch:", countCombo, replaceBox};

        int opt = JOptionPane.showConfirmDialog(parent, msg,
                "Fetch Live Chicago Crime Data",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (opt == JOptionPane.OK_OPTION) {
            int sel = counts[countCombo.getSelectedIndex()];
            new FetchAPIWorker(sel, replaceBox.isSelected(), dataStore, parent).execute();
        }
    }

    private void buildDialog() {
        dialog = new JDialog(parent, "Fetching live data…", false);
        dialog.setSize(380, 120);
        dialog.setLocationRelativeTo(parent);
        dialog.setLayout(new BorderLayout());
        dialog.setUndecorated(false);

        JPanel p = new JPanel(new GridLayout(3, 1, 4, 4));
        p.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        p.setBackground(MainFrame.bgPanel);

        JLabel title = new JLabel("🌐  Connecting to Chicago Data Portal…");
        title.setForeground(MainFrame.accentCyan);
        title.setFont(new Font("Segoe UI", Font.BOLD, 13));

        statusLabel = new JLabel("Sending request…");
        statusLabel.setForeground(MainFrame.textMuted);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        bar = new JProgressBar();
        bar.setIndeterminate(true);
        bar.setForeground(MainFrame.accentCyan);
        bar.setBackground(MainFrame.bgCard);

        p.add(title);
        p.add(statusLabel);
        p.add(bar);
        dialog.add(p);
        dialog.setVisible(true);
    }

    @Override
    protected Integer doInBackground() throws Exception {
        publish("Connecting to Chicago Data Portal…");
        ChicagoDataAPI api = new ChicagoDataAPI();
        List<CrimeRecord> records = api.fetchRecentCrimes(limit);

        publish(String.format("Received %,d records. Saving…", records.size()));

        if (replaceExisting) {
            dataStore.clearCrimes();
        }
        for (CrimeRecord r : records) {
            dataStore.addCrime(r);
        }
        dataStore.saveCrimes();
        return records.size();
    }

    @Override
    protected void process(List<String> chunks) {
        if (!chunks.isEmpty()) statusLabel.setText(chunks.get(chunks.size() - 1));
    }

    @Override
    protected void done() {
        dialog.dispose();
        try {
            int count = get();
            JOptionPane.showMessageDialog(parent,
                    String.format("✓  Loaded %,d live crime records from Chicago!", count),
                    "Fetch Complete", JOptionPane.INFORMATION_MESSAGE);
            parent.refreshAll();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(parent,
                    "<html><b>Fetch failed:</b><br>" + e.getMessage()
                            + "<br><br><small>Check your internet connection or try loading a CSV file instead.</small></html>",
                    "Fetch Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
