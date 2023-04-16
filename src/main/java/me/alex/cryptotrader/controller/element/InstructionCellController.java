package me.alex.cryptotrader.controller.element;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import me.alex.cryptotrader.instruction.ActionType;
import me.alex.cryptotrader.instruction.ConditionType;
import me.alex.cryptotrader.instruction.TimePeriod;
import me.alex.cryptotrader.manager.ConfigurationManager;
import me.alex.cryptotrader.models.Instruction;
import me.alex.cryptotrader.util.DatabaseUtils;

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

    private final Instruction instruction;

    public InstructionCellController(Instruction instruction) {
        this.instruction = instruction;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        if (instruction.getType() == Instruction.InstructionType.IF || instruction.getType() == Instruction.InstructionType.ELSE_IF) {
            comboCondition.valueProperty().bindBidirectional(instruction.conditionProperty());
            comboAction.valueProperty().bindBidirectional(instruction.actionProperty());

            comboCondition.setItems(FXCollections.observableArrayList(ConditionType.CONDITIONS));
            comboCondition.valueProperty().addListener((observable, oldValue, newValue) -> onConditionSelected(newValue));
            comboAction.valueProperty().addListener((observable, oldValue, newValue) -> onActionSelected(newValue));

            onConditionSelected(instruction.getCondition());

            // Disable action and value box.
            comboAction.setDisable(comboCondition.getSelectionModel().isEmpty() || comboCondition.getSelectionModel().getSelectedItem().getSupportedInstructions().isEmpty());

        } else if (instruction.getType() == Instruction.InstructionType.ACTION) {
            comboCondition.valueProperty().bindBidirectional(instruction.conditionProperty());

            comboCondition.setItems(FXCollections.observableArrayList(ConditionType.ACTION_CONDITIONS));
            comboCondition.valueProperty().addListener((observable, oldValue, newValue) -> onConditionSelected(newValue));

            onConditionSelected(instruction.getCondition());

        } else if (instruction.getType() == Instruction.InstructionType.WAIT) {
            comboTimePeriod.valueProperty().bindBidirectional(instruction.timePeriodProperty());
            comboTimePeriod.setItems(FXCollections.observableArrayList(TimePeriod.values()));
            comboTimePeriod.valueProperty().addListener((observable, oldValue, newValue) -> onTimePeriodSelected(newValue));

        } else if (instruction.getType() == Instruction.InstructionType.VALUE) {
            comboTimePeriod.valueProperty().bindBidirectional(instruction.timePeriodProperty());
            txtValue.textProperty().bindBidirectional(instruction.valueProperty());

            invalidateInstruction();

            comboTimePeriod.setItems(FXCollections.observableArrayList(TimePeriod.values()));
            comboTimePeriod.valueProperty().addListener((observable, oldValue, newValue) -> onTimePeriodSelected(newValue));
            txtValue.textProperty().addListener((observable, oldValue, newValue) -> onValueChange(newValue));
        }

        // Assign the background pane, so we can recolor it later.
        instruction.setController(this);
    }

    @FXML
    public void deleteInstruction() {
        ConfigurationManager.get().getCurrentStrategy().getInstructions().remove(instruction);
        DatabaseUtils.deleteInstruction(instruction);
    }

    public void invalidateInstruction() {
        if (instruction.getType() != Instruction.InstructionType.VALUE) {
            return;
        }

        int index = instruction.getStrategy().getInstructions().indexOf(instruction);
        boolean comboEnabled = false;
        boolean valueEnabled = false;

        // Reset state.
        comboTimePeriod.setDisable(false);
        txtValue.setDisable(false);

        if (index > 0) {
            Instruction previous = instruction.getStrategy().getInstructions().get(index - 1);
            ComboBox<TimePeriod> timePeriod = previous.getController().comboTimePeriod;

            // Disable the elements if the previous instruction isn't of IF type.
            if (previous.getType() == Instruction.InstructionType.IF || previous.getType() == Instruction.InstructionType.ELSE_IF) {
                ConditionType condition = previous.getCondition();
                ActionType action = previous.getAction();

                if (condition != null && (action != null || condition.getSupportedInstructions().isEmpty()) && (timePeriod == null || !timePeriod.isDisabled())) {

                    if (action != null && action.canInputValue()) {
                        valueEnabled = true;
                    }

                    comboEnabled = condition.hasTimePeriod();
                }
            }
        }

        // If we could not find the previous instruction, disable the elements.
        comboTimePeriod.setDisable(!comboEnabled);
        txtValue.setDisable(!valueEnabled);
    }

    private void onConditionSelected(ConditionType type) {
        instruction.setCondition(type);

        if (type != null) {

            if (comboAction != null) {
                // Add supported instruction types to the action combo box, and if it is none, then disable the element.
                List<ActionType> supportedTypes = type.getSupportedInstructions();
                comboAction.setItems(FXCollections.observableArrayList(supportedTypes));
                comboAction.setDisable(supportedTypes.isEmpty());
            }

            if (txtValue != null) {
                if (type == ConditionType.BUY_AS_MUCH_AS_POSSIBLE || type == ConditionType.SELL_ALL) {
                    txtValue.setDisable(true);
                    instruction.setValue("0");
                }
            }

        }

        notifyInstructionUpdated();
    }

    private void onValueChange(String newValue) {
        instruction.setValue(newValue);

        int index = instruction.getStrategy().getInstructions().indexOf(instruction);

        notifyInstructionUpdated();

        if (index > 0) {
            Instruction previous = instruction.getStrategy().getInstructions().get(index - 1);

            if (newValue.contains("%")) {
                ActionType type = previous.getAction();
                comboTimePeriod.setDisable(type == null || !type.canHavePercentage());

                if (comboTimePeriod.isDisabled()) {
                    instruction.setTimePeriod(null);
                }
            }
        }
    }

    private void onActionSelected(ActionType type) {
        instruction.setAction(type);
        notifyInstructionUpdated();
    }

    private void onTimePeriodSelected(TimePeriod type) {
        instruction.setTimePeriod(type);
        notifyInstructionUpdated();
    }

    private void notifyInstructionUpdated() {
        instruction.getStrategy().getInstructions().forEach(instruction -> {
            InstructionCellController controller = instruction.getController();
            if (controller != null) controller.invalidateInstruction();
        });
    }

    public AnchorPane getBackground() {
        return background;
    }
}
