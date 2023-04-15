package me.alex.cryptotrader.controller.main;

import javafx.fxml.FXML;
import me.alex.cryptotrader.manager.ViewManager;

public class BaseController {

    @FXML
    public void closeProgram() {
        System.exit(0);
    }

    @FXML
    public void minimiseProgram() {
        ViewManager.get().minimiseWindow();
    }

}
