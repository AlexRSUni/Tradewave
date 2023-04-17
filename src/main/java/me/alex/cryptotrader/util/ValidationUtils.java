package me.alex.cryptotrader.util;

import me.alex.cryptotrader.models.Instruction;
import me.alex.cryptotrader.models.Strategy;

public class ValidationUtils {

    public static String validateInstructionCompleting(Strategy strategy) {
        for (int i = 0; i < strategy.getInstructions().size(); i++) {
            Instruction instruction = strategy.getInstructions().get(i);

            if (instruction.getType() == Instruction.InstructionType.IF || instruction.getType() == Instruction.InstructionType.ELSE_IF) {
                if (instruction.getCondition() == null || (instruction.getAction() == null && !instruction.getCondition().getSupportedInstructions().isEmpty())) {
                    return "IF/ELSE IF on line " + (i + 1) + " is not filled out!";
                }

                if (i + 1 < strategy.getInstructions().size()) {
                    Instruction next = strategy.getInstructions().get(i + 1);

                    if (next.getType() != Instruction.InstructionType.VALUE) {
                        return "IF/ELSE IF on line " + (i + 1) + " is missing VALUE on following line!";
                    }
                }

            } else if (instruction.getType() == Instruction.InstructionType.ACTION) {
                if (instruction.getCondition() == null || instruction.getValue().isEmpty()) {
                    return "ACTION on line " + (i + 1) + " is not filled out!";
                }

            } else if (instruction.getType() == Instruction.InstructionType.WAIT) {
                if (instruction.getTimePeriod() == null) {
                    return "WAIT on line " + (i + 1) + " is not filled out!";
                }

            } else if (instruction.getType() == Instruction.InstructionType.VALUE) {
                if (i == 0) {
                    return "VALUE on line " + (i + 1) + " must be below an IF/ELSE IF condition!";
                }

                if (!instruction.getController().comboTimePeriod.isDisabled() && instruction.getTimePeriod() == null) {
                    return "VALUE on line " + (i + 1) + " is not filled out!";
                }

                Instruction above = strategy.getInstructions().get(i - 1);
                if (above.getType() != Instruction.InstructionType.IF && above.getType() != Instruction.InstructionType.ELSE_IF) {
                    return "VALUE on line " + (i + 1) + " must be below an IF/ELSE IF condition!";
                }
            }
        }

        return null;
    }

    public static String validateInstructionOrder(Strategy strategy) {

        for (int i = 0; i < strategy.getInstructions().size(); ) {
            Instruction instruction = strategy.getInstructions().get(i++);

            if (instruction.getType() == Instruction.InstructionType.IF) {
                int outcome = validateIfStatement(strategy, i, 2);

                if (outcome == -1) {
                    return "Incomplete IF condition on line " + i;
                }

                i += outcome;

            } else if (instruction.getType() == Instruction.InstructionType.END_IF) {
                return "Extra END IF on line " + i;
            }
        }

        return null;
    }

    private static int validateIfStatement(Strategy strategy, int index, int depth) {
        int counter = 0;

        for (int i = index; i < strategy.getInstructions().size(); ) {
            Instruction instruction = strategy.getInstructions().get(i++);

            if (instruction.getType() == Instruction.InstructionType.IF) {
                int outcome = validateIfStatement(strategy, i, depth + 1);

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
