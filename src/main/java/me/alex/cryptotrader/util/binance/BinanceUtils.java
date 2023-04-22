package me.alex.cryptotrader.util.binance;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BinanceUtils {

    private static final Map<String, JSONArray> FILTER_DATA = new HashMap<>();
    private static final Map<String, String> SYMBOL_TO_NAME = new HashMap<>();

    public static String getSymbolName(String symbol) {
        return SYMBOL_TO_NAME.getOrDefault(symbol, symbol);
    }

    public static List<double[]> fetchHistoryTradingData(String symbol, String interval, long timePeriodSeconds) {
        long startTime = Instant.now().minusSeconds(timePeriodSeconds).toEpochMilli();
        long endTime = Instant.now().toEpochMilli();
        return fetchHistoryTradingData(symbol, interval, startTime, endTime);
    }

    public static List<double[]> fetchHistoryTradingData(String symbol, String interval, long startTime, long endTime) {
        List<double[]> historicData = new ArrayList<>();
        OkHttpClient client = new OkHttpClient();

        String url = "https://api.binance.com/api/v3/klines?symbol=" + symbol + "&interval=" + interval + "&startTime=" + startTime + "&endTime=" + endTime;

        Request request = new Request.Builder()
                .url(url)
                .build();

        try {
            Response response = client.newCall(request).execute();

            String jsonResponse = response.body().string();
            JSONArray jsonArray = new JSONArray(jsonResponse);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONArray klineData = jsonArray.getJSONArray(i);

                long openTime = klineData.getLong(0);
                double open = klineData.getDouble(1);
                double high = klineData.getDouble(2);
                double low = klineData.getDouble(3);
                double close = klineData.getDouble(4);

                historicData.add(new double[]{openTime, open, high, low, close});
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return historicData;
    }

    public static void fetchTokenToNameData() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://api.coingecko.com/api/v3/coins/list")
                .build();

        try {
            Response response = client.newCall(request).execute();

            String jsonResponse = response.body().string();
            JSONArray jsonArray = new JSONArray(jsonResponse);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String symbol = jsonObject.getString("symbol").toUpperCase();
                String name = jsonObject.getString("name");
                SYMBOL_TO_NAME.put(symbol, name);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<String> fetchBinanceTradingPairs() {
        List<String> tradingPairs = new ArrayList<>();
        String url = "https://api.binance.com/api/v3/exchangeInfo";

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();

        try {
            Response response = client.newCall(request).execute();

            String responseBody = response.body().string();
            JSONObject jsonObject = new JSONObject(responseBody);
            JSONArray symbolsArray = jsonObject.getJSONArray("symbols");

            for (int i = 0; i < symbolsArray.length(); i++) {
                JSONObject symbol = symbolsArray.getJSONObject(i);
                String tradingPair = symbol.getString("symbol");
                tradingPairs.add(tradingPair);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return tradingPairs;
    }

    private static void requestExchangeFilters(String tradingPair) {
        // Fetch exchange info
        OkHttpClient httpClient = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://api.binance.com/api/v3/exchangeInfo")
                .build();

        try {
            Response response = httpClient.newCall(request).execute();

            if (response.isSuccessful()) {
                String responseBody = response.body().string();
                JSONObject jsonObject = new JSONObject(responseBody);
                JSONArray symbols = jsonObject.getJSONArray("symbols");

                for (int i = 0; i < symbols.length(); i++) {
                    JSONObject symbol = symbols.getJSONObject(i);
                    if (symbol.getString("symbol").equalsIgnoreCase(tradingPair)) {
                        JSONArray filters = symbol.getJSONArray("filters");
                        FILTER_DATA.put(tradingPair, filters);
                        break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static double fetchMinNotional(String tradingPair) {
        JSONArray data = FILTER_DATA.get(tradingPair);

        if (data == null) {
            requestExchangeFilters(tradingPair);
            data = FILTER_DATA.get(tradingPair);
        }

        if (data == null) {
            return -1;
        }

        for (int i = 0; i < data.length(); i++) {
            JSONObject filter = data.getJSONObject(i);
            if (filter.getString("filterType").equals("NOTIONAL")) {
                return filter.getDouble("minNotional");
            }
        }

        return -1;
    }

}
