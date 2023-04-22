package me.alex.cryptotrader.util;

import javafx.application.Platform;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.Pane;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.List;

/**
 * This class handles displaying market data and trades on a line chart.
 */

public class MarketPanel {

    // Graph data.

    private final List<Double> graphData = new ArrayList<>();
    private final List<ScatterData> scatterData = new ArrayList<>();
    private final NumberAxis xAxis, yAxis;

    private LineChart<Number, Number> lineChart;
    private ScatterChart<Number, Number> scatterChart;

    private XYChart.Series<Number, Number> lineSeries;
    private XYChart.Series<Number, Number> buySeries;
    private XYChart.Series<Number, Number> sellSeries;

    // Other variables.

    private final String axisName;
    private final boolean hasScatter;

    public MarketPanel(boolean hasScatter, String axisName, Pane pane) {
        this.hasScatter = hasScatter;
        this.axisName = axisName;
        this.xAxis = new NumberAxis();
        this.yAxis = new NumberAxis();

        // If applicable, create our scatter chart.
        if (hasScatter) {
            createScatterChart(pane);
        }

        // Create our line chart.
        createLineChart(pane);
    }

    // Refresh and populates the graph with all our data. Also modifies the scaling and bounds so that all the data
    // always fits in the graph.
    public void updateChart() {
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

        // Update our upper and lower bounds. If the scale is tiny, modify the margin we increase the values
        // by so they fit in the screen appropriately.
        if (minPrice < 1 && maxPrice < 1) {
            yAxis.setUpperBound(maxPrice * 1.0005);
            yAxis.setLowerBound(minPrice * 0.9995);
        } else {
            yAxis.setUpperBound(maxPrice * 1.01);
            yAxis.setLowerBound(minPrice * 0.99);
        }

        if (hasScatter) {
            for (ScatterData data : scatterData) {
                addTransactionGraphPoint(data.getTick(), data.getValue(), data.isGreen());
            }
        }
    }

    public void addGraphData(double value, boolean update) {
        graphData.add(value);
        if (update) updateChart();
    }

    public void addGraphData(List<Double> values, boolean update) {
        graphData.addAll(values);
        if (update) updateChart();
    }

    public void addScatterData(int tick, double value, boolean isGreen) {
        scatterData.add(new ScatterData(tick, value, isGreen));
        addTransactionGraphPoint(tick, value, isGreen);
    }

    // Reset chart data.
    public void resetChart() {
        lineSeries.getData().clear();

        if (hasScatter) {
            buySeries.getData().clear();
            sellSeries.getData().clear();
        }
    }

    // Reset all data.
    public void resetAll() {
        graphData.clear();
        scatterData.clear();
        resetChart();
    }

    // Set graphs title. If we have a scatter graph, set its title to empty so the 2 still line up.
    public void setTitle(String title) {
        lineChart.setTitle(title);
        if (hasScatter) scatterChart.setTitle(" ");
    }

    // Add transaction points to the scatter graph.
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

    // Create our main data line chart.
    private void createLineChart(Pane pane) {
        // Graph data.
        lineChart = new LineChart<>(xAxis, yAxis);

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
        lineSeries.setName(axisName);
        lineChart.setCreateSymbols(false);
        lineChart.setAnimated(false);
        lineChart.getData().add(lineSeries);

        lineChart.lookup(".chart-plot-background").setStyle("-fx-background-color: transparent;");
        pane.getChildren().add(lineChart);
    }

    // Create our overlay scatter graph which will display trade points.
    private void createScatterChart(Pane pane) {
        scatterChart = new ScatterChart<>(xAxis, yAxis);

        buySeries = new XYChart.Series<>();
        sellSeries = new XYChart.Series<>();

        scatterChart.getData().add(buySeries);
        scatterChart.getData().add(sellSeries);

        scatterChart.setOpacity(1);
        scatterChart.setHorizontalGridLinesVisible(false);
        scatterChart.setVerticalGridLinesVisible(false);
        scatterChart.setAlternativeColumnFillVisible(false);
        scatterChart.setAlternativeRowFillVisible(false);
        scatterChart.setAnimated(false);

        scatterChart.lookup(".chart-plot-background").setStyle("-fx-background-color: transparent;");
        pane.getChildren().add(scatterChart);
    }

    public List<Double> getGraphData() {
        return graphData;
    }

    private static class ScatterData {

        private final int tick;
        private final double value;
        private final boolean green;

        public ScatterData(int tick, double value, boolean green) {
            this.tick = tick;
            this.value = value;
            this.green = green;
        }

        public int getTick() {
            return tick;
        }

        public double getValue() {
            return value;
        }

        public boolean isGreen() {
            return green;
        }
    }

}
