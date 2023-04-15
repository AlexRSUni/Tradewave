package me.alex.cryptotrader.instruction;

public abstract class CryptoInstruction {

    private final InstructionType type;

    public CryptoInstruction(InstructionType type) {
        this.type = type;
    }

    abstract boolean shouldMakeTransaction(double price);

    public InstructionType getType() {
        return type;
    }
}
