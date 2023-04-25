package me.alex.cryptotrader.manager;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.layout.StackPane;
import me.alex.cryptotrader.controller.main.TradingController;
import me.alex.cryptotrader.models.Strategy;
import me.alex.cryptotrader.models.Transaction;
import me.alex.cryptotrader.profile.UserProfile;
import me.alex.cryptotrader.util.MarketPanel;
import me.alex.cryptotrader.util.Utilities;
import me.alex.cryptotrader.util.binance.AggTradesListener;
import me.alex.cryptotrader.util.binance.BinanceUtils;
import me.alex.cryptotrader.util.trading.TradingSession;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

public class TradingManager {

    private final ObservableList<Transaction> transactions;
    private final TradingController controller;
    private final MarketPanel marketPanel;

    private AggTradesListener tradingListener;
    private TradingSession tradingSession;
    private Strategy strategy;
    private String tokenPair;

    private long lastGraphUpdate;
    private boolean isTrading, isPaused;

    public TradingManager(TradingController controller, StackPane pane, ObservableList<Transaction> transactions) {
        this.marketPanel = new MarketPanel(true, "Price", pane);
        this.controller = controller;
        this.transactions = transactions;
    }

    public void selectTradingStrategy(Strategy strategy) {
        this.strategy = strategy;

        if (strategy == null) {
            return;
        }

        this.tokenPair = strategy.tokenProperty().get();

        // Reset graph data.
        marketPanel.resetAll();

        // Fetch and inject historic data.
        List<double[]> historicData = BinanceUtils.fetchHistoryTradingData(tokenPair, "1m", 60 * 60);
        Collections.reverse(historicData);
        historicData.stream().map(data -> data[4]).forEach(value -> marketPanel.addGraphData(value, false));

        // Update the charts title.
        marketPanel.setTitle("Live " + strategy.tokenProperty().get() + " Price");
        marketPanel.updateChart();

        // Register trade data.
        tradingSession = new TradingSession(false, strategy, UserProfile.get().getOwnedToken(strategy.getTokenPairNames()[0]), UserProfile.get().getOwnedToken(strategy.getTokenPairNames()[1]), this::processTrade);

        // Start trading listener.
        startTradeListener();
    }

    public void startTrading() {
        if (isTrading) {
            return;
        }

        isTrading = true;
    }

    public void stopTrading(String haltCondition) {
        if (!isTrading) {
            return;
        }

        isTrading = isPaused = false;

        Platform.runLater(() -> {
            controller.stopTrading();

            if (haltCondition != null) {
                Utilities.sendErrorAlert("Trading was stopped.", haltCondition);
            }
        });
    }

    public void togglePaused() {
        isPaused = !isPaused;
    }

    private void startTradeListener() {
        // Close our trading listener if it exists.
        if (tradingListener != null) {
            tradingListener.close();
        }

        tradingListener = createListener(tokenPair, price -> {
            // Add in the market transaction to our trade.
            tradingSession.addMarketTransaction(System.currentTimeMillis(), price);

            if (isTrading && !isPaused) {
                // Run our strategy on the incoming trade.
                String haltCondition = strategy.onTradePrice(System.currentTimeMillis(), price, tradingSession);

                // If there was a halt condition, stop trading.
                if (haltCondition != null) stopTrading(haltCondition);
            }

            Platform.runLater(() -> {
                if (System.currentTimeMillis() - lastGraphUpdate > 5 * 1000) {
                    marketPanel.addGraphData(price, true);
                    lastGraphUpdate = System.currentTimeMillis();
                }

                controller.onUpdate(strategy, price, tradingSession);
            });
        });
    }

    private void processTrade(double[] trade) {
        Platform.runLater(() -> {
            String[] tokenPair = strategy.getTokenPairNames();

            int tick = marketPanel.getGraphData().size();
            double amount = trade[2];
            double price = trade[3];

            this.transactions.add(
                    new Transaction(
                            strategy.tokenProperty().get(),
                            Utilities.formatPrice(price, tokenPair[1]),
                            (amount > 0 ? "+" : "") + Utilities.FORMAT_TWO_DECIMAL_PLACE.format(amount) + " " + tokenPair[0],
                            Utilities.SHORT_TIME_FORMAT.format(new Date((long) trade[1])),
                            amount > 0 ? "green" : "red",
                            price,
                            tick
                    )
            );

            marketPanel.addScatterData(tick, price, amount > 0);
        });
    }

    public AggTradesListener createListener(String tokenPair, Consumer<Double> priceConsumer) {
        return new AggTradesListener(tokenPair, priceConsumer);
    }

    public Strategy getStrategy() {
        return strategy;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public boolean isTrading() {
        return isTrading;
    }

    public static TradingManager get() {
        return TradingController.get().getManager();
    }

}
