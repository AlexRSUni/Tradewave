package me.alex.cryptotrader.instruction.impl;

import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.OrderStatus;
import com.binance.api.client.domain.TimeInForce;
import com.binance.api.client.domain.account.NewOrder;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.exception.BinanceApiException;
import me.alex.cryptotrader.instruction.ConditionType;
import me.alex.cryptotrader.instruction.CryptoInstruction;
import me.alex.cryptotrader.models.Instruction;
import me.alex.cryptotrader.models.Strategy;
import me.alex.cryptotrader.profile.UserProfile;
import me.alex.cryptotrader.util.trading.TradingSession;
import org.apache.commons.lang3.math.NumberUtils;

import java.text.DecimalFormat;

public class ActionInstruction extends CryptoInstruction {

    private static final DecimalFormat FORMAT_TRADE = new DecimalFormat("#.########");

    public ActionInstruction(Instruction instruction) {
        super(instruction);
    }

    @Override
    public boolean checkInstruction(long timestamp, double price, TradingSession session) {
        ConditionType condition = instruction.getCondition();
        String value = instruction.getValue();

        switch (condition) {

            case BUY -> {
                double toBuy = NumberUtils.toDouble(value, -1);

                if (toBuy != -1) {
                    if (session.getCurrencyAmount() >= toBuy * price) {
                        if (buy(UserProfile.get().getClient(), instruction.getStrategy(), toBuy, price, session)) {
                            session.addUserTransaction(timestamp, toBuy, price);
                            return true;
                        } else {
                            setFailReason("Buy transaction was not completed.");
                            return false;
                        }
                    }
                }

                setFailReason("Did not have enough funds for purchase!");
                return false;
            }

            case SELL -> {
                double toSell = NumberUtils.toDouble(value, -1);

                if (toSell != -1) {
                    if (session.getTokenAmount() >= toSell) {
                        if (sell(UserProfile.get().getClient(), instruction.getStrategy(), toSell, price, session)) {
                            session.addUserTransaction(timestamp, -toSell, price);
                            return true;
                        } else {
                            setFailReason("Sell transaction was not completed.");
                            return false;
                        }
                    }
                }

                setFailReason("Did not have enough tokens to sell!");
                return false;
            }

            case SELL_ALL -> {
                double amountOwned = session.getTokenAmount();

                if (amountOwned > 0) {
                    if (sell(UserProfile.get().getClient(), instruction.getStrategy(), amountOwned, price, session)) {
                        session.addUserTransaction(timestamp, -amountOwned, price);
                        return true;
                    } else {
                        setFailReason("Sell transaction was not completed.");
                        return false;
                    }
                }

                return true;
            }

        }

        setFailReason("Invalid Action in instruction!");
        return false;
    }

    private boolean buy(BinanceApiRestClient client, Strategy strategy, double buy, double price, TradingSession data) {
        if (data.isTest()) {
            data.incTokenAmount(buy);
            data.incCurrencyAmount(-(buy * price));
            return true;
        }

        try {
            NewOrderResponse response = client.newOrder(NewOrder.limitBuy(strategy.tokenProperty().get(), TimeInForce.FOK, FORMAT_TRADE.format(buy), FORMAT_TRADE.format(price * 1.01)));

            if (response.getStatus() == OrderStatus.FILLED) {
                return true;
            }
        } catch (BinanceApiException | NullPointerException e) {
            e.printStackTrace();
        }

        return false;
    }

    private boolean sell(BinanceApiRestClient client, Strategy strategy, double sell, double price, TradingSession data) {
        if (data.isTest()) {
            data.incTokenAmount(-sell);
            data.incCurrencyAmount(sell * price);
            return true;
        }

        try {
            NewOrderResponse response = client.newOrder(NewOrder.limitSell(strategy.tokenProperty().get(), TimeInForce.FOK, FORMAT_TRADE.format(sell), FORMAT_TRADE.format(price * 0.99)));

            if (response.getStatus() == OrderStatus.FILLED) {
                return true;
            }
        } catch (BinanceApiException | NullPointerException e) {
            e.printStackTrace();
        }

        return false;
    }

}
