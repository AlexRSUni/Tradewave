package me.alex.cryptotrader.controller.main;

import com.jfoenix.controls.JFXListView;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import me.alex.cryptotrader.CryptoApplication;
import me.alex.cryptotrader.factory.StrategyCellFactory;
import me.alex.cryptotrader.manager.ConfigurationManager;
import me.alex.cryptotrader.manager.ViewManager;
import me.alex.cryptotrader.models.Strategy;
import me.alex.cryptotrader.profile.UserProfile;
import me.alex.cryptotrader.util.DatabaseUtils;
import me.alex.cryptotrader.util.Utilities;
import org.apache.commons.lang3.math.NumberUtils;

public class StrategyController extends BaseController {

    // Main Interface

    @FXML
    private Label lblStatus;
    @FXML
    private Text txtError;
    @FXML
    private TextField txtStrategyName;
    @FXML
    private ComboBox<String> comboToken;
    @FXML
    private JFXListView<Strategy> strategyList;

    // Filtering

    @FXML
    private ComboBox<String> comboFilterToken;
    @FXML
    private TextField txtFilterName;
    @FXML
    private TextField txtFilterAmount;

    // Variables

    private ConfigurationManager manager;

    @FXML
    public void initialize() {
        // Init manager.
        this.manager = new ConfigurationManager();

        // Setup instruction list.
        FilteredList<Strategy> filteredStrategies = new FilteredList<>(UserProfile.get().getStrategies(), s -> true);
        strategyList.setItems(filteredStrategies);
        strategyList.setCellFactory(e -> new StrategyCellFactory(true));

        // Setup token combo box.
        FilteredList<String> filteredItems = new FilteredList<>(CryptoApplication.get().getTradingPairs(), s -> true);
        comboToken.setItems(filteredItems);
        comboToken.setSelectionModel(new FilteredComboBoxSelectionModel<>(filteredItems));
        comboToken.getEditor().textProperty().addListener((observable, oldValue, newValue) -> {
            String userInput = comboToken.getEditor().getText();

            filteredItems.setPredicate(item -> {
                if (item.toLowerCase().startsWith(userInput.toLowerCase())) {
                    return true;
                } else {
                    return userInput.isEmpty(); // Display all items when the input is empty
                }
            });

            updateStatus(userInput);
        });

        // Setup token filter combo box.
        setupFilter(filteredStrategies, filteredItems);
    }

    @FXML
    public void createStrategy() {
        String name = txtStrategyName.getText();
        String token = comboToken.getSelectionModel().getSelectedItem();

        if (name == null || name.isEmpty() || token == null || token.isEmpty()) {
            txtError.setText("You must fill in all fields!");
            return;
        }

        if (!CryptoApplication.get().getTradingPairs().contains(token)) {
            txtError.setText("Invalid token pair selected!");
            return;
        }

        Strategy strategy = DatabaseUtils.createStrategy(UserProfile.get().getUsername(), name, token);
        UserProfile.get().getStrategies().add(strategy);
        DashboardController.get().updateStrategyStatus();

        // Clear GUI
        txtStrategyName.setText("");
        comboToken.getEditor().setText("");

        manager.setCurrentStrategy(strategy, false);
    }

    private void setupFilter(FilteredList<Strategy> filteredStrategies, FilteredList<String> filteredItems) {
        // Setup name filtering.
        txtFilterName.textProperty().addListener((observable, oldValue, newValue) -> filteredStrategies.setPredicate(item -> {
            if (item.nameProperty().get().toLowerCase().startsWith(newValue.toLowerCase())) {
                return true;
            } else {
                return newValue.isEmpty(); // Display all items when the input is empty
            }
        }));

        // Setup amount filtering.
        txtFilterAmount.textProperty().addListener((observable, oldValue, newValue) -> filteredStrategies.setPredicate(item -> {
            int amount = item.getInstructions().size();
            int check = NumberUtils.toInt(newValue, -1);

            System.out.println(newValue + " - " + check);

            if (check == -1) {
                return true;
            }

            if (amount >= check) {
                return true;
            } else {
                return newValue.isEmpty(); // Display all items when the input is empty
            }
        }));

        // Setup combo box filtering.
        comboFilterToken.setItems(filteredItems);
        comboFilterToken.setSelectionModel(new FilteredComboBoxSelectionModel<>(filteredItems));
        comboFilterToken.getEditor().textProperty().addListener((observable, oldValue, newValue) -> {
            String userInput = comboFilterToken.getEditor().getText();

            filteredItems.setPredicate(item -> {
                if (item.toLowerCase().startsWith(userInput.toLowerCase())) {
                    return true;
                } else {
                    return userInput.isEmpty(); // Display all items when the input is empty
                }
            });

            filteredStrategies.setPredicate(item -> {
                if (item.tokenProperty().get().toLowerCase().startsWith(userInput.toLowerCase())) {
                    return true;
                } else {
                    return userInput.isEmpty(); // Display all items when the input is empty
                }
            });
        });
    }

    private void updateStatus(String tokenPair) {
        if (CryptoApplication.get().getTradingPairs().contains(tokenPair)) {
            String[] data = Utilities.getTokenPairNames(tokenPair);

            String first = data[0];
            String second = data[1];

            if (first == null || first.isEmpty() || second == null || second.isEmpty()) {
                lblStatus.setText("");
                return;
            }

            lblStatus.setText("Trading " + Utilities.getSymbolName(first) + " in " + Utilities.getSymbolName(second));
        } else {
            lblStatus.setText("");
        }
    }

    public ConfigurationManager getManager() {
        return manager;
    }

    public static StrategyController get() {
        return ViewManager.get().getController(ViewManager.get().getStrategyView());
    }

    private static class FilteredComboBoxSelectionModel<T> extends SingleSelectionModel<T> {
        private final FilteredList<T> filteredItems;

        public FilteredComboBoxSelectionModel(FilteredList<T> filteredItems) {
            this.filteredItems = filteredItems;
        }

        @Override
        protected T getModelItem(int index) {
            return filteredItems.getSource().get(index);
        }

        @Override
        protected int getItemCount() {
            return filteredItems.getSource().size();
        }

        @Override
        public void select(int index) {
            if (index == -1) return;
            int sourceIndex = filteredItems.getSourceIndex(index);
            super.select(sourceIndex);
        }

        @Override
        public void clearAndSelect(int index) {
            if (index == -1) return;
            int sourceIndex = filteredItems.getSourceIndex(index);
            super.clearAndSelect(sourceIndex);
        }
    }

}
