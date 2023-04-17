package me.alex.cryptotrader.instruction;

import me.alex.cryptotrader.models.Instruction;
import me.alex.cryptotrader.util.trading.TradingData;

public abstract class CryptoInstruction {

    protected final Instruction instruction;

    public CryptoInstruction(Instruction instruction) {
        this.instruction = instruction;
    }

    public abstract boolean checkCondition(long timestamp, double price, TradingData data);

    protected Instruction getNext() {
        return getAdjacent(1);
    }

    protected Instruction getPrevious() {
        return getAdjacent(-1);
    }

    private Instruction getAdjacent(int i) {
        int index = instruction.getStrategy().getInstructions().indexOf(instruction);

        try {
            return instruction.getStrategy().getInstructions().get(index + i);
        } catch (Exception ex) {
            return null;
        }
    }

    public Instruction getInstruction() {
        return instruction;
    }
}
