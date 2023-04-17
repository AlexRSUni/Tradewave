package me.alex.cryptotrader.models;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import me.alex.cryptotrader.controller.element.InstructionCellController;
import me.alex.cryptotrader.instruction.ActionType;
import me.alex.cryptotrader.instruction.ConditionType;
import me.alex.cryptotrader.instruction.CryptoInstruction;
import me.alex.cryptotrader.instruction.TimePeriod;
import me.alex.cryptotrader.instruction.impl.ActionInstruction;
import me.alex.cryptotrader.instruction.impl.IfInstruction;
import me.alex.cryptotrader.instruction.impl.StopInstruction;
import me.alex.cryptotrader.instruction.impl.WaitInstruction;

public class Instruction {

    // Core variables.
    private final Strategy strategy;
    private final InstructionType type;
    private final CryptoInstruction instructionImpl;
    private final int id;

    // Instruction variables.
    private ConditionType condition;
    private ActionType action;
    private TimePeriod timePeriod;
    private String value;
    private int priority;

    // Interface variables.
    private final ObjectProperty<ConditionType> conditionProperty;
    private final ObjectProperty<ActionType> actionProperty;
    private final ObjectProperty<TimePeriod> timePeriodProperty;
    private final StringProperty valueProperty;
    private InstructionCellController controller;

    public Instruction(int id, int priority, InstructionType type, String data, Strategy strategy) {
        this.id = id;
        this.strategy = strategy;
        this.priority = priority;
        this.type = type;
        parseRawData(data);

        // Setup interface properties.
        this.conditionProperty = new SimpleObjectProperty<>(this, "Condition", condition);
        this.actionProperty = new SimpleObjectProperty<>(this, "Action", action);
        this.timePeriodProperty = new SimpleObjectProperty<>(this, "Period", timePeriod);
        this.valueProperty = new SimpleStringProperty(this, "Value", value);

        // Create instruction implementation.
        InstructionType.ManagerConstructor constructor = type.getConstructor();
        this.instructionImpl = constructor == null ? null : constructor.construct(this);
    }

    private void parseRawData(String data) {
        String[] split = data.split(":");

        if (split.length != 4) {
            return;
        }

        if (!split[0].equalsIgnoreCase("null")) {
            condition = ConditionType.valueOf(split[0]);
        }

        if (!split[1].equalsIgnoreCase("null")) {
            action = ActionType.valueOf(split[1]);
        }

        if (!split[2].equalsIgnoreCase("null")) {
            timePeriod = TimePeriod.valueOf(split[2]);
        }

        if (!split[3].equalsIgnoreCase("null")) {
            value = split[3];
        }
    }

    public String getRawData() {
        String rawData = "";

        rawData += (condition == null ? "null" : condition.name()) + ":";
        rawData += (action == null ? "null" : action.name()) + ":";
        rawData += (timePeriod == null ? "null" : timePeriod.name()) + ":";
        rawData += String.valueOf(value);

        return rawData;
    }

    public ObjectProperty<ConditionType> conditionProperty() {
        return conditionProperty;
    }

    public ObjectProperty<ActionType> actionProperty() {
        return actionProperty;
    }

    public ObjectProperty<TimePeriod> timePeriodProperty() {
        return timePeriodProperty;
    }

    public StringProperty valueProperty() {
        return valueProperty;
    }

    public void setCondition(ConditionType condition) {
        this.condition = condition;
        this.conditionProperty.set(condition);
    }

    public ConditionType getCondition() {
        return condition;
    }

    public void setAction(ActionType action) {
        this.action = action;
        this.actionProperty.set(action);
    }

    public ActionType getAction() {
        return action;
    }

    public void setTimePeriod(TimePeriod timePeriod) {
        this.timePeriod = timePeriod;
        this.timePeriodProperty.set(timePeriod);
    }

    public TimePeriod getTimePeriod() {
        return timePeriod;
    }

    public void setValue(String value) {
        this.value = value;
        this.valueProperty.set(value);
    }

    public String getValue() {
        return value;
    }

    public void setPriority(int i) {
        this.priority = i;
    }

    public int getPriority() {
        return priority;
    }

    public void setController(InstructionCellController controller) {
        this.controller = controller;
    }

    public InstructionCellController getController() {
        return controller;
    }

    public InstructionType getType() {
        return type;
    }

    public Strategy getStrategy() {
        return strategy;
    }

    public CryptoInstruction getInstructionImpl() {
        return instructionImpl;
    }

    public int getId() {
        return id;
    }

    public enum InstructionType {

        IF(IfInstruction::new),
        ELSE_IF(IfInstruction::new),
        ACTION(ActionInstruction::new),
        WAIT(WaitInstruction::new),
        STOP(StopInstruction::new),
        ELSE(null),
        END_IF(null),
        VALUE(null),
        ;

        private final ManagerConstructor constructor;

        InstructionType(ManagerConstructor constructor) {
            this.constructor = constructor;
        }

        public String getFilename() {
            return "instruction_cell_" + name().toLowerCase() + ".fxml";
        }

        public ManagerConstructor getConstructor() {
            return constructor;
        }

        public interface ManagerConstructor {
            CryptoInstruction construct(Instruction instruction);
        }

    }

}
