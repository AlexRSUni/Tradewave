package me.alex.cryptotrader.controller.main;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXListView;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.util.Callback;
import me.alex.cryptotrader.factory.InstructionCellFactory;
import me.alex.cryptotrader.manager.StrategyManager;
import me.alex.cryptotrader.manager.ViewManager;
import me.alex.cryptotrader.models.Instruction;
import me.alex.cryptotrader.models.Strategy;
import me.alex.cryptotrader.profile.UserProfile;
import me.alex.cryptotrader.util.DatabaseUtils;
import me.alex.cryptotrader.util.Utilities;
import me.alex.cryptotrader.util.ValidationUtils;

import java.util.Timer;
import java.util.TimerTask;

public class InstructionController extends BaseController {

    @FXML
    private JFXButton btnIf;
    @FXML
    private JFXButton btnAction;
    @FXML
    private JFXButton btnElseIf;
    @FXML
    private JFXButton btnElse;
    @FXML
    private JFXButton btnWait;
    @FXML
    private JFXButton btnStop;
    @FXML
    private JFXButton btnOr;
    @FXML
    private JFXButton btnDivider;

    @FXML
    private Label lblLive;
    @FXML
    private Label lblLiveVal;
    @FXML
    private Label ownedTokenVal;
    @FXML
    private Label ownedToken;
    @FXML
    private Label ownedCurrency;
    @FXML
    private Label ownedCurrencyVal;
    @FXML
    private JFXListView<Instruction> instructionList;

    // Variables

    private StrategyManager manager;
    private Timer valueTask;

    private String[] tokenPair;

    @FXML
    public void initialize() {
        // Init manager.
        StrategyController controller = ViewManager.get().getController(ViewManager.get().getStrategyView());
        this.manager = controller.getManager();

        // Setup instruction list.
        instructionList.setCellFactory(createDragAndDropFactory());

        // Setup click handlers.
        btnIf.setOnMouseClicked(mouseEvent -> createInstruction(Instruction.InstructionType.IF));
        btnOr.setOnMouseClicked(mouseEvent -> createInstruction(Instruction.InstructionType.OR));
        btnElse.setOnMouseClicked(mouseEvent -> createInstruction(Instruction.InstructionType.ELSE));
        btnElseIf.setOnMouseClicked(mouseEvent -> createInstruction(Instruction.InstructionType.ELSE_IF));

        btnAction.setOnMouseClicked(mouseEvent -> createInstruction(Instruction.InstructionType.ACTION));
        btnWait.setOnMouseClicked(mouseEvent -> createInstruction(Instruction.InstructionType.WAIT));
        btnStop.setOnMouseClicked(mouseEvent -> createInstruction(Instruction.InstructionType.STOP));
        btnDivider.setOnMouseClicked(mouseEvent -> createInstruction(Instruction.InstructionType.DIVIDER));
    }

    public void setup() {
        // Store some data locally.
        tokenPair = manager.getCurrentStrategy().getTokenPairNames();

        // Setup instruction list.
        instructionList.setItems(manager.getCurrentStrategy().getInstructions());

        // Update data labels.
        lblLive.setText("Live " + tokenPair[0] + " Price:");
        ownedToken.setText("Owned " + tokenPair[0] + ":");
        ownedCurrency.setText("Owned " + tokenPair[1] + ":");

        // Setup task for updating label text with token prices.
        setupUpdatingTask();
    }

    @FXML
    public void saveStrategy() {
        Strategy strategy = manager.getCurrentStrategy();

        // Validate our instructions.
        String error = ValidationUtils.validateInstructionCompleting(strategy, manager.getTradesListener().getCurrentPrice());

        if (error == null) {
            error = ValidationUtils.validateInstructionOrder(strategy);
        }

        if (error == null) {

            // Save and reset currently selected strategy.
            manager.setCurrentStrategy(null, true);
            valueTask.cancel();

        } else {
            Utilities.sendAlert("Formatting error in Instructions list!", error);
        }
    }

    private void createInstruction(Instruction.InstructionType type) {
        Strategy currentStrategy = manager.getCurrentStrategy();

        Instruction instruction = DatabaseUtils.createInstruction(UserProfile.get().getUsername(), currentStrategy.getInstructions().size() + 1, type, "", currentStrategy);
        currentStrategy.getInstructions().add(instruction);
        manager.getCurrentStrategy().updateStrategy();

        if (type == Instruction.InstructionType.IF) {
            createInstruction(Instruction.InstructionType.END_IF);
        }
    }

    private void setupUpdatingTask() {
        if (valueTask != null) {
            valueTask.cancel();
        }

        valueTask = new Timer();

        valueTask.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    lblLiveVal.setText(Utilities.formatPrice(manager.getTradesListener().getCurrentPrice(), tokenPair[1]));
                    ownedTokenVal.setText(Utilities.FORMAT_TWO_DECIMAL_PLACE.format(UserProfile.get().getOwnedToken(tokenPair[0])) + " " + tokenPair[0]);
                    ownedCurrencyVal.setText(Utilities.FORMAT_TWO_DECIMAL_PLACE.format(UserProfile.get().getOwnedToken(tokenPair[1])) + " " + tokenPair[1]);
                    instructionList.getItems().forEach(instruction -> instruction.getController().updateEstimatedValue(manager.getTradesListener().getCurrentPrice()));
                });
            }
        }, 1000L, 1000L);
    }

    // Creates a factory handler for the ListCell which allows us to drag and drop the instructions in the list
    // to re-order them.
    private Callback<ListView<Instruction>, ListCell<Instruction>> createDragAndDropFactory() {
        DataFormat SERIALIZED_MIME_TYPE = new DataFormat("application/x-java-serialized-object");

        return e -> {
            InstructionCellFactory cell = new InstructionCellFactory();

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

                    // Update order + priority.
                    for (int i = 0; i < instructionList.getItems().size(); i++) {
                        Instruction instruction = instructionList.getItems().get(i);
                        instruction.setPriority(i + 1);
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

    public StrategyManager getManager() {
        return manager;
    }

    public static InstructionController get() {
        return ViewManager.get().getController(ViewManager.get().getInstructionView());
    }

}
