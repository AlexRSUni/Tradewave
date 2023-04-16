package me.alex.cryptotrader.manager;

import me.alex.cryptotrader.controller.main.StrategyController;
import me.alex.cryptotrader.models.Strategy;
import me.alex.cryptotrader.util.DatabaseUtils;
import me.alex.cryptotrader.util.binance.AggTradesListener;

public class ConfigurationManager {

    private Strategy currentStrategy;
    private AggTradesListener tradesListener;

    public ConfigurationManager() {
        // Empty
    }

    public void setCurrentStrategy(Strategy strategy, boolean save) {
        // Save the strategy.
        if (save && this.currentStrategy != null) {
            DatabaseUtils.saveStrategy(this.currentStrategy);
        }

        this.currentStrategy = strategy;

        // If we have a trade listener running, cancel it.
        if (tradesListener != null) {
            tradesListener.close();
        }

        if (strategy == null) {
            ViewManager.get().getCurrentMenu().set("Configure");
        } else {
            ViewManager.get().getCurrentMenu().set("Edit Strategy");
            tradesListener = TradingManager.get().createListener(strategy.tokenProperty().get(), null);
        }
    }

    public boolean hasStrategySelected() {
        return currentStrategy != null;
    }

    public AggTradesListener getTradesListener() {
        return tradesListener;
    }

    public Strategy getCurrentStrategy() {
        return currentStrategy;
    }

    public static ConfigurationManager get() {
        return StrategyController.get().getManager();
    }

}
