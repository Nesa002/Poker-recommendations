package com.ftn.sbnz.service.services;

import com.ftn.sbnz.model.models.CategoryStrongerThan;
import com.ftn.sbnz.model.models.HandCategory;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.QueryResults;
import org.kie.api.runtime.rule.QueryResultsRow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class HandEvalTestService {

    @Autowired
    private KieContainer kieContainer;

    @Autowired
    private HandService handService;

    public Map<String, Boolean> testQueries() {
        
        KieSession kieSession = kieContainer.newKieSession("handEvalSession");
        
        Map<String, Boolean> testResults = new HashMap<>();

        try {
            for (CategoryStrongerThan fact : handService.getCategoryHierarchy()) {
                kieSession.insert(fact);
            }
            for (HandCategory fact : handService.getAllHandCategories()) {
                kieSession.insert(fact);
            }

            QueryResults results1 = kieSession.getQueryResults(
                "isHandAtLeastCategory", "AA", "Premium"
            );
            testResults.put("Test 1: 'AA' >= 'Premium'", results1.size() > 0);


            QueryResults results2 = kieSession.getQueryResults(
                "isHandAtLeastCategory", "AQs", "Medium"
            );
            testResults.put("Test 2: 'AQs' >= 'Medium'", results2.size() > 0);


            QueryResults results3 = kieSession.getQueryResults(
                "isHandAtLeastCategory", "72o", "Strong"
            );
            testResults.put("Test 3: '72o' >= 'Strong'", results3.size() > 0);

            QueryResults results4 = kieSession.getQueryResults(
                "isHandAtLeastCategory", "KJs", "Strong"
            );
            testResults.put("Test 4: 'KJs' >= 'Strong'", results4.size() > 0);


            QueryResults results5 = kieSession.getQueryResults(
                "isHandAtLeastCategory", "AQs", "Playable"
            );
            testResults.put("Test 5: 'AQs' >= 'Playable' (Rekurzija)", results5.size() > 0);

        } finally {
            kieSession.dispose();
        }

        return testResults;
    }
}