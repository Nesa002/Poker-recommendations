package com.ftn.sbnz.service.services;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;


@Service
public class HandService {

    private final Map<String, Double> handWinPct = new HashMap<>();

    // Path to your file (can be absolute or classpath resource)
    private final String fileName = "service\\service\\src\\main\\resources\\poker_hands.txt";

    @PostConstruct
    public void init() {
        loadHands();
    }

    private void loadHands() {
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the preflop win percentage for a given hand.
     * Normalizes input to uppercase.
     */
    public Double getWinPercentage(String hand) {
        if (hand == null) return 0.0;
        return handWinPct.getOrDefault(hand.toUpperCase(), 0.0);
    }

    public Double getTableAggressiveness(int hand) {
        return 1.0;
    }

    // Optional: get all hands
    public Map<String, Double> getAllHands() {
        return new HashMap<>(handWinPct);
    }
}
