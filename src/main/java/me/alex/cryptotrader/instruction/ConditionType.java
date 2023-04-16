package me.alex.cryptotrader.instruction;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum ConditionType {

    PRICE(Arrays.asList(ActionType.PRICE_CONDITIONS), true, true),
    PRICE_SINCE_LAST_TRANSACTION(Arrays.asList(ActionType.PRICE_CONDITIONS), true, true),
    MARKET_CONDITION(Arrays.asList(ActionType.MARKET_CONDITIONS), true, true),
    NOT_MADE_TRANSACTION(Collections.emptyList(), true, false),
    HAS_MADE_TRANSACTION(Collections.emptyList(), true, false),
    OWNED_TOKEN_AMOUNT(Arrays.asList(ActionType.WALLET_CONDITIONS), false, false),
    OWNED_CURRENCY_AMOUNT(Arrays.asList(ActionType.WALLET_CONDITIONS), false, false),

    BUY(Collections.emptyList(), false, false),
    SELL(Collections.emptyList(), false, false),
    BUY_AS_MUCH_AS_POSSIBLE(Collections.emptyList(), false, false),
    SELL_ALL(Collections.emptyList(), false, false),
    ;

    public static final ConditionType[] CONDITIONS = new ConditionType[]{
            PRICE, PRICE_SINCE_LAST_TRANSACTION, MARKET_CONDITION, NOT_MADE_TRANSACTION, HAS_MADE_TRANSACTION, OWNED_TOKEN_AMOUNT, OWNED_CURRENCY_AMOUNT
    };

    public static final ConditionType[] ACTION_CONDITIONS = new ConditionType[]{
            BUY, SELL, BUY_AS_MUCH_AS_POSSIBLE, SELL_ALL
    };

    private final List<ActionType> supportedInstructions;
    private final boolean hasTimePeriod;
    private final boolean includeTokenName;

    ConditionType(List<ActionType> supportedInstructions, boolean hasTimePeriod, boolean includeTokenName) {
        this.supportedInstructions = supportedInstructions;
        this.hasTimePeriod = hasTimePeriod;
        this.includeTokenName = includeTokenName;
    }

    public List<ActionType> getSupportedInstructions() {
        return supportedInstructions;
    }

    public boolean hasTimePeriod() {
        return hasTimePeriod;
    }

    public boolean shouldIncludeTokenName() {
        return includeTokenName;
    }

}
