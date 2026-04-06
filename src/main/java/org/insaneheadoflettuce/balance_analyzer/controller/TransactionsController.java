package org.insaneheadoflettuce.balance_analyzer.controller;

import com.google.common.collect.Streams;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import org.insaneheadoflettuce.balance_analyzer.IntervalChopper;
import org.insaneheadoflettuce.balance_analyzer.TransactionCollection;
import org.insaneheadoflettuce.balance_analyzer.TransactionInterval;
import org.insaneheadoflettuce.balance_analyzer.dao.AccountRepository;
import org.insaneheadoflettuce.balance_analyzer.dao.ClusterRepository;
import org.insaneheadoflettuce.balance_analyzer.dao.TransactionRepository;
import org.insaneheadoflettuce.balance_analyzer.model.Cluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@SuppressWarnings("unused")
@Controller
public class TransactionsController {
    private static final DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy.MM");
    private static final DateTimeFormatter yearFormatter = DateTimeFormatter.ofPattern("yyyy");

    @Autowired
    TransactionRepository transactionRepository;

    @Autowired
    ClusterRepository clusterRepository;

    @Autowired
    AccountRepository accountRepository;

    private static final Logger logger = LoggerFactory.getLogger(TransactionsController.class);

    private final Counter requestCounter = Metrics.counter("transaction_controller_requests");

    @Timed("transaction_controller_index")
    @RequestMapping("/")
    public String index(Model model) {
        requestCounter.increment();

        model.addAttribute("accounts", accountRepository.findAll());
        model.addAttribute("transactionRepository", transactionRepository);

        //model.addAttribute("latestTransaction", transactionRepository.getFirstByOrderByValueDateDesc().orElse(Transaction.UNDEFINED));
        //model.addAttribute("latestBookedTransaction", transactionRepository.getFirstByStateOrderByValueDateDesc(Transaction.State.BOOKED).orElse(Transaction.UNDEFINED));
        return "index";
    }

    @RequestMapping("/clusters")
    public String clusters(Model model) {
        requestCounter.increment();

        final var clusters = clusterRepository.findAll();
        final var positive = StreamSupport.stream(clusters.spliterator(), false)
                .filter(Cluster::isConsuming)
                .filter(c -> c.getDifferentialMovement().getValue() > 0.)
                .sorted(Cluster.descendingComparator);
        final var negative = StreamSupport.stream(clusters.spliterator(), false)
                .filter(Cluster::isConsuming)
                .filter(c -> c.getDifferentialMovement().getValue() < 0.)
                .sorted(Cluster.ascendingComparator);
        model.addAttribute("consumingClusters", Streams.concat(positive, negative).collect(Collectors.toList()));

        model.addAttribute("nonconsumingClusters", StreamSupport.stream(clusters.spliterator(), false)
                .filter(Predicate.not(Cluster::isConsuming))
                .collect(Collectors.toList()));
        return "clusters";
    }

    static class Year {
        private final TransactionInterval year;
        private final List<TransactionInterval> months;

        Year(TransactionInterval year) {
            this.year = year;
            this.months = new ArrayList<>();
        }

        TransactionInterval addMonth(TransactionInterval month) {
            months.add(month);
            return month;
        }

        public TransactionCollection getYear() {
            return year.setType(TransactionInterval.Type.YEAR);
        }

        public List<TransactionCollection> getMonths() {
            return months.stream().map(t -> t.setType(TransactionInterval.Type.MONTH)).collect(Collectors.toList());
        }
    }

    @RequestMapping("/cluster/{id}")
    public String cluster(@PathVariable Long id, Model model) {
        requestCounter.increment();

        final var optionalCluster = clusterRepository.findById(id);
        if (optionalCluster.isEmpty()) {
            return "error";
        }

        final var cluster = optionalCluster.get();
        model.addAttribute("cluster", cluster);

        final var optionalRange = transactionRepository.getRange();
        if (optionalRange.isEmpty()) {
            return "error";
        }

        final List<Year> years = new ArrayList<>();
        new IntervalChopper(optionalRange.get()).walkYearsBack((yearBegin, yearEnd) ->
        {
            final var yearInterval = new TransactionInterval(yearBegin, yearEnd, cluster);
            final var year = new Year(yearInterval);
            if (year.year.getTransactions().isEmpty()) {
                logger.debug("Ignoring empty year: " + yearBegin.format(yearFormatter));
                return;
            }
            new IntervalChopper(yearBegin, yearEnd).walkMonthsBack((monthBegin, monthEnd) ->
            {
                final var month = new TransactionInterval(monthBegin, monthEnd, cluster, yearInterval);
                if (month.getTransactions().isEmpty()) {
                    logger.debug("Ignoring empty month: " + monthBegin.format(monthFormatter));
                    return;
                }
                logger.debug("Processing month: " + monthBegin.format(monthFormatter));
                year.addMonth(month);
            });
            years.add(year);
        });
        model.addAttribute("years", years);
        return "cluster";
    }

    @RequestMapping(value = "/transactions", method = RequestMethod.GET)
    public String transactionList(Model model) {
        requestCounter.increment();

        model.addAttribute("transactions", transactionRepository.getAllByOrderByValueDateDesc());
        return "transactions";
    }

    @RequestMapping(value = "/transactions/{accountId}", method = RequestMethod.GET)
    public String transactionList(@PathVariable Long accountId, Model model) {
        requestCounter.increment();

        model.addAttribute("transactions", transactionRepository.getAllByAccountIdOrderByValueDateDesc(accountId));
        return "transactions";
    }

    @RequestMapping("/transaction/{id}")
    public String transaction(@PathVariable Long id, Model model) {
        requestCounter.increment();

        final var transaction = transactionRepository.findById(id);
        if (transaction.isEmpty()) {
            return "error";
        }
        model.addAttribute("transaction", transaction.get());
        return "transaction";
    }
}
