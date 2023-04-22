package me.alex.cryptotrader.util;

import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import me.alex.cryptotrader.CryptoApplication;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Utilities {

    public static final SimpleDateFormat SHORT_TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

    public static final DecimalFormat FORMAT_TWO_DECIMAL_PLACE = new DecimalFormat("#,###.##");
    public static final DecimalFormat FORMAT_SIX_DECIMAL_PLACE = new DecimalFormat("#,###.######");

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";


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

    public static String formatPrice(double price, String token) {
        if (price < 1) {
            return Utilities.FORMAT_SIX_DECIMAL_PLACE.format(price) + " " + token;
        } else {
            return Utilities.FORMAT_TWO_DECIMAL_PLACE.format(price) + " " + token;
        }
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
