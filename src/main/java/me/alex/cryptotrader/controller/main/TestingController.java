package me.alex.cryptotrader.controller.main;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import me.alex.cryptotrader.factory.TransactionCellFactory;
import me.alex.cryptotrader.instruction.TimePeriod;
import me.alex.cryptotrader.manager.TestingManager;
import me.alex.cryptotrader.manager.ViewManager;
import me.alex.cryptotrader.models.Strategy;
import me.alex.cryptotrader.models.Transaction;
import me.alex.cryptotrader.profile.UserProfile;
import me.alex.cryptotrader.util.Utilities;

public class TestingController extends BaseController {

    @FXML
    private Label lblWallet;
    @FXML
    private TextField txtStartingAmount;
    @FXML
    private Label lblTransactions;
    @FXML
    private Label lblToken;
    @FXML
    private Label lblTokenChange;
    @FXML
    private Label lblCurrency;
    @FXML
    private Label lblCurrencyChange;
    @FXML
    private Label lblTotal;
    @FXML
    private AnchorPane testingPanel;
    @FXML
    private StackPane stackPane;
    @FXML
    private ComboBox<Strategy> comboStrategy;
    @FXML
    private ComboBox<TimePeriod> comboTimePeriod;
    @FXML
    private ListView<Transaction> listTransactions;

    // Variables

    private final ObservableList<Transaction> transactions = FXCollections.observableArrayList();

    private TestingManager manager;

    @FXML
    public void initialize() {
        // Setup manager.
        manager = new TestingManager(this, stackPane, transactions);

        // Setup transaction list.
        listTransactions.setItems(transactions);
        listTransactions.setCellFactory(e -> new TransactionCellFactory(true));

        // Set combo boxes.
        comboStrategy.setItems(UserProfile.get().getStrategies());
        comboStrategy.valueProperty().addListener((observable, oldValue, newValue) -> manager.onStrategySelected(newValue));

        comboTimePeriod.setItems(FXCollections.observableArrayList(TimePeriod.TESTING));
        comboTimePeriod.valueProperty().addListener((observable, oldValue, newValue) -> manager.onTimePeriodSelected(newValue));
    }

    @FXML
    public void startTest() {
        manager.startStrategyTest(this.txtStartingAmount.textProperty().get());
    }

    @FXML
    public void resetTest() {
        transactions.clear();
        testingPanel.setVisible(true);
        manager.clearTradeSeries();
    }

    public void onTestFinished(Strategy strategy, double startToken, double endToken, double startCurrency, double endCurrency, double startValue, double endValue) {
        String[] tokenPairs = strategy.getTokenPairNames();

        // Hide the panel so we can see the results.
        testingPanel.setVisible(false);

        // Set label values.
        lblTransactions.setText(String.valueOf(transactions.size()));
        lblToken.setText(tokenPairs[0] + " Balance:");
        lblCurrency.setText(tokenPairs[1] + " Balance:");

        lblTokenChange.setText(Utilities.FORMAT_TWO_DECIMAL_PLACE.format(startToken) + " → " + Utilities.FORMAT_TWO_DECIMAL_PLACE.format(endToken));
        lblCurrencyChange.setText(Utilities.FORMAT_TWO_DECIMAL_PLACE.format(startCurrency) + " → " + Utilities.FORMAT_TWO_DECIMAL_PLACE.format(endCurrency));

        double startingTotal = (startToken * startValue) + startCurrency;
        double endingTotal = (endValue * endToken) + endCurrency;
        double percentageChange = ((endingTotal / startingTotal) * 100) - 100;

        lblTotal.setText(Utilities.FORMAT_TWO_DECIMAL_PLACE.format(endingTotal) + " " + tokenPairs[1] + " (" + Utilities.FORMAT_TWO_DECIMAL_PLACE.format(percentageChange) + "%)");
    }

    public void updateCurrencyDisplay(String token, boolean disabled) {
        this.txtStartingAmount.setDisable(disabled);
        this.lblWallet.setText("");

        if (token != null) {
            this.txtStartingAmount.setPromptText("Starting " + token);
            this.lblWallet.setText("(In Wallet: " + UserProfile.get().getOwnedToken(token) + " " + token + ")");
        }
    }

    public static TestingController get() {
        return ViewManager.get().getController(ViewManager.get().getTestingView());
    }

}
