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

import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.ftn.sbnz.model.models.TableAggressivenessEvent;

@Service
public class HandService {

    private final Map<String, Double> handWinPct = new HashMap<>();
    private List<HandCategory> handCategories = new ArrayList<>();

    // File name relative to src/main/resources
    private final String fileName = "poker_hands.txt";

    private final KieSession cepRulesSession;

    @Autowired
    public HandService(@Qualifier("cepRulesSession") KieSession cepRulesSession) {
        this.cepRulesSession = cepRulesSession;
        loadHands();
    }

    public KieSession getCepRulesSession() {
        return cepRulesSession;
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
                if (percentage >= 0.75) { // npr. 80%+
                    category = "Premium";
                } else if (percentage >= 0.60) { // 65%-80%
                    category = "Strong";
                } else if (percentage >= 0.50) { // 50%-65%
                    category = "Medium";
                } else if (percentage >= 0.40) { // 30-50%
                    category = "Playable";
                } else {
                    category = "Weak";
                }
                this.handCategories.add(new HandCategory(hand, category));
                System.out.println(hand +" "+ category+ " "+ percentage);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load poker hands from " + fileName, e);
        }
    }

    public Double getWinPercentage(String hand) {
        if (hand == null) return 0.0;
        return handWinPct.getOrDefault(hand.toUpperCase(), 0.0);
    }

    public boolean isTableAggressive() {
        long now = System.currentTimeMillis();
        long activeCount = cepRulesSession
            .getObjects(o -> o instanceof TableAggressivenessEvent)
            .stream()
            .map(o -> (TableAggressivenessEvent) o)
            .filter(e -> (now - e.getEventTime().getTime()) <=  600000)
            .count();

        return activeCount > 0;
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

    public Map<String, Double> getAllHands() {
        return new HashMap<>(handWinPct);
    }
}
