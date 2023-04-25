package me.alex.cryptotrader.models;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import me.alex.cryptotrader.controller.element.InstructionCellController;
import me.alex.cryptotrader.instruction.ContextState;
import me.alex.cryptotrader.instruction.InstructionContext;
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
    private InstructionContext context;
    private ContextState state;
    private TimePeriod timePeriod;
    private String value;
    private int priority;

    // Interface variables.
    private final ObjectProperty<InstructionContext> contextProperty;
    private final ObjectProperty<ContextState> stateProperty;
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
        this.contextProperty = new SimpleObjectProperty<>(this, "Context", context);
        this.stateProperty = new SimpleObjectProperty<>(this, "State", state);
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
            context = InstructionContext.valueOf(split[0]);
        }

        if (!split[1].equalsIgnoreCase("null")) {
            state = ContextState.valueOf(split[1]);
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

        rawData += (context == null ? "null" : context.name()) + ":";
        rawData += (state == null ? "null" : state.name()) + ":";
        rawData += (timePeriod == null ? "null" : timePeriod.name()) + ":";
        rawData += String.valueOf(value);

        return rawData;
    }

    public ObjectProperty<InstructionContext> contextProperty() {
        return contextProperty;
    }

    public ObjectProperty<ContextState> stateProperty() {
        return stateProperty;
    }

    public ObjectProperty<TimePeriod> timePeriodProperty() {
        return timePeriodProperty;
    }

    public StringProperty valueProperty() {
        return valueProperty;
    }

    public void setContext(InstructionContext context) {
        this.context = context;
        this.contextProperty.set(context);
    }

    public InstructionContext getContext() {
        return context;
    }

    public void setState(ContextState state) {
        this.state = state;
        this.stateProperty.set(state);
    }

    public ContextState getState() {
        return state;
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
        OR(IfInstruction::new),
        ACTION(ActionInstruction::new),
        WAIT(WaitInstruction::new),
        STOP(StopInstruction::new),
        DIVIDER(null),
        ELSE(null),
        END_IF(null),
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
