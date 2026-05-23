package ui;

import model.CrimeRecord;
import util.CSVParser;
import util.DataStore;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class LoadDataWorker extends SwingWorker<Integer, String> {
    private final String filePath;
    private final DataStore dataStore;
    private final MainFrame parent;
    private JDialog dialog;
    private JLabel statusLabel;

    public LoadDataWorker(String filePath, DataStore dataStore, MainFrame parent) {
        this.filePath = filePath;
        this.dataStore = dataStore;
        this.parent = parent;
        buildDialog();
    }

    private void buildDialog() {
        dialog = new JDialog(parent, "Loading...", false);
        dialog.setSize(380, 120);
        dialog.setLocationRelativeTo(parent);
        dialog.setLayout(new BorderLayout());

        JPanel p = new JPanel(new GridLayout(3, 1, 4, 4));
        p.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        p.setBackground(MainFrame.bgPanel);
        JLabel title = new JLabel("📂  Parsing CSV file…");
        title.setForeground(MainFrame.accentCyan);
        title.setFont(new Font("Segoe UI", Font.BOLD, 13));
        statusLabel = new JLabel("Reading records…");
        statusLabel.setForeground(MainFrame.textMuted);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        JProgressBar bar = new JProgressBar();
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
        publish("Parsing CSV file...");
        List<CrimeRecord> records = new CSVParser().parseCrimeCSV(filePath);
        publish(String.format("Saving %,d records...", records.size()));
        dataStore.clearCrimes();
        for (CrimeRecord r : records) dataStore.addCrime(r);
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
                    String.format("Loaded %,d crime records!", count),
                    "Done", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(parent,
                    "Error: " + e.getMessage(), "Load Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
