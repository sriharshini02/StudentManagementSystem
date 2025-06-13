package SMS.controllers;

import SMS.db.DatabaseConnector;
import SMS.utils.LoggerUtil;
import SMS.utils.SessionManager;

import javafx.fxml.FXML;
import javafx.scene.control.Alert; // Still needed for general alerts
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Label; // Import Label

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Node;

import java.io.IOException;
import java.sql.*;
import javafx.event.ActionEvent;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel; // FXML ID for the error message label

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();

        // Clear any previous error messages
        errorLabel.setText("");

        LoggerUtil.logInfo("Admin Login attempt by username: " + username);

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please enter both username and password.");
            LoggerUtil.logWarning("Admin login failed: Empty fields.");
            return;
        }

        try (Connection conn = DatabaseConnector.getConnection()) {
            String sql = "SELECT a.username, ad.institute FROM auth_user a " +
                         "JOIN admin ad ON a.adminID = ad.adminID " +
                         "WHERE a.username = ? AND a.password = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, username);
                stmt.setString(2, password);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    String adminUsername = rs.getString("username");
                    String institute = rs.getString("institute");

                    SessionManager.setAdminSession(adminUsername, institute);

                    LoggerUtil.logInfo("Admin Login successful for user: " + adminUsername + " from institute: " + institute);

                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/SMS/views/dashboard.fxml"));
                    Parent root = loader.load();

                    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                    stage.setScene(new Scene(root));
                    stage.setTitle("Admin Dashboard"); 
                    stage.show();

                } else {
                    LoggerUtil.logWarning("Admin Login failed: Invalid credentials for username: " + username);
                    errorLabel.setText("Invalid username or password."); // Display error on label
                }
            }
        } catch (Exception e) {
            LoggerUtil.logSevere("Database error during admin login for user: " + username, e);
            errorLabel.setText("Database error occurred. Please try again later."); // Display error on label
        }
    }

    /**
     * This private showAlert method is now less critical for login failures
     * as errorLabel is used, but kept for potential future general alerts.
     */
    private void showAlert(String title, String message) {
        LoggerUtil.logInfo("Showing alert: " + title + " - " + message);
        Alert alert = new Alert(Alert.AlertType.ERROR); 
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void goToRoleSelection(ActionEvent event) {
        LoggerUtil.logInfo("Navigating back to role selection screen from Admin Login.");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/SMS/views/role_selection.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Select Role");
            stage.show();
        } catch (IOException e) {
            LoggerUtil.logSevere("Failed to load role selection screen from Admin Login.", e);
            showAlert("Navigation Error", "Could not load the role selection screen.");
        }
    }

    @FXML
    private void goToRegister(ActionEvent event) {
        LoggerUtil.logInfo("Navigating to admin registration screen from Admin Login.");
        try {
            Parent registerRoot = FXMLLoader.load(getClass().getResource("/SMS/views/register.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(registerRoot));
            stage.setTitle("Admin Registration"); 
            stage.show();
        } catch (IOException e) {
            LoggerUtil.logSevere("Failed to load admin registration screen.", e);
            showAlert("Navigation Error", "Could not load the admin registration screen.");
        }
    }
}
