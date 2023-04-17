package me.alex.cryptotrader.manager;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.StackPane;
import javafx.util.StringConverter;
import me.alex.cryptotrader.controller.main.TestingController;
import me.alex.cryptotrader.instruction.TimePeriod;
import me.alex.cryptotrader.models.Strategy;
import me.alex.cryptotrader.models.Transaction;
import me.alex.cryptotrader.profile.UserProfile;
import me.alex.cryptotrader.util.Utilities;
import me.alex.cryptotrader.util.trading.TradingData;
import org.apache.commons.lang3.math.NumberUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TestingManager {

    private final ObservableList<Transaction> transactions;
    private final TestingController controller;

    // Graph data.
    private LineChart<Number, Number> lineChart;

    private XYChart.Series<Number, Number> lineSeries;
    private XYChart.Series<Number, Number> buySeries;
    private XYChart.Series<Number, Number> sellSeries;

    private NumberAxis xAxis, yAxis;

    // Selected strategy.
    private Strategy currentStrategy;
    private TimePeriod period;
    private List<double[]> historicData;

    public TestingManager(TestingController controller, StackPane stackPane, ObservableList<Transaction> transactions) {
        this.transactions = transactions;
        this.controller = controller;

        // Create the graph which will display the price.
        createGraphs(stackPane);
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

        TradingData data = new TradingData(true, profile.getOwnedToken(tokenPair[0]), NumberUtils.toDouble(currency, profile.getOwnedToken(tokenPair[1])), trade -> {
            double amount = trade[2];
            double price = trade[3];

            this.transactions.add(
                    new Transaction(
                            currentStrategy.tokenProperty().get(),
                            Utilities.FORMAT_TWO_DECIMAL_PLACE.format(price) + " " + tokenPair[1],
                            (amount > 0 ? "+" : "") + Utilities.FORMAT_TWO_DECIMAL_PLACE.format(amount) + " " + tokenPair[0],
                            Utilities.SHORT_TIME_FORMAT.format(new Date((long) trade[1])),
                            amount > 0 ? "green" : "red"
                    )
            );

            if (amount > 0) {
                XYChart.Data<Number, Number> buyPoint = new XYChart.Data<>(trade[0], price);
                buySeries.getData().add(buyPoint);
                Platform.runLater(() -> buyPoint.getNode().getStyleClass().add("buy-point"));
            } else {
                XYChart.Data<Number, Number> sellPoint = new XYChart.Data<>(trade[0], price);
                sellSeries.getData().add(sellPoint);
                Platform.runLater(() -> sellPoint.getNode().getStyleClass().add("sell-point"));
            }
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

        controller.onTestFinished(currentStrategy, data.getStartingToken(), data.getTokenAmount(), data.getStartingCurrency(), data.getCurrencyAmount());
    }

    private String processTransaction(long timestamp, double price, TradingData data) {
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

    public void clearTradeSeries() {
        buySeries.getData().clear();
        sellSeries.getData().clear();
    }

    private void fetchAndPopulateGraph() {
        List<Double> testTrades = gatherTestTrades(period, currentStrategy.tokenProperty().get());
        updateChart(testTrades);

        // Update text box status.
        controller.updateCurrencyDisplay(currentStrategy.getTokenPairNames()[1], false);

        // Update the charts title.
        lineChart.setTitle(period.getShortName() + " " + currentStrategy.tokenProperty().get() + " Prices");
    }

    private List<Double> gatherTestTrades(TimePeriod period, String token) {
        historicData = new ArrayList<>();

        switch (period) {
            case _48_HOURS -> {
                long start = Instant.now().minusSeconds(48 * 60 * 60).toEpochMilli();
                historicData.addAll(Utilities.fetchHistoryTradingData(token, "1m", start, start + 28800000));
                historicData.addAll(Utilities.fetchHistoryTradingData(token, "1m", start + 28800000, start + 57600000));
                historicData.addAll(Utilities.fetchHistoryTradingData(token, "1m", start + 57600000, start + 86400000));
                historicData.addAll(Utilities.fetchHistoryTradingData(token, "1m", start + 86400000, start + 115200000));
                historicData.addAll(Utilities.fetchHistoryTradingData(token, "1m", start + 115200000, start + 144000000));
                historicData.addAll(Utilities.fetchHistoryTradingData(token, "1m", start + 144000000, Instant.now().toEpochMilli()));
            }
            case _24_HOURS -> {
                long start = Instant.now().minusSeconds(24 * 60 * 60).toEpochMilli();
                historicData.addAll(Utilities.fetchHistoryTradingData(token, "1m", start, start + 28800000));
                historicData.addAll(Utilities.fetchHistoryTradingData(token, "1m", start + 28800000, start + 57600000));
                historicData.addAll(Utilities.fetchHistoryTradingData(token, "1m", start + 57600000, Instant.now().toEpochMilli()));
            }
            case _12_HOURS -> {
                long start = Instant.now().minusSeconds(12 * 60 * 60).toEpochMilli();
                historicData.addAll(Utilities.fetchHistoryTradingData(token, "1m", start, start + 21600000));
                historicData.addAll(Utilities.fetchHistoryTradingData(token, "1m", start + 21600000, Instant.now().toEpochMilli()));
            }
            case _6_HOURS -> historicData.addAll(Utilities.fetchHistoryTradingData(token, "1m", 6 * 60 * 60));
            case _3_HOURS -> historicData.addAll(Utilities.fetchHistoryTradingData(token, "1m", 3 * 60 * 60));
        }

        List<Double> testTrades = new ArrayList<>();

        for (double[] data : historicData) {
            testTrades.add(data[1]); // Add open price.
            testTrades.add(data[4]); // Add close price.
        }

        return testTrades;
    }

    private void createGraphs(StackPane stackPane) {
        xAxis = new NumberAxis();
        yAxis = new NumberAxis();

        lineChart = new LineChart<>(xAxis, yAxis);
        ScatterChart<Number, Number> scatterChart = new ScatterChart<>(xAxis, yAxis);

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

        lineSeries = new XYChart.Series<>();
        lineSeries.setName("Price");
        lineChart.setCreateSymbols(false);
        lineChart.setAnimated(false);
        lineChart.getData().add(lineSeries);

        buySeries = new XYChart.Series<>();
        sellSeries = new XYChart.Series<>();

        scatterChart.getData().add(buySeries);
        scatterChart.getData().add(sellSeries);

        scatterChart.setOpacity(1);
        scatterChart.setHorizontalGridLinesVisible(false);
        scatterChart.setVerticalGridLinesVisible(false);
        scatterChart.setAlternativeColumnFillVisible(false);
        scatterChart.setAlternativeRowFillVisible(false);
        scatterChart.setTitle(" ");
        scatterChart.setAnimated(false);

        // Other configurations
        lineChart.lookup(".chart-plot-background").setStyle("-fx-background-color: transparent;");
        scatterChart.lookup(".chart-plot-background").setStyle("-fx-background-color: transparent;");

        stackPane.getChildren().addAll(lineChart, scatterChart);
    }

    private void updateChart(List<Double> graphData) {
        lineSeries.getData().clear();
        buySeries.getData().clear();
        sellSeries.getData().clear();

        for (int i = 0; i < graphData.size(); i++) {
            double price = graphData.get(i);
            lineSeries.getData().add(new XYChart.Data<>(i + 1, price));
        }

        xAxis.setAutoRanging(false);
        xAxis.setLowerBound(0);
        xAxis.setUpperBound(graphData.size());
        xAxis.setTickUnit(100);

        double minPrice = graphData.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
        double maxPrice = graphData.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        yAxis.setLowerBound(minPrice);
        yAxis.setUpperBound(maxPrice);
    }
}
