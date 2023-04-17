package me.alex.cryptotrader.util.trading;

import me.alex.cryptotrader.instruction.ActionType;

import java.util.LinkedList;
import java.util.Queue;

public class PeriodChange {

    private final LinkedList<ActionType> previousStates = new LinkedList<>();

    private final int max;

    private int up, down;

    public PeriodChange(int max) {
        this.max = max;
    }

    public void inc(boolean up) {
        if (this.up + down > max) {
            previousStates.addFirst(getState());
            this.up = down = 0;
        }

        if (up) {
            this.up++;
        } else {
            this.down++;
        }
    }

    public int getUp() {
        return up;
    }

    public int getDown() {
        return down;
    }

    public int getPercentage(boolean up) {
        return (int) Math.round((up ? this.up / (double) getSize() : this.down / (double) getSize()) * 100);
    }

    public ActionType getState() {
        int percentage = getPercentage(true);

        if (percentage > 90) {
            return ActionType.SKYROCKETING;
        } else if (percentage > 75) {
            return ActionType.FAST_CLIMB;
        } else if (percentage > 60) {
            return ActionType.CLIMBING;
        } else if (percentage > 40) {
            return ActionType.UNSTABLE;
        } else if (percentage > 25) {
            return ActionType.DECLINING;
        } else if (percentage > 10) {
            return ActionType.FAST_DECLINE;
        } else {
            return ActionType.IN_FREEFALL;
        }
    }

    public int getRecentStates(int check, ActionType... states) {
        int counter = 0;
        for (int i = 0; i < Math.min(previousStates.size(), check); i++) {
            ActionType checkState = previousStates.get(i);
            for (ActionType state : states) {
                if (checkState == state) {
                    counter++;
                }
            }
        }
        return counter;
    }

    public boolean fullEnough() {
        return (getSize() / (double) max) > 0.75;
    }

    public int getSize() {
        return up + down;
    }

    public Queue<ActionType> getPreviousStates() {
        return previousStates;
    }

}
