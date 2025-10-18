package com.ftn.sbnz.service.services;

import com.ftn.sbnz.kjar.rules.RulesGenerator;
import com.ftn.sbnz.model.models.Round;
// import com.ftn.sbnz.model.models.Round.GameFaza; // Uklonjeno

import java.util.List;

import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.QueryResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ActivateRulesService {
    private final KieContainer kieContainer;
    private final HandService handService;
    private final HandEvalQueryService handEvalQueryService;

    @Autowired
    public ActivateRulesService(KieContainer kieContainer, HandService handService,
                                HandEvalQueryService handEvalQueryService) {
        this.kieContainer = kieContainer;
        this.handService = handService;
        this.handEvalQueryService = handEvalQueryService;
    }

    public void fireRules() {

        // Define players and chip stacks for testing
        // Hero is at index 2 (position 2) -> Bad Position (<= threshold 2)
        String[] playersBadPos = {"Alice", "Bob", "Hero", "David"};
        Integer[] chipsStrongEcon = {100, 100, 500, 100}; // Hero has strong economy
        Integer[] chipsWeakEcon = {500, 500, 100, 500};   // Hero has weak economy

        // Define players for "good position" testing
        // Hero is at index 3 (position 3) -> Good Position (> threshold 2)
        String[] playersGoodPos = {"Alice", "Bob", "David", "Hero"};
        Integer[] chipsGoodPosStrongEcon = {100, 100, 100, 500};
        Integer[] chipsGoodPosWeakEcon = {500, 500, 500, 100};

        // Define High Stakes and Normal Stakes values
        int hsRaise = 25; // High Stakes Raise (25 > 100*0.2 AND 25 >= 5*5)
        int nsRaise = 4;  // Normal Stakes Raise (4 <= 100*0.2 AND 4 < 5*5)
        int pot = 100;
        int bb = 5;

        List<Round> rounds = List.of(
            // ===================================
            // === SCENARIOS FOR "HIGH_STAKES" ===
            // ===================================

            // R1: HS Premium Hand (RAISE)
            // Rule: "High Stakes Strong Hand" -> isHandAtLeast("Premium")
            new Round("AA", 1, 2, playersBadPos, chipsWeakEcon, hsRaise, pot, bb),

            // R2: HS Good Pos, Strong Hand (RAISE)
            // Rule: "High Stakes Good Position Economy" -> OR isHandAtLeast("Strong")
            new Round("AKo", 2, 3, playersGoodPos, chipsWeakEcon, hsRaise, pot, bb),

            // R3: HS Good Pos, Medium Hand + Strong Econ (RAISE)
            // Rule: "High Stakes Good Position Economy" -> ($r.getStrongEconomy()... && isHandAtLeast("Medium"))
            new Round("QJs", 3, 3, playersGoodPos, chipsGoodPosStrongEcon, hsRaise, pot, bb),

            // R4: HS Good Pos, Medium Hand + Aggressive Table (RAISE)
            // Rule: "High Stakes Good Position Economy" -> (handService.isTableAggressive() && isHandAtLeast("Medium"))
            // ASSUMPTION: HandService.isTableAggressive() will return TRUE for this test
            new Round("QJs", 4, 3, playersGoodPos, chipsGoodPosWeakEcon, hsRaise, pot, bb),

            // R5: HS Bad Pos, Strong Hand + Strong Econ (RAISE)
            // Rule: "High Stakes Bad Position" -> isHandAtLeast("Strong")
            new Round("AQo", 5, 2, playersBadPos, chipsStrongEcon, hsRaise, pot, bb),

            // R6: HS Fallback - Bad Pos, Weak Econ, Medium Hand (CALL)
            // Rule: "High Stakes Call Fallback" -> None of the above RAISE rules match
            new Round("99", 6, 2, playersBadPos, chipsWeakEcon, hsRaise, pot, bb),

            // R7: HS Fallback - Good Pos, Weak Econ, Playable Hand, Passive Table (CALL)
            // Rule: "High Stakes Call Fallback" -> "HS Good Pos Econ" fails because hand < Medium and table passive
            // ASSUMPTION: HandService.isTableAggressive() will return FALSE for this test
            new Round("K5o", 7, 3, playersGoodPos, chipsGoodPosWeakEcon, hsRaise, pot, bb),


            // =====================================
            // === SCENARIOS FOR "NORMAL_STAKES" ===
            // =====================================

            // R8: NS Premium Hand (RAISE)
            // Rule: "Normal Stakes Strong Hand" -> isHandAtLeast("Premium")
            new Round("KK", 8, 2, playersBadPos, chipsWeakEcon, nsRaise, pot, bb),

            // R9: NS Good Pos, Medium Hand (RAISE)
            // Rule: "Normal Stakes Good Position Economy" -> OR isHandAtLeast("Medium")
            new Round("QJs", 9, 3, playersGoodPos, chipsGoodPosWeakEcon, nsRaise, pot, bb),

            // R10: NS Good Pos, Playable Hand + Strong Econ (RAISE)
            // Rule: "Normal Stakes Good Position Economy" -> ($r.getStrongEconomy()... && isHandAtLeast("Playable"))
            new Round("K9o", 10, 3, playersGoodPos, chipsGoodPosStrongEcon, nsRaise, pot, bb),

            // R11: NS Good Pos, Playable Hand + Aggressive Table (RAISE)
            // Rule: "Normal Stakes Good Position Economy" -> (handService.isTableAggressive() && isHandAtLeast("Playable"))
            // ASSUMPTION: HandService.isTableAggressive() will return TRUE for this test
            new Round("K9o", 11, 3, playersGoodPos, chipsGoodPosWeakEcon, nsRaise, pot, bb),

            // R12: NS Bad Pos, Strong Hand (RAISE)
            // Rule: "Normal Stakes Bad Position" -> OR isHandAtLeast("Strong")
            new Round("AKo", 12, 2, playersBadPos, chipsWeakEcon, nsRaise, pot, bb),

            // R13: NS Bad Pos, Medium Hand + Strong Econ (RAISE)
            // Rule: "Normal Stakes Bad Position" -> ($r.getStrongEconomy() && isHandAtLeast("Medium"))
            new Round("QJs", 13, 2, playersBadPos, chipsStrongEcon, nsRaise, pot, bb),

            // R14: NS Fallback - Bad Pos, Weak Econ, Weak Hand (CALL)
            // Rule: "Normal Stakes Call Fallback" -> None of the above RAISE rules match
            new Round("72o", 14, 2, playersBadPos, chipsWeakEcon, nsRaise, pot, bb),

            // R15: NS Fallback - Good Pos, Weak Econ, Weak Hand, Passive Table (CALL)
            // Rule: "Normal Stakes Call Fallback" -> "NS Good Pos Econ" fails because hand < Playable and table passive
            // ASSUMPTION: HandService.isTableAggressive() will return FALSE for this test
            new Round("72o", 15, 3, playersGoodPos, chipsGoodPosWeakEcon, nsRaise, pot, bb)
        );

        // Koristimo "session-per-round" pristup
        for (Round round : rounds) {
            KieSession kSession = kieContainer.newKieSession("forwardRulesSession");
            try {
                // Štampamo ulazne parametre radi lakšeg debagovanja
                System.out.println("\n--- Evaluating FORWARD for hand: " + round.getHand() +
                                   ", Pos: " + round.getPlayerPosition() +
                                   ", Raise: " + round.getCurrentRaise() +
                                   ", Pot: " + round.getPot() +
                                   ", Econ: " + (round.getStrongEconomy() ? "Strong" : "Weak") +
                                   ", TableAgg: " + (handService.isTableAggressive() ? "Aggressive" : "Passive") + " ---");

                // 1. Postavi Globale koje DRL ZAHTEVA
                kSession.setGlobal("handEvalService", handEvalQueryService);
                kSession.setGlobal("handService", handService); // (Za isTableAggressive)
                kSession.setGlobal("min_bigger_raise", 0.2);
                kSession.setGlobal("min_bigger_raise_blinds", 5);
                kSession.setGlobal("bad_position_treshold", 2);

                // 2. Ubaci činjenicu
                kSession.insert(round);

                // 3. Postavi fokus na activation-group
                kSession.getAgenda().getAgendaGroup("high_stakes").setFocus();
                kSession.getAgenda().getAgendaGroup("normal_stakes").setFocus();

                kSession.fireAllRules();

            } finally {
                kSession.dispose();
            }
        }
    }

    // Metoda fireRulesBackwards() je obrisana.
    
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