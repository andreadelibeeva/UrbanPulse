package util;

import model.CrimeRecord;
import model.SocialIndicator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class CSVParser {

    // Chicago dataset date format: MM/dd/yyyy hh:mm:ss a
    private static final DateTimeFormatter CHICAGO_FMT =
            DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss a");

    // Fallback ISO format
    private static final DateTimeFormatter ISO_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public List<CrimeRecord> parseCrimeCSV(String filePath) throws IOException {
        List<CrimeRecord> records = new ArrayList<>();
        int skipped = 0;
        int idCounter = 1;

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String headerLine = reader.readLine();   // skip header
            if (headerLine == null) return records;

            // Auto-detect layout: official file has "Case Number" in column 1
            boolean officialLayout = headerLine.toLowerCase().contains("case number");

            // Official:  date=2, type=5, desc=6, arrest=8, district=11, lat=19, lon=20
            // Legacy:    date=1, type=4, desc=5, arrest=7, district=10  (no lat/lon)
            int iDate     = officialLayout ? 2  : 1;
            int iType     = officialLayout ? 5  : 4;
            int iDesc     = officialLayout ? 6  : 5;
            int iArrest   = officialLayout ? 8  : 7;
            int iDistrict = officialLayout ? 11 : 10;
            int iLat      = officialLayout ? 19 : -1;
            int iLon      = officialLayout ? 20 : -1;
            int minCols   = officialLayout ? 12 : 11;

            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    String[] cols = splitCSV(line);
                    if (cols.length < minCols) { skipped++; continue; }

                    LocalDate date = parseDate(cols[iDate].trim());
                    if (date == null) { skipped++; continue; }

                    String crimeType = cols[iType].trim().toUpperCase();
                    String description = cols[iDesc].trim();
                    String district = cols[iDistrict].trim();
                    boolean arrested = "true".equalsIgnoreCase(cols[iArrest].trim());
                    int hour = extractHour(cols[iDate].trim());

                    CrimeRecord rec = new CrimeRecord(
                            idCounter++, district, date,
                            crimeType, description, arrested, hour);

                    // Parse lat / lon when available
                    if (iLat >= 0 && iLon >= 0
                            && cols.length > iLon
                            && !cols[iLat].trim().isEmpty()
                            && !cols[iLon].trim().isEmpty()) {
                        try {
                            rec.setLat(Double.parseDouble(cols[iLat].trim()));
                            rec.setLon(Double.parseDouble(cols[iLon].trim()));
                        } catch (NumberFormatException ignored) {}
                    }

                    records.add(rec);
                    if (records.size() >= 10_000) break;

                } catch (Exception e) {
                    skipped++;
                }
            }
        }

        System.out.printf("Parsed %d records. Skipped %d.%n", records.size(), skipped);
        return records;
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private String[] splitCSV(String line) {
        List<String> tokens = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();
        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString());
        return tokens.toArray(new String[0]);
    }

    private LocalDate parseDate(String raw) {
        try {
            return LocalDate.parse(raw, CHICAGO_FMT);
        } catch (DateTimeParseException e) {
            try {
                return LocalDate.parse(raw.substring(0, 10), ISO_FMT);
            } catch (Exception e2) {
                return null;
            }
        }
    }

    private int extractHour(String dateStr) {
        try {
            String[] parts = dateStr.split(" ");
            if (parts.length >= 3) {
                int h = Integer.parseInt(parts[1].split(":")[0]);
                if ("PM".equalsIgnoreCase(parts[2]) && h != 12) h += 12;
                if ("AM".equalsIgnoreCase(parts[2]) && h == 12) h  = 0;
                return h;
            }
        } catch (Exception ignored) {}
        return 0;
    }
}