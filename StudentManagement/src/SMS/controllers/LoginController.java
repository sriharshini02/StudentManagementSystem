package SMS.controllers;

import SMS.db.DatabaseConnector;
import SMS.utils.LoggerUtil;
import SMS.utils.SessionManager;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

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
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();

        LoggerUtil.logInfo("Login attempt by username: " + username);

        try (Connection conn = DatabaseConnector.getConnection()) {
            String sql = "SELECT a.username, ad.institute FROM auth_user a " +
                         "JOIN admin ad ON a.adminID = ad.adminID " +
                         "WHERE a.username = ? AND a.password = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String adminUsername = rs.getString("username");
                String institute = rs.getString("institute");

                SessionManager.setSession(adminUsername, institute);

                LoggerUtil.logInfo("Login successful for user: " + adminUsername + " from institute: " + institute);

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/SMS/views/dashboard.fxml"));
                Parent root = loader.load();

                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setTitle("Admin Dashboard");
                stage.show();

            } else {
                LoggerUtil.logWarning("Login failed: Invalid credentials for username: " + username);
                showAlert("Login Failed", "Invalid credentials.");
            }
        } catch (Exception e) {
            LoggerUtil.logSevere("Database error during login for user: " + username, e);
            showAlert("Error", "Database error occurred.");
        }
    }

    private void showAlert(String title, String message) {
        LoggerUtil.logInfo("Showing alert: " + title + " - " + message);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void goToRegister(ActionEvent event) {
        LoggerUtil.logInfo("Navigating to registration screen");
        try {
            Parent registerRoot = FXMLLoader.load(getClass().getResource("/SMS/views/register.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(registerRoot));
            stage.setTitle("Registration");
            stage.show();
        } catch (IOException e) {
            LoggerUtil.logSevere("Failed to load registration screen", e);
            showAlert("Navigation Error", "Could not load the registration screen.");
        }
    }
}
