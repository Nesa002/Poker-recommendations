package com.ftn.sbnz.service.services;


import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ftn.sbnz.model.models.Round;

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
    KieSession kSession = kieContainer.newKieSession();
    try {
        // Inject your HandService into the DRL as global
        kSession.setGlobal("handService", handService);

        // Set the global parameters for your rules
        kSession.setGlobal("min_bigger_raise", 0.2);         // 20% of pot
        kSession.setGlobal("min_bigger_raise_blinds", 5);    // 5 blinds
        kSession.setGlobal("risk_margin", 15.0);             // 15% margin
        kSession.setGlobal("bad_position_treshold", 3);      // positions <= 3 considered bad
        kSession.setGlobal("min_strong_hand", 60.0);         // 60% hand strength
        kSession.setGlobal("min_medium_hand", 40.0);         // 40% hand strength
        kSession.setGlobal("tableAggressiveness", 2.0);      // total aggressiveness

        // Create example rounds to trigger different rules
        Round strongHandHighStake = new Round(
            "AA", 1, 4,
            new String[]{"Alice", "Bob", "Charlie", "David"},
            new Integer[]{100, 100, 100, 100},
            25, 100, 5 // currentRaise=25, pot=100, bigBlindSize=5
        );

        Round goodPositionEconomy = new Round(
            "KQ", 2, 4,
            new String[]{"Alice", "Bob", "Charlie", "David"},
            new Integer[]{150, 100, 50, 80},
            30, 100, 5
        );

        Round badPositionStrong = new Round(
            "AK", 3, 1,
            new String[]{"Alice", "Bob", "Charlie", "David"},
            new Integer[]{200, 50, 50, 50},
            30, 100, 5
        );

        Round highStakeFallback = new Round(
            "72", 4, 2,
            new String[]{"Alice", "Bob", "Charlie", "David"},
            new Integer[]{50, 50, 50, 50},
            25, 100, 5
        );


        // Add some player actions to test sums (optional)
        strongHandHighStake.addPlayerActions("Alice", 1);
        strongHandHighStake.addPlayerActions("Bob", 0);

        // Insert facts
        kSession.insert(strongHandHighStake);
        kSession.insert(goodPositionEconomy);
        kSession.insert(badPositionStrong);
        kSession.insert(highStakeFallback);

        // Fire all rules
        kSession.fireAllRules();

    } finally {
        kSession.dispose();
    }
  }
}
