package me.alex.cryptotrader.manager;

import me.alex.cryptotrader.controller.main.StrategyController;
import me.alex.cryptotrader.models.Strategy;
import me.alex.cryptotrader.util.DatabaseUtils;

public class ConfigurationManager {

    private Strategy currentStrategy;

    public ConfigurationManager() {
        // Empty
    }

    public void setCurrentStrategy(Strategy strategy, boolean save) {
        // Save the strategy.
        if (save && this.currentStrategy != null) {
            DatabaseUtils.saveStrategy(this.currentStrategy);
        }

        this.currentStrategy = strategy;

        if (strategy == null) {
            ViewManager.get().getCurrentMenu().set("Configure");
        } else {
            ViewManager.get().getCurrentMenu().set("Edit Strategy");
        }
    }

    public boolean hasStrategySelected() {
        return currentStrategy != null;
    }

    public Strategy getCurrentStrategy() {
        return currentStrategy;
    }

    public static ConfigurationManager get() {
        return StrategyController.get().getManager();
    }

}
