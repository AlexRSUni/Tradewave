package me.alex.cryptotrader.controller.main;

import com.jfoenix.controls.JFXButton;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import me.alex.cryptotrader.factory.FundCellFactory;
import me.alex.cryptotrader.factory.TransactionCellFactory;
import me.alex.cryptotrader.manager.TradingManager;
import me.alex.cryptotrader.manager.ViewManager;
import me.alex.cryptotrader.models.Fund;
import me.alex.cryptotrader.models.Instruction;
import me.alex.cryptotrader.models.Strategy;
import me.alex.cryptotrader.models.Transaction;
import me.alex.cryptotrader.profile.UserProfile;
import me.alex.cryptotrader.util.Utilities;
import me.alex.cryptotrader.util.trading.TradingSession;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TradingController extends BaseController {

    @FXML
    private AnchorPane startPanel;
    @FXML
    private StackPane stackPane;
    @FXML
    private JFXButton btnPause;
    @FXML
    private Label lblStatus;
    @FXML
    private Label txtValue;
    @FXML
    private Label lblTrades;
    @FXML
    private Label lblPrice;
    @FXML
    private Label lblProfit;
    @FXML
    private Label lblMarket;
    @FXML
    private ListView<Fund> listFunds;
    @FXML
    private ListView<Transaction> listTransactions;
    @FXML
    private ComboBox<Strategy> comboStrategy;
    @FXML
    private TextField txtStopLoss;

    // Variables

    private final ObservableList<Transaction> transactions = FXCollections.observableArrayList();

    private TradingManager manager;

    private int tradeCounter;
    private boolean waitingForConnection;

    @FXML
    public void initialize() {
        // Init manager.
        this.manager = new TradingManager(this, stackPane, transactions);

        // Setup funds list.
        listFunds.setItems(UserProfile.get().getFunds());
        listFunds.setCellFactory(e -> new FundCellFactory());

        // Setup transaction list.
        listTransactions.setItems(transactions);
        listTransactions.setCellFactory(e -> new TransactionCellFactory(false));

        // Setup combo box
        comboStrategy.setItems(UserProfile.get().getStrategies());
        comboStrategy.valueProperty().addListener((observable, oldValue, newValue) -> onStrategyChange(newValue));

        // Setup labels
        lblPrice.setText("");
        lblProfit.setText("");
        lblMarket.setText("");
        lblTrades.setText("");

        lblStatus.setText("NOT CONNECTED");
        lblStatus.setStyle("-fx-text-fill: red;");
    }

    @FXML
    public void startTrading() {
        if (manager.getStrategy() == null) {
            Utilities.sendAlert("Trading was cancelled.", "You have not selected a strategy!");
            return;
        }

        List<Instruction> generatedInstructions = Collections.emptyList();
        String stopLoss = txtStopLoss.textProperty().get();

        if (!stopLoss.isEmpty()) {
            double value = NumberUtils.toDouble(stopLoss, -1);

            if (value == -1) {
                Utilities.sendAlert("Invalid stop-loss value!", "Please provide a valid stop loss price!");
                return;
            } else {
                generatedInstructions = createStopLossInstructions(manager.getStrategy(), value);
            }
        }

        manager.startTrading(generatedInstructions);
        startPanel.setVisible(false);

        // Update status display.
        setStatusText("TRADING " + manager.getStrategy().getTokenPairNames()[0], "#3ab23c");
    }

    @FXML
    public void stopTrading() {
        manager.stopTrading(null);
        startPanel.setVisible(true);

        // Update status display.
        setStatusText("WAITING TO START", "red");
    }

    @FXML
    public void pauseTrading() {
        manager.togglePaused();

        // Update status display.
        if (manager.isPaused()) {
            btnPause.textProperty().set("CONTINUE");
            setStatusText("PAUSED " + "(" + manager.getStrategy().getTokenPairNames()[0] + ")", "orange");
        } else {
            btnPause.textProperty().set("PAUSE");
            setStatusText("TRADING " + manager.getStrategy().getTokenPairNames()[0], "#3ab23c");
        }
    }

    public void onUpdate(Strategy strategy, double value, TradingSession data) {
        String token = strategy.getTokenPairNames()[1];

        if (waitingForConnection) {
            setStatusText("WAITING TO START", "red");
            waitingForConnection = false;
        }

        txtValue.setText("Current " + strategy.getTokenPairNames()[0] + " Value:");
        lblPrice.setText(Utilities.formatPrice(value, token));
        lblTrades.setText(String.valueOf(++tradeCounter));

        if (data != null) {
            lblMarket.setText(data.getMarketCondition().name().replace("_", " "));

            double startingValue = data.getStartingCurrency() + (data.getStartingToken() * data.getInitialPrice());
            double endingValue = data.getCurrencyAmount() + (data.getTokenAmount() * data.getLastPrice());
            double difference = transactions.isEmpty() ? 0 : endingValue - startingValue;

            lblProfit.setText((difference >= 0 ? "+" : "") + Utilities.formatPrice(difference, token));
        }

        refreshFundList(strategy);
    }

    public void refreshFundList(Strategy strategy) {
        listFunds.setItems(UserProfile.get().getFunds());

        listFunds.setItems(UserProfile.get().getFunds().filtered(fund -> {
            String token = fund.getToken();
            return token.equalsIgnoreCase(strategy.getTokenPairNames()[0]) || token.equalsIgnoreCase(strategy.getTokenPairNames()[1]);
        }));
    }

    private void onStrategyChange(Strategy strategy) {
        waitingForConnection = true;
        setStatusText("CONNECTING...", "#e4aca3");

        transactions.clear();

        refreshFundList(strategy);

        manager.selectTradingStrategy(strategy);
    }

    private void setStatusText(String text, String color) {
        lblStatus.setText(text);
        lblStatus.setStyle("-fx-text-fill: " + color + ";");
    }

    private List<Instruction> createStopLossInstructions(Strategy strategy, double price) {
        List<Instruction> instructions = new ArrayList<>();

        instructions.add(new Instruction(-1, -1, Instruction.InstructionType.IF, "PRICE:DROPS_BELOW:null:" + price, strategy));
        instructions.add(new Instruction(-1, -1, Instruction.InstructionType.ACTION, "SELL_ALL:null:null:null", strategy));
        instructions.add(new Instruction(-1, -1, Instruction.InstructionType.STOP, "", strategy));
        instructions.add(new Instruction(-1, -1, Instruction.InstructionType.END_IF, "", strategy));

        return instructions;
    }

    public TradingManager getManager() {
        return manager;
    }

    public static TradingController get() {
        return ViewManager.get().getController(ViewManager.get().getTradingView());
    }

}
