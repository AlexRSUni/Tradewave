package me.alex.cryptotrader.controller.element;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import me.alex.cryptotrader.instruction.ActionType;
import me.alex.cryptotrader.instruction.ConditionType;
import me.alex.cryptotrader.instruction.TimePeriod;
import me.alex.cryptotrader.manager.StrategyManager;
import me.alex.cryptotrader.models.Instruction;
import me.alex.cryptotrader.util.DatabaseUtils;
import me.alex.cryptotrader.util.Utilities;
import org.apache.commons.lang3.math.NumberUtils;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class InstructionCellController implements Initializable {

    @FXML
    public AnchorPane background;
    @FXML
    public ComboBox<ConditionType> comboCondition;
    @FXML
    public ComboBox<ActionType> comboAction;
    @FXML
    public ComboBox<TimePeriod> comboTimePeriod;
    @FXML
    public TextField txtValue;
    @FXML
    public Label lblEstimation;

    private final Instruction instruction;

    public InstructionCellController(Instruction instruction) {
        this.instruction = instruction;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        if (instruction.getType() == Instruction.InstructionType.IF || instruction.getType() == Instruction.InstructionType.ELSE_IF || instruction.getType() == Instruction.InstructionType.OR) {

            // Setup condition combo box.
            comboCondition.valueProperty().bindBidirectional(instruction.conditionProperty());
            comboCondition.setItems(FXCollections.observableArrayList(ConditionType.CONDITIONS));
            comboCondition.valueProperty().addListener((observable, oldValue, newValue) -> onConditionSelected(newValue));

            // Setup action combo box.
            comboAction.valueProperty().bindBidirectional(instruction.actionProperty());
            comboAction.valueProperty().addListener((observable, oldValue, newValue) -> onActionSelected(newValue));

            // Setup time period combo box.
            comboTimePeriod.valueProperty().bindBidirectional(instruction.timePeriodProperty());
            comboTimePeriod.setItems(FXCollections.observableArrayList(TimePeriod.SHORT));
            comboTimePeriod.valueProperty().addListener((observable, oldValue, newValue) -> onTimePeriodSelected(newValue));

            // Setup value text box.
            txtValue.textProperty().bindBidirectional(instruction.valueProperty());
            txtValue.textProperty().addListener((observable, oldValue, newValue) -> onValueChange(newValue));

            onConditionSelected(instruction.getCondition());

        } else if (instruction.getType() == Instruction.InstructionType.ACTION) {

            // Setup condition combo box.
            comboCondition.valueProperty().bindBidirectional(instruction.conditionProperty());
            comboCondition.setItems(FXCollections.observableArrayList(ConditionType.ACTION_CONDITIONS));
            comboCondition.valueProperty().addListener((observable, oldValue, newValue) -> onConditionSelected(newValue));

            // Setup value text box.
            txtValue.textProperty().bindBidirectional(instruction.valueProperty());
            txtValue.textProperty().addListener((observable, oldValue, newValue) -> onValueChange(newValue));

        } else if (instruction.getType() == Instruction.InstructionType.WAIT) {

            // Setup time period combo box.
            comboTimePeriod.valueProperty().bindBidirectional(instruction.timePeriodProperty());
            comboTimePeriod.setItems(FXCollections.observableArrayList(TimePeriod.SHORT));
            comboTimePeriod.valueProperty().addListener((observable, oldValue, newValue) -> onTimePeriodSelected(newValue));

        }

        // Check what elements should and shouldn't be enabled on initialization.
        checkWhatShouldBeDisabled();

        // Assign the background pane, so we can recolor it later.
        instruction.setController(this);
    }

    @FXML
    public void deleteInstruction() {
        StrategyManager.get().getCurrentStrategy().getInstructions().remove(instruction);
        DatabaseUtils.deleteInstruction(instruction);
        instruction.getStrategy().updateStrategy();
    }

    public void updateEstimatedValue(double price) {
        if (lblEstimation != null) {
            double value = NumberUtils.toDouble(instruction.getValue(), -1);

            if (value != -1) {
                lblEstimation.setText("(~" + Utilities.formatPrice(value * price, instruction.getStrategy().getTokenPairNames()[1]) + ")");
            } else {
                lblEstimation.setText("");
            }
        }
    }

    private void onConditionSelected(ConditionType type) {
        instruction.setCondition(type);

        if (type != null && comboAction != null) {
            // Add supported instruction types to the action combo box.
            List<ActionType> supportedTypes = type.getSupportedInstructions();
            comboAction.setItems(FXCollections.observableArrayList(supportedTypes));
        }

        checkWhatShouldBeDisabled();
    }

    private void onValueChange(String newValue) {
        instruction.setValue(newValue);
        checkWhatShouldBeDisabled();
    }

    private void onActionSelected(ActionType type) {
        instruction.setAction(type);
        checkWhatShouldBeDisabled();
    }

    private void onTimePeriodSelected(TimePeriod type) {
        instruction.setTimePeriod(type);
        checkWhatShouldBeDisabled();
    }

    private void checkWhatShouldBeDisabled() {
        ConditionType condition = instruction.getCondition();
        ActionType action = instruction.getAction();


        // Reset states before disabling.
        if (comboAction != null) comboAction.setDisable(false);
        if (comboTimePeriod != null) comboTimePeriod.setDisable(false);
        if (txtValue != null) txtValue.setDisable(false);

        // If no condition is selected, disable all.
        if (comboCondition != null && comboCondition.getSelectionModel().getSelectedItem() == null) {
            if (comboAction != null) comboAction.setDisable(true);
            if (comboTimePeriod != null) comboTimePeriod.setDisable(true);
            txtValue.setDisable(true);
            return;
        }

        // If condition doesn't have any actions, disable the action combo box.
        if (comboAction != null && condition != null && condition.getSupportedInstructions().isEmpty()) {
            comboAction.setDisable(true);
            if (txtValue != null) txtValue.setDisable(true);
        }

        // If no action is selected, disable the text value and time period combo box.
        if (condition != null && !condition.getSupportedInstructions().isEmpty() && comboAction != null && comboAction.getSelectionModel().getSelectedItem() == null) {
            txtValue.setDisable(true);
            comboTimePeriod.setDisable(true);
        }

        // If the action or condition does not allow the text box or time period to be edited.
        if (action != null && !action.canInputValue()) {
            if (txtValue != null) txtValue.setDisable(true);
        }

        // If the action or condition does not support the time period combo box.
        if ((action != null && !action.canHaveTimePeriod()) || (condition != null && !condition.canHaveTimePeriod())) {
            if (comboTimePeriod != null) comboTimePeriod.setDisable(true);
        }

        // If time periods are support but the value doesn't have a percentage, then disable the time period.
        if (comboTimePeriod != null && !comboTimePeriod.isDisabled() && txtValue != null && action != null && action.canInputValue()
                && (txtValue.textProperty().get() == null || !txtValue.textProperty().get().contains("%"))) {
//            comboTimePeriod.setDisable(true);
        }

        if (txtValue != null && condition == ConditionType.SELL_ALL) {
            txtValue.setDisable(true);
        }

        if (lblEstimation != null && txtValue != null) {
            lblEstimation.setDisable(txtValue.isDisable());
        }

    }

    public AnchorPane getBackground() {
        return background;
    }
}
