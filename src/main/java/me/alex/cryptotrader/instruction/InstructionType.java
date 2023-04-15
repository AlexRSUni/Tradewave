package me.alex.cryptotrader.instruction;

public enum InstructionType {

    RISES_ABOVE(false),
    DROPS_BELOW(false),

    INCREASES_TO(false),
    INCREASES_BY(true),

    DECREASES_TO(false),
    DECREASES_BY(true),
    ;

    private final boolean canHavePercentage;

    InstructionType(boolean canHavePercentage) {
        this.canHavePercentage = canHavePercentage;
    }

    public boolean canHavePercentage() {
        return canHavePercentage;
    }
}
