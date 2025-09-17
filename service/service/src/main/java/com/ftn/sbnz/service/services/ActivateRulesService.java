package com.ftn.sbnz.service.services;


import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ActivateRulesService {
  private final KieContainer kieContainer;

  @Autowired
  public ActivateRulesService(KieContainer kieContainer) {
    this.kieContainer = kieContainer;
  }

  public void fireRules() {
    KieSession kSession = kieContainer.newKieSession();
    
  }
}
