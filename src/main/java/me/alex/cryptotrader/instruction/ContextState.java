package me.alex.cryptotrader.instruction;

public enum ContextState {

    // Price Condition
    RISES_ABOVE(true, false),
    DROPS_BELOW(true, false),
    INCREASES_TO(true, false),
    INCREASES_BY(true, true),
    DECREASES_TO(true, false),
    DECREASES_BY(true, true),

    // Market Condition
    SKYROCKETING(false, true),
    FAST_CLIMB(false, true),
    CLIMBING(false, true),
    UNSTABLE(false, true),
    DECLINING(false, true),
    FAST_DECLINE(false, true),
    IN_FREEFALL(false, true),

    // Wallet Condition
    IS_EQUAL_TO(true, false),
    IS_NOT_EQUAL_TO(true, false),
    IS_LESS_THAN(true, false),
    IS_MORE_THAN(true, false),

    // Last Transaction Type
    NONE_YET(false, false),
    BUY(false, false),
    SELL(false, false),
    ;

    public static final ContextState[] PRICE_CONDITIONS = new ContextState[]{
            RISES_ABOVE, DROPS_BELOW, INCREASES_TO, INCREASES_BY, DECREASES_TO, DECREASES_BY
    };

    public static final ContextState[] MARKET_CONDITIONS = new ContextState[]{
            SKYROCKETING, FAST_CLIMB, CLIMBING, UNSTABLE, DECLINING, FAST_DECLINE, IN_FREEFALL
    };

    public static final ContextState[] WALLET_CONDITIONS = new ContextState[]{
            IS_EQUAL_TO, IS_NOT_EQUAL_TO, IS_LESS_THAN, IS_MORE_THAN
    };

    public static final ContextState[] LAST_TRANSACTION = new ContextState[]{
            BUY, SELL, NONE_YET
    };

    private final boolean canInputValue;
    private final boolean canHaveTimePeriod;

    ContextState(boolean canInputValue, boolean canHaveTimePeriod) {
        this.canInputValue = canInputValue;
        this.canHaveTimePeriod = canHaveTimePeriod;
    }

    public boolean canInputValue() {
        return canInputValue;
    }

    public boolean canHaveTimePeriod() {
        return canHaveTimePeriod;
    }

}
