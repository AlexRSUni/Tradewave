package me.alex.cryptotrader.models;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import me.alex.cryptotrader.controller.element.InstructionCellController;
import me.alex.cryptotrader.util.Utilities;

import java.util.Arrays;

public class Instruction {

    private final int id;
    private final Strategy strategy;
    private final int rawPriority;

    private final StringProperty priority;
    private final StringProperty type;
    private final StringProperty action;
    private final StringProperty amount;

    private InstructionCellController controller;
    private String[] data;
    private String rawAction;
    private double rawAmount;

    public Instruction(int id, int priority, String type, String action, double amount, Strategy strategy) {
        this.id = id;
        this.rawAction = action;
        this.rawPriority = priority;
        this.data = action.split(":");
        this.strategy = strategy;

        this.priority = new SimpleStringProperty(this, "Priority", String.valueOf(priority));
        this.type = new SimpleStringProperty(this, "Type", type);
        this.action = new SimpleStringProperty(this, "Action", getFormattedAction());
        this.amount = new SimpleStringProperty(this, "Amount", "");

        setRawAmount(amount);
    }

    private String getFormattedAction() {
        String[] names = strategy.getTokenPairNames();
        String formatted = "When " + names[0] + " price ";

        switch (data.length) {
            case 2 -> {
                formatted += data[0].replace("_", " ") + " ";
                formatted += Utilities.FORMAT_TWO_DECIMAL_PLACE.format(Double.parseDouble(data[1])) + " " + names[1];
            }
            case 3 -> {
                formatted += data[0].replace("_", " ") + " ";
                formatted += data[1] + " over ";
                formatted += data[2].replace("_", " ");
            }
            default -> throw new RuntimeException("Invalid data when formatting action! {data="
                    + Arrays.toString(data) + "}");
        }

        return formatted;
    }

    public void setAction(String action) {
        this.rawAction = action;
        this.data = action.split(":");
        this.action.set(getFormattedAction());
    }

    public void setRawAmount(double rawAmount) {
        this.rawAmount = rawAmount;
        this.amount.set(rawAmount + " " + strategy.getTokenPairNames()[0]);
    }

    public int getId() {
        return id;
    }

    public String[] getData() {
        return data;
    }

    public String getRawAction() {
        return rawAction;
    }

    public double getRawAmount() {
        return rawAmount;
    }

    public int getRawPriority() {
        return rawPriority;
    }

    public StringProperty priorityProperty() {
        return priority;
    }

    public StringProperty typeProperty() {
        return type;
    }

    public StringProperty actionProperty() {
        return action;
    }

    public StringProperty amountProperty() {
        return amount;
    }

    public void setController(InstructionCellController controller) {
        this.controller = controller;
    }

    public InstructionCellController getController() {
        return controller;
    }

}
