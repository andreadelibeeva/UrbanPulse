package model;

public class SocialIndicator extends DataRecord {

    private static final long serialVersionUID = 1L;

    private double unemploymentRate;
    private double povertyRate;
    private double schoolDropoutRate;
    private double populationDensity;
    private double medianIncome;

    public SocialIndicator(int id, String district,
                           double unemploymentRate, double povertyRate,
                           double schoolDropoutRate, double populationDensity,
                           double medianIncome) {
        super(id, district);
        this.unemploymentRate  = unemploymentRate;
        this.povertyRate       = povertyRate;
        this.schoolDropoutRate = schoolDropoutRate;
        this.populationDensity = populationDensity;
        this.medianIncome      = medianIncome;
    }

    public double getUnemploymentRate()  { return unemploymentRate; }
    public double getPovertyRate()       { return povertyRate; }
    public double getSchoolDropoutRate() { return schoolDropoutRate; }
    public double getPopulationDensity() { return populationDensity; }
    public double getMedianIncome()      { return medianIncome; }

    public void setUnemploymentRate(double v)  { this.unemploymentRate = v; }
    public void setPovertyRate(double v)       { this.povertyRate = v; }
    public void setSchoolDropoutRate(double v) { this.schoolDropoutRate = v; }
    public void setPopulationDensity(double v) { this.populationDensity = v; }
    public void setMedianIncome(double v)      { this.medianIncome = v; }

    public double getDeprivationScore() {
        return (unemploymentRate * 0.35)
                + (povertyRate * 0.35)
                + (schoolDropoutRate * 0.20)
                + (Math.min(populationDensity / 1000.0, 10) * 0.10);
    }

    @Override
    public String getSummary() {
        return String.format("District %s | Unemployment: %.1f%% | Poverty: %.1f%%",
                district, unemploymentRate, povertyRate);
    }
}