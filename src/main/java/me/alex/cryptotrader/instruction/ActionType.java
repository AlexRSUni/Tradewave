package me.alex.cryptotrader.instruction;

public enum ActionType {

    // Price Condition
    RISES_ABOVE(false, true, false),
    DROPS_BELOW(false, true, false),
    INCREASES_TO(false, true, false),
    INCREASES_BY(true, true, true),
    DECREASES_TO(false, true, false),
    DECREASES_BY(true, true, true),

    // Market Condition
    SKYROCKETING(false, false, true),
    FAST_CLIMB(false, false, true),
    CLIMBING(false, false, true),
    UNSTABLE(false, false, true),
    DECLINING(false, false, true),
    FAST_DECLINE(false, false, true),
    IN_FREEFALL(false, false, true),

    // Wallet Condition
    IS_EQUAL_TO(false, true, false),
    IS_NOT_EQUAL_TO(false, true, false),
    IS_LESS_THAN(false, true, false),
    IS_MORE_THAN(false, true, false),

    NONE_YET(false, false, false),
    BUY(false, false, false),
    SELL(false, false, false),
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

    public static final ActionType[] LAST_TRANSACTION = new ActionType[]{
            BUY, SELL, NONE_YET
    };

    private final boolean canHavePercentage;
    private final boolean canInputValue;
    private final boolean canHaveTimePeriod;

    ActionType(boolean canHavePercentage, boolean canInputValue, boolean canHaveTimePeriod) {
        this.canHavePercentage = canHavePercentage;
        this.canInputValue = canInputValue;
        this.canHaveTimePeriod = canHaveTimePeriod;
    }

    public boolean canHavePercentage() {
        return canHavePercentage;
    }

    public boolean canInputValue() {
        return canInputValue;
    }

    public boolean canHaveTimePeriod() {
        return canHaveTimePeriod;
    }

}
