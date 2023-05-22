package ru.company.poc.jsonb;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.Locale;

@SpringBootApplication
public class JsonBApplication {
    public static void main(String[] args) {
        Locale.setDefault(Locale.forLanguageTag("ru"));

        var context = SpringApplication.run(JsonBApplication.class, args);
        var entityMetricsService = context.getBean(EntityMetricsService.class);


        context.getBean(AttributeSearchResultExporter.class)
                .queryTypeName("equal-attributes")
                .addExplainResult(1, entityMetricsService.getCostOfQueryByAttributesUsingFieldEqualOperator(1, 3))
                .addExplainResult(3, entityMetricsService.getCostOfQueryByAttributesUsingFieldEqualOperator(3, 3))
                .addExplainResult(5, entityMetricsService.getCostOfQueryByAttributesUsingFieldEqualOperator(5, 3))
                .addExplainResult(7, entityMetricsService.getCostOfQueryByAttributesUsingFieldEqualOperator(7, 3))
                .export();

        context.getBean(AttributeSearchResultExporter.class)
                .queryTypeName("includes-attributes")
                .addExplainResult(1, entityMetricsService.getCostOfQueryByAttributesUsingIncludesOperator(1, 3))
                .addExplainResult(3, entityMetricsService.getCostOfQueryByAttributesUsingIncludesOperator(3, 3))
                .addExplainResult(5, entityMetricsService.getCostOfQueryByAttributesUsingIncludesOperator(5, 3))
                .addExplainResult(7, entityMetricsService.getCostOfQueryByAttributesUsingIncludesOperator(7, 3))
                .export();

        context.getBean(AttributeSearchResultExporter.class)
                .queryTypeName("like-attributes")
                .addExplainResult(1, entityMetricsService.getCostOfQueryByAttributesLike(1, 3))
                .addExplainResult(3, entityMetricsService.getCostOfQueryByAttributesLike(3, 3))
                .addExplainResult(5, entityMetricsService.getCostOfQueryByAttributesLike(5, 3))
                .addExplainResult(7, entityMetricsService.getCostOfQueryByAttributesLike(7, 3))
                .export();

        context.getBean(AttributeSearchResultExporter.class)
                .queryTypeName("insertion")
                .addExplainResult(1, entityMetricsService.getCostOfInsertion(1))
                .export();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}