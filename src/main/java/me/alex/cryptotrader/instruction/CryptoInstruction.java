package me.alex.cryptotrader.instruction;

public abstract class CryptoInstruction {

    private final ActionType type;

    public CryptoInstruction(ActionType type) {
        this.type = type;
    }

    abstract boolean shouldMakeTransaction(double price);

    public ActionType getType() {
        return type;
    }
}
