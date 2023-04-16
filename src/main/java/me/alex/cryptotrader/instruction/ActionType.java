package me.alex.cryptotrader.instruction;

public enum ActionType {

    // Price Condition
    RISES_ABOVE(false, true),
    DROPS_BELOW(false, true),
    INCREASES_TO(false, true),
    INCREASES_BY(true, true),
    DECREASES_TO(false, true),
    DECREASES_BY(true, true),

    // Market Condition
    SKYROCKETING(false, false),
    FAST_CLIMB(false, false),
    CLIMBING(false, false),
    UNSTABLE(false, false),
    DECLINING(false, false),
    FAST_DECLINE(false, false),
    IN_FREEFALL(false, false),

    // Wallet Condition
    IS_EQUAL_TO(false, true),
    IS_NOT_EQUAL_TO(false, true),
    IS_LESS_THAN(false, true),
    IS_MORE_THAN(false, true),
    ;

    public static final ActionType[] PRICE_CONDITIONS = new ActionType[]{
            RISES_ABOVE, DROPS_BELOW, INCREASES_TO, INCREASES_BY, DECREASES_TO, DECREASES_BY
    };

    public static final ActionType[] MARKET_CONDITIONS = new ActionType[]{
            SKYROCKETING, FAST_CLIMB, CLIMBING, UNSTABLE, DECLINING, FAST_DECLINE, IN_FREEFALL
    };

    public static final ActionType[] WALLET_CONDITIONS = new ActionType[]{
            IS_EQUAL_TO, IS_NOT_EQUAL_TO, IS_LESS_THAN, IS_MORE_THAN
    };

    private final boolean canHavePercentage;
    private final boolean canInputValue;

    ActionType(boolean canHavePercentage, boolean canInputValue) {
        this.canHavePercentage = canHavePercentage;
        this.canInputValue = canInputValue;
    }

    public boolean canHavePercentage() {
        return canHavePercentage;
    }

    public boolean canInputValue() {
        return canInputValue;
    }

}
