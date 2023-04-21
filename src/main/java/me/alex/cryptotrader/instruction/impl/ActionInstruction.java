package me.alex.cryptotrader.instruction.impl;

import me.alex.cryptotrader.instruction.ConditionType;
import me.alex.cryptotrader.instruction.CryptoInstruction;
import me.alex.cryptotrader.models.Instruction;
import me.alex.cryptotrader.util.trading.TradingData;
import org.apache.commons.lang3.math.NumberUtils;

public class ActionInstruction extends CryptoInstruction {

    public ActionInstruction(Instruction instruction) {
        super(instruction);
    }

    @Override
    public boolean checkCondition(long timestamp, double price, TradingData data) {
        ConditionType condition = instruction.getCondition();
        String value = instruction.getValue();

        switch (condition) {

            case BUY -> {
                double toBuy = NumberUtils.toDouble(value, -1);

                if (toBuy != -1) {
                    double cost = toBuy * price;

                    if (data.getCurrencyAmount() >= cost) {
                        data.incTokenAmount(toBuy);
                        data.incCurrencyAmount(-cost);

                        data.addUserTransaction(timestamp, toBuy, price);
                        return true;
                    }
                }

                setFailReason("Did not have enough funds for purchase!");
                return false;
            }

            case SELL -> {
                double toSell = NumberUtils.toDouble(value, -1);

                if (toSell != -1) {
                    double sellValue = toSell * price;

                    if (data.getTokenAmount() >= toSell) {
                        data.incTokenAmount(-toSell);
                        data.incCurrencyAmount(sellValue);

                        data.addUserTransaction(timestamp, -toSell, price);
                        return true;
                    }
                }

                setFailReason("Did not have enough tokens to sell!");
                return false;
            }

            case BUY_AS_MUCH_AS_POSSIBLE -> {
                double amountToBuy = data.getCurrencyAmount() * price;

                if (amountToBuy > 0) {
                    data.incTokenAmount(amountToBuy);
                    data.incCurrencyAmount(-data.getCurrencyAmount());

                    data.addUserTransaction(timestamp, amountToBuy, price);
                }

                return true;
            }

            case SELL_ALL -> {
                double amountOwned = data.getTokenAmount();

                if (amountOwned > 0) {
                    data.incTokenAmount(-amountOwned);
                    data.incCurrencyAmount(amountOwned * price);

                    data.addUserTransaction(timestamp, -amountOwned, price);
                }

                return true;
            }

        }

        setFailReason("Invalid Action in instruction!");
        return false;
    }

}
