package me.alex.cryptotrader.models;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Transaction {

    private final StringProperty type;
    private final StringProperty amount;
    private final StringProperty date;

    private final double rawPrice;
    private final double lastPrice;

    public Transaction(String type, String amount, String date, double rawPrice, double lastPrice) {
        this.type = new SimpleStringProperty(this, "Type", type);
        this.amount = new SimpleStringProperty(this, "Amount", amount);
        this.date = new SimpleStringProperty(this, "Date", date);
        this.rawPrice = rawPrice;
        this.lastPrice = lastPrice;
    }

    public StringProperty typeProperty() {
        return type;
    }

    public StringProperty amountProperty() {
        return amount;
    }

    public StringProperty dateProperty() {
        return date;
    }

    public double getRawPrice() {
        return rawPrice;
    }

    public double getLastPrice() {
        return lastPrice;
    }
}
