package me.alex.cryptotrader.controller.main;

import javafx.fxml.Initializable;
import javafx.scene.layout.BorderPane;
import me.alex.cryptotrader.manager.ViewManager;

import java.net.URL;
import java.util.ResourceBundle;

public class InterfaceController implements Initializable {

    public BorderPane clientParent;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        ViewManager.get().getCurrentMenu().addListener((observableValue, oldVal, newVal) -> {
            switch (newVal) {
                // Trading Page
                case "Trading" -> clientParent.setCenter(ViewManager.get().getTradingView());
                // Testing Page
                case "Testing" -> clientParent.setCenter(ViewManager.get().getTestingView());
                // Configure Page
                case "Configure" -> clientParent.setCenter(ViewManager.get().getStrategyView());
                // Edit Strategy Page
                case "Edit Strategy" -> clientParent.setCenter(ViewManager.get().getInstructionView());
                // Default to Dashboard
                default -> clientParent.setCenter(ViewManager.get().getDashboardView());
            }
        });
    }
}
