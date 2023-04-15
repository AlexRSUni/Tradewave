package me.alex.cryptotrader.manager;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import me.alex.cryptotrader.models.Transaction;
import me.alex.cryptotrader.profile.UserProfile;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class DashboardManager {

    private static final String API_URL = "https://api.coinbase.com/v2/prices/<token>/historic?period=day";
    private static final String WS_API_URL = "wss://api-pub.bitfinex.com/ws/2";

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    private final ObservableList<Transaction> transactions;
    private final List<Double> cryptoPrices;

    private final String token;
    private final String apiUrl;

    private double lastPrice;

    public DashboardManager(VBox graphBox, ObservableList<Transaction> transactions) {
        this.cryptoPrices = new ArrayList<>();
        this.transactions = transactions;

        // Setup token and API url.
        this.token = UserProfile.get().getDashboardToken();
        this.apiUrl = API_URL.replace("<token>", token);

        // Create the graph which will display the price.
        createPriceGraph(graphBox);
    }

    private void createPriceGraph(VBox graphBox) {
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);

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

        chart.setTitle("24h " + token + " Prices");
        chart.setCreateSymbols(false);
        chart.setAnimated(false);

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Price");
        chart.getData().add(series);

        // Other configurations
        chart.lookup(".chart-plot-background").setStyle("-fx-background-color: transparent;");

        graphBox.getChildren().add(chart);

        startWebHook(series, yAxis);
    }

    private void startWebHook(XYChart.Series<Number, Number> series, NumberAxis yAxis) {
        try {
            fetchHistoricData(series, yAxis); // Fetch historic data

            WebSocketClient client = new WebSocketClient(new URI(WS_API_URL)) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    send("{\"event\":\"subscribe\",\"channel\":\"ticker\",\"symbol\":\"t" + token.replace("-", "") + "\"}");
                }

                @Override
                public void onMessage(String message) {
                    String[] split = message.split(",\\[");

                    if (split.length > 1) {
                        String[] data = split[1].split(",");
                        double price = Double.parseDouble(data[0]);
                        cryptoPrices.add(price);

                        Platform.runLater(() -> {
                            addMarketTransaction(price, timeFormat.format(new Date()));
                            updateChart(series, cryptoPrices, yAxis);
                        });
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                }

                @Override
                public void onError(Exception ex) {
                    ex.printStackTrace();
                }
            };

            client.connect();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void addMarketTransaction(double price, String time) {
        transactions.add(0, new Transaction(token, "£" + ((int) price), time, price, lastPrice));
        lastPrice = price;
    }

    private void fetchHistoricData(XYChart.Series<Number, Number> series, NumberAxis yAxis) throws Exception {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(apiUrl)
                .build();

        Response response = client.newCall(request).execute();
        String json = response.body().string();

        JSONArray data = new JSONObject(json).getJSONObject("data").getJSONArray("prices");

        for (int i = 0; i < data.length(); i++) {
            JSONObject object = data.getJSONObject(i);

            double price = object.getDouble("price");
            String timestamp = object.getString("time");

            cryptoPrices.add(price);
            addMarketTransaction(price, timestamp.split("T")[1].replace("Z", ""));
        }

        Collections.reverse(cryptoPrices);

        updateChart(series, cryptoPrices, yAxis);
    }

    private void updateChart(XYChart.Series<Number, Number> series, List<Double> prices, NumberAxis yAxis) {
        series.getData().clear();
        for (int i = 0; i < prices.size(); i++) {
            double price = prices.get(i);
            series.getData().add(new XYChart.Data<>(i + 1, price));
        }

        double minPrice = prices.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
        double maxPrice = prices.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        yAxis.setLowerBound(minPrice);
        yAxis.setUpperBound(maxPrice);
    }
}
