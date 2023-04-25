package me.alex.cryptotrader.util.trading;

import me.alex.cryptotrader.instruction.ContextState;

import java.util.LinkedList;
import java.util.Queue;

public class MarketState {

    private final LinkedList<ContextState> previousStates = new LinkedList<>();

    private final int max;

    private int up, down;

    public MarketState(int max) {
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

    public ContextState getState() {
        int percentage = getPercentage(true);

        if (percentage > 90) {
            return ContextState.SKYROCKETING;
        } else if (percentage > 75) {
            return ContextState.FAST_CLIMB;
        } else if (percentage > 60) {
            return ContextState.CLIMBING;
        } else if (percentage > 40) {
            return ContextState.UNSTABLE;
        } else if (percentage > 25) {
            return ContextState.DECLINING;
        } else if (percentage > 10) {
            return ContextState.FAST_DECLINE;
        } else {
            return ContextState.IN_FREEFALL;
        }
    }

    public int getRecentStates(int check, ContextState... states) {
        int counter = 0;
        for (int i = 0; i < Math.min(previousStates.size(), check); i++) {
            ContextState checkState = previousStates.get(i);
            for (ContextState state : states) {
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

    public Queue<ContextState> getPreviousStates() {
        return previousStates;
    }

}
