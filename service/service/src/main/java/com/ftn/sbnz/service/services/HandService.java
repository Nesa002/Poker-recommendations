package com.ftn.sbnz.service.services;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.ftn.sbnz.model.models.TableAggressivenessEvent;

@Service
public class HandService {

    private final Map<String, Double> handWinPct = new HashMap<>();

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

    public boolean getTableAggressiveness() {
        long now = System.currentTimeMillis();
        long activeCount = cepRulesSession
            .getObjects(o -> o instanceof TableAggressivenessEvent)
            .stream()
            .map(o -> (TableAggressivenessEvent) o)
            .filter(e -> (now - e.getEventTime().getTime()) <=  600000)
            .count();

        return activeCount > 0;
    }

    public Map<String, Double> getAllHands() {
        return new HashMap<>(handWinPct);
    }
}
