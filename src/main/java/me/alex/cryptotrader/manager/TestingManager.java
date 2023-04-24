package me.alex.cryptotrader.manager;

import javafx.collections.ObservableList;
import javafx.scene.layout.StackPane;
import me.alex.cryptotrader.controller.main.TestingController;
import me.alex.cryptotrader.instruction.TimePeriod;
import me.alex.cryptotrader.models.Strategy;
import me.alex.cryptotrader.models.Transaction;
import me.alex.cryptotrader.profile.UserProfile;
import me.alex.cryptotrader.util.MarketPanel;
import me.alex.cryptotrader.util.Utilities;
import me.alex.cryptotrader.util.binance.BinanceUtils;
import me.alex.cryptotrader.util.trading.TradingSession;
import org.apache.commons.lang3.math.NumberUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TestingManager {

    private final ObservableList<Transaction> transactions;
    private final TestingController controller;
    private final MarketPanel marketPanel;

    // Historic data cache.
    private List<double[]> historicData;

    // Selected strategy.
    private Strategy currentStrategy;
    private TimePeriod period;

    public TestingManager(TestingController controller, StackPane stackPane, ObservableList<Transaction> transactions) {
        this.marketPanel = new MarketPanel(true, "Price", stackPane);
        this.transactions = transactions;
        this.controller = controller;
    }

    public void startStrategyTest(String currency) {
        if (currentStrategy == null || period == null || historicData == null) {
            Utilities.sendErrorAlert("Failed to start test!", "Both a strategy and time period must be selected!");
            return;
        }

        if (currency != null && !currency.isEmpty() && !NumberUtils.isCreatable(currency)) {
            Utilities.sendErrorAlert("Invalid starting currency!", "The number '" + currency + "' is not a valid starting currency amount!");
            return;
        }

        if (historicData.isEmpty()) {
            Utilities.sendErrorAlert("Could not load historic data!", "Failed to load any historic trading data. Could be throttled by Binance!");
            return;
        }

        this.transactions.clear();

        String[] tokenPair = currentStrategy.getTokenPairNames();
        UserProfile profile = UserProfile.get();

        TradingSession data = new TradingSession(true, currentStrategy, profile.getOwnedToken(tokenPair[0]), NumberUtils.toDouble(currency, profile.getOwnedToken(tokenPair[1])), trade -> {
            double amount = trade[2];
            double price = trade[3];

            this.transactions.add(
                    new Transaction(
                            currentStrategy.tokenProperty().get(),
                            Utilities.formatPrice(price, tokenPair[1]),
                            (amount > 0 ? "+" : "") + Utilities.FORMAT_TWO_DECIMAL_PLACE.format(amount) + " " + tokenPair[0],
                            Utilities.SHORT_TIME_FORMAT.format(new Date((long) trade[1])),
                            amount > 0 ? "green" : "red",
                            amount,
                            -1
                    )
            );

            marketPanel.addScatterData((int) trade[0], price, amount > 0);
        });

        String haltCondition = null;

        for (double[] trade : historicData) {
            haltCondition = processTransaction((long) trade[0], trade[1], data);

            if (haltCondition != null) {
                break;
            }

            haltCondition = processTransaction((long) (trade[0] + 30_000), trade[4], data);

            if (haltCondition != null) {
                break;
            }
        }

        if (haltCondition != null) {
            Utilities.sendErrorAlert("Testing stopped before it could finish!", haltCondition);
        }

        controller.onTestFinished(currentStrategy, data.getStartingToken(), data.getTokenAmount(),
                data.getStartingCurrency(), data.getCurrencyAmount(),
                historicData.get(0)[4], historicData.get(historicData.size() - 1)[4]);
    }

    private String processTransaction(long timestamp, double price, TradingSession data) {
        data.addMarketTransaction(timestamp, price);
        return currentStrategy.onTradePrice(timestamp, price, data);
    }

    public void onStrategySelected(Strategy strategy) {
        this.currentStrategy = strategy;

        controller.updateCurrencyDisplay(null, true);

        if (period != null) {
            fetchAndPopulateGraph();
        }
    }

    public void onTimePeriodSelected(TimePeriod period) {
        this.period = period;

        controller.updateCurrencyDisplay(null, true);

        if (currentStrategy != null) {
            fetchAndPopulateGraph();
        }
    }

    private void fetchAndPopulateGraph() {
        List<Double> testTrades = gatherTestTrades(period, currentStrategy.tokenProperty().get());
        marketPanel.addGraphData(testTrades, true);

        // Update text box status.
        controller.updateCurrencyDisplay(currentStrategy.getTokenPairNames()[1], false);

        // Update the charts title.
        marketPanel.setTitle(period.getShortName() + " " + currentStrategy.tokenProperty().get() + " Prices");
    }

    // Loads our historic data from the Binance API. For longer time periods, we need to string together requests as
    // the API only responds with so many values.
    private List<Double> gatherTestTrades(TimePeriod period, String token) {
        historicData = new ArrayList<>();

        switch (period) {
            case _48_HOURS -> {
                long start = Instant.now().minusSeconds(48 * 60 * 60).toEpochMilli();
                historicData.addAll(BinanceUtils.fetchHistoryTradingData(token, "1m", start, start + 28800000));
                historicData.addAll(BinanceUtils.fetchHistoryTradingData(token, "1m", start + 28800000, start + 57600000));
                historicData.addAll(BinanceUtils.fetchHistoryTradingData(token, "1m", start + 57600000, start + 86400000));
                historicData.addAll(BinanceUtils.fetchHistoryTradingData(token, "1m", start + 86400000, start + 115200000));
                historicData.addAll(BinanceUtils.fetchHistoryTradingData(token, "1m", start + 115200000, start + 144000000));
                historicData.addAll(BinanceUtils.fetchHistoryTradingData(token, "1m", start + 144000000, Instant.now().toEpochMilli()));
            }
            case _24_HOURS -> {
                long start = Instant.now().minusSeconds(24 * 60 * 60).toEpochMilli();
                historicData.addAll(BinanceUtils.fetchHistoryTradingData(token, "1m", start, start + 28800000));
                historicData.addAll(BinanceUtils.fetchHistoryTradingData(token, "1m", start + 28800000, start + 57600000));
                historicData.addAll(BinanceUtils.fetchHistoryTradingData(token, "1m", start + 57600000, Instant.now().toEpochMilli()));
            }
            case _12_HOURS -> {
                long start = Instant.now().minusSeconds(12 * 60 * 60).toEpochMilli();
                historicData.addAll(BinanceUtils.fetchHistoryTradingData(token, "1m", start, start + 21600000));
                historicData.addAll(BinanceUtils.fetchHistoryTradingData(token, "1m", start + 21600000, Instant.now().toEpochMilli()));
            }
            case _6_HOURS -> historicData.addAll(BinanceUtils.fetchHistoryTradingData(token, "1m", 6 * 60 * 60));
            case _3_HOURS -> historicData.addAll(BinanceUtils.fetchHistoryTradingData(token, "1m", 3 * 60 * 60));
        }

        List<Double> testTrades = new ArrayList<>();

        for (double[] data : historicData) {
            testTrades.add(data[1]); // Add open price.
            testTrades.add(data[4]); // Add close price.
        }

        return testTrades;
    }

    public MarketPanel getMarketPanel() {
        return marketPanel;
    }
}
