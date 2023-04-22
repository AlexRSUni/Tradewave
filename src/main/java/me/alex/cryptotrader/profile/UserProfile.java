package me.alex.cryptotrader.profile;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.account.Account;
import com.binance.api.client.domain.account.AssetBalance;
import com.binance.api.client.exception.BinanceApiException;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import me.alex.cryptotrader.CryptoApplication;
import me.alex.cryptotrader.manager.ViewManager;
import me.alex.cryptotrader.models.Fund;
import me.alex.cryptotrader.models.Strategy;
import me.alex.cryptotrader.util.DatabaseUtils;
import me.alex.cryptotrader.util.Utilities;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static me.alex.cryptotrader.util.DatabaseUtils.DB_URL;

public class UserProfile {

    public static final long BINANCE_API_WAIT = 30_000L;

    private final ObservableList<Strategy> strategies = FXCollections.observableArrayList();
    private final ObservableList<Fund> funds = FXCollections.observableArrayList();

    private final Map<String, Double> ownedTokens = new HashMap<>();
    private final String username;

    private BinanceApiRestClient client;
    private Account binanceAccount;
    private String dashboardToken;

    private boolean stayLoggedIn;

    public UserProfile(String username, String password) {
        this.username = username;

        loadUserData(password).whenComplete((completed, throwable) -> {
            if (throwable != null || !completed) {
//                if (throwable != null) throwable.printStackTrace();
                CryptoApplication.get().logoutCurrentUser(false, "Failed to login. Throttled by Binance.");
                return;
            }

            // If log was successful, display the interface.
            Platform.runLater(() -> ViewManager.get().showScene("interface"));

            loadFunds();
            loadStrategies();
        });
    }

    private CompletableFuture<Boolean> loadUserData(String password) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        try (
                Connection connection = DriverManager.getConnection(DB_URL);
                PreparedStatement query = connection.prepareStatement("SELECT * FROM users WHERE username = ?")
        ) {
            query.setString(1, username);
            ResultSet result = query.executeQuery();

            if (result.next()) {
                this.dashboardToken = result.getString("preferredToken");

                // Load Binance Account
                String apiKey = Utilities.decryptStringUsingPassword(result.getString("apiKey"), password);
                String secretKey = Utilities.decryptStringUsingPassword(result.getString("secretKey"), password);
                loadBinanceAccount(apiKey, secretKey, future);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            future.completeExceptionally(e);
        }

        return future;
    }

    public void saveUserData() {
        try (
                Connection connection = DriverManager.getConnection(DB_URL);
                PreparedStatement query = connection.prepareStatement("UPDATE users SET " +
                        "stayLoggedIn = ?, " +
                        "preferredToken = ? " +
                        "WHERE username = ?")
        ) {
            query.setBoolean(1, stayLoggedIn);
            query.setString(2, dashboardToken);
            query.setString(3, username);

            query.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateFund(String token, double amount) {
        ownedTokens.put(token, amount);

        Platform.runLater(() -> {
            // Update Fund model to reflect changes to the GUI.
            for (Fund fund : funds) {
                if (fund.getToken().equalsIgnoreCase(token)) {
                    fund.fundProperty().set(amount + " " + token);
                }
            }
        });
    }

    private void loadFunds() {
        for (AssetBalance balance : binanceAccount.getBalances()) {
            double amount = Double.parseDouble(balance.getFree());

            if (amount > 0) {
                ownedTokens.put(balance.getAsset(), amount);
                funds.add(new Fund(balance.getAsset(), balance.getFree()));
            }

            CryptoApplication.get().getAvailableCurrencies().add(balance.getAsset());
        }
    }

    private void loadBinanceAccount(String apiKey, String secretKey, CompletableFuture<Boolean> future) {
        BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance(apiKey, secretKey);
        BinanceApiRestClient client = factory.newRestClient();

        try {
            this.binanceAccount = client.getAccount(BINANCE_API_WAIT, System.currentTimeMillis());
            future.complete(this.binanceAccount != null);
        } catch (BinanceApiException ex) {
            future.completeExceptionally(ex);
            return;
        }

        this.client = client;
    }

    private void loadStrategies() {
        strategies.addAll(DatabaseUtils.loadStrategies(username));
    }

    public void setDashboardToken(String dashboardToken) {
        this.dashboardToken = dashboardToken;
    }

    public void setStayLoggedIn(boolean bool) {
        this.stayLoggedIn = bool;
    }

    public double getOwnedToken(String token) {
        return ownedTokens.getOrDefault(token, 0D);
    }

    public String getUsername() {
        return username;
    }

    public String getDashboardToken() {
        return dashboardToken;
    }

    public ObservableList<Fund> getFunds() {
        return funds;
    }

    public ObservableList<Strategy> getStrategies() {
        return strategies;
    }

    public BinanceApiRestClient getClient() {
        return client;
    }

    public static UserProfile get() {
        return CryptoApplication.get().getProfile();
    }

}
