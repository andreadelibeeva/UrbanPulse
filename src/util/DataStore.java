package util;

import model.CrimeRecord;
import model.SocialIndicator;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class DataStore {
    private static final String crimeFile = "crimes.ser";
    private static final String socialFile = "social.ser";

    private List<CrimeRecord> crimes = new ArrayList<>();
    private List<SocialIndicator> socials = new ArrayList<>();

    //Save
    public void saveCrimes() {
        try (ObjectOutputStream outStream = new ObjectOutputStream(new FileOutputStream(crimeFile))) {
            outStream.writeObject(crimes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveSocials() {
        try (ObjectOutputStream outStream = new ObjectOutputStream(new FileOutputStream(socialFile))) {
            outStream.writeObject(socials);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Load
    @SuppressWarnings("unchecked")
    public void loadCrimes() {
        File f = new File(crimeFile);
        if(!f.exists()) {
            return;
        }
        try (ObjectInputStream inStream = new ObjectInputStream(new FileInputStream(f))) {
            crimes = (List<CrimeRecord>) inStream.readObject();
        } catch (Exception e) {
            System.err.println("Could not load crimes: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public void loadSocials() {
        File f = new File(socialFile);
        if(!f.exists()) {
            return;
        }
        try (ObjectInputStream inStream = new ObjectInputStream(new FileInputStream(f))) {
            socials = (List<SocialIndicator>) inStream.readObject();
        } catch (Exception e) {
            System.err.println("Could not load socials: " + e.getMessage());
        }
    }

    public void loadAll() {
        loadCrimes();
        loadSocials();
    }

    public void saveAll() throws IOException {
        saveCrimes();
        saveSocials();
    }

    //CRUD - Crimes
    public void addCrime(CrimeRecord record) {
        crimes.add(record);
    }

    public List<CrimeRecord> getAllCrimes() {
        return new ArrayList<>(crimes);
    }

    public boolean deleteCrime(int id) {
        return crimes.removeIf(c -> c.getId() == id);
    }

    public boolean updateCrime(CrimeRecord updated) {
        for(int i = 0; i < crimes.size(); i++) {
            if(crimes.get(i).getId() == updated.getId()) {
                crimes.set(i, updated);
                return true;
            }
        }
        return false;
    }

    public CrimeRecord findCrimeById(int id) {
        return crimes.stream()
                .filter(c -> c.getId() == id)
                .findFirst().orElse(null);
    }

    //CRUD - Socials
    public void addSocial(SocialIndicator si) {
        socials.removeIf(s -> s.getDistrict().equals(si.getDistrict()));
        socials.add(si);
    }

    public List<SocialIndicator> getAllSocials() {
        return new ArrayList<>(socials);
    }

    public boolean deleteSocials(String district) {
            return socials.removeIf(s -> s.getDistrict().equals(district));
    }

    public SocialIndicator findSocialByDistrict(String district) {
        return socials.stream()
                .filter(s -> s.getDistrict().equals(district))
                .findFirst().orElse(null);
    }

    //Helpers
    public int getTotalCrimes() {
        return crimes.size();
    }

    public int getNextCrimeId() {
        return crimes.stream().mapToInt(CrimeRecord::getId).max().orElse(0) + 1;
    }

    public int getNextSocialId() {
        return socials.stream().mapToInt(SocialIndicator::getId).max().orElse(0) + 1;
    }

    public double getArrestRate() {
        if (crimes.isEmpty()) return 0;
        long arrested = crimes.stream().filter(CrimeRecord::isArrested).count();
        return (arrested / (double) crimes.size()) * 100.0;
    }

    public String getTopCrimeType() {
        return crimes.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        CrimeRecord::getCrimeType, java.util.stream.Collectors.counting()))
                .entrySet().stream()
                .max(java.util.Map.Entry.comparingByValue())
                .map(java.util.Map.Entry::getKey)
                .orElse("N/A");
    }

    ublic java.util.List<String> getDistricts() {
        return crimes.stream()
                .map(CrimeRecord::getDistrict)
                .distinct()
                .sorted()
                .collect(java.util.stream.Collectors.toList());
    }

    public java.util.List<String> getCrimeTypes() {
        return crimes.stream()
                .map(CrimeRecord::getCrimeType)
                .distinct()
                .sorted()
                .collect(java.util.stream.Collectors.toList());
    }

    public java.util.Map<String, Integer> getCrimeCountByDistrict() {
        java.util.Map<String, Integer> map = new java.util.HashMap<>();
        for (CrimeRecord c : crimes) {
            map.merge(c.getDistrict(), 1, Integer::sum);
        }
        return map;
    }

    public java.util.PriorityQueue<java.util.Map.Entry<String, Integer>> getRankedDistricts() {
        java.util.PriorityQueue<java.util.Map.Entry<String, Integer>> pq =
                new java.util.PriorityQueue<>(
                        (a, b) -> b.getValue() - a.getValue()
                );
        pq.addAll(getCrimeCountByDistrict().entrySet());
        return pq;
    }
}
