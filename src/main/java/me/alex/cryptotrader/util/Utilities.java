package me.alex.cryptotrader.util;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.util.Pair;
import me.alex.cryptotrader.CryptoApplication;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Utilities {

    public static final SimpleDateFormat SHORT_TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
    public static final DecimalFormat FORMAT_TWO_DECIMAL_PLACE = new DecimalFormat("#,###.##");
    public static final DecimalFormat FORMAT_ONE_DECIMAL_PLACE = new DecimalFormat("#,###.#");

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";

    private static final Map<String, String> SYMBOL_TO_NAME = new HashMap<>();

    public static void runTask(Runnable runnable) {
        Task<Void> fetchTradingPairsTask = new Task<>() {
            @Override
            protected Void call() {
                runnable.run();
                return null;
            }
        };

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(fetchTradingPairsTask);
        executorService.shutdown();
    }

    public static void sendErrorAlert(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error!");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static String[] splitTokenPairSymbols(String tokenPair) {
        String[] data = new String[2];

        for (String token : CryptoApplication.get().getAvailableCurrencies()) {
            if (tokenPair.startsWith(token)) {
                data[0] = token;
                data[1] = tokenPair.replace(token, "");
                break;
            }
        }

        return data;
    }

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

    public static String encryptStringUsingPassword(String string, String password) {
        try {
            byte[] initVector = new byte[16];
            SecureRandom secureRandom = new SecureRandom();
            secureRandom.nextBytes(initVector);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(initVector);

            SecretKey secretKey = getSecretKey(password);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);

            byte[] encryptedBytes = cipher.doFinal(string.getBytes(StandardCharsets.UTF_8));
            byte[] combinedPayload = new byte[initVector.length + encryptedBytes.length];

            System.arraycopy(initVector, 0, combinedPayload, 0, initVector.length);
            System.arraycopy(encryptedBytes, 0, combinedPayload, initVector.length, encryptedBytes.length);

            return Base64.getEncoder().encodeToString(combinedPayload);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static String decryptStringUsingPassword(String encryptedString, String password) {
        try {
            byte[] combinedPayload = Base64.getDecoder().decode(encryptedString);

            byte[] initVector = new byte[16];
            System.arraycopy(combinedPayload, 0, initVector, 0, 16);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(initVector);

            byte[] encryptedBytes = new byte[combinedPayload.length - 16];
            System.arraycopy(combinedPayload, 16, encryptedBytes, 0, encryptedBytes.length);

            SecretKey secretKey = getSecretKey(password);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);

            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private static SecretKey getSecretKey(String password) {
        byte[] key = password.getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(key, ALGORITHM);
    }
}
