package me.alex.cryptotrader.controller.main;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.jfoenix.controls.JFXRadioButton;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import me.alex.cryptotrader.manager.ViewManager;
import me.alex.cryptotrader.profile.UserProfile;
import me.alex.cryptotrader.util.DatabaseUtils;

public class AccountController extends BaseController {

    // Login

    @FXML
    private TextField txtUsername;
    @FXML
    private PasswordField txtPassword;
    @FXML
    public Label lblMessage;
    @FXML
    private Label lblMessageAlt;
    @FXML
    private JFXRadioButton stayedLoggedIn;

    // Welcome Back

    @FXML
    public Text txtLoggedIn;
    @FXML
    private Label lblMessageRem;
    @FXML
    private PasswordField txtPasswordRem;

    // Register

    @FXML
    private TextField txtRegUsername;
    @FXML
    private PasswordField txtRegPassword;
    @FXML
    private PasswordField txtConfirmPassword;
    @FXML
    private PasswordField txtApiKey;
    @FXML
    private PasswordField txtSecretKey;
    @FXML
    private Label lblRegMessage;

    @FXML
    public void handleRegister() {
        String username = txtRegUsername.getText();

        String password = txtRegPassword.getText();
        String confirmPassword = txtConfirmPassword.getText();

        String apiKey = txtApiKey.getText();
        String secretKey = txtSecretKey.getText();

        String errorMessage = null;

        // Some password validations.
        if (!password.equals(confirmPassword)) {
            errorMessage = "Your passwords did not match!";
        } else if (password.length() < 16) {
            errorMessage = "You password must be at least 8 characters long!";
        }

        BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance(apiKey, secretKey);
        BinanceApiRestClient client = factory.newRestClient();

        try {
            client.getAccount(UserProfile.BINANCE_API_WAIT, System.currentTimeMillis());
        } catch (Exception ex) {
            errorMessage = "Invalid Binance API/Secret Key!";
        }

        if (errorMessage != null) {
            lblRegMessage.setText(errorMessage);
            txtRegPassword.clear();
            txtConfirmPassword.clear();
            return;
        }

        if (DatabaseUtils.registerUser(username, password, apiKey, secretKey)) {
            AccountController controller = ViewManager.get().showScene("login");
            controller.lblMessageAlt.setText("Account created successfully!");
        } else {
            lblRegMessage.setText("Error! Registration failed.");
        }
    }

    @FXML
    public void resetRememberMeState() {
        String currentUsername = txtLoggedIn.getText();
        DatabaseUtils.savePersistentUser(currentUsername);
        ViewManager.get().showScene("login");
    }

    @FXML
    public void handleLogin() {
        processLogin(txtUsername.getText(), txtPassword.getText(), stayedLoggedIn.isSelected());
    }

    @FXML
    public void showRegistrationPage() {
        ViewManager.get().showScene("register");
    }

    @FXML
    public void showLoginPage() {
        ViewManager.get().showScene("login");
    }

    @FXML
    public void handleLoginRemembered() {
        processLogin(txtLoggedIn.getText(), txtPasswordRem.getText(), true);
    }

    private void processLogin(String username, String password, boolean stayLoggedIn) {
        if (!DatabaseUtils.validateLogin(username, password, stayLoggedIn)) {
            lblMessageRem.setText("Invalid username or password.");
        }
    }
}