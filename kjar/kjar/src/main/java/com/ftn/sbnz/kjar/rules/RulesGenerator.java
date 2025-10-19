package com.ftn.sbnz.kjar.rules;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.drools.template.ObjectDataCompiler;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RulesGenerator {

    public static void generateDRL() throws Exception {
        ClassLoader classLoader = RulesGenerator.class.getClassLoader();

        InputStream globalsStream = classLoader.getResourceAsStream("rules/template/globals.drl");
        if (globalsStream == null) {
            throw new RuntimeException("Globals file not found!");
        }
        String globals = new String(globalsStream.readAllBytes(), StandardCharsets.UTF_8);

        globalsStream = classLoader.getResourceAsStream("rules/template/globals_fold.drl");
        if (globalsStream == null) {
            throw new RuntimeException("Globals file not found!");
        }
        String globals_fold = new String(globalsStream.readAllBytes(), StandardCharsets.UTF_8);

        // Forward rules
        generateDRLForTemplate(
                classLoader,
                "rules/template/poker_rules_template.drl",
                "rules/template/poker_rules.csv",
                "forward",
                globals
        );

        // Fold rules
        generateDRLForTemplate(
                classLoader,
                "rules/template/fold_template.drl",
                "rules/template/fold_data.csv",
                "fold",
                globals_fold
        );
    }

    private static void generateDRLForTemplate(
            ClassLoader classLoader,
            String templatePath,
            String csvPath,
            String outputFolderName,
            String globals
    ) throws Exception {

        // Učitavanje template fajla
        InputStream templateStream = classLoader.getResourceAsStream(templatePath);
        if (templateStream == null) {
            throw new RuntimeException("Template file not found: " + templatePath);
        }

        // Učitavanje CSV fajla
        InputStream csvStream = classLoader.getResourceAsStream(csvPath);
        if (csvStream == null) {
            throw new RuntimeException("CSV file not found: " + csvPath);
        }

        // Parsiranje CSV-a
        List<Map<String, Object>> rulesData = new ArrayList<>();
        try (CSVParser parser = CSVParser.parse(new InputStreamReader(csvStream, StandardCharsets.UTF_8),
                CSVFormat.DEFAULT.builder()
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .build())) {

            for (CSVRecord record : parser) {
                Map<String, Object> map = new HashMap<>();
                for (String header : parser.getHeaderMap().keySet()) {
                    map.put(header, record.get(header));
                }
                rulesData.add(map);
            }
        }

        ObjectDataCompiler compiler = new ObjectDataCompiler();
        String rulesDrl = compiler.compile(rulesData, templateStream);

        String drl = globals + "\n" + rulesDrl;

        // Zapisivanje
        Path projectRoot = Paths.get("").toAbsolutePath();
        Path outputDir = projectRoot.resolve("kjar/kjar/src/main/resources/rules/" + outputFolderName);
        Files.createDirectories(outputDir);

        Path outputFile = outputDir.resolve("generated_rules.drl");
        Files.writeString(outputFile, drl, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        System.out.println("DRL saved to: " + outputFile.toAbsolutePath());
    }
}
