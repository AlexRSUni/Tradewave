package me.alex.cryptotrader.util.trading;

import javafx.collections.ObservableList;
import me.alex.cryptotrader.instruction.ActionType;
import me.alex.cryptotrader.instruction.TimePeriod;
import me.alex.cryptotrader.models.Strategy;
import me.alex.cryptotrader.models.Transaction;
import me.alex.cryptotrader.util.Utilities;

import java.util.*;
import java.util.function.Consumer;

public class TradingData {

    private final Map<Long, Double> previousPrices = new LinkedHashMap<>();
    private final Map<Long, ActionType> previousMarketConditions = new LinkedHashMap<>();

    private final RollingAverage rollingAverage = new RollingAverage(4);
    private final PeriodChange periodChange = new PeriodChange(500);

    private final Consumer<double[]> transactionConsumer;
    private final double startingToken, startingCurrency;
    private final boolean isTest;

    // Transaction data.
    private long lastMarketTransaction = -1;
    private long lastTransaction = -1;
    private double priceAtLastTransaction = -1;

    // Tracking values.
    private int tradeCounter;
    private double lastAverage;

    // Token amounts.
    private double tokenAmount;
    private double currencyAmount;

    // Other variables.
    private boolean shouldStop;
    private long waitTimestamp;
    private long waitDuration;

    public TradingData(boolean isTest, double startingToken, double startingCurrency, Consumer<double[]> transactionConsumer) {
        this.isTest = isTest;
        this.startingToken = this.tokenAmount = startingToken;
        this.startingCurrency = this.currencyAmount = startingCurrency;
        this.transactionConsumer = transactionConsumer;
    }

    public void addMarketTransaction(long timestamp, double price) {
        this.lastMarketTransaction = timestamp;
        this.previousPrices.put(timestamp, price);
        this.rollingAverage.add(price);

        double average = rollingAverage.getAverage();
        double averageChange = average - lastAverage;
        lastAverage = average;

        // If there has been a change in the average value, increment our period change counter so we can monitor the
        // markets state at the current time.
        if (averageChange != 0) {
            periodChange.inc(averageChange > 0);
        }

        this.previousMarketConditions.put(timestamp, periodChange.getState());
        this.tradeCounter++;
    }

    public void addUserTransaction(long timestamp, double amount, double price) {
        transactionConsumer.accept(new double[]{tradeCounter, timestamp, amount, price});
        lastTransaction = timestamp;
        priceAtLastTransaction = price;
    }

    public double getPriceAtPeriodBefore(TimePeriod period) {
        long targetPeriod = lastMarketTransaction - period.getMilliseconds();

        for (Map.Entry<Long, Double> entry : previousPrices.entrySet()) {
            if (entry.getKey() > targetPeriod) {
                return entry.getValue();
            }
        }

        return -1;
    }

    public ActionType getMostFrequencyMarketState(TimePeriod period) {
        long targetPeriod = lastMarketTransaction - period.getMilliseconds();

        Map<ActionType, Integer> conditionFrequency = new HashMap<>();

        for (Map.Entry<Long, ActionType> entry : previousMarketConditions.entrySet()) {
            if (entry.getKey() > targetPeriod) {
                conditionFrequency.put(entry.getValue(), conditionFrequency.getOrDefault(entry.getValue(), 0) + 1);
            }
        }

        return Collections.max(conditionFrequency.entrySet(), Comparator.comparingInt(Map.Entry::getValue)).getKey();
    }

    public void setHalt(long timestamp, TimePeriod duration) {
        if (duration == null) return;
        this.waitTimestamp = timestamp;
        this.waitDuration = duration.getMilliseconds();
    }

    public boolean isHalted() {
        return waitTimestamp + waitDuration > lastMarketTransaction;
    }

    public void setShouldStop(boolean shouldStop) {
        this.shouldStop = shouldStop;
    }

    public boolean shouldStop() {
        return shouldStop;
    }

    public ActionType getMarketCondition() {
        return periodChange.getState();
    }

    public double getTokenAmount() {
        return tokenAmount;
    }

    public void incTokenAmount(double amount) {
        this.tokenAmount += amount;
    }

    public double getCurrencyAmount() {
        return currencyAmount;
    }

    public void incCurrencyAmount(double amount) {
        this.currencyAmount += amount;
    }

    public boolean isTest() {
        return isTest;
    }

    public double getStartingToken() {
        return startingToken;
    }

    public double getStartingCurrency() {
        return startingCurrency;
    }

    public long getLastTransaction() {
        return lastTransaction;
    }

    public double getPriceAtLastTransaction() {
        return priceAtLastTransaction;
    }
}
