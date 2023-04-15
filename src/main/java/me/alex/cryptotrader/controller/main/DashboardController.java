package me.alex.cryptotrader.controller.main;

import com.jfoenix.controls.JFXListView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import me.alex.cryptotrader.factory.FundCellFactory;
import me.alex.cryptotrader.factory.StrategyCellFactory;
import me.alex.cryptotrader.factory.TransactionCellFactory;
import me.alex.cryptotrader.manager.DashboardManager;
import me.alex.cryptotrader.manager.ViewManager;
import me.alex.cryptotrader.models.Fund;
import me.alex.cryptotrader.models.Strategy;
import me.alex.cryptotrader.models.Transaction;
import me.alex.cryptotrader.profile.UserProfile;

public class DashboardController extends BaseController {

    @FXML
    private Text txtInstructionStatus;
    @FXML
    private VBox graphBox;
    @FXML
    private JFXListView<Transaction> transactionList;
    @FXML
    private JFXListView<Strategy> stategyList;
    @FXML
    private JFXListView<Fund> fundList;

    // Variables

    private final ObservableList<Transaction> transactions = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Setup manager.
        new DashboardManager(graphBox, transactions);

        // Setup transaction list.
        transactionList.setItems(transactions);
        transactionList.setCellFactory(e -> new TransactionCellFactory());

        // Setup funds list.
        fundList.setItems(UserProfile.get().getFunds());
        fundList.setCellFactory(e -> new FundCellFactory());

        // Setup instruction list.
        stategyList.setItems(UserProfile.get().getStrategies());
        stategyList.setCellFactory(e -> new StrategyCellFactory(false));

        updateStrategyStatus();
    }

    public void updateStrategyStatus() {
        txtInstructionStatus.setText(stategyList.getItems().isEmpty() ? "None Available" : "");
    }

    public static DashboardController get() {
        return ViewManager.get().getController(ViewManager.get().getDashboardView());
    }

}
