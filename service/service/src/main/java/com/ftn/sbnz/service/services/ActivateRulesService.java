package com.ftn.sbnz.service.services;


import com.ftn.sbnz.kjar.rules.RulesGenerator;
import com.ftn.sbnz.model.models.Round;

import java.util.List;

import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class ActivateRulesService {
  private final KieContainer kieContainer;
  private final HandService handService;

  @Autowired
  public ActivateRulesService(KieContainer kieContainer, HandService handService) {
    this.kieContainer = kieContainer;
    this.handService = handService;
  }

  public void fireRules() {
    KieSession kSession = kieContainer.newKieSession("forwardRulesSession");
    try {
        // Inject your HandService into the DRL as global
        kSession.setGlobal("handService", handService);

        // Set the global parameters for your rules
        kSession.setGlobal("min_bigger_raise", 0.2);         // 20% of pot
        kSession.setGlobal("min_bigger_raise_blinds", 5);    // 5 blinds
        kSession.setGlobal("risk_margin", 15.0);             // 15% margin
        kSession.setGlobal("bad_position_treshold", 2);      // positions <= 3 considered bad
        kSession.setGlobal("min_strong_hand", 60.0);         // 60% hand strength
        kSession.setGlobal("min_medium_hand", 40.0);         // 40% hand strength
        kSession.setGlobal("tableAggressiveness", 2.0);      // total aggressiveness

        List<Round> rounds = List.of(
                new Round("AA", 1, 3,
                        new String[]{"Alice", "Bob", "Charlie", "David"},
                        new Integer[]{100, 100, 100, 100},
                        25, 100, 5),
                new Round("66", 2, 3,
                        new String[]{"Alice", "Bob", "Charlie", "David"},
                        new Integer[]{150, 100, 250, 80},
                        30, 100, 5),
                new Round("AKo", 3, 1,
                        new String[]{"Alice", "Bob", "Charlie", "David"},
                        new Integer[]{200, 350, 50, 50},
                        30, 100, 5),
                new Round("K5o", 4, 2,
                        new String[]{"Alice", "Bob", "Charlie", "David"},
                        new Integer[]{50, 50, 50, 50},
                        25, 100, 5)
            );


        // Optionally add player actions
        rounds.get(0).addPlayerActions("Alice", 1);
        rounds.get(0).addPlayerActions("Bob", 0);

        // Insert and fire rules **one Round at a time**
        for (Round round : rounds) {
            kSession.insert(round);
            kSession.fireAllRules(); // only evaluates rules for this Round
        }

        // Fire all rules
        kSession.fireAllRules();

    } finally {
        kSession.dispose();
    }
  }

    public void generateRules() {
        try {
            RulesGenerator.generateDRL();
            System.out.println("DRL rules generated successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to generate DRL rules", e);
        }
    }
}
