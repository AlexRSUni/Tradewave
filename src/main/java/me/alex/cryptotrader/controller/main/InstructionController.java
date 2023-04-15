package me.alex.cryptotrader.controller.main;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXListView;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import me.alex.cryptotrader.factory.InstructionCellFactory;
import me.alex.cryptotrader.instruction.InstructionType;
import me.alex.cryptotrader.instruction.TimePeriod;
import me.alex.cryptotrader.manager.ConfigurationManager;
import me.alex.cryptotrader.manager.ViewManager;
import me.alex.cryptotrader.models.Instruction;
import me.alex.cryptotrader.models.Strategy;
import me.alex.cryptotrader.profile.UserProfile;
import me.alex.cryptotrader.util.DatabaseUtils;
import org.apache.commons.lang3.math.NumberUtils;

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
    private ComboBox<InstructionType> comboAction;
    @FXML
    private JFXListView<Instruction> instructionList;
    @FXML
    private Label lblError;
    @FXML
    private Label lblTokenIn;
    @FXML
    private Label lblTokenOut;

    // Variables

    private ConfigurationManager manager;

    private Instruction selectedInstruction;
    private boolean newInstruction;

    @FXML
    public void initialize() {
        // Init manager.
        StrategyController controller = ViewManager.get().getController(ViewManager.get().getStrategyView());
        this.manager = controller.getManager();

        // Setup combo boxes.
        comboAction.setItems(FXCollections.observableArrayList(InstructionType.values()));
        comboPeriod.setItems(FXCollections.observableArrayList(TimePeriod.values()));
        comboBos.setItems(FXCollections.observableArrayList("BUY", "SELL"));

        // Setup instruction list.
        instructionList.setItems(manager.getCurrentStrategy().getInstructions());

        // Setup drag and dropping instructions.
        instructionList.setCellFactory(e -> {
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
        });

        // Setup text box listener for value change.
        txtAmount.textProperty().addListener((observable, oldValue, newValue) -> comboPeriod.setDisable(!newValue.contains("%")));
    }

    public void selectInstruction(Instruction instruction) {
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
            case 2 -> {
                comboAction.getSelectionModel().select(InstructionType.valueOf(data[0]));
                txtAmount.setText(data[1]);
            }
            case 3 -> {
                comboAction.getSelectionModel().select(InstructionType.valueOf(data[0]));
                txtAmount.setText(data[1]);
                comboPeriod.getSelectionModel().select(TimePeriod.valueOf(data[2]));
                comboPeriod.setDisable(false);
            }
            default -> {
                unselectInstruction();
                return;
            }
        }

        String[] tokens = manager.getCurrentStrategy().getTokenPairNames();
        lblTokenIn.setText(tokens[0]);
        lblTokenOut.setText(tokens[1]);

        this.selectedInstruction = instruction;

        instruction.getController().getBackground().setStyle("-fx-background-color: #DDBBE0; -fx-background-radius: 10px;");
    }

    public void unselectInstruction() {
        // Reset cached instruction.
        this.selectedInstruction = null;

        // Reset interface state.
        instructionList.getSelectionModel().clearSelection();
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

    @FXML
    public boolean updateInstruction() {
        InstructionType type = comboAction.getValue();
        String amount = txtAmount.getText();
        TimePeriod period = comboPeriod.getValue();
        String bos = comboBos.getValue();
        String purchase = txtTransaction.getText();

        // Check that none of the essential options are invalid.
        if (type == null || amount == null || amount.isEmpty() || bos == null || bos.isEmpty() || purchase == null || purchase.isEmpty()) {
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
        String actionStr = type.name() + ":" + amount;
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
        selectedInstruction = new Instruction(-1, currentStrategy.getInstructions().size() + 1, "BUY", "INCREASES_TO:30566", 1, currentStrategy);
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
    }

}
