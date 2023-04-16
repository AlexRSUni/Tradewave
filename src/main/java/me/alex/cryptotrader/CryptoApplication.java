package me.alex.cryptotrader;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import me.alex.cryptotrader.controller.main.AccountController;
import me.alex.cryptotrader.manager.ViewManager;
import me.alex.cryptotrader.profile.UserProfile;
import me.alex.cryptotrader.util.DatabaseUtils;
import me.alex.cryptotrader.util.Utilities;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CryptoApplication extends Application {

    private static CryptoApplication instance;

    // List of available trading pairs loaded from Binance.
    private final ObservableList<String> tradingPairs = FXCollections.observableArrayList();
    private final ObservableList<String> availableCurrencies = FXCollections.observableArrayList();

    // View Manager
    private ViewManager viewManager;

    // The UserProfile on the logged-in user.
    private UserProfile profile;

    @Override
    public void start(Stage stage) {
        // Set our class instance for easy accessing.
        instance = this;

        // Create our view factory.
        this.viewManager = new ViewManager(stage);

        // Set base application data, like having a hidden top bar, the title, it being non-resizable and the app icon.
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setTitle("TradeWave");
        stage.setResizable(false);
        stage.getIcons().add(new Image(Objects.requireNonNull(CryptoApplication.class.getResourceAsStream("crypto.png"))));

        // Initialize our local database, ensuring that it exists.
        DatabaseUtils.initializeDatabase();

        // Check if we have a "stay logged in" user. Login straight away if that is the case.
        String rememberedUser = DatabaseUtils.checkForPersistentUser();
        if (rememberedUser != null) {
            AccountController controller = viewManager.showScene("login_known");
            controller.txtLoggedIn.setText(rememberedUser);
        } else {
            viewManager.showScene("login");
        }

        loadTradingPairs();
        loadSymbolToName();
    }

    public void loadUserProfile(String username, String password, boolean stayLoggedIn) {
        this.profile = new UserProfile(username, password);
        this.profile.setStayLoggedIn(stayLoggedIn);
    }

    public void logoutCurrentUser(boolean save, String errorMessage) {
        if (save && profile != null) {
            profile.saveUserData();
        }

        this.profile = null;

        AccountController controller = viewManager.showScene("login");
        if (errorMessage != null) controller.lblMessage.setText(errorMessage);
    }

    // Use a task to load our trading pairs from the API otherwise it will freeze the program on startup.
    private void loadTradingPairs() {
        Task<List<String>> fetchTradingPairsTask = new Task<>() {
            @Override
            protected List<String> call() {
                return Utilities.fetchBinanceTradingPairs();
            }
        };

        // This will be executed when the Task is completed
        fetchTradingPairsTask.setOnSucceeded(workerStateEvent -> {
            this.tradingPairs.addAll(fetchTradingPairsTask.getValue());
        });

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(fetchTradingPairsTask);
        executorService.shutdown();
    }

    // Use a task to load our token to name data from the API otherwise it will freeze the program on startup.
    private void loadSymbolToName() {
        Utilities.runTask(Utilities::fetchTokenToNameData);
    }

    public UserProfile getProfile() {
        return profile;
    }

    public ViewManager getViewManager() {
        return viewManager;
    }

    public ObservableList<String> getTradingPairs() {
        return tradingPairs;
    }

    public ObservableList<String> getAvailableCurrencies() {
        return availableCurrencies;
    }

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Ensure we always save the profile data on application close.
            if (instance.profile != null) instance.profile.saveUserData();
        }));

        launch();
    }

    public static CryptoApplication get() {
        return instance;
    }
}