package me.alex.cryptotrader.manager;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;
import me.alex.cryptotrader.models.Transaction;
import me.alex.cryptotrader.profile.UserProfile;
import me.alex.cryptotrader.util.MarketPanel;
import me.alex.cryptotrader.util.Utilities;
import me.alex.cryptotrader.util.binance.AggTradesListener;
import me.alex.cryptotrader.util.binance.BinanceUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class DashboardManager {

    private final ObservableList<Transaction> transactions;
    private final ComboBox<String> comboBox;
    private final MarketPanel marketPanel;

    private AggTradesListener tradesListener;

    // Trading data.
    private String token;
    private double lastPrice;
    private long lastGraphUpdate;

    public DashboardManager(VBox graphBox, ObservableList<Transaction> transactions, ComboBox<String> comboBox) {
        this.marketPanel = new MarketPanel(false, "Price", graphBox);
        this.transactions = transactions;
        this.lastGraphUpdate = System.currentTimeMillis();
        this.comboBox = comboBox;

        // Create and setup data.
        setupTokenVisualData(UserProfile.get().getDashboardToken());
    }

    public void setupTokenVisualData(String token) {
        if (token.equalsIgnoreCase(this.token)) {
            return;
        }

        // Cache our used token locally.
        this.token = token;

        String unit = Utilities.splitTokenPairSymbols(token)[1];

        // Update the charts title.
        marketPanel.setTitle("24h " + token + " Prices");

        // Create trade listener.
        createTradeListener(token, unit);

        // Fetch historic data.
        List<double[]> historicData = BinanceUtils.fetchHistoryTradingData(token, "5m", 24 * 60 * 60);

        // If no data is found, just reset the token.
        if (historicData.isEmpty()) {
            setupTokenVisualData("BTCUSDT");
            comboBox.getEditor().setText("BTCUSDT");
            return;
        }

        // Clear previously collected data.
        this.transactions.clear();
        marketPanel.resetAll();

        List<Transaction> bulkTransactions = new ArrayList<>();

        // Add all historic data to the graph.
        historicData.forEach(data -> bulkTransactions.add(addMarketTransaction(data[4], Utilities.SHORT_TIME_FORMAT.format(new Date((long) data[0])), unit, true)));

        // Add historic data to our transaction record.
        Collections.reverse(bulkTransactions);
        transactions.addAll(bulkTransactions);

        // Save the users option locally.
        UserProfile.get().setDashboardToken(token);
    }

    private void createTradeListener(String tokenPair, String unit) {
        // Cancel the trade listener if we have one already.
        if (tradesListener != null) {
            tradesListener.close();
        }

        // Create trade listener for graph.
        tradesListener = TradingManager.get().createListener(tokenPair, price -> {
            Platform.runLater(() -> addMarketTransaction(price, Utilities.SHORT_TIME_FORMAT.format(new Date()), unit, false));
        });
    }

    private Transaction addMarketTransaction(double price, String time, String unit, boolean bulkData) {
        Transaction transaction = new Transaction(
                token,
                Utilities.FORMAT_TWO_DECIMAL_PLACE.format(price) + " " + unit,
                null,
                time,
                price > lastPrice ? "green" : price == lastPrice ? "gray" : "red",
                price,
                -1
        );

        // Cache price locally.
        lastPrice = price;

        // Only update the graph in intervals (5 mins) to preserve the scale.
        if (bulkData || System.currentTimeMillis() - lastGraphUpdate > 300_000) {
            marketPanel.addGraphData(price, false);
            lastGraphUpdate = System.currentTimeMillis();
        }

        if (!bulkData) {
            // Add the transaction.
            transactions.add(0, transaction);

            // Update the chart visuals.
            marketPanel.updateChart();
            return null;
        }

        return transaction;
    }
}
