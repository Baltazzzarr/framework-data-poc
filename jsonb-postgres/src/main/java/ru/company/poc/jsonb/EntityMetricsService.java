package ru.company.poc.jsonb;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Slf4j
@RequiredArgsConstructor
public class EntityMetricsService {

    private final JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper;

    private List<String> attributeCodes;

    private List<String> dictionaryValues;

    private final Random rnd = new Random();

    @PostConstruct
    public void postConstruct() {
        this.attributeCodes = jdbcTemplate.query("select attr.code as code from eav.attribute attr",
                (rs, rowNum) -> rs.getString("code"));
        this.dictionaryValues = jdbcTemplate.query("select dict.value as value from dictionary dict",
                (rs, rowNum) -> rs.getString("value"));
    }

    public QueryExplainResult getCostOfQueryByAttributesUsingIncludesOperator(int attrCount, int times) {
        var query = getQuery(getSubQuery(attrCount, Condition.INCLUDE));
        log.debug("Executing {} times query {} ", times, query);
        return returnBestResult(query, times);
    }

    public QueryExplainResult getCostOfQueryByAttributesUsingFieldEqualOperator(int attrCount, int times) {
        var query = getQuery(getSubQuery(attrCount, Condition.EQUAL));
        log.debug("Executing {} times query {} ", times, query);
        return returnBestResult(query, times);
    }

    public QueryExplainResult getCostOfQueryByAttributesLike(int attrCount, int times) {
        var query = getQuery(getSubQuery(attrCount, Condition.LIKE));
        log.debug("Executing {} times query {} ", times, query);
        return returnBestResult(query, times);
    }

    @SneakyThrows
    public QueryExplainResult getCostOfInsertion(int times)  {
        var entityName = UUID.randomUUID().toString();
        var properties = new HashMap<String, String>();
        attributeCodes.forEach(attr ->
                properties.put(attr, dictionaryValues.get(rnd.nextInt(dictionaryValues.size()))));

        var query = "insert into entity(name, properties) values ('%s', '%s')".formatted(entityName,
                objectMapper.writeValueAsString(properties));

        return getCost(query);
    }

    private QueryExplainResult returnBestResult(String query, int times) {
        var costs = IntStream.range(0, times)
                .mapToObj(i -> getCost(query))
                .toList();
        return costs.stream()
                .sorted()
                .findFirst()
                .orElseThrow();
    }

    private String getQuery(String subQuery) {
        return "select e.id from entity e where (%s) limit 100".formatted(subQuery);
    }

    @SneakyThrows
    private String  getSubQuery(int argCount, Condition condition) {
        Collections.shuffle(attributeCodes);
        Collections.shuffle(dictionaryValues);

        var requestedAttrCodes = new ArrayList<>(attributeCodes.subList(0, argCount));
        var values = dictionaryValues.subList(0, requestedAttrCodes.size())
                .stream()
                .map(val ->  condition.isEqual() ? val : val.substring(1, val.length() - 1))
                .toList();

        if (condition == Condition.INCLUDE) {
            var content = IntStream.range(0, requestedAttrCodes.size())
                    .boxed()
                    .collect(Collectors.toMap(requestedAttrCodes::get, values::get));
            return "e.properties @> '%s'".formatted(objectMapper.writeValueAsString(content));
        }

        return IntStream.range(0, requestedAttrCodes.size())
                .mapToObj(index -> getExistExpression(condition, requestedAttrCodes.get(index), values.get(index)))
                .collect(Collectors.joining(" and "));
    }

    @SneakyThrows
    private QueryExplainResult getCost(String query) {
        var result = (String) jdbcTemplate.query("explain (analyze true, format json) " + query,
                (ResultSetExtractor<Object>) rs -> {
                    if (rs.next()) {
                        return rs.getString(1);
                    }
                    throw new RuntimeException("Empty RS");
                }
        );
        log.debug("PLAN: {}", result);
        var plan = objectMapper.readTree(result)
                .get(0)
                .get("Plan");
        var cost = plan.get("Total Cost").asDouble();
        var totalTime = plan.get("Actual Total Time").asDouble();
        return new QueryExplainResult(cost, totalTime);
    }

    private String getExistExpression(Condition condition, String attrCode, String value) {
        if (condition == Condition.EQUAL) {
            return "e.properties ->> '%s' = '%s'".formatted(attrCode, value);
        }
        if (condition == Condition.LIKE) {
            return "e.properties ->> '%s' like '%s'".formatted(attrCode, "%" + value + "%");
        }
        throw new RuntimeException("Invalid condition");
    }
}
