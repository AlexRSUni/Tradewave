package me.alex.cryptotrader.controller.main;

import com.jfoenix.controls.JFXListView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import me.alex.cryptotrader.CryptoApplication;
import me.alex.cryptotrader.factory.FundCellFactory;
import me.alex.cryptotrader.factory.StrategyCellFactory;
import me.alex.cryptotrader.factory.TransactionCellFactory;
import me.alex.cryptotrader.manager.DashboardManager;
import me.alex.cryptotrader.manager.ViewManager;
import me.alex.cryptotrader.models.Fund;
import me.alex.cryptotrader.models.Strategy;
import me.alex.cryptotrader.models.Transaction;
import me.alex.cryptotrader.profile.UserProfile;
import me.alex.cryptotrader.util.FilteredComboBoxSelectionModel;

public class DashboardController extends BaseController {

    @FXML
    private Text txtInstructionStatus;
    @FXML
    private VBox graphBox;
    @FXML
    private JFXListView<Transaction> transactionList;
    @FXML
    private JFXListView<Strategy> strategyList;
    @FXML
    private JFXListView<Fund> fundList;
    @FXML
    private ComboBox<String> comboToken;

    // Variables

    private final ObservableList<Transaction> transactions = FXCollections.observableArrayList();

    private DashboardManager manager;

    @FXML
    public void initialize() {
        // Setup manager.
        manager = new DashboardManager(graphBox, transactions, comboToken);

        // Setup transaction list.
        transactionList.setItems(transactions);
        transactionList.setCellFactory(e -> new TransactionCellFactory(false));

        // Setup funds list.
        fundList.setItems(UserProfile.get().getFunds());
        fundList.setCellFactory(e -> new FundCellFactory());

        // Setup instruction list.
        strategyList.setItems(UserProfile.get().getStrategies());
        strategyList.setCellFactory(e -> new StrategyCellFactory(false));

        // Setup token combo box.
        FilteredList<String> filteredItems = new FilteredList<>(CryptoApplication.get().getTradingPairs(), s -> true);

        comboToken.setItems(filteredItems);
        comboToken.setSelectionModel(new FilteredComboBoxSelectionModel<>(filteredItems));
        comboToken.getEditor().setText(UserProfile.get().getDashboardToken());

        comboToken.getEditor().textProperty().addListener((observable, oldValue, newValue) -> {
            String userInput = comboToken.getEditor().getText();

            filteredItems.setPredicate(item -> {
                if (item.toLowerCase().startsWith(userInput.toLowerCase())) {
                    return true;
                } else {
                    return userInput.isEmpty(); // Display all items when the input is empty
                }
            });

            updateSelectedTokenPair(newValue);
        });

        updateStrategyStatus();
    }

    private void updateSelectedTokenPair(String tokenPair) {
        if (CryptoApplication.get().getTradingPairs().contains(tokenPair)) {
            manager.setupTokenVisualData(tokenPair);
        }
    }

    public void updateStrategyStatus() {
        txtInstructionStatus.setText(strategyList.getItems().isEmpty() ? "None Available" : "");
    }

    public static DashboardController get() {
        return ViewManager.get().getController(ViewManager.get().getDashboardView());
    }

}
