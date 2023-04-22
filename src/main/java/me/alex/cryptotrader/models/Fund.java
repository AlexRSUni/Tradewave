package me.alex.cryptotrader.models;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Fund {

    private final StringProperty fund;
    private final String token;

    public Fund(String token, String amount) {
        this.token = token;
        this.fund = new SimpleStringProperty(this, "Fund", (amount + " " + token));
    }

    public StringProperty fundProperty() {
        return fund;
    }

    public String getToken() {
        return token;
    }
}
