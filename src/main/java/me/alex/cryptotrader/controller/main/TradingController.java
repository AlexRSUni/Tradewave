package me.alex.cryptotrader.controller.main;

import javafx.fxml.FXML;
import me.alex.cryptotrader.manager.TradingManager;
import me.alex.cryptotrader.manager.ViewManager;

public class TradingController extends BaseController {

    private TradingManager manager;

    @FXML
    public void initialize() {
        // Init manager.
        this.manager = new TradingManager();
    }

    public TradingManager getManager() {
        return manager;
    }

    public static TradingController get() {
        return ViewManager.get().getController(ViewManager.get().getTradingView());
    }

}
