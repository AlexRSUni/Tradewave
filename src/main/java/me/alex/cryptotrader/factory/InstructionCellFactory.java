package me.alex.cryptotrader.factory;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.control.ListCell;
import me.alex.cryptotrader.CryptoApplication;
import me.alex.cryptotrader.controller.element.InstructionCellController;
import me.alex.cryptotrader.models.Instruction;

public class InstructionCellFactory extends ListCell<Instruction> {

    @Override
    protected void updateItem(Instruction instruction, boolean empty) {
        super.updateItem(instruction, empty);

        if (empty) {
            setText(null);
            setGraphic(null);
        } else {
            FXMLLoader loader = new FXMLLoader(CryptoApplication.class.getResource("instructions/" + instruction.getType().getFilename()));
            InstructionCellController controller = new InstructionCellController(instruction);
            loader.setController(controller);

            setText(null);

            if (instruction.getType() == Instruction.InstructionType.DIVIDER) {
                setPadding(new Insets(10, 5, 10, 5));
            } else {
                setPadding(new Insets(5, 5, 5, 5));
            }

            try {
                setGraphic(loader.load());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

}