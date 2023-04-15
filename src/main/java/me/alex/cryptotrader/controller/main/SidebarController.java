package me.alex.cryptotrader.controller.main;

import com.jfoenix.controls.JFXButton;
import javafx.fxml.FXML;
import me.alex.cryptotrader.CryptoApplication;
import me.alex.cryptotrader.manager.ViewManager;
import me.alex.cryptotrader.profile.UserProfile;

public class SidebarController {

    @FXML
    private JFXButton dashboardButton;
    @FXML
    public JFXButton tradingButton;
    @FXML
    private JFXButton testingButton;
    @FXML
    private JFXButton configButton;

    @FXML
    public void displayDashboard() {
        displayPane("Dashboard", dashboardButton);
    }

    @FXML
    public void displayTrading() {
        displayPane("Trading", tradingButton);
    }

    @FXML
    public void displayTesting() {
        displayPane("Testing", testingButton);
    }

    @FXML
    public void displayConfig() {
        if (StrategyController.get().getManager().hasStrategySelected()) {
            displayPane("Edit Strategy", configButton);
        } else {
            displayPane("Configure", configButton);
        }
    }

    @FXML
    public void logout() {
        UserProfile.get().setStayLoggedIn(false);
        CryptoApplication.get().logoutCurrentUser(true, null);
    }

    private void displayPane(String option, JFXButton button) {
        ViewManager.get().getCurrentMenu().set(option);

        // Reset the button colors
        setButtonColor(dashboardButton, "#6166aa");
        setButtonColor(tradingButton, "#6166aa");
        setButtonColor(testingButton, "#6166aa");
        setButtonColor(configButton, "#6166aa");

        // Set the panes button to a different color.
        setButtonColor(button, "#544177");
    }

    private void setButtonColor(JFXButton button, String color) {
        button.setStyle("-fx-background-color: " + color + ";");
    }

}
