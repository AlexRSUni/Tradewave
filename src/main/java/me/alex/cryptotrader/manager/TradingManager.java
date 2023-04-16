package me.alex.cryptotrader.manager;

import me.alex.cryptotrader.controller.main.TradingController;
import me.alex.cryptotrader.util.binance.AggTradesListener;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class TradingManager {

    private final List<AggTradesListener> aggTradeListener = new ArrayList<>();

    public TradingManager() {
        // Empty
    }

    public AggTradesListener createListener(String tokenPair, Consumer<Double> priceConsumer) {
        AggTradesListener listener = new AggTradesListener(tokenPair, priceConsumer);
        aggTradeListener.add(listener);
        return listener;
    }

    public void stopAll() {
        aggTradeListener.forEach(AggTradesListener::close);
        aggTradeListener.clear();
    }

    public static TradingManager get() {
        return TradingController.get().getManager();
    }

}
