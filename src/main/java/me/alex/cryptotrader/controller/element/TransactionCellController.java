package me.alex.cryptotrader.controller.element;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import me.alex.cryptotrader.models.Transaction;

import java.net.URL;
import java.util.ResourceBundle;

public class TransactionCellController implements Initializable {

    @FXML
    public Label dateLabel;
    @FXML
    public Label typeLabel;
    @FXML
    public Label priceLabel;

    private final Transaction transaction;

    public TransactionCellController(Transaction transaction) {
        this.transaction = transaction;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        dateLabel.textProperty().bind(transaction.dateProperty());
        typeLabel.textProperty().bind(transaction.typeProperty());
        priceLabel.textProperty().bind(transaction.amountProperty());
        priceColor(transaction.getRawPrice(), transaction.getLastPrice());
    }

    private void priceColor(double rawPrice, double lastPrice) {
        if (lastPrice < rawPrice) {
            priceLabel.setStyle("-fx-background-color: green; -fx-background-radius: 10px;");
        } else if (rawPrice < lastPrice) {
            priceLabel.setStyle("-fx-background-color: red; -fx-background-radius: 10px;");
        } else {
            priceLabel.setStyle("-fx-background-color: gray; -fx-background-radius: 10px;");
        }
    }

}
