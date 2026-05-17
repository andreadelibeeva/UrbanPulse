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
    //Chicago dataset date format
    private static final DateTimeFormatter CHICAGO_FMT =
            DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss a");

    //Fallback ISO fformat
    private static final DateTimeFormatter ISO_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    //Crime CSV
    public List<CrimeRecord> parseCrimeCSV(String filePath) throws IOException {
        List<CrimeRecord> records = new ArrayList<>();
        int skipped = 0;
        int idCounter = 1;

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            reader.readLine();
            String line;

            while((line = reader.readLine()) != null) {
                try {
                    String[] cols = splitCSV(line);
                    if (cols.length < 10) {
                        continue;
                    }

                    LocalDate date = parseDate(cols[1].trim());
                    if (date == null) {
                        skipped++;
                        continue;
                    }

                    String crimeType = cols[4].trim().toUpperCase();
                    String description = cols[5].trim();
                    String district = cols[10].trim();
                    boolean arrested = "true".equalsIgnoreCase(cols[7].trim());
                    int hour = extractHour(cols[1].trim());

                    records.add(new CrimeRecord(
                            idCounter++, district, date,
                            crimeType, description, arrested, hour
                    ));

                    if(records.size() >= 10000) {
                        break;
                    }
                } catch (Exception e) {
                    skipped++;
                }
            }
        }

        System.out.printf("Parse %d records. Skipped %d.%n", records.size(), skipped);
        return records;
    }

    private String[] splitCSV(String line) {
        List<String> tokens = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();
        for(char c : line.toCharArray()) {
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
                if ("PM".equals(parts[2]) && h != 12) {
                    h += 12;
                }
                if("AM".equals(parts[2]) && h == 12) {
                    h = 0;
                }
                return h;
            }
        } catch (Exception ignored) {}
        return 0;
    }
}
