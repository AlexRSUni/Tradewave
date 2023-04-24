package me.alex.cryptotrader.instruction;

import me.alex.cryptotrader.models.Instruction;
import me.alex.cryptotrader.util.trading.TradingSession;

public abstract class CryptoInstruction {

    protected final Instruction instruction;

    private String failReason;

    public CryptoInstruction(Instruction instruction) {
        this.instruction = instruction;
    }

    /**
     * @param timestamp of the trade.
     * @param price at this trade.
     * @param session of the current trading session.
     * @return if the instruction was completed successfully.
     */
    public abstract boolean checkInstruction(long timestamp, double price, TradingSession session);

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
