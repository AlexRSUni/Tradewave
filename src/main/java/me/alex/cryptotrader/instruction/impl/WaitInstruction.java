package me.alex.cryptotrader.instruction.impl;

import me.alex.cryptotrader.instruction.CryptoInstruction;
import me.alex.cryptotrader.models.Instruction;
import me.alex.cryptotrader.util.trading.TradingData;

public class WaitInstruction extends CryptoInstruction {

    public WaitInstruction(Instruction instruction) {
        super(instruction);
    }

    @Override
    public boolean checkCondition(long timestamp, double price, TradingData data) {
        data.setHalt(timestamp, instruction.getTimePeriod());
        return true;
    }
}
