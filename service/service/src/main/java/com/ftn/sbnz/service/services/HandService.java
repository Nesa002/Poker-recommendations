package com.ftn.sbnz.service.services;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class HandService {

    private final Map<String, Double> handWinPct = new HashMap<>();

    // File name relative to src/main/resources
    private final String fileName = "poker_hands.txt";

    @PostConstruct
    public void init() {
        loadHands();
    }

    private void loadHands() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(fileName)) {
            if (is == null) {
                throw new RuntimeException(fileName + " not found in resources!");
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;

                    String[] parts = line.split(",");
                    if (parts.length == 2) {
                        String hand = parts[0].trim().toUpperCase();
                        Double pct = Double.parseDouble(parts[1].trim());
                        handWinPct.put(hand, pct);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load poker hands from " + fileName, e);
        }
    }

    public Double getWinPercentage(String hand) {
        if (hand == null) return 0.0;
        return handWinPct.getOrDefault(hand.toUpperCase(), 0.0);
    }

    public Double getTableAggressiveness(int hand) {
        return 1.0;
    }

    public Map<String, Double> getAllHands() {
        return new HashMap<>(handWinPct);
    }
}
