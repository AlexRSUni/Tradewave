package me.alex.cryptotrader.util;

import me.alex.cryptotrader.models.Instruction;
import me.alex.cryptotrader.models.Strategy;

public class ValidationUtils {

    public static String validateInstructions(Strategy strategy) {

        for (int i = 0; i < strategy.getInstructions().size(); ) {
            Instruction instruction = strategy.getInstructions().get(i++);

            if (instruction.getType() == Instruction.InstructionType.IF) {
                int outcome = validateIfStatement(strategy, i, 1);

                if (outcome == -1) {
                    return "Bad IF condition on line " + i;
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
