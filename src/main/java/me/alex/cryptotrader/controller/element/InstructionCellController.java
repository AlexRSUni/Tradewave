package me.alex.cryptotrader.controller.element;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import me.alex.cryptotrader.instruction.ContextState;
import me.alex.cryptotrader.instruction.InstructionContext;
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
    public ComboBox<InstructionContext> contextCombo;
    @FXML
    public ComboBox<ContextState> stateCombo;
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

            // Setup context combo box.
            contextCombo.valueProperty().bindBidirectional(instruction.contextProperty());
            contextCombo.setItems(FXCollections.observableArrayList(InstructionContext.CONDITIONS));
            contextCombo.valueProperty().addListener((observable, oldValue, newValue) -> onConditionSelected(newValue));

            // Setup state combo box.
            stateCombo.valueProperty().bindBidirectional(instruction.stateProperty());
            stateCombo.valueProperty().addListener((observable, oldValue, newValue) -> onActionSelected(newValue));

            // Setup time period combo box.
            comboTimePeriod.valueProperty().bindBidirectional(instruction.timePeriodProperty());
            comboTimePeriod.setItems(FXCollections.observableArrayList(TimePeriod.SHORT));
            comboTimePeriod.valueProperty().addListener((observable, oldValue, newValue) -> onTimePeriodSelected(newValue));

            // Setup value text box.
            txtValue.textProperty().bindBidirectional(instruction.valueProperty());
            txtValue.textProperty().addListener((observable, oldValue, newValue) -> onValueChange(newValue));

            onConditionSelected(instruction.getContext());

        } else if (instruction.getType() == Instruction.InstructionType.ACTION) {

            // Setup context combo box.
            contextCombo.valueProperty().bindBidirectional(instruction.contextProperty());
            contextCombo.setItems(FXCollections.observableArrayList(InstructionContext.ACTION_CONDITIONS));
            contextCombo.valueProperty().addListener((observable, oldValue, newValue) -> onConditionSelected(newValue));

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

    private void onConditionSelected(InstructionContext type) {
        instruction.setContext(type);

        if (type != null && stateCombo != null) {
            // Add supported instruction types to the state combo box.
            List<ContextState> supportedTypes = type.getSupportedInstructions();
            stateCombo.setItems(FXCollections.observableArrayList(supportedTypes));
        }

        checkWhatShouldBeDisabled();
    }

    private void onValueChange(String newValue) {
        instruction.setValue(newValue);
        checkWhatShouldBeDisabled();
    }

    private void onActionSelected(ContextState type) {
        instruction.setState(type);
        checkWhatShouldBeDisabled();
    }

    private void onTimePeriodSelected(TimePeriod type) {
        instruction.setTimePeriod(type);
        checkWhatShouldBeDisabled();
    }

    private void checkWhatShouldBeDisabled() {
        InstructionContext context = instruction.getContext();
        ContextState state = instruction.getState();


        // Reset states before disabling.
        if (stateCombo != null) stateCombo.setDisable(false);
        if (comboTimePeriod != null) comboTimePeriod.setDisable(false);
        if (txtValue != null) txtValue.setDisable(false);

        // If no context is selected, disable all.
        if (contextCombo != null && contextCombo.getSelectionModel().getSelectedItem() == null) {
            if (stateCombo != null) stateCombo.setDisable(true);
            if (comboTimePeriod != null) comboTimePeriod.setDisable(true);
            txtValue.setDisable(true);
            return;
        }

        // If context doesn't have any states, disable the state combo box.
        if (stateCombo != null && context != null && context.getSupportedInstructions().isEmpty()) {
            stateCombo.setDisable(true);
            if (txtValue != null) txtValue.setDisable(true);
        }

        // If no state is selected, disable the text value and time period combo box.
        if (context != null && !context.getSupportedInstructions().isEmpty() && stateCombo != null && stateCombo.getSelectionModel().getSelectedItem() == null) {
            txtValue.setDisable(true);
            comboTimePeriod.setDisable(true);
        }

        // If the state or context does not allow the text box or time period to be edited.
        if (state != null && !state.canInputValue()) {
            if (txtValue != null) txtValue.setDisable(true);
        }

        // If the state or context does not support the time period combo box.
        if ((state != null && !state.canHaveTimePeriod()) || (context != null && !context.canHaveTimePeriod())) {
            if (comboTimePeriod != null) comboTimePeriod.setDisable(true);
        }

        // If context is SELL_ALL, then we do not need to input a value.
        if (txtValue != null && context == InstructionContext.SELL_ALL) {
            txtValue.setDisable(true);
        }

        // If the value box is disabled, then disable the estimation text.
        if (lblEstimation != null && txtValue != null) {
            lblEstimation.setDisable(txtValue.isDisable());
        }

    }

    public AnchorPane getBackground() {
        return background;
    }
}
