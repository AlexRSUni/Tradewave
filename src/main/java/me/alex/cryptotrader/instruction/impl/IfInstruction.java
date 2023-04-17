package me.alex.cryptotrader.instruction.impl;

import me.alex.cryptotrader.instruction.ActionType;
import me.alex.cryptotrader.instruction.ConditionType;
import me.alex.cryptotrader.instruction.CryptoInstruction;
import me.alex.cryptotrader.instruction.TimePeriod;
import me.alex.cryptotrader.models.Instruction;
import me.alex.cryptotrader.util.trading.TradingData;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.HashMap;
import java.util.Map;

public class IfInstruction extends CryptoInstruction {

    public static Map<ActionType, Integer> COUNTER = new HashMap<>();

    public IfInstruction(Instruction instruction) {
        super(instruction);
    }

    @Override
    public boolean checkCondition(long timestamp, double price, TradingData data) {
        ConditionType condition = instruction.getCondition();
        ActionType action = instruction.getAction();

        Instruction value = getNext();
        TimePeriod period = value.getTimePeriod();

        switch (condition) {

            case PRICE -> {
                String target = value.getValue();
                return handlePriceComparison(action, period, price, target, data);
            }

            case PRICE_SINCE_LAST_TRANSACTION -> {
                String target = value.getValue();
                double priceAtLastTransaction = data.getPriceAtLastTransaction();
                return priceAtLastTransaction != -1 && handlePriceComparison(action, period, priceAtLastTransaction, target, data);
            }

            case MARKET_CONDITION -> {
                ActionType mostFrequent = data.getMostFrequencyMarketState(period);

                COUNTER.put(mostFrequent, COUNTER.getOrDefault(mostFrequent, 0) + 1);

                return mostFrequent == action;
            }

            case HAS_MADE_TRANSACTION, NOT_MADE_TRANSACTION -> {
                long timeSinceLastTransaction = timestamp - data.getLastTransaction();

                if (condition == ConditionType.HAS_MADE_TRANSACTION) {
                    return timeSinceLastTransaction < period.getMilliseconds();
                } else {
                    return timeSinceLastTransaction > period.getMilliseconds();
                }
            }

            case OWNED_TOKEN_AMOUNT -> {
                String target = value.getValue();
                double compareAmount = NumberUtils.toDouble(target, -1);
                return compareAmount != -1 && handleWalletComparison(action, compareAmount, data.getTokenAmount());
            }

            case OWNED_CURRENCY_AMOUNT -> {
                String target = value.getValue();
                double compareAmount = NumberUtils.toDouble(target, -1);
                return compareAmount != -1 && handleWalletComparison(action, compareAmount, data.getCurrencyAmount());
            }

        }

        return false;
    }

    private boolean handlePriceComparison(ActionType action, TimePeriod period, double price, String target, TradingData data) {

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
                double parsedPercentage = NumberUtils.toDouble(target.replace("%", ""), -1);
                double priceBefore = data.getPriceAtPeriodBefore(period);

                if (parsedPercentage != -1 && priceBefore != -1) {
                    return false;
                }

                if (action == ActionType.INCREASES_BY) {
                    return price >= (priceBefore * 1 + (parsedPercentage / 100D));
                } else {
                    return price <= (priceBefore * 1 - (parsedPercentage / 100D));
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
