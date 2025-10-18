package com.ftn.sbnz.service.services;

import com.ftn.sbnz.model.models.CategoryStrongerThan;
import com.ftn.sbnz.model.models.HandCategory;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.QueryResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HandEvalQueryService {

    @Autowired
    private KieContainer kieContainer;

    @Autowired
    private HandService handService; 

    public boolean isHandAtLeast(String hand, String requiredCategory) {
        
        KieSession kieSession = kieContainer.newKieSession("handEvalSession");
        
        try {
            for (CategoryStrongerThan fact : handService.getCategoryHierarchy()) {
                kieSession.insert(fact);
            }
            for (HandCategory fact : handService.getAllHandCategories()) {
                kieSession.insert(fact);
            }

            QueryResults results = kieSession.getQueryResults(
                "isHandAtLeastCategory", hand, requiredCategory
            );

            return results.size() > 0;

        } finally {
            kieSession.dispose();
        }
    }
}