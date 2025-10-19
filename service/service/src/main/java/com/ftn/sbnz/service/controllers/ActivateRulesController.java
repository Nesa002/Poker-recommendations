package com.ftn.sbnz.service.controllers;

import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ftn.sbnz.model.models.RaiseEvent;
import com.ftn.sbnz.model.models.Round;
import com.ftn.sbnz.service.services.ActivateRulesService;
import com.ftn.sbnz.service.services.HandService;
import com.ftn.sbnz.service.services.HandEvalTestService;

@RestController
@RequestMapping("/rule-example")
@CrossOrigin(origins = "http://localhost:4200")
public class ActivateRulesController {
  
    private ActivateRulesService service;
    private HandService handService;
    private HandEvalTestService handEvalTestService;


    @Autowired
    public ActivateRulesController(ActivateRulesService service, HandService handService, HandEvalTestService handEvalTestService) {
        this.service = service;
        this.handService = handService;
        this.handEvalTestService = handEvalTestService;
    }

    @PostMapping("/get-decision")
    public ResponseEntity<Round> getDecisionForRound(@RequestBody Round currentRound) {

        System.out.println("\n--- TRAŽI SE ODLUKA ZA RUKU: " + currentRound.getHand() + " ---");
        System.out.println("Primljen objekat: " + currentRound.toString()); // Za debagovanje

        service.evaluateFoldRules(currentRound);

        System.out.println("Odluka FOLD faze: " + currentRound.getSuggestedAction());

        if ("NO FOLD".equals(currentRound.getSuggestedAction())) {
            System.out.println("-> Nije FOLD, proveravam RAISE/CALL...");
            service.evaluateRaiseRules(currentRound);
        }

        System.out.println("Konačna odluka: " + currentRound.getSuggestedAction());
        return ResponseEntity.ok(currentRound);
    }

    @GetMapping("/log-raise")
    public String logRaiseEvent(
            @RequestParam String playerName,
            @RequestParam double amount) {
        
        RaiseEvent raise = new RaiseEvent();
        raise.setPlayerId(playerName);
        raise.setAmount(amount);
        raise.setEventTime(new Date());

        handService.getCepRulesSession().insert(raise);

        handService.getCepRulesSession().fireAllRules();

        System.out.println("Logged RaiseEvent for " + playerName);
        return "Raise event logged for player: " + playerName + " with amount: " + amount;
    }

    // --------------- funkcije za testiranje -------------------------

    @GetMapping("/forward/test")
    public void fireAllRules() {
        service.fireRules();
    }

    @GetMapping("/fold/test")
    public void foldRules() {
        service.fireRulesFold();
    }

    @GetMapping("/generate-rules")
    public void generateRules() {
        service.generateRules();
    }

    @GetMapping("/simulate-raise")
    public void simulateRaiseEvents() {
        int random = ThreadLocalRandom.current().nextInt(1, 7);

        RaiseEvent raise = new RaiseEvent();
        raise.setPlayerId("Player" + random);
        raise.setAmount(50 + random * 10);
        raise.setEventTime(new Date());

        handService.getCepRulesSession().insert(raise);

        handService.getCepRulesSession().fireAllRules();
    }

    @GetMapping("/aggressiveness")
    public String getAggressiveness() {
        handService.getCepRulesSession().fireAllRules();

        boolean isAggressive = handService.isTableAggressive();

        return isAggressive
                ? "Table is aggressive! (CEP detected)"
                : "Table is not aggressive.";
    } 
    @GetMapping("/hand-eval")
    public ResponseEntity<Map<String, Boolean>> testHandEvaluationQuery() {
        
        Map<String, Boolean> results = handEvalTestService.testQueries();
        
        return ResponseEntity.ok(results);
    }
}
