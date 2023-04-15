package me.alex.cryptotrader.models;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import me.alex.cryptotrader.util.Utilities;

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

    public String[] getTokenPairNames() {
        return Utilities.getTokenPairNames(token.get());
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
}
