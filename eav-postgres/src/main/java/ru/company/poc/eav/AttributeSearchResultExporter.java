package ru.company.poc.eav;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Scope("prototype")
public class AttributeSearchResultExporter {

    @Value("${spring.liquibase.contexts}")
    private String contexts;

    private String queryTypeName;

    private final Map<Integer, QueryExplainResult> results = new LinkedHashMap<>();

    public AttributeSearchResultExporter addExplainResult(int attributesUsed, QueryExplainResult result) {
        results.put(attributesUsed, result);
        return this;
    }

    public AttributeSearchResultExporter queryTypeName(String queryTypeName) {
        this.queryTypeName = queryTypeName;
        return this;
    }

    @SneakyThrows
    public void export() {
        var fileName = "EAV-%s-%s.csv".formatted(queryTypeName, contexts);
        try (var writer = new PrintWriter(new File(fileName))){
            writer.println("Number of Queries; Cost; Time");
            results.entrySet()
                    .stream()
                    .sorted(Comparator.comparingInt(Map.Entry::getKey))
                    .forEach(e -> writer.println("%s; %f; %f".formatted(e.getKey(), e.getValue().cost(), e.getValue().actualTime())));
        }
    }
}
