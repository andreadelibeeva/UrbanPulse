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

    private static final String ENDPOINT =
            "https://data.cityofchicago.org/resource/ijzp-q8t2.json";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    public List<CrimeRecord> fetchRecentCrimes(int limit) throws Exception {
        int safeLimit = Math.max(1, Math.min(limit, 50_000));

       URI uri = new URI(
                "https",
                "data.cityofchicago.org",
                "/resource/ijzp-q8t2.json",
                "$limit=" + safeLimit + "&$order=date DESC",
                null
        );

        HttpRequest req = HttpRequest.newBuilder()
                .uri(uri)
                .header("Accept",     "application/json")
                .header("User-Agent", "UrbanPulse/1.0 (school-project)")
                .timeout(Duration.ofSeconds(90))
                .GET()
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new RuntimeException(
                    "Chicago API returned HTTP " + resp.statusCode()
                            + ". Body: " + resp.body().substring(0, Math.min(200, resp.body().length())));
        }

        List<CrimeRecord> result = parseJsonArray(resp.body());
        if (result.isEmpty()) {
            throw new RuntimeException(
                    "API responded OK but returned 0 parseable records. "
                            + "The response format may have changed.");
        }
        return result;
    }

    private List<String> splitTopLevelObjects(String json) {
        List<String> out = new ArrayList<>();
        int depth = 0;
        int start = -1;
        boolean inString = false;
        boolean escaped  = false;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            if (escaped)           { escaped = false; continue; }
            if (c == '\\' && inString) { escaped = true; continue; }
            if (c == '"')          { inString = !inString; continue; }
            if (inString)          continue;

            if (c == '{') {
                if (depth == 0) start = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start >= 0) {
                    out.add(json.substring(start, i + 1));
                    start = -1;
                }
            }
        }
        return out;
    }

    private List<CrimeRecord> parseJsonArray(String body) {
        List<CrimeRecord> out = new ArrayList<>();
        int idCounter = 1;

        for (String obj : splitTopLevelObjects(body)) {
            String district = field(obj, "district");
            String dateStr  = field(obj, "date");
            String type     = field(obj, "primary_type");
            String desc     = field(obj, "description");
            String arrestS  = field(obj, "arrest");
            String latS     = field(obj, "latitude");
            String lonS     = field(obj, "longitude");

            if (district.isEmpty() || dateStr.isEmpty() || type.isEmpty()) continue;

            LocalDate date;
            int hour = 0;
            try {
                // Socrata ISO timestamp: 2024-10-21T14:33:00.000
                date = LocalDate.parse(dateStr.substring(0, 10));
                if (dateStr.length() >= 13) {
                    hour = Integer.parseInt(dateStr.substring(11, 13));
                }
            } catch (Exception e) {
                continue;
            }

            boolean arrested = "true".equalsIgnoreCase(arrestS);

            CrimeRecord rec = new CrimeRecord(
                    idCounter++, district.trim(), date,
                    type.trim().toUpperCase(), desc.trim(), arrested, hour);

            if (!latS.isEmpty() && !lonS.isEmpty()) {
                try {
                    rec.setLat(Double.parseDouble(latS));
                    rec.setLon(Double.parseDouble(lonS));
                } catch (NumberFormatException ignored) {}
            }

            out.add(rec);
        }
        return out;
    }

    private static final Pattern FIELD_RX =
            Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]*)\"");

    private String field(String obj, String key) {
        Matcher m = FIELD_RX.matcher(obj);
        while (m.find()) {
            if (m.group(1).equalsIgnoreCase(key)) return m.group(2);
        }
        return "";
    }
}