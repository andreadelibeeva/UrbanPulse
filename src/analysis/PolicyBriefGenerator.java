package analysis;

import analysis.CorrelationEngine.CorrelationResult;
import model.SocialIndicator;
import util.Exportable;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PolicyBriefGenerator implements Exportable {
   private String lastContent = "";

   public String generate(String cityName, int totalCrimes, double arrestRate,
                          String topCrimeType, String peakHour,
                          List<CorrelationResult> correlations,
                          String strongestPredictor) {
       String date = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"));
       StringBuilder sb = new StringBuilder();

       sb.append("═══════════════════════════════════════════════════════\n");
       sb.append("        URBAN PULSE — POLICY BRIEF\n");
       sb.append("        City: ").append(cityName).append("\n");
       sb.append("        Generated: ").append(date).append("\n");
       sb.append("═══════════════════════════════════════════════════════\n\n");

       sb.append("1. EXECUTIVE SUMMARY\n");
       sb.append("───────────────────────────────────────────────────────\n");
       sb.append(String.format(
               "Analysis of %,d crime records. Arrest rate: %.1f%%. " +
                       "Most prevalent crime: %s. Peak activity: %s. " +
                       "Strongest social predictor: %s.\n\n",
               totalCrimes, arrestRate, topCrimeType, peakHour, strongestPredictor));

       sb.append("2. CORRELATION ANALYSIS (Pearson r)\n");
       sb.append("───────────────────────────────────────────────────────\n");
       for (CorrelationResult r : correlations) {
           sb.append(String.format("  %-25s r = %+.3f  %s\n",
                   r.indicator, r.pearsonR, r.interpretation));
       }
       sb.append("\n");

       sb.append("3. THEORETICAL FRAMEWORK\n");
       sb.append("───────────────────────────────────────────────────────\n");
       sb.append("  Social Disorganization Theory (Shaw & McKay, 1942):\n");
       sb.append("  Crime concentrates in areas of poverty and instability.\n\n");
       sb.append("  Strain Theory (Merton, 1938):\n");
       sb.append("  Economic deprivation creates pressure toward criminal adaptation.\n\n");

       sb.append("4. POLICY RECOMMENDATIONS\n");
       sb.append("───────────────────────────────────────────────────────\n");
       int i = 1;
       for (CorrelationResult r : correlations) {
           if (Math.abs(r.pearsonR) >= 0.3) {
               sb.append(String.format("  %d. %s\n\n", i++, r.policyImplication));
           }
       }

       sb.append("5. LIMITATIONS\n");
       sb.append("───────────────────────────────────────────────────────\n");
       sb.append("  • Correlation does not imply causation.\n");
       sb.append("  • Reported crime may underrepresent actual incidents.\n");
       sb.append("  • District-level analysis masks intra-district variation.\n\n");

       sb.append("═══════════════════════════════════════════════════════\n");
       sb.append("  Urban Pulse v1.0 | Computational Social Science Tool\n");
       sb.append("═══════════════════════════════════════════════════════\n");

       lastContent = sb.toString();
       return lastContent;
   }

   @Override
    public void export(String filePath) {
       try (PrintWriter pw = new PrintWriter(new FileWriter(filePath))) {
           pw.print(lastContent);
       } catch (IOException e) {
           e.printStackTrace();
       }
   }
}
