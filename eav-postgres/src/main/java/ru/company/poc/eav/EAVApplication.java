package ru.company.poc.eav;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.Locale;

@SpringBootApplication
public class EAVApplication {

    public static void main(String[] args) {
        Locale.setDefault(Locale.forLanguageTag("ru"));

        var context = SpringApplication.run(EAVApplication.class, args);
        var entityMetricsService = context.getBean(EntityMetricsService.class);

        context.getBean(AttributeSearchResultExporter.class)
                .queryTypeName("equal-attributes")
                .addExplainResult(1, entityMetricsService.getCostOfQueryByAttributesEqual(1, 3))
                .addExplainResult(3, entityMetricsService.getCostOfQueryByAttributesEqual(3, 3))
                .addExplainResult(5, entityMetricsService.getCostOfQueryByAttributesEqual(5, 3))
                .addExplainResult(7, entityMetricsService.getCostOfQueryByAttributesEqual(7, 3))
                .export();


        context.getBean(AttributeSearchResultExporter.class)
                .queryTypeName("like-attributes")
                .addExplainResult(1, entityMetricsService.getCostOfQueryByAttributesLike(1, 3))
                .addExplainResult(3, entityMetricsService.getCostOfQueryByAttributesLike(3, 3))
                .addExplainResult(5, entityMetricsService.getCostOfQueryByAttributesLike(5, 3))
                .addExplainResult(7, entityMetricsService.getCostOfQueryByAttributesLike(7, 3))
                .export();

        context.getBean(AttributeSearchResultExporter.class)
                .queryTypeName("intersect-equal-attributes")
                .addExplainResult(1, entityMetricsService.getCostOfQueryByAttributesEqualUsingIntersectIn(1, 3))
                .addExplainResult(3, entityMetricsService.getCostOfQueryByAttributesEqualUsingIntersectIn(3, 3))
                .addExplainResult(5, entityMetricsService.getCostOfQueryByAttributesEqualUsingIntersectIn(5, 3))
                .addExplainResult(7, entityMetricsService.getCostOfQueryByAttributesEqualUsingIntersectIn(7, 3))
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