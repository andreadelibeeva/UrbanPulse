package analysis;

import model.SocialIndicator;
import java.util.*;

public class CorrelationEngine {
    public static final String unemployment = "Unemployment Rate";
    public static final String poverty = "Poverty Rate";
    public static final String dropout = "School Dropout Rate";
    public static final String density = "Population Density";
    public static final String income = "Median Income";

    public static class CorrelationResult {
        public final String indicator;
        public final double pearsonR;
        public final String interpretation;
        public final String policyImplication;

        public CorrelationResult(String indicator, double pearsonR) {
            this.indicator = indicator;
            this.pearsonR = pearsonR;
            this.interpretation = interpret(pearsonR);
            this.policyImplication = policyFor(indicator, pearsonR);
        }

        private String interpret(double r) {
            double abs = Math.abs(r);
            String dir = r > 0 ? "positive" : "negative";
            if(abs >= 0.8) return "Very strong" + dir + " correlation";
            if (abs >= 0.6) return "Strong " + dir + " correlation";
            if (abs >= 0.4) return "Moderate " + dir + " correlation";
            if (abs >= 0.2) return "Weak " + dir + " correlation";
            return "No meaningful correlation";
        }

        private String policyFor(String indicator, double r) {
            if (Math.abs(r) < 0.3) {
                return "No clear policy signal from this indicator.";
            }
            return switch (indicator) {
                case unemployment -> r > 0
                        ? "Job creation programs and vocational training in high-crime districts amy reduce crime."
                        : "Employment alone may not be the primary driver - consider other interventions";
                case poverty -> r > 0
                        ? "Targeted welfare, housing support, and poverty reduction programs are recommended."
                        : "Poverty-crime link is weak here - investigate local socioeconomic context.";
                case dropout -> r > 0
                        ? "Investment in schools, after-school programs, and youth engagement is critical."
                        : "Educational attainment may not directly predict crime in this dataset.";
                case density -> r > 0
                        ? "Urban planning interventions (green spaces, community centers) may help dense areas"
                        : "High density alone does not predict crime - social cohesion may be a factor.";
                case income -> r < 0
                        ? "Income inequality appears linked to crime. Progressive economic policies may help."
                        : "Higher-income areas show more crime - explore white-collar or property crime types.";
                default -> "Review the data distribution for this indicator.";
            };
        }
    }

    //main api
    public List<CorrelationResult> computeAll(
            Map<String, Integer> crimeCountByDistrict,
            Map<String, SocialIndicator> socialByDistrict
    ) {
        List<String> commonDistricts = new ArrayList<>(crimeCountByDistrict.keySet());
        commonDistricts.retainAll(socialByDistrict.keySet());

        if(commonDistricts.size() < 3) {
            System.err.println("Too few matching districts to compute meaningful correlation");
            return Collections.emptyList();
        }

        double[] crimeRates = commonDistricts.stream()
                .mapToDouble(d -> crimeCountByDistrict.get(d))
                .toArray();

        List<CorrelationResult> results = new ArrayList<>();

        results.add(new CorrelationResult(unemployment,
                pearson(crimeRates, extract(commonDistricts, socialByDistrict, unemployment))));
        results.add(new CorrelationResult(poverty,
                pearson(crimeRates, extract(commonDistricts, socialByDistrict, poverty))));
        results.add(new CorrelationResult(dropout,
                pearson(crimeRates, extract(commonDistricts, socialByDistrict, dropout))));
        results.add(new CorrelationResult(density,
                pearson(crimeRates, extract(commonDistricts, socialByDistrict, density))));
        results.add(new CorrelationResult(income,
                pearson(crimeRates, extract(commonDistricts, socialByDistrict, income))));

        //sort by strongest correlation first
        results.sort((a, b) -> Double.compare(Math.abs(b.pearsonR), Math.abs(a.pearsonR)));
        return results;
    }

    //identifies the strongest predictor of crime across all indicators
    public String strongestPredictor(List<CorrelationResult> results) {
        if(results.isEmpty()) {
            return "Insufficient data";
        }
        CorrelationResult top = results.get(0);
        if(Math.abs(top.pearsonR) < 0.3) {
            return "No strong predictor found in this dataset.";
        }
        return String.format("%s (r = %+.3f)", top.indicator, top.pearsonR);
    }

    //pearson r computation
    public double pearson(double[] x, double[] y) {
        if (x.length != y.length || x.length < 2) {
            return 0;
        }

        double meanX = mean(x), meanY = mean(y);
        double num = 0, denomX = 0, denomY = 0;

        for(int i = 0; i < x.length; i++) {
            double dx = x[i] - meanX;
            double dy = y[i] - meanY;
            num += dx * dy;
            denomX += dx * dx;
            denomY += dy * dy;
        }

        double denom = Math.sqrt(denomX * denomY);
        return denom == 0 ? 0 : num / denom;
    }

    private double mean(double[] arr) {
        double sum = 0;
        for (double v : arr) {
            sum += v;
        }
        return sum / arr.length;
    }

    private double[] extract(List<String> districts,
                           Map<String, SocialIndicator> socialMap,
                           String indicator) {
        return districts.stream().mapToDouble(d -> {
            SocialIndicator si = socialMap.get(d);
            return switch (indicator) {
                case unemployment -> si.getUnemploymentRate();
                case  poverty -> si.getPovertyRate();
                case dropout -> si.getSchoolDropoutRate();
                case density -> si.getPopulationDensity();
                case income -> si.getMedianIncome();
                default -> 0;
            };
        }).toArray();
    }
}
