package ui;

import model.CrimeRecord;
import util.CSVParser;
import util.DataStore;

import javax.swing.*;
import java.util.List;

public class LoadDataWorker extends SwingWorker<Integer, String> {
    private final String filePath;
    private final DataStore dataStore;
    private final JFrame parent;
    private JDialog dialog;
    private JLabel statusLabel;

    public LoadDataWorker(String filePath, DataStore dataStore, JFrame parent) {
        this.filePath = filePath;
        this.dataStore = dataStore;
        this.parent = parent;
        buildDialog();
    }

    private void buildDialog() {
        dialog = new JDialog(parent, "Loading...", false);
        dialog.setSize(340, 100);
        dialog.setLocationRelativeTo(parent);
        dialog.setLayout(new java.awt.BorderLayout());
        JPanel p = new JPanel(new java.awt.GridLayout(2, 1, 6, 6));
        p.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        p.setBackground(MainFrame.bgPanel);
        statusLabel = new JLabel("Parsing CSV...");
        statusLabel.setForeground(MainFrame.textPrimary);
        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        p.add(statusLabel);
        p.add(bar);
        dialog.add(p);
        dialog.setVisible(true);
    }

    @Override
    protected Integer doInBackground() throws Exception {
        publish("Parsing CSV file...");
        List<CrimeRecord> records = new CSVParser().parseCrimeCSV(filePath);
        publish("Saving " + records.size() + " records...");
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
