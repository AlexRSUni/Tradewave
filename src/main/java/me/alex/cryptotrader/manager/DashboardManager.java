package me.alex.cryptotrader.manager;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import me.alex.cryptotrader.models.Transaction;
import me.alex.cryptotrader.profile.UserProfile;
import me.alex.cryptotrader.util.Utilities;
import me.alex.cryptotrader.util.binance.AggTradesListener;
import me.alex.cryptotrader.util.binance.BinanceUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class DashboardManager {

    // Data displayed on graphs.
    private final ObservableList<Transaction> transactions;
    private final List<Double> graphData;

    // Trade listener.
    private AggTradesListener tradesListener;

    // Graph data.
    private LineChart<Number, Number> chart;
    private XYChart.Series<Number, Number> series;
    private NumberAxis xAxis, yAxis;

    // Selection combo box.
    private final ComboBox<String> comboBox;

    // Trading data.
    private String token;
    private double lastPrice;
    private long lastGraphUpdate;

    public DashboardManager(VBox graphBox, ObservableList<Transaction> transactions, ComboBox<String> comboBox) {
        this.graphData = new ArrayList<>();
        this.transactions = transactions;
        this.lastGraphUpdate = System.currentTimeMillis();
        this.comboBox = comboBox;

        // Create the graph which will display the price.
        createPriceGraph(graphBox);

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
        chart.setTitle("24h " + token + " Prices");

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
        this.graphData.clear();
        this.transactions.clear();
        this.series.getData().clear();

        List<Transaction> bulkTransactions = new ArrayList<>();

        // Add all historic data to the graph.
        historicData.forEach(data -> bulkTransactions.add(addMarketTransaction(data[4], Utilities.SHORT_TIME_FORMAT.format(new Date((long) data[0])), unit, true)));

        Collections.reverse(bulkTransactions);
        transactions.addAll(bulkTransactions);

        // Save the users option locally.
        UserProfile.get().setDashboardToken(token);

        // Finally, update the chart.
        updateChart();
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

    private void createPriceGraph(VBox graphBox) {
        xAxis = new NumberAxis();
        yAxis = new NumberAxis();

        chart = new LineChart<>(xAxis, yAxis);

        yAxis.setAutoRanging(false);
        yAxis.setTickMarkVisible(false);
        yAxis.setTickUnit(100);

        xAxis.setLabel("");
        xAxis.setTickUnit(1);
        xAxis.setMinorTickVisible(false);

        xAxis.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number object) {
                return "";
            }

            @Override
            public Number fromString(String string) {
                return null;
            }
        });

        chart.setCreateSymbols(false);
        chart.setAnimated(false);

        series = new XYChart.Series<>();
        series.setName("Price");
        chart.getData().add(series);

        // Other configurations
        chart.lookup(".chart-plot-background").setStyle("-fx-background-color: transparent;");

        graphBox.getChildren().add(chart);
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
            graphData.add(price);
            lastGraphUpdate = System.currentTimeMillis();
        }

        if (!bulkData) {
            // Add the transaction.
            transactions.add(0, transaction);

            // Update the chart visuals.
            updateChart();
            return null;
        }

        return transaction;
    }

    private void updateChart() {
        series.getData().clear();

        for (int i = 0; i < graphData.size(); i++) {
            double price = graphData.get(i);
            series.getData().add(new XYChart.Data<>(i + 1, price));
        }

        double minPrice = graphData.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
        double maxPrice = graphData.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        yAxis.setLowerBound(minPrice);
        yAxis.setUpperBound(maxPrice);
    }
}
