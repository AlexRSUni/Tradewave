module me.alex.cryptotrader {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires spring.security.crypto;
    requires com.jfoenix;
    requires org.json;
    requires Java.WebSocket;
    requires okhttp;
    requires binance.api.client;

    opens me.alex.cryptotrader to javafx.fxml;
    opens me.alex.cryptotrader.controller.main to javafx.fxml;
    opens me.alex.cryptotrader.controller.element to javafx.fxml;

    exports me.alex.cryptotrader;
    exports me.alex.cryptotrader.profile;
    exports me.alex.cryptotrader.models;
    exports me.alex.cryptotrader.factory;
    exports me.alex.cryptotrader.instruction;
    exports me.alex.cryptotrader.controller.main;
    exports me.alex.cryptotrader.controller.element;
    exports me.alex.cryptotrader.manager;
}