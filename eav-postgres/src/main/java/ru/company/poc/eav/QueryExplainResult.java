package ru.company.poc.eav;

public record QueryExplainResult (double cost, double actualTime) implements Comparable<QueryExplainResult> {

    public QueryExplainResult add(QueryExplainResult result) {
        return new QueryExplainResult(this.cost + result.cost, this.actualTime + result.actualTime);
    }

    @Override
    public int compareTo(QueryExplainResult result) {
        return Double.compare(this.actualTime, result.actualTime);
    }
}
