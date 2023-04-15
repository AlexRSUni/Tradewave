package me.alex.cryptotrader.factory;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.ListCell;
import me.alex.cryptotrader.CryptoApplication;
import me.alex.cryptotrader.controller.element.StrategyCellController;
import me.alex.cryptotrader.models.Strategy;

public class StrategyCellFactory extends ListCell<Strategy> {

    private final boolean editable;

    public StrategyCellFactory(boolean editable) {
        this.editable = editable;
    }

    @Override
    protected void updateItem(Strategy strategy, boolean empty) {
        super.updateItem(strategy, empty);

        if (empty) {
            setText(null);
            setGraphic(null);
        } else {
            FXMLLoader loader = new FXMLLoader(CryptoApplication.class.getResource("strategy_cell" + (editable ? "_edit" : "") + ".fxml"));
            StrategyCellController controller = new StrategyCellController(strategy);
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
