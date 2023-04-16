package me.alex.cryptotrader.util.binance;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.domain.market.AggTrade;
import me.alex.cryptotrader.util.Utilities;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class AggTradesListener {

    private final Consumer<Double> tokenPriceConsumer;

    private Map<Long, AggTrade> aggTradesCache;
    private Closeable stream;

    private double currentPrice;

    public AggTradesListener(String symbol, Consumer<Double> tokenPriceConsumer) {
        this.tokenPriceConsumer = tokenPriceConsumer;

        Utilities.runTask(() -> {
            initializeAggTradesCache(symbol);
            startAggTradesEventStreaming(symbol);
        });
    }

    // Initializes the aggTrades cache by using the REST API.
    private void initializeAggTradesCache(String symbol) {
        BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance();
        BinanceApiRestClient client = factory.newRestClient();
        List<AggTrade> aggTrades = client.getAggTrades(symbol.toUpperCase());

        this.aggTradesCache = new HashMap<>();
        for (AggTrade aggTrade : aggTrades) {
            aggTradesCache.put(aggTrade.getAggregatedTradeId(), aggTrade);
        }
    }

    // Begins streaming of agg trades events.
    private void startAggTradesEventStreaming(String symbol) {
        BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance();
        BinanceApiWebSocketClient client = factory.newWebSocketClient();

        stream = client.onAggTradeEvent(symbol.toLowerCase(), response -> {
            Long aggregatedTradeId = response.getAggregatedTradeId();
            AggTrade updateAggTrade = aggTradesCache.get(aggregatedTradeId);
            if (updateAggTrade == null) {
                // new agg trade
                updateAggTrade = new AggTrade();
            }
            updateAggTrade.setAggregatedTradeId(aggregatedTradeId);
            updateAggTrade.setPrice(response.getPrice());
            updateAggTrade.setQuantity(response.getQuantity());
            updateAggTrade.setFirstBreakdownTradeId(response.getFirstBreakdownTradeId());
            updateAggTrade.setLastBreakdownTradeId(response.getLastBreakdownTradeId());
            updateAggTrade.setBuyerMaker(response.isBuyerMaker());

            // Store the updated agg trade in the cache
            aggTradesCache.put(aggregatedTradeId, updateAggTrade);
            currentPrice = Double.parseDouble(updateAggTrade.getPrice());

            if (tokenPriceConsumer != null) {
                tokenPriceConsumer.accept(currentPrice);
            }
        });
    }

    public void close() {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public Map<Long, AggTrade> getAggTradesCache() {
        return aggTradesCache;
    }
}