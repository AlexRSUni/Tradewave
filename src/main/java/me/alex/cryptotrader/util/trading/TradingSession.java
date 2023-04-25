package me.alex.cryptotrader.util.trading;

import com.binance.api.client.domain.account.Account;
import me.alex.cryptotrader.instruction.ContextState;
import me.alex.cryptotrader.instruction.TimePeriod;
import me.alex.cryptotrader.models.Strategy;
import me.alex.cryptotrader.profile.UserProfile;

import java.util.*;
import java.util.function.Consumer;

public class TradingSession {

    private final Map<Long, Double> previousPrices = new LinkedHashMap<>();
    private final Map<Long, ContextState> previousMarketConditions = new LinkedHashMap<>();

    private final RollingAverage rollingAverage = new RollingAverage(4);
    private final MarketState marketState = new MarketState(500);
    private final Strategy strategy;

    private final Consumer<double[]> transactionConsumer;
    private final double startingToken, startingCurrency;
    private final boolean isTest;

    // Transaction data.
    private long lastMarketTransaction = -1;
    private long lastTransaction = -1;
    private double priceAtLastTransaction = -1;
    private double initialPrice = -1;
    private boolean lastTransactionWasBuy;

    // Tracking values.
    private int tradeCounter;
    private double lastAverage;

    // Token amounts.
    private double tokenAmount;
    private double currencyAmount;
    private double lastPrice;

    // Other variables.
    private boolean shouldStop, isWaiting;
    private long waitTimestamp;
    private long waitDuration;

    public TradingSession(boolean isTest, Strategy strategy, double startingToken, double startingCurrency, Consumer<double[]> transactionConsumer) {
        this.isTest = isTest;
        this.strategy = strategy;
        this.startingToken = this.tokenAmount = startingToken;
        this.startingCurrency = this.currencyAmount = startingCurrency;
        this.transactionConsumer = transactionConsumer;
    }

    public void addMarketTransaction(long timestamp, double price) {
        this.lastMarketTransaction = timestamp;
        this.lastPrice = price;
        this.previousPrices.put(timestamp, price);
        this.rollingAverage.add(price);

        double average = rollingAverage.getAverage();
        double averageChange = average - lastAverage;
        lastAverage = average;

        // If there has been a change in the average value, increment our period change counter so we can monitor the
        // markets state at the current time.
        if (averageChange != 0) {
            marketState.inc(averageChange > 0);
        }

        this.previousMarketConditions.put(timestamp, marketState.getState());
        this.tradeCounter++;
    }

    public void addUserTransaction(long timestamp, double amount, double price) {
        if (initialPrice == -1) {
            initialPrice = price;
        }

        transactionConsumer.accept(new double[]{tradeCounter, timestamp, amount, price});
        lastTransaction = timestamp;
        priceAtLastTransaction = price;
        lastTransactionWasBuy = amount > 0;

        if (!isTest) {
            isWaiting = true;

            // Reload our balances from the API to ensure we have the updated balances locally. We will set the program
            // to the wait state while we do this to avoid any further transactions being processed while we are waiting
            // for a response.
            UserProfile profile = UserProfile.get();
            Account account = profile.getClient().getAccount(UserProfile.BINANCE_API_WAIT, System.currentTimeMillis());

            String[] tokenPair = strategy.getTokenPairNames();
            profile.updateFund(tokenPair[0], Double.parseDouble(account.getAssetBalance(tokenPair[0]).getFree()));
            profile.updateFund(tokenPair[1], Double.parseDouble(account.getAssetBalance(tokenPair[1]).getFree()));

            isWaiting = false;
        }
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

    public ContextState getMostFrequencyMarketState(TimePeriod period) {
        long targetPeriod = lastMarketTransaction - period.getMilliseconds();

        Map<ContextState, Integer> contextFrequency = new HashMap<>();

        for (Map.Entry<Long, ContextState> entry : previousMarketConditions.entrySet()) {
            if (entry.getKey() > targetPeriod) {
                contextFrequency.put(entry.getValue(), contextFrequency.getOrDefault(entry.getValue(), 0) + 1);
            }
        }

        return Collections.max(contextFrequency.entrySet(), Comparator.comparingInt(Map.Entry::getValue)).getKey();
    }

    public void setHalt(long timestamp, TimePeriod duration) {
        if (duration == null) return;
        this.waitTimestamp = timestamp;
        this.waitDuration = duration.getMilliseconds();
    }

    public double getTokenAmount() {
        if (isTest) {
            return tokenAmount;
        } else {
            return UserProfile.get().getOwnedToken(strategy.getTokenPairNames()[0]);
        }
    }

    public void incTokenAmount(double amount) {
        this.tokenAmount += amount;
    }

    public double getCurrencyAmount() {
        if (isTest) {
            return currencyAmount;
        } else {
            return UserProfile.get().getOwnedToken(strategy.getTokenPairNames()[1]);
        }
    }

    public void incCurrencyAmount(double amount) {
        this.currencyAmount += amount;
    }

    public boolean isWaiting() {
        return isWaiting || waitTimestamp + waitDuration > lastMarketTransaction;
    }

    public double getInitialPrice() {
        return initialPrice;
    }

    public double getLastPrice() {
        return lastPrice;
    }

    public void setShouldStop(boolean shouldStop) {
        this.shouldStop = shouldStop;
    }

    public boolean shouldStop() {
        return shouldStop;
    }

    public ContextState getMarketCondition() {
        return marketState.getState();
    }

    public boolean isTest() {
        return isTest;
    }

    public boolean wasLastTransactionBuy() {
        return lastTransactionWasBuy;
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
