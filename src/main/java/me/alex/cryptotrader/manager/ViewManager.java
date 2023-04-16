package me.alex.cryptotrader.manager;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import me.alex.cryptotrader.CryptoApplication;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ViewManager {

    // Pane controller reference
    private final Map<BorderPane, Object> paneControllers = new HashMap<>();

    // Our stage reference.
    private final Stage stage;

    private final StringProperty currentMenu;

    private BorderPane dashboardView;
    private BorderPane tradingView;
    private BorderPane testingView;
    private BorderPane strategyView;
    private BorderPane instructionView;

    private double x, y;

    public ViewManager(Stage stage) {
        this.stage = stage;
        this.currentMenu = new SimpleStringProperty("");
    }

    public <T> T showScene(String file) {
        FXMLLoader fxmlLoader = new FXMLLoader(CryptoApplication.class.getResource(file + ".fxml"));
        Scene scene;

        try {
            scene = new Scene(fxmlLoader.load());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        scene.setOnMousePressed(mouseEvent -> {
            x = mouseEvent.getSceneX();
            y = mouseEvent.getSceneY();
        });

        scene.setOnMouseDragged(mouseEvent -> {
            stage.setX(mouseEvent.getScreenX() - x);
            stage.setY(mouseEvent.getScreenY() - y);
        });

        stage.setScene(scene);
        stage.show();

        return fxmlLoader.getController();
    }

    public void minimiseWindow() {
        stage.setIconified(true);
    }

    public BorderPane getDashboardView() {
        if (dashboardView == null) {
            try {
                FXMLLoader loader = new FXMLLoader(CryptoApplication.class.getResource("dashboard.fxml"));
                dashboardView = loader.load();
                paneControllers.put(dashboardView, loader.getController());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return dashboardView;
    }

    public BorderPane getTradingView() {
        if (tradingView == null) {
            try {
                FXMLLoader loader = new FXMLLoader(CryptoApplication.class.getResource("trading.fxml"));
                tradingView = loader.load();
                paneControllers.put(tradingView, loader.getController());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return tradingView;
    }

    public BorderPane getTestingView() {
        if (testingView == null) {
            try {
                FXMLLoader loader = new FXMLLoader(CryptoApplication.class.getResource("testing.fxml"));
                testingView = loader.load();
                paneControllers.put(testingView, loader.getController());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return testingView;
    }

    public BorderPane getStrategyView() {
        if (strategyView == null) {
            try {
                FXMLLoader loader = new FXMLLoader(CryptoApplication.class.getResource("configure.fxml"));
                strategyView = loader.load();
                paneControllers.put(strategyView, loader.getController());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return strategyView;
    }

    public BorderPane getInstructionView() {
        if (instructionView == null) {
            try {
                FXMLLoader loader = new FXMLLoader(CryptoApplication.class.getResource("new_instruction.fxml"));
                instructionView = loader.load();
                paneControllers.put(instructionView, loader.getController());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return instructionView;
    }

    @SuppressWarnings("unchecked")
    public <T> T getController(BorderPane pane) {
        return (T) paneControllers.get(pane);
    }

    public StringProperty getCurrentMenu() {
        return currentMenu;
    }

    public static ViewManager get() {
        return CryptoApplication.get().getViewManager();
    }

}
