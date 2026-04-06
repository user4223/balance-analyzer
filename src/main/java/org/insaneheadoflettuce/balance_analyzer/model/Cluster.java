package org.insaneheadoflettuce.balance_analyzer.model;

import jakarta.persistence.*;
import org.insaneheadoflettuce.balance_analyzer.AbstractTransactionCollection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Entity
public class Cluster extends AbstractTransactionCollection {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String name;
    boolean isConsuming;
    @ManyToMany
    private final List<Transaction> transactions = new ArrayList<>();

    public static final Comparator<Cluster> ascendingComparator = (a, b) ->
    {
        final var f = a.getDifferentialMovement().getValue();
        final var s = b.getDifferentialMovement().getValue();
        return Double.valueOf(f - s).intValue();
    };

    public static final Comparator<Cluster> descendingComparator = (a, b) ->
    {
        final var f = a.getDifferentialMovement().getValue();
        final var s = b.getDifferentialMovement().getValue();
        return Double.valueOf(s - f).intValue();
    };

    protected Cluster() {
        super(null);
    }

    public static Cluster create(String name, boolean isConsuming, List<Transaction> transactions) {
        if (isConsuming && transactions.stream().anyMatch(Transaction::isClustered)) {
            throw new IllegalArgumentException("Transaction cannot be consumed twice");
        }

        final var cluster = new Cluster();
        cluster.name = name;
        cluster.isConsuming = isConsuming;
        cluster.transactions.addAll(transactions.stream()
                .map(transaction -> transaction.add(cluster))
                .collect(Collectors.toList()));
        return cluster;
    }

    public static Cluster create(String name, List<Cluster> clusters) {
        final var cluster = new Cluster();
        cluster.name = name;
        cluster.isConsuming = false;
        cluster.transactions.addAll(clusters.stream()
                .flatMap(c -> c.getTransactions().stream())
                .sorted(Comparator.comparing(Transaction::getValueDate).reversed())
                .collect(Collectors.toList()));
        return cluster;
    }

    public Long getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isConsuming() {
        return isConsuming;
    }

    @Override
    public List<Transaction> getTransactions() {
        return transactions;
    }
}
