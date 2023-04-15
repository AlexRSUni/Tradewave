package me.alex.cryptotrader.factory;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.ListCell;
import me.alex.cryptotrader.CryptoApplication;
import me.alex.cryptotrader.controller.element.FundCellController;
import me.alex.cryptotrader.models.Fund;

public class FundCellFactory extends ListCell<Fund> {

    @Override
    protected void updateItem(Fund fund, boolean empty) {
        super.updateItem(fund, empty);

        if (empty) {
            setText(null);
            setGraphic(null);
        } else {
            FXMLLoader loader = new FXMLLoader(CryptoApplication.class.getResource("fund_cell.fxml"));
            FundCellController controller = new FundCellController(fund);
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