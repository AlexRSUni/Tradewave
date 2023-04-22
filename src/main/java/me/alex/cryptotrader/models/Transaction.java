package me.alex.cryptotrader.models;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Transaction {

    private final StringProperty type;
    private final StringProperty price;
    private final StringProperty amount;
    private final StringProperty date;

    private final String boxColor;
    private final int tick;
    private final double tradeValue;

    public Transaction(String token, String price, String amount, String time, String boxColor, double tradeValue, int tick) {
        this.type = new SimpleStringProperty(this, "Type", token);
        this.price = new SimpleStringProperty(this, "Price", price);
        this.amount = new SimpleStringProperty(this, "Amount", amount);
        this.date = new SimpleStringProperty(this, "Date", time);
        this.boxColor = boxColor;
        this.tradeValue = tradeValue;
        this.tick = tick;
    }

    public StringProperty amountProperty() {
        return amount;
    }

    public StringProperty typeProperty() {
        return type;
    }

    public StringProperty priceProperty() {
        return price;
    }

    public StringProperty dateProperty() {
        return date;
    }

    public double getTradeValue() {
        return tradeValue;
    }

    public String getBoxColor() {
        return boxColor;
    }

    public int getTick() {
        return tick;
    }
}
