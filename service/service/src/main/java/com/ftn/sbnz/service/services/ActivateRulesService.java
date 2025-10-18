package com.ftn.sbnz.service.services;


import com.ftn.sbnz.kjar.rules.RulesGenerator;
import com.ftn.sbnz.model.models.Round;

import java.util.Collection;
import java.util.List;

import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.QueryResults;
import org.kie.api.runtime.rule.QueryResultsRow;
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
        kSession.setGlobal("risk_margin", 0.15);             // 15% margin
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

    } finally {
        kSession.dispose();
    }
  }

  public void fireRulesBackwards() {
        List<Round> rounds = List.of(
            /**
             * SCENARIO 1: DON'T FOLD
             * Razlog: Imamo "KK" (82.0%), što je jaka ruka (preko 60%). Nijedno pravilo za fold se ne aktivira.
             */
            new Round("KK", 1, 3, // ruka, br. runde, pozicija
                new String[]{"Alice", "Bob", "Charlie", "David"},
                new Integer[]{100, 200, 150, 180}, // čipovi
                10, 50, 5), // raise, pot, big blind

            /**
             * SCENARIO 2: FOLD ZBOG VELIKOG RAISE-A (Salience 100)
             * Razlog: Raise (30) je veći od 5 velikih blindova (5 * 5 = 25). Naša ruka "KTo" (53.0%) nije jaka (< 60%).
             */
            new Round("KTo", 2, 2,
                new String[]{"Alice", "Bob", "Charlie", "David"},
                new Integer[]{150, 100, 250, 80},
                30, 100, 5),

            /**
             * SCENARIO 3: FOLD ZBOG LOŠE POZICIJE (Salience 90)
             * Razlog: Pozicija je 2 (<= 2). Ruka "A2o" (48.0%) nije jaka (< 60%).
             * NAPOMENA: Za ovaj slučaj, tableAggressiveness mora biti negativan!
             */
            new Round("A2o", 3, 1,
                new String[]{"Alice", "Bob", "Charlie", "David"},
                new Integer[]{200, 350, 50, 50},
                10, 80, 5),

            /**
             * SCENARIO 4: FOLD ZBOG SLABE EKONOMIJE (Salience 80)
             * Razlog: Ruka "72o" (35.0%) je slaba (< 40%). Raise i pozicija su OK.
             * Naš igrač (Charlie) ima 50 čipova, a Bob ima 200, tako da nema jaku ekonomiju.
             */
            new Round("72o", 4, 1, // Igrac je "Charlie"
                new String[]{"Alice", "Bob", "Charlie", "David"},
                new Integer[]{100, 200, 50, 80},
                10, 60, 5),

            /**
             * SCENARIO 5: FOLD ZBOG SLABE RUKE (Salience 70 - Opšti slučaj)
             * Razlog: Ruka "32o" (31.0%) je slaba (< 40%). Svi ostali uslovi (raise, pozicija, ekonomija) su dobri,
             * tako da se aktivira ovo poslednje pravilo za fold kao "catch-all".
             */
            new Round("32o", 5, 3, // Igrac je "David"
                new String[]{"Alice", "Bob", "Charlie", "David"},
                new Integer[]{100, 80, 120, 250},
                10, 40, 5)
        );

        for (Round round : rounds) {
            KieSession kSession = kieContainer.newKieSession("foldRulesSession");
            try {
                // Set globals
                kSession.setGlobal("handService", handService);
                kSession.setGlobal("min_bigger_raise", 0.2);
                kSession.setGlobal("min_bigger_raise_blinds", 5);
                kSession.setGlobal("bad_position_treshold", 2);
                kSession.setGlobal("min_strong_hand", 60.0);
                kSession.setGlobal("min_medium_hand", 40.0);
                kSession.setGlobal("tableAggressiveness", 2.0);

                System.out.println("--- Evaluating round with hand: " + round.getHand() + " ---");

                // Insert the round object
                kSession.insert(round);

                // STEP 1: Fire rules. This will ONLY run the "Derive..." rules
                // to create all the necessary helper facts.
                kSession.fireAllRules();

                // STEP 2: Now that facts exist, explicitly call the query from Java.
                QueryResults results = kSession.getQueryResults("shouldFold", round);

                // STEP 3: Check the results and print the decision.
                if (results.size() > 0) {
                    System.out.println("Fold (via backward chaining) for hand: " + round.getHand());
                } else {
                    System.out.println("Don't fold (via backward chaining) for hand: " + round.getHand());
                }

            } finally {
                // Dispose of the session to clean up everything for the next round.
                kSession.dispose();
            }
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
