package com.ftn.sbnz.service.controllers;

import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ftn.sbnz.model.models.RaiseEvent;
import com.ftn.sbnz.service.services.ActivateRulesService;
import com.ftn.sbnz.service.services.HandService;
import com.ftn.sbnz.service.services.HandEvalTestService;

@RestController
@RequestMapping("/rule-example")
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

    @GetMapping("/forward/test")
    public void fireAllRules() {
        service.fireRules();
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
