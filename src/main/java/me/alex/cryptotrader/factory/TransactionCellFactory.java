package me.alex.cryptotrader.factory;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.ListCell;
import me.alex.cryptotrader.CryptoApplication;
import me.alex.cryptotrader.controller.element.TransactionCellController;
import me.alex.cryptotrader.models.Transaction;

public class TransactionCellFactory extends ListCell<Transaction> {

    @Override
    protected void updateItem(Transaction transaction, boolean empty) {
        super.updateItem(transaction, empty);

        if (empty) {
            setText(null);
            setGraphic(null);
        } else {
            FXMLLoader loader = new FXMLLoader(CryptoApplication.class.getResource("transaction_cell.fxml"));
            TransactionCellController controller = new TransactionCellController(transaction);
            loader.setController(controller);

            setText(null);

            try {
                setGraphic(loader.load());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

    }
}
