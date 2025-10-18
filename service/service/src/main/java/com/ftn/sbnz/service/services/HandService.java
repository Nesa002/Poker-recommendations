package com.ftn.sbnz.service.services;

import com.ftn.sbnz.model.models.CategoryStrongerThan;
import com.ftn.sbnz.model.models.HandCategory;
import com.ftn.sbnz.model.models.Round;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class HandService {

    private final Map<String, Double> handWinPct = new HashMap<>();
    private List<HandCategory> handCategories = new ArrayList<>();

    // File name relative to src/main/resources
    private final String fileName = "poker_hands.txt";

    @PostConstruct
    public void init() {
        loadHands();
    }

    private void loadHands() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("poker_hands.txt");
             Scanner scanner = new Scanner(is)) {

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] parts = line.split(",");
                String hand = parts[0];
                Double percentage = Double.parseDouble(parts[1]);
                handWinPct.put(hand, percentage);
                
                String category;
                if (percentage >= 0.90) { // npr. 80%+
                    category = "Premium";
                } else if (percentage >= 0.80) { // 65%-80%
                    category = "Strong";
                } else if (percentage >= 0.70) { // 50%-65%
                    category = "Medium";
                } else if (percentage >= 0.60) { // 30-50%
                    category = "Playable";
                } else {
                    category = "Weak";
                }
                this.handCategories.add(new HandCategory(hand, category));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load poker hands from " + fileName, e);
        }
    }

    public Double getWinPercentage(String hand) {
        if (hand == null) return 0.0;
        return handWinPct.getOrDefault(hand.toUpperCase(), 0.0);
    }

    public List<HandCategory> getAllHandCategories() {
        return this.handCategories;
    }

    public List<CategoryStrongerThan> getCategoryHierarchy() {
        List<CategoryStrongerThan> hierarchy = new ArrayList<>();
        hierarchy.add(new CategoryStrongerThan("Premium", "Strong"));
        hierarchy.add(new CategoryStrongerThan("Strong", "Medium"));
        hierarchy.add(new CategoryStrongerThan("Medium", "Playable"));
        hierarchy.add(new CategoryStrongerThan("Playable", "Weak"));
        return hierarchy;
    }

    public Double getTableAggressiveness(int hand) {
        return 1.0;
    }

    public Map<String, Double> getAllHands() {
        return new HashMap<>(handWinPct);
    }
}
