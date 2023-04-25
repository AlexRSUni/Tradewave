package me.alex.cryptotrader.models;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import me.alex.cryptotrader.instruction.CryptoInstruction;
import me.alex.cryptotrader.util.Utilities;
import me.alex.cryptotrader.util.trading.TradingSession;

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

    public String onTradePrice(long timestamp, double price, TradingSession data) {
        String haltCondition = null;

        int depth = 0;
        int success = -1;
        int failed = -1;
        boolean waitForEndIf = false;

        if (data.isWaiting()) {
            return null;
        }

        for (Instruction instruction : instructions) {
            CryptoInstruction impl = instruction.getInstructionImpl();

            if (depth > failed) {

                // Check if the current instruction is an IF, if it is, ensure that the condition has been met,
                // otherwise flag that we have failed the if statement, which will skip all following instructions
                // until an ELSE / ELSE IF or END IF are reached.
                if (instruction.getType() == Instruction.InstructionType.IF
                        || instruction.getType() == Instruction.InstructionType.ELSE_IF
                        || instruction.getType() == Instruction.InstructionType.OR) {

                    if (instruction.getType() == Instruction.InstructionType.IF || success < depth) {
                        depth++;

                        // Check condition.
                        if (!impl.checkInstruction(timestamp, price, data)) {
                            failed = depth;
                        } else {
                            success = depth;
                        }
                    }

                } else {

                    if (!waitForEndIf) {

                        if (instruction.getType() == Instruction.InstructionType.ELSE) {
                            waitForEndIf = true;
                        } else {
                            // Check condition. If condition is failed, terminate the trading.
                            if (impl != null && !impl.checkInstruction(timestamp, price, data)) {
                                haltCondition = impl.getFailReason();
                                break;
                            }
                        }

                    }

                    if (instruction.getType() == Instruction.InstructionType.END_IF) {
                        depth--;
                        success--;
                        waitForEndIf = false;
                    }

                }

            } else {

                if (instruction.getType() == Instruction.InstructionType.IF) {

                    depth++;
                    failed = depth;
                    waitForEndIf = true;

                } else {
                    if (!waitForEndIf) {

                        if (instruction.getType() == Instruction.InstructionType.OR
                                || instruction.getType() == Instruction.InstructionType.ELSE_IF
                                || instruction.getType() == Instruction.InstructionType.ELSE) {

                            if (impl != null) {
                                // Check condition.
                                if (impl.checkInstruction(timestamp, price, data)) {
                                    failed--;
                                    success = depth;
                                }
                            } else {
                                failed--;
                                success = depth;
                            }

                        } else if (instruction.getType() == Instruction.InstructionType.END_IF) {
                            depth--;
                            failed--;
                        }

                    } else {

                        if (instruction.getType() == Instruction.InstructionType.END_IF) {
                            depth--;
                            failed--;
                            waitForEndIf = false;
                        }

                    }
                }

            }

            // Validate variables.
            if (depth < 0) depth = 0;
            if (success <= 0) success = -1;
            if (failed <= 0) failed = -1;

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
