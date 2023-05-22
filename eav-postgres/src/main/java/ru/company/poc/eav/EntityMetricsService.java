package ru.company.poc.eav;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntityMetricsService {

    private final JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper;

    private final Random rnd = new Random();

    private Map<Long, String> attrToCodes;

    private List<String> attributeCodes;

    private List<String> dictionaryValues;

    @PostConstruct
    public void postConstruct() {
        this.attrToCodes = jdbcTemplate.query("select attr.id as id, attr.code as code from eav.attribute attr",
                (rs, rowNum) -> Pair.of(rs.getLong("id"), rs.getString("code")))
                .stream()
                .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
        this.attributeCodes = new ArrayList<>(attrToCodes.values());
        this.dictionaryValues = jdbcTemplate.query("select dict.value as value from dictionary dict",
                (rs, rowNum) -> rs.getString("value"));
    }

    public QueryExplainResult getCostOfQueryByAttributesEqual(int attrCount, int times) {
        var query = getQuery(getSubQuery(attrCount, Condition.EQUAL));
        log.debug("Executing {} times query {} ", times, query);
        return returnBestResult(query, times);
    }

    public QueryExplainResult getCostOfQueryByAttributesEqualUsingIntersectIn(int attrCount, int times) {
        var query = getSubQuery(attrCount, Condition.IN);
        log.debug("Executing {} times query {} ", times, query);
        return returnBestResult(query, times);
    }

    public QueryExplainResult getCostOfQueryByAttributesLike(int attrCount, int times) {
        var query = getQuery(getSubQuery(attrCount, Condition.LIKE));
        log.debug("Executing {} times query {} ", times, query);
        return returnBestResult(query, times);
    }

    public String getQuery(int attrCount, Condition condition) {
        return getQuery(getSubQuery(attrCount,condition));
    }

    public QueryExplainResult getCostOfInsertion(int times) {
        var entityName = UUID.randomUUID().toString();
        var query = "insert into entity(name) values ('%s')".formatted(entityName);
        var totalCost = getCost(query);
        var entityId =  (Long) jdbcTemplate.query("select id from entity where name = '%s'".formatted(entityName),
                (ResultSetExtractor<Object>) rs -> {
                    if (rs.next()) {
                        return rs.getLong(1);
                    }
                    throw new RuntimeException("Empty RS");
                }
        );
        var insertQuery = "insert into value(attribute_id, entity_id, value) values ";
        var valuesStr = attrToCodes.keySet()
                .stream()
                .map(attrId -> "(%s, %s, '%s')".formatted(attrId, entityId, dictionaryValues.get(rnd.nextInt(dictionaryValues.size()))))
                .collect(Collectors.joining(","));

        return totalCost.add(getCost(insertQuery + valuesStr));
    }

    private QueryExplainResult returnBestResult(String query, int times) {
        return IntStream.range(0, times)
                .mapToObj(i -> getCost(query))
                .sorted()
                .findFirst()
                .orElseThrow();
    }

    private String getQuery(String subQuery) {
        return "select e.id from entity e where (%s) limit 100".formatted(subQuery);
    }

    private String getSubQuery(int argCount, Condition condition) {
        Collections.shuffle(attributeCodes);
        Collections.shuffle(dictionaryValues);

        var requestedAttrCodes = new ArrayList<>(attributeCodes.subList(0, argCount));
        var values = dictionaryValues.subList(0, requestedAttrCodes.size())
                .stream()
                .map(val ->  condition.isEqual() ? val : val.substring(1, val.length() - 1))
                .toList();

        if (condition == Condition.IN) {
            var queryTemplate = """
                    select v.entity_id from value v
                            join attribute a on v.attribute_id = a.id
                            where (a.code, v.value) in (row('%s', '%s'))
                    """;
            return IntStream.range(0, requestedAttrCodes.size())
                    .mapToObj(index -> queryTemplate.formatted(requestedAttrCodes.get(index), values.get(index)))
                    .collect(Collectors.joining(" intersect "));
        }

        return IntStream.range(0, requestedAttrCodes.size())
                .mapToObj(index -> getExistExpression(requestedAttrCodes.get(index), values.get(index), condition))
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
        var plan = objectMapper.readTree(result)
                .get(0)
                .get("Plan");
        var cost = plan.get("Total Cost").asDouble();
        var totalTime = plan.get("Actual Total Time").asDouble();
        return new QueryExplainResult(cost, totalTime);
    }

    private String getExistExpression(String attrCode, String value, Condition condition) {
        return """
               exists(select val.id
                        from value val
                              join attribute attr on val.attribute_id = attr.id
                              where attr.code = '%s'
                                and %s
                                and e.id = val.entity_id)
               """.formatted(attrCode, condition.format("val.value", value));
    }
}
