package me.alex.cryptotrader.instruction.impl;

import me.alex.cryptotrader.instruction.ActionType;
import me.alex.cryptotrader.instruction.ConditionType;
import me.alex.cryptotrader.instruction.CryptoInstruction;
import me.alex.cryptotrader.instruction.TimePeriod;
import me.alex.cryptotrader.models.Instruction;
import me.alex.cryptotrader.util.trading.TradingSession;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.HashMap;
import java.util.Map;

public class IfInstruction extends CryptoInstruction {

    public static Map<ActionType, Integer> COUNTER = new HashMap<>();

    public IfInstruction(Instruction instruction) {
        super(instruction);
    }

    @Override
    public boolean checkInstruction(long timestamp, double price, TradingSession session) {
        ConditionType condition = instruction.getCondition();
        ActionType action = instruction.getAction();

        String value = instruction.getValue();
        TimePeriod period = instruction.getTimePeriod();

        switch (condition) {

            case PRICE -> {
                return handlePriceComparison(action, period, price, value, session, -1);
            }

            case PRICE_SINCE_LAST_TRANSACTION -> {
                double priceAtLastTransaction = session.getPriceAtLastTransaction();
                return priceAtLastTransaction != -1 && handlePriceComparison(action, period, price, value, session, priceAtLastTransaction);
            }

            case MARKET_CONDITION -> {
                ActionType mostFrequent = session.getMostFrequencyMarketState(period);

                COUNTER.put(mostFrequent, COUNTER.getOrDefault(mostFrequent, 0) + 1);

                return mostFrequent == action;
            }

            case HAS_MADE_TRANSACTION, NOT_MADE_TRANSACTION -> {
                long timeSinceLastTransaction = timestamp - session.getLastTransaction();

                if (condition == ConditionType.HAS_MADE_TRANSACTION) {
                    return timeSinceLastTransaction < period.getMilliseconds();
                } else {
                    return timeSinceLastTransaction > period.getMilliseconds();
                }
            }

            case OWNED_TOKEN_AMOUNT -> {
                double compareAmount = NumberUtils.toDouble(value, -1);
                return compareAmount != -1 && handleWalletComparison(action, compareAmount, session.getTokenAmount());
            }

            case OWNED_CURRENCY_AMOUNT -> {
                double compareAmount = NumberUtils.toDouble(value, -1);
                return compareAmount != -1 && handleWalletComparison(action, compareAmount, session.getCurrencyAmount());
            }

            case LAST_TRANSACTION_WAS -> {
                if (action == ActionType.NONE_YET) {
                    return session.getLastTransaction() == -1;
                }

                return action == ActionType.BUY && session.wasLastTransactionBuy() || action == ActionType.SELL && !session.wasLastTransactionBuy();
            }

        }

        return false;
    }

    private boolean handlePriceComparison(ActionType action, TimePeriod period, double price, String target, TradingSession data, double beforeOverride) {

        switch (action) {
            case INCREASES_TO, RISES_ABOVE -> {
                double parsedValue = NumberUtils.toDouble(target, -1);
                return parsedValue != -1 && price > parsedValue;
            }
            case DECREASES_TO, DROPS_BELOW -> {
                double parsedValue = NumberUtils.toDouble(target, -1);
                return parsedValue != -1 && price < parsedValue;
            }
            case INCREASES_BY, DECREASES_BY -> {
                double parsedValue = NumberUtils.toDouble(target.replace("%", ""), -1);
                double priceBefore = beforeOverride != -1 ? beforeOverride : data.getPriceAtPeriodBefore(period);

                if (parsedValue == -1 || priceBefore == -1) {
                    return false;
                }

                if (target.contains("%")) {
                    if (action == ActionType.INCREASES_BY) {
                        return price >= (priceBefore * 1 + (parsedValue / 100D));
                    } else {
                        return price <= (priceBefore * 1 - (parsedValue / 100D));
                    }
                } else {
                    if (action == ActionType.INCREASES_BY) {
                        return price >= (priceBefore + parsedValue);
                    } else {
                        return price <= (priceBefore - parsedValue);
                    }
                }
            }
        }

        return false;
    }

    private boolean handleWalletComparison(ActionType action, double value, double amountInWallet) {

        switch (action) {
            case IS_EQUAL_TO -> {
                return amountInWallet == value;
            }
            case IS_NOT_EQUAL_TO -> {
                return amountInWallet != value;
            }
            case IS_LESS_THAN -> {
                return amountInWallet < value;
            }
            case IS_MORE_THAN -> {
                return amountInWallet > value;
            }
        }

        return false;
    }

}
