package me.alex.cryptotrader.controller.main;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXListView;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.util.Callback;
import me.alex.cryptotrader.factory.InstructionCellFactory;
import me.alex.cryptotrader.instruction.ActionType;
import me.alex.cryptotrader.instruction.ConditionType;
import me.alex.cryptotrader.instruction.TimePeriod;
import me.alex.cryptotrader.manager.ConfigurationManager;
import me.alex.cryptotrader.manager.ViewManager;
import me.alex.cryptotrader.models.Instruction;
import me.alex.cryptotrader.models.Strategy;
import me.alex.cryptotrader.profile.UserProfile;
import me.alex.cryptotrader.util.DatabaseUtils;
import me.alex.cryptotrader.util.Utilities;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class InstructionController extends BaseController {

    private static final DataFormat SERIALIZED_MIME_TYPE = new DataFormat("application/x-java-serialized-object");

    @FXML
    private JFXButton btnCreate;
    @FXML
    private JFXButton btnUpdate;
    @FXML
    private JFXButton btnDelete;
    @FXML
    private TextField txtAmount;
    @FXML
    private TextField txtTransaction;
    @FXML
    private ComboBox<TimePeriod> comboPeriod;
    @FXML
    private ComboBox<String> comboBos;
    @FXML
    private ComboBox<ConditionType> comboCondition;
    @FXML
    private ComboBox<ActionType> comboAction;
    @FXML
    private JFXListView<Instruction> instructionList;
    @FXML
    private Label lblError;
    @FXML
    private Label lblTokenIn;
    @FXML
    private Label lblTokenOut;
    @FXML
    private Label lblEstimatedPurchase;
    @FXML
    private Label lblEstimatedTransaction;

    // Variables

    private ConfigurationManager manager;
    private Instruction selectedInstruction;
    private Timer valueTask;

    private boolean newInstruction;

    @FXML
    public void initialize() {
        // Init manager.
        StrategyController controller = ViewManager.get().getController(ViewManager.get().getStrategyView());
        this.manager = controller.getManager();

        // Setup combo boxes.
        comboCondition.setItems(FXCollections.observableArrayList(ConditionType.values()));
        comboPeriod.setItems(FXCollections.observableArrayList(TimePeriod.values()));
        comboBos.setItems(FXCollections.observableArrayList("BUY", "SELL"));

        // Setup instruction list.
        instructionList.setItems(manager.getCurrentStrategy().getInstructions());
        instructionList.setCellFactory(createDragAndDropFactory());

        // Setup text and combo box listener for value change.
        comboCondition.valueProperty().addListener((observable, oldValue, newValue) -> onConditionSelected(newValue));
        comboAction.valueProperty().addListener((observable, oldValue, newValue) -> onActionSelected(newValue));
        txtAmount.textProperty().addListener((observable, oldValue, newValue) -> comboPeriod.setDisable(!newValue.contains("%")));

        // Setup task for updating label text with token prices.
        setupUpdatingTask();
    }

    @FXML
    public boolean updateInstruction() {
        ConditionType condition = comboCondition.getValue();
        ActionType type = comboAction.getValue();
        String amount = txtAmount.getText();
        TimePeriod period = comboPeriod.getValue();
        String bos = comboBos.getValue();
        String purchase = txtTransaction.getText();

        // Check that none of the essential options are invalid.
        if (condition == null || type == null || amount == null || amount.isEmpty() || bos == null || bos.isEmpty() || purchase == null || purchase.isEmpty()) {
            lblError.setText("You must fill in all options!");
            return false;
        }

        boolean isPercentage = false;

        // Validate target amount value
        if (amount.contains("%")) {
            if (!NumberUtils.isCreatable(amount.replace("%", ""))) {
                lblError.setText("Invalid percentage value provided!");
                return false;
            }

            if (period == null) {
                lblError.setText("You must provide a time period for percentage values!");
                return false;
            }

            isPercentage = true;
        } else {
            if (!NumberUtils.isCreatable(amount)) {
                lblError.setText("Invalid numerical target provided!");
                return false;
            }
        }

        if (!NumberUtils.isCreatable(purchase)) {
            lblError.setText("Invalid numerical purchase amount provided!");
            return false;
        }

        if (isPercentage && !type.canHavePercentage()) {
            lblError.setText("This action does not support percentages!");
            return false;
        }

        // Update action.
        String actionStr = condition.name() + ":" + type.name() + ":" + amount;
        if (isPercentage) actionStr += (":" + period.name());

        selectedInstruction.setAction(actionStr);

        // Update other data.
        selectedInstruction.setRawAmount(Double.parseDouble(purchase));
        selectedInstruction.typeProperty().set(bos);

        // Don't update new instructions because they don't have a controller yet.
        if (!newInstruction) {
            // Update color of buy/sell box.
            selectedInstruction.getController().updateState();
            manager.getCurrentStrategy().updateStrategy();
            DatabaseUtils.saveInstruction(selectedInstruction);
        }

        return true;
    }

    @FXML
    public void deleteInstruction() {
        manager.getCurrentStrategy().getInstructions().remove(selectedInstruction);
        unselectInstruction();
        DatabaseUtils.deleteInstruction(selectedInstruction);
    }

    @FXML
    public void createInstruction() {
        Strategy currentStrategy = manager.getCurrentStrategy();

        // Create a dummy instruction with some placeholder information. This will be overriden by the values in the GUI
        // when updateInstruction() is called.
        selectedInstruction = new Instruction(-1, currentStrategy.getInstructions().size() + 1, "BUY", "PRICE:INCREASES_TO:30566", 1, currentStrategy);
        newInstruction = true;

        if (updateInstruction()) {
            // Once the dummy instructions data has been populated, create the real new instruction and save it to
            // the database.
            selectedInstruction = DatabaseUtils.createInstruction(
                    UserProfile.get().getUsername(),
                    selectedInstruction.getRawPriority(),
                    selectedInstruction.typeProperty().get(),
                    selectedInstruction.getRawAction(),
                    selectedInstruction.getRawAmount(),
                    currentStrategy
            );

            currentStrategy.getInstructions().add(selectedInstruction);
            manager.getCurrentStrategy().updateStrategy();
        }

        newInstruction = false;
    }

    @FXML
    public void backToStrategies() {
        manager.setCurrentStrategy(null, true);
        valueTask.cancel();
    }

    @FXML
    private void unselectInstruction() {
        // Reset cached instruction.
        this.selectedInstruction = null;

        // Reset interface state.
        instructionList.getSelectionModel().clearSelection();
        comboCondition.getSelectionModel().clearSelection();
        comboAction.getSelectionModel().clearSelection();
        txtAmount.setText("");
        comboPeriod.getSelectionModel().clearSelection();
        comboBos.getSelectionModel().clearSelection();
        txtTransaction.setText("");
        lblError.setText("");
        lblTokenIn.setText("");
        lblTokenOut.setText("");

        btnCreate.setVisible(true);
        btnUpdate.setVisible(false);
        btnDelete.setVisible(false);
        comboPeriod.setDisable(true);
    }

    private void onConditionSelected(ConditionType type) {
        if (type == null) return;
        // Add supported instruction types to the action combo box, and if it is none, then disable the element.
        List<ActionType> supportedTypes = type.getSupportedInstructions();
        comboAction.setItems(FXCollections.observableArrayList(supportedTypes));
        comboAction.setDisable(supportedTypes.isEmpty());
        comboPeriod.setDisable(!type.hasTimePeriod());

        // Special condition

        // Clear the selection if the combo box is disabled.
        if (comboAction.isDisabled()) {
            comboAction.getSelectionModel().clearSelection();
        }

        // Clear the selection if the combo box is disabled.
        if (comboPeriod.isDisabled()) {
            comboPeriod.getSelectionModel().clearSelection();
        }
    }

    private void onActionSelected(ActionType type) {
        ConditionType condition = comboCondition.getValue();
        txtAmount.setDisable(condition == null || type == null || !type.canInputValue());

        if (condition != null && condition.hasTimePeriod()) {
            comboPeriod.setDisable(false);
        } else {
            comboPeriod.setDisable(type == null || !type.canHavePercentage());
        }

        // Clear the text field if it is disabled.
        if (txtAmount.isDisabled()) {
            txtAmount.setText("");
        }

        // Clear the selection if the combo box is disabled.
        if (comboPeriod.isDisabled()) {
            comboPeriod.getSelectionModel().clearSelection();
        }
    }

    private void selectInstruction(Instruction instruction) {
        if (instruction == null) {
            unselectInstruction();
            return;
        }

        // Reset states.
        comboPeriod.setDisable(true);
        comboPeriod.getSelectionModel().clearSelection();
        lblError.setText("");

        // Update button visibility
        btnCreate.setVisible(false);
        btnUpdate.setVisible(true);
        btnDelete.setVisible(true);

        String[] data = instruction.getData();

        comboBos.getSelectionModel().select(instruction.typeProperty().get());
        txtTransaction.setText(String.valueOf(instruction.getRawAmount()));

        switch (data.length) {
            case 3 -> {
                comboCondition.getSelectionModel().select(ConditionType.valueOf(data[0]));
                comboAction.getSelectionModel().select(ActionType.valueOf(data[1]));
                txtAmount.setText(data[2]);
            }
            case 4 -> {
                comboCondition.getSelectionModel().select(ConditionType.valueOf(data[0]));
                comboAction.getSelectionModel().select(ActionType.valueOf(data[1]));
                txtAmount.setText(data[2]);
                comboPeriod.getSelectionModel().select(TimePeriod.valueOf(data[3]));
            }
            default -> {
                unselectInstruction();
                return;
            }
        }

        comboCondition.setDisable(true);
        comboAction.setDisable(comboAction.getSelectionModel().isEmpty());
        txtAmount.setDisable(txtAmount.getText().isEmpty());
        comboPeriod.setDisable(comboAction.getSelectionModel().isEmpty());

        String[] tokens = manager.getCurrentStrategy().getTokenPairNames();
        lblTokenIn.setText(tokens[0]);
        lblTokenOut.setText(tokens[1]);

        this.selectedInstruction = instruction;

        instruction.getController().getBackground().setStyle("-fx-background-color: #DDBBE0; -fx-background-radius: 10px;");
    }

    private void setupUpdatingTask() {
        String[] names = manager.getCurrentStrategy().getTokenPairNames();

        valueTask = new Timer();

        valueTask.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                String transaction = txtTransaction.getText();

                Platform.runLater(() -> {
                    if (ConditionType.OWNED_TOKEN_AMOUNT == comboCondition.getValue()) {
                        // Update the label with the currently owned token.
                        lblEstimatedPurchase.setText("(OWNED: " + Utilities.FORMAT_TWO_DECIMAL_PLACE.format(UserProfile.get().getOwnedToken(names[0]))
                                + " " + names[0] + ")");
                    } else if (ConditionType.OWNED_CURRENCY_AMOUNT == comboCondition.getValue()) {
                        // Update the label with the currently owned currency.
                        lblEstimatedPurchase.setText("(OWNED: " + Utilities.FORMAT_TWO_DECIMAL_PLACE.format(UserProfile.get().getOwnedToken(names[1]))
                                + " " + names[1] + ")");
                    } else {
                        // Update the label with current market value.
                        lblEstimatedPurchase.setText("(LIVE: " + Utilities.FORMAT_TWO_DECIMAL_PLACE.format(manager.getTradesListener().getCurrentPrice())
                                + " " + names[1] + ")");
                    }

                    if (NumberUtils.isCreatable(transaction)) {
                        lblEstimatedTransaction.setText("(" + Utilities.FORMAT_TWO_DECIMAL_PLACE.format(manager.getTradesListener().getCurrentPrice() * Double.parseDouble(transaction))
                                + " " + names[1] + ")");
                    }
                });
            }
        }, 0, 1000L);
    }

    // Creates a factory handler for the ListCell which allows us to drag and drop the instructions in the list
    // to re-order them.
    private Callback<ListView<Instruction>, ListCell<Instruction>> createDragAndDropFactory() {
        return e -> {
            InstructionCellFactory cell = new InstructionCellFactory();

            cell.setOnMouseClicked(event -> {
                if (!cell.isEmpty()) {
                    selectInstruction(cell.getItem());
                } else {
                    unselectInstruction();
                }
            });

            // Detect when a cell is being dragged.
            cell.setOnDragDetected(event -> {
                if (!cell.isEmpty()) {
                    Integer index = cell.getIndex();
                    Dragboard db = cell.startDragAndDrop(TransferMode.MOVE);
                    db.setDragView(cell.snapshot(null, null));
                    ClipboardContent cc = new ClipboardContent();
                    cc.put(SERIALIZED_MIME_TYPE, index);
                    db.setContent(cc);
                    event.consume();
                }
            });

            cell.setOnDragOver(event -> {
                if (event.getGestureSource() != cell && event.getDragboard().hasContent(SERIALIZED_MIME_TYPE)) {
                    event.acceptTransferModes(TransferMode.MOVE);
                    event.consume();
                }
            });

            cell.setOnDragEntered(event -> {
                if (!cell.isEmpty() && event.getGestureSource() != cell && event.getDragboard().hasContent(SERIALIZED_MIME_TYPE)) {
                    cell.setOpacity(0.3);
                }
            });

            cell.setOnDragExited(event -> {
                if (event.getGestureSource() != cell && event.getDragboard().hasContent(SERIALIZED_MIME_TYPE)) {
                    cell.setOpacity(1);
                }
            });

            cell.setOnDragDropped(event -> {
                if (cell != event.getGestureSource() && event.getDragboard().hasContent(SERIALIZED_MIME_TYPE)) {
                    int draggedIndex = (int) event.getDragboard().getContent(SERIALIZED_MIME_TYPE);

                    Instruction draggedItem = instructionList.getItems().remove(draggedIndex);
                    int dropIndex = cell.isEmpty() ? instructionList.getItems().size() : cell.getIndex();

                    instructionList.getItems().add(dropIndex, draggedItem);
                    event.setDropCompleted(true);
                    instructionList.getSelectionModel().select(dropIndex);
                    event.consume();

                    // Update priority.
                    for (int i = 0; i < instructionList.getItems().size(); i++) {
                        instructionList.getItems().get(i).priorityProperty().set(String.valueOf(i + 1));
                    }
                }
            });

            cell.setOnDragDone(event -> {
                if (event.getTransferMode() == TransferMode.MOVE) {
                    cell.setOpacity(1);
                }
            });

            return cell;
        };
    }

}
