package org.insaneheadoflettuce.balance_analyzer;

import org.insaneheadoflettuce.balance_analyzer.model.Transaction;

import java.util.List;

public interface TransactionCollection {
    String getName();

    boolean isConsuming();

    int getSize();

    List<Transaction> getTransactions();

    Number getDifferentialMovement();

    Number getAbsoluteMovement();

    Number getPositiveMovement();

    Number getNegativeMovement();

    List<String> getAccountColors();

    String getPercentalDifferentialMovement();
}
