package com.ftn.sbnz.service.controllers;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ftn.sbnz.service.services.ActivateRulesService;
import com.ftn.sbnz.service.services.HandEvalTestService;

@RestController
@RequestMapping("/rule-example")
public class ActivateRulesController {
  
    private ActivateRulesService service;

    @Autowired
    private HandEvalTestService handEvalTestService;

    @Autowired
    public ActivateRulesController(ActivateRulesService service) {
        this.service = service;
    }

    @GetMapping("/forward")
    public void fireAllRules() {
        service.fireRules();
    }

    @GetMapping("/backwards")
    public void fireAllRulesBackwards() {
        service.fireRulesBackwards();
    }

    @GetMapping("/generate-rules")
    public void generateRules() {
        service.generateRules();
    }

    @GetMapping("/hand-eval")
    public ResponseEntity<Map<String, Boolean>> testHandEvaluationQuery() {
        
        Map<String, Boolean> results = handEvalTestService.testQueries();
        
        return ResponseEntity.ok(results);
    }
}
