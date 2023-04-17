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
    @FXML
    public Label amountLabel;

    private final Transaction transaction;

    public TransactionCellController(Transaction transaction) {
        this.transaction = transaction;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        dateLabel.textProperty().bind(transaction.dateProperty());
        typeLabel.textProperty().bind(transaction.typeProperty());
        priceLabel.textProperty().bind(transaction.priceProperty());

        if (amountLabel != null) {
            amountLabel.textProperty().bind(transaction.amountProperty());
        }

        priceLabel.setStyle("-fx-background-color: " + transaction.getBoxColor() + "; -fx-background-radius: 10px;");
    }

}
