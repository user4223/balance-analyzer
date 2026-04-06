package org.insaneheadoflettuce.balance_analyzer;

import org.insaneheadoflettuce.balance_analyzer.model.Account;
import org.insaneheadoflettuce.balance_analyzer.model.Transaction;

import java.util.List;

public abstract class AbstractTransactionCollection implements TransactionCollection {

    private final TransactionInterval parentInterval;

    protected AbstractTransactionCollection(TransactionInterval parentInterval) {
        this.parentInterval = parentInterval;
    }

    @Override
    public final int getSize() {
        return getTransactions().size();
    }

    @Override
    public final List<String> getAccountColors() {
        return getTransactions().stream()
                .map(Transaction::getAccount)
                .distinct()
                .map(Account::getColor)
                .toList();
    }

    @Override
    public final Number getDifferentialMovement() {
        return new Number(getTransactions().stream()
                .map(Transaction::getAmount)
                .mapToDouble(Number::getValue)
                .sum());
    }

    @Override
    public final Number getAbsoluteMovement() {
        return new Number(getTransactions().stream()
                .map(Transaction::getAmount)
                .mapToDouble(a -> Math.abs(a.getValue()))
                .sum(), true);
    }

    @Override
    public final Number getPositiveMovement() {
        return new Number(getTransactions().stream()
                .map(Transaction::getAmount)
                .filter(Number::isPositive)
                .mapToDouble(Number::getValue)
                .sum());
    }

    @Override
    public final Number getNegativeMovement() {
        return new Number(getTransactions().stream()
                .map(Transaction::getAmount)
                .filter(Number::isNegative)
                .mapToDouble(Number::getValue)
                .sum());
    }

    @Override
    public final String getPercentalDifferentialMovement() {
        if (parentInterval == null) {
            return "";
        }

        final var current = getDifferentialMovement();
        if (current.isNegative()) {
            final var maximumNegative = parentInterval.getNegativeMovement().getValue().intValue();
            if (maximumNegative == 0) {
                return "";
            }

            final var count = Math.round((current.getValue() * 100. / maximumNegative));
            return "-".repeat((int) (20 * count / 100));
            //return String.valueOf(count);
        } else if (current.isPositive()) {
            final var maximumPostive = parentInterval.getPositiveMovement().getValue().intValue();
            if (maximumPostive == 0) {
                return "";
            }

            final var count = Math.round((current.getValue() * 100. / maximumPostive));
            return "+".repeat((int) (20 * count / 100));
            //return String.valueOf(count);
        }
        return "";
    }
}
