package me.alex.cryptotrader.controller.element;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import me.alex.cryptotrader.models.Instruction;

import java.net.URL;
import java.util.ResourceBundle;

public class InstructionCellController implements Initializable {

    @FXML
    public AnchorPane background;
    @FXML
    public Label priorityLabel;
    @FXML
    public Label typeLabel;
    @FXML
    public Label actionLabel;
    @FXML
    public Label priceLabel;

    private final Instruction instruction;

    public InstructionCellController(Instruction instruction) {
        this.instruction = instruction;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        priorityLabel.textProperty().bind(instruction.priorityProperty());
        typeLabel.textProperty().bind(instruction.typeProperty());
        actionLabel.textProperty().bind(instruction.actionProperty());
        priceLabel.textProperty().bind(instruction.amountProperty());
        updateState();

        // Assign the background pane, so we can recolor it later.
        instruction.setController(this);
    }

    public void updateState() {
        if (instruction.typeProperty().get().equalsIgnoreCase("BUY")) {
            instruction.typeProperty().set("BUY");
            typeLabel.setStyle("-fx-background-color: green; -fx-background-radius: 10px;");
        } else {
            instruction.typeProperty().set("SELL");
            typeLabel.setStyle("-fx-background-color: red; -fx-background-radius: 10px;");
        }
    }

    public AnchorPane getBackground() {
        return background;
    }
}
