package util;

import model.CrimeRecord;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChicagoDataAPI {
    private static final String endpoint =
            "https://data.cityofchicago.org/resource/ijzp-q8t2.json";
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    public List<CrimeRecord> fetchRecentCrimes(int limit) throws Exception {
        int safeLimit = Math.max(1, Math.min(limit, 50_000));
        String url = endpoint + "limit" + safeLimit + "&$order = date DESC";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(45))
                .GET()
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if(resp.statusCode() != 200) {
            throw new RuntimeException("Chicago API returned HTTP" + resp.statusCode());
        }
        return parseJsonArray(resp.body());
    }

    private static final Pattern objectRx = Pattern.compile("\\{[^\\{\\}]*\\}");

    private List<CrimeRecord> parseJsonArray(String body) {
        List<CrimeRecord> out = new ArrayList<>();
        Matcher m = objectRx.matcher(body);
        int idCounter = 1;
        while (m.find()) {
            String obj = m.group();
            String district = field(obj, "district");
            String dateStr  = field(obj, "date");
            String type     = field(obj, "primary_type");
            String desc     = field(obj, "description");
            String arrestS  = field(obj, "arrest");

            if (district.isEmpty() || dateStr.isEmpty() || type.isEmpty()) continue;

            LocalDate date;
            int hour = 0;
            try {
                // Socrata ISO timestamp: 2024-10-21T14:33:00.000
                date = LocalDate.parse(dateStr.substring(0, 10));
                hour = Integer.parseInt(dateStr.substring(11, 13));
            } catch (Exception e) {
                continue;
            }
            boolean arrested = "true".equalsIgnoreCase(arrestS);

            out.add(new CrimeRecord(
                    idCounter++, district, date,
                    type.toUpperCase(), desc, arrested, hour));
        }
        return out;
    }

    private String field(String obj, String key) {
        Pattern p = Pattern.compile("" + key + "\\s*:\\s*\\([^\\]*)");
        Matcher m = p.matcher(obj);
        return m.find() ? m.group(1) : "";
    }
}
