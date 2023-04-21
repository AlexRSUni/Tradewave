package me.alex.cryptotrader.instruction;

import me.alex.cryptotrader.models.Instruction;
import me.alex.cryptotrader.util.trading.TradingData;

public abstract class CryptoInstruction {

    protected final Instruction instruction;

    private String failReason;

    public CryptoInstruction(Instruction instruction) {
        this.instruction = instruction;
    }

    public abstract boolean checkCondition(long timestamp, double price, TradingData data);

    public void setFailReason(String failReason) {
        this.failReason = failReason;
    }

    public String getFailReason() {
        return failReason;
    }

    public Instruction getInstruction() {
        return instruction;
    }
}
