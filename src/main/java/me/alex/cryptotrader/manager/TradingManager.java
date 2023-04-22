package me.alex.cryptotrader.manager;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.StackPane;
import javafx.util.StringConverter;
import me.alex.cryptotrader.controller.main.TradingController;
import me.alex.cryptotrader.models.Strategy;
import me.alex.cryptotrader.models.Transaction;
import me.alex.cryptotrader.profile.UserProfile;
import me.alex.cryptotrader.util.Utilities;
import me.alex.cryptotrader.util.binance.AggTradesListener;
import me.alex.cryptotrader.util.binance.BinanceUtils;
import me.alex.cryptotrader.util.trading.TradingData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

public class TradingManager {

    private final List<Double> graphData = new ArrayList<>();
    private final ObservableList<Transaction> transactions;
    private final TradingController controller;

    private LineChart<Number, Number> lineChart;
    private XYChart.Series<Number, Number> lineSeries;
    private XYChart.Series<Number, Number> buySeries;
    private XYChart.Series<Number, Number> sellSeries;
    private NumberAxis xAxis, yAxis;

    private AggTradesListener tradingListener;
    private TradingData tradingData;
    private Strategy strategy;
    private String tokenPair;

    private long lastGraphUpdate;
    private boolean isTrading, isPaused;

    public TradingManager(TradingController controller, StackPane pane, ObservableList<Transaction> transactions) {
        this.controller = controller;
        this.transactions = transactions;

        // Setup graphs.
        createGraphs(pane);
    }

    public void selectTradingStrategy(Strategy strategy) {
        this.strategy = strategy;

        if (strategy == null) {
            return;
        }

        this.tokenPair = strategy.tokenProperty().get();

        graphData.clear();
        resetChart();

        // Fetch and inject historic data.
        List<double[]> historicData = BinanceUtils.fetchHistoryTradingData(tokenPair, "1m", 60 * 60);
        Collections.reverse(historicData);
        historicData.stream().map(data -> data[4]).forEach(graphData::add);
        updateChart();

        // Update the charts title.
        lineChart.setTitle("Live " + strategy.tokenProperty().get() + " Price");

        // Register trade data.
        tradingData = new TradingData(false, strategy, UserProfile.get().getOwnedToken(strategy.getTokenPairNames()[0]), UserProfile.get().getOwnedToken(strategy.getTokenPairNames()[1]), this::processTrade);

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
            tradingData.addMarketTransaction(System.currentTimeMillis(), price);

            if (isTrading && !isPaused) {
                String output = strategy.onTradePrice(System.currentTimeMillis(), price, tradingData);
                if (output != null) stopTrading(output);
            }

            Platform.runLater(() -> {
                if (System.currentTimeMillis() - lastGraphUpdate > 5 * 1000) {
                    graphData.add(price);
                    updateChart();
                    lastGraphUpdate = System.currentTimeMillis();
                }

                controller.onUpdate(strategy, price, tradingData);
            });
        });
    }

    private void processTrade(double[] trade) {
        Platform.runLater(() -> {
            String[] tokenPair = strategy.getTokenPairNames();

            int tick = graphData.size();
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

            addTransactionGraphPoint(tick, price, amount > 0);
        });
    }

    private void createGraphs(StackPane pane) {
        xAxis = new NumberAxis();
        yAxis = new NumberAxis();

        // Graph data.
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
        sellSeries.setName("Price");

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

        pane.getChildren().addAll(lineChart, scatterChart);
    }

    private void updateChart() {
        resetChart();

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

        for (Transaction transaction : transactions) {
            addTransactionGraphPoint(transaction.getTick(), transaction.getTradeValue(), transaction.getBoxColor().equalsIgnoreCase("green"));
        }
    }

    private void addTransactionGraphPoint(int tick, double price, boolean buy) {
        if (buy) {
            XYChart.Data<Number, Number> buyPoint = new XYChart.Data<>(tick + 1, price);
            buySeries.getData().add(buyPoint);
            Platform.runLater(() -> buyPoint.getNode().getStyleClass().add("buy-point"));
        } else {
            XYChart.Data<Number, Number> sellPoint = new XYChart.Data<>(tick + 1, price);
            sellSeries.getData().add(sellPoint);
            Platform.runLater(() -> sellPoint.getNode().getStyleClass().add("sell-point"));
        }
    }

    private void resetChart() {
        lineSeries.getData().clear();
        buySeries.getData().clear();
        sellSeries.getData().clear();
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
