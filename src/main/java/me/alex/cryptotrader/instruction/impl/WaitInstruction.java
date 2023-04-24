package me.alex.cryptotrader.instruction.impl;

import me.alex.cryptotrader.instruction.CryptoInstruction;
import me.alex.cryptotrader.models.Instruction;
import me.alex.cryptotrader.util.trading.TradingSession;

public class WaitInstruction extends CryptoInstruction {

    public WaitInstruction(Instruction instruction) {
        super(instruction);
    }

    @Override
    public boolean checkInstruction(long timestamp, double price, TradingSession session) {
        session.setHalt(timestamp, instruction.getTimePeriod());
        return true;
    }
}
