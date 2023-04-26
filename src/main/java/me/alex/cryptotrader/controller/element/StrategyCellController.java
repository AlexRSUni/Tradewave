package me.alex.cryptotrader.controller.element;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import me.alex.cryptotrader.controller.main.DashboardController;
import me.alex.cryptotrader.controller.main.StrategyController;
import me.alex.cryptotrader.manager.TradingManager;
import me.alex.cryptotrader.manager.ViewManager;
import me.alex.cryptotrader.models.Strategy;
import me.alex.cryptotrader.profile.UserProfile;
import me.alex.cryptotrader.util.DatabaseUtils;
import me.alex.cryptotrader.util.Utilities;

import java.net.URL;
import java.util.ResourceBundle;

public class StrategyCellController implements Initializable {

    @FXML
    public Label tokenLabel;
    @FXML
    public Label nameLabel;
    @FXML
    public Label countLabel;

    private final Strategy strategy;

    public StrategyCellController(Strategy strategy) {
        this.strategy = strategy;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        tokenLabel.textProperty().bind(strategy.tokenProperty());
        nameLabel.textProperty().bind(strategy.nameProperty());
        countLabel.textProperty().bind(strategy.countProperty());
        updateColor();
    }

    @FXML
    public void editStrategy() {
        if (validateCurrentlyUsedStrategy()) {
            StrategyController controller = ViewManager.get().getController(ViewManager.get().getStrategyView());
            controller.getManager().setCurrentStrategy(strategy, false);
        }
    }

    @FXML
    public void deleteStrategy() {
        if (validateCurrentlyUsedStrategy()) {
            UserProfile.get().getStrategies().remove(strategy);
            DatabaseUtils.deleteStrategy(strategy);
            DashboardController.get().updateStrategyStatus();
        }
    }

    // Ensure that the strategy we are modifying or delete isn't currently being traded with.
    private boolean validateCurrentlyUsedStrategy() {
        Strategy tradingStrategy = TradingManager.get().getStrategy();

        if (tradingStrategy != null && tradingStrategy == strategy && TradingManager.get().isTrading()) {
            Utilities.sendAlert("Cannot modify strategy!", "This strategy is currently in use! Stop trading first!");
            return false;
        }

        return true;
    }

    private void updateColor() {
        if (!strategy.getInstructions().isEmpty()) {
            countLabel.setStyle("-fx-background-color: green; -fx-background-radius: 10px;");
        } else {
            countLabel.setStyle("-fx-background-color: gray; -fx-background-radius: 10px;");
        }
    }

}
