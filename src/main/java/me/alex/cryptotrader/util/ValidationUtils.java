package me.alex.cryptotrader.util;

import me.alex.cryptotrader.instruction.InstructionContext;
import me.alex.cryptotrader.models.Instruction;
import me.alex.cryptotrader.models.Strategy;
import me.alex.cryptotrader.util.binance.BinanceUtils;
import org.apache.commons.lang3.math.NumberUtils;

public class ValidationUtils {

    public static String validateInstructionCompleting(Strategy strategy, double price) {
        for (int i = 0; i < strategy.getInstructions().size(); i++) {
            Instruction instruction = strategy.getInstructions().get(i);
            Instruction previous = i == 0 ? null : strategy.getInstructions().get(i - 1);

            if (instruction.getType() == Instruction.InstructionType.IF
                    || instruction.getType() == Instruction.InstructionType.ELSE_IF
                    || instruction.getType() == Instruction.InstructionType.OR) {

                if (instruction.getContext() == null || (instruction.getState() == null
                        && !instruction.getContext().getSupportedInstructions().isEmpty())) {
                    return instruction.getType().name() + " on line " + (i + 1) + " is not filled out!";
                }

                if (!instruction.getController().comboTimePeriod.isDisabled() && instruction.getTimePeriod() == null) {
                    return instruction.getType().name() + " on line " + (i + 1) + " is not filled out!";
                }

                if (instruction.getType() == Instruction.InstructionType.OR && (previous == null
                        || (previous.getType() != Instruction.InstructionType.IF
                        && previous.getType() != Instruction.InstructionType.ELSE_IF))) {
                    return "OR on line " + (i + 1) + " most be following an IF/ELSE IF!";
                }

            } else if (instruction.getType() == Instruction.InstructionType.ACTION) {
                if (instruction.getContext() == null || (!instruction.getContext().getSupportedInstructions().isEmpty()
                        && (instruction.getValue() == null || instruction.getValue().isEmpty()))) {
                    return "ACTION on line " + (i + 1) + " is not filled out!";
                }

                if (instruction.getValue() != null && !NumberUtils.isCreatable(instruction.getValue())) {
                    return "ACTION on line " + (i + 1) + " has an invalid value!";
                }

                if (instruction.getContext() != InstructionContext.SELL_ALL) {
                    double min = BinanceUtils.fetchMinNotional(strategy.tokenProperty().get());
                    if (min > 0 && min >= NumberUtils.toDouble(instruction.getValue(), -1) * price) {
                        return "ACTION on line " + (i + 1) + " must have trade value of at least "
                                + min + " " + strategy.getTokenPairNames()[1] + "!";
                    }
                }

            } else if (instruction.getType() == Instruction.InstructionType.WAIT) {
                if (instruction.getTimePeriod() == null) {
                    return "WAIT on line " + (i + 1) + " is not filled out!";
                }

            }
        }

        return null;
    }

    public static String validateInstructionOrder(Strategy strategy) {

        for (int i = 0; i < strategy.getInstructions().size(); ) {
            Instruction instruction = strategy.getInstructions().get(i++);

            if (instruction.getType() == Instruction.InstructionType.IF) {
                int outcome = validateIfStatement(strategy, i);

                if (outcome == -1) {
                    return "Incomplete IF instruction on line " + i;
                }

                i += outcome;

            } else if (instruction.getType() == Instruction.InstructionType.END_IF) {
                return "Extra END IF on line " + i;
            }
        }

        return null;
    }

    private static int validateIfStatement(Strategy strategy, int index) {
        int counter = 0;

        for (int i = index; i < strategy.getInstructions().size(); ) {
            Instruction instruction = strategy.getInstructions().get(i++);

            if (instruction.getType() == Instruction.InstructionType.IF) {
                int outcome = validateIfStatement(strategy, i);

                if (outcome == -1) {
                    return -1;
                }

                i += outcome;
                counter += (outcome + 1);
            } else {
                counter++;
            }

            if (instruction.getType() == Instruction.InstructionType.END_IF) {
                return counter;
            }
        }

        return -1;
    }

}
