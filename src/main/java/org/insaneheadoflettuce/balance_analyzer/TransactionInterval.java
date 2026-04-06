package org.insaneheadoflettuce.balance_analyzer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.insaneheadoflettuce.balance_analyzer.model.Cluster;
import org.insaneheadoflettuce.balance_analyzer.model.Transaction;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class TransactionInterval extends AbstractTransactionCollection {
    public enum Type {
        DAY,
        MONTH,
        YEAR
    }

    static Map<Type, String> typeMap = Map.of(
            Type.DAY, "day",
            Type.MONTH, "month",
            Type.YEAR, "year"
    );

    private static final Logger logger = LogManager.getLogger(TransactionInterval.class);

    private final LocalDate begin;
    private final LocalDate end;
    private final Cluster cluster;
    private Type type;

    static Map<Type, DateTimeFormatter> dateFormatterMap = Map.of(
            Type.DAY, DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            Type.MONTH, DateTimeFormatter.ofPattern("MM.yyyy"),
            Type.YEAR, DateTimeFormatter.ofPattern("yyyy"));

    static String getShortestPossibleIntervalString(LocalDate begin, LocalDate end) {
        if (begin.getYear() == end.getYear()) {
            final var m1 = begin.getMonth();
            final var m2 = end.getMonth();
            if (m1 == m2) // Same month
            {
                final var d1 = begin.getDayOfMonth();
                final var d2 = end.getDayOfMonth();
                if (d1 == d2) // Same day
                {
                    return begin.format(dateFormatterMap.get(Type.DAY));
                }
                if (begin == begin.with(TemporalAdjusters.firstDayOfMonth()) && end == end.with(TemporalAdjusters.lastDayOfMonth())) {
                    return begin.format(dateFormatterMap.get(Type.MONTH));
                }
            }
            if (begin == begin.with(TemporalAdjusters.firstDayOfYear()) && end == end.with(TemporalAdjusters.lastDayOfYear())) {
                return begin.format(dateFormatterMap.get(Type.YEAR));
            }
        }
        return begin.format(dateFormatterMap.get(Type.DAY)) + " - " + end.format(dateFormatterMap.get(Type.DAY));
    }

    public TransactionInterval(LocalDate begin, LocalDate end, Cluster cluster) {
        this(begin, end, cluster, null);
    }

    public TransactionInterval(LocalDate begin, LocalDate end, Cluster cluster, TransactionInterval parentInterval) {
        super(parentInterval);
        this.begin = begin;
        this.end = end;
        this.cluster = cluster;
        this.type = Type.DAY;

        logger.debug("Interval: " + begin.toString() + " - " + end.toString());
    }

    public String getIntervalString() {
        return getShortestPossibleIntervalString(begin, end);
    }

    public TransactionInterval setType(Type type) {
        this.type = type;
        return this;
    }

    public String getTypeString() {
        return typeMap.get(type);
    }

    @Override
    public String getName() {
        return cluster.getName();
    }

    @Override
    public boolean isConsuming() {
        return cluster.isConsuming();
    }

    @Override
    public List<Transaction> getTransactions() {
        final var from = begin.minusDays(1);
        final var to = end.plusDays(1);
        final Predicate<Transaction> isInInterval = (transaction) ->
        {
            final var date = transaction.getValueDate();
            return date.isAfter(from) && date.isBefore(to);
        };

        return cluster.getTransactions().stream()
                .filter(isInInterval)
                .sorted(Comparator.comparing(Transaction::getValueDate).reversed())
                .toList();
    }
}
