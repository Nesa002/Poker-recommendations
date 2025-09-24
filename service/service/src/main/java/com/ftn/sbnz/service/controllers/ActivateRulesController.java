package com.ftn.sbnz.service.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ftn.sbnz.service.services.ActivateRulesService;

@RestController
@RequestMapping("/rule-example")
public class ActivateRulesController {
  
    private ActivateRulesService service;

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
}
