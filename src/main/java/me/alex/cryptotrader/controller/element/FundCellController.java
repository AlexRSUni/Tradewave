package me.alex.cryptotrader.controller.element;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import me.alex.cryptotrader.models.Fund;

import java.net.URL;
import java.util.ResourceBundle;

public class FundCellController implements Initializable {

    @FXML
    public Label fundLabel;

    private final Fund fund;

    public FundCellController(Fund fund) {
        this.fund = fund;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        fundLabel.textProperty().bind(fund.fundProperty());
    }

}
