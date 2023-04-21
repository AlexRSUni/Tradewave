package me.alex.cryptotrader.models;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import me.alex.cryptotrader.instruction.CryptoInstruction;
import me.alex.cryptotrader.util.Utilities;
import me.alex.cryptotrader.util.trading.TradingData;

public class Strategy {

    private final ObservableList<Instruction> instructions;
    private final int id;

    private final StringProperty count;
    private final StringProperty name;
    private final StringProperty token;

    public Strategy(int id, String name, String token, ObservableList<Instruction> instructions) {
        this.id = id;
        this.count = new SimpleStringProperty(String.valueOf(instructions.size()));
        this.name = new SimpleStringProperty(name);
        this.token = new SimpleStringProperty(token);
        this.instructions = instructions;
    }

    public String onTradePrice(long timestamp, double price, TradingData data) {
        String haltCondition = null;
        boolean failedIf = false;

        if (data.isHalted()) {
            return null;
        }

        for (Instruction instruction : instructions) {
            CryptoInstruction impl = instruction.getInstructionImpl();

            if (!failedIf) {

                // Check if the current instruction is an IF, if it is, ensure that the condition has been met,
                // otherwise flag that we have failed the if statement, which will skip all following instructions
                // until an ELSE / ELSE IF or END IF are reached.
                if (instruction.getType() == Instruction.InstructionType.IF || instruction.getType() == Instruction.InstructionType.ELSE_IF) {

                    // Check condition.
                    if (!impl.checkCondition(timestamp, price, data)) {
                        failedIf = true;
                    }

                } else if (impl != null) {

                    // Check condition. If condition is failed, terminate the trading.
                    if (!impl.checkCondition(timestamp, price, data)) {
                        haltCondition = impl.getFailReason();
                        break;
                    }

                }

            } else {

                // If condition is ELSE or END IF, then we can simply set the Failed IF flag back to false and move
                // onto the next instruction.
                if (instruction.getType() == Instruction.InstructionType.END_IF || instruction.getType() == Instruction.InstructionType.ELSE) {
                    failedIf = false;

                } else if (instruction.getType() == Instruction.InstructionType.ELSE_IF) {

                    // If the condition is ELSE IF, check if the condition is met, otherwise preserve the Failed IF flag.

                    if (impl.checkCondition(timestamp, price, data)) {
                        failedIf = false;
                    }

                }

            }

            // If our trading data is set to should stop (e.g. a STOP condition has run) then terminate the trading.
            if (data.shouldStop()) {
                haltCondition = "Stop Condition Reached";
                break;
            }
        }

        return haltCondition;
    }

    public String[] getTokenPairNames() {
        return Utilities.splitTokenPairSymbols(token.get());
    }

    public int getId() {
        return id;
    }

    public void updateStrategy() {
        this.count.set(String.valueOf(instructions.size()));
    }

    public StringProperty countProperty() {
        return count;
    }

    public StringProperty nameProperty() {
        return name;
    }

    public StringProperty tokenProperty() {
        return token;
    }

    public ObservableList<Instruction> getInstructions() {
        return instructions;
    }

    @Override
    public String toString() {
        return name.get();
    }
}
