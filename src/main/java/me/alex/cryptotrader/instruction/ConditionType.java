package me.alex.cryptotrader.instruction;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum ConditionType {

    PRICE(Arrays.asList(ActionType.PRICE_CONDITIONS), true),
    PRICE_SINCE_LAST_TRANSACTION(Arrays.asList(ActionType.PRICE_CONDITIONS), true),
    MARKET_CONDITION(Arrays.asList(ActionType.MARKET_CONDITIONS), true),
    NOT_MADE_TRANSACTION(Collections.emptyList(), true),
    HAS_MADE_TRANSACTION(Collections.emptyList(), true),
    OWNED_TOKEN_AMOUNT(Arrays.asList(ActionType.WALLET_CONDITIONS), false),
    OWNED_CURRENCY_AMOUNT(Arrays.asList(ActionType.WALLET_CONDITIONS), false),

    BUY(Collections.emptyList(), false),
    SELL(Collections.emptyList(), false),
    BUY_AS_MUCH_AS_POSSIBLE(Collections.emptyList(), false),
    SELL_ALL(Collections.emptyList(), false),
    ;

    public static final ConditionType[] CONDITIONS = new ConditionType[]{
            PRICE, PRICE_SINCE_LAST_TRANSACTION, MARKET_CONDITION, NOT_MADE_TRANSACTION, HAS_MADE_TRANSACTION, OWNED_TOKEN_AMOUNT, OWNED_CURRENCY_AMOUNT
    };

    public static final ConditionType[] ACTION_CONDITIONS = new ConditionType[]{
            BUY, SELL, BUY_AS_MUCH_AS_POSSIBLE, SELL_ALL
    };

    private final List<ActionType> supportedInstructions;
    private final boolean hasTimePeriod;

    ConditionType(List<ActionType> supportedInstructions, boolean hasTimePeriod) {
        this.supportedInstructions = supportedInstructions;
        this.hasTimePeriod = hasTimePeriod;
    }

    public List<ActionType> getSupportedInstructions() {
        return supportedInstructions;
    }

    public boolean hasTimePeriod() {
        return hasTimePeriod;
    }


}
