package me.alex.cryptotrader.instruction;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum InstructionContext {

    PRICE(Arrays.asList(ContextState.PRICE_CONDITIONS), true),
    PRICE_SINCE_LAST_TRANSACTION(Arrays.asList(ContextState.PRICE_CONDITIONS), false),
    MARKET_CONDITION(Arrays.asList(ContextState.MARKET_CONDITIONS), true),
    NOT_MADE_TRANSACTION(Collections.emptyList(), true),
    HAS_MADE_TRANSACTION(Collections.emptyList(), true),
    OWNED_TOKEN_AMOUNT(Arrays.asList(ContextState.WALLET_CONDITIONS), false),
    OWNED_CURRENCY_AMOUNT(Arrays.asList(ContextState.WALLET_CONDITIONS), false),
    LAST_TRANSACTION_WAS(Arrays.asList(ContextState.LAST_TRANSACTION), false),

    BUY(Collections.emptyList(), false),
    SELL(Collections.emptyList(), false),
    SELL_ALL(Collections.emptyList(), false),
    ;

    public static final InstructionContext[] CONDITIONS = new InstructionContext[]{
            PRICE, PRICE_SINCE_LAST_TRANSACTION, MARKET_CONDITION, NOT_MADE_TRANSACTION, HAS_MADE_TRANSACTION,
            OWNED_TOKEN_AMOUNT, OWNED_CURRENCY_AMOUNT, LAST_TRANSACTION_WAS
    };

    public static final InstructionContext[] ACTION_CONDITIONS = new InstructionContext[]{
            BUY, SELL, SELL_ALL
    };

    private final List<ContextState> supportedInstructions;
    private final boolean hasTimePeriod;

    InstructionContext(List<ContextState> supportedInstructions, boolean hasTimePeriod) {
        this.supportedInstructions = supportedInstructions;
        this.hasTimePeriod = hasTimePeriod;
    }

    public List<ContextState> getSupportedInstructions() {
        return supportedInstructions;
    }

    public boolean canHaveTimePeriod() {
        return hasTimePeriod;
    }


}
