package SMS.controllers;

import SMS.db.DatabaseConnector;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.stage.Stage;
import SMS.utils.LoggerUtil;
import java.io.IOException;
import java.sql.*;
import SMS.utils.SessionManager;

public class RegisterController {
    @FXML private TextField nameField;
    @FXML private TextField usernameField;
    @FXML private TextField instituteField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;

    @FXML
    private void handleRegister(ActionEvent event) {
        String name = nameField.getText();
        String username = usernameField.getText();
        String institute = instituteField.getText();
        String password = passwordField.getText();

        LoggerUtil.logInfo("Registration attempt by username: " + username);

        if (name.isEmpty() || username.isEmpty() || institute.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please fill all fields.");
            LoggerUtil.logWarning("Registration failed: Missing fields");
            return;
        }

        try (Connection conn = DatabaseConnector.getConnection()) {
            conn.setAutoCommit(false);

            String adminInsert = "INSERT INTO admin (name, username, institute) VALUES (?, ?, ?)";
            PreparedStatement adminStmt = conn.prepareStatement(adminInsert, Statement.RETURN_GENERATED_KEYS);
            adminStmt.setString(1, name);
            adminStmt.setString(2, username);
            adminStmt.setString(3, institute);
            adminStmt.executeUpdate();

            ResultSet rs = adminStmt.getGeneratedKeys();
            int adminID = -1;
            if (rs.next()) {
                adminID = rs.getInt(1);
                LoggerUtil.logInfo("New admin inserted with ID: " + adminID);
            } else {
                throw new SQLException("Failed to retrieve admin ID.");
            }
            String authInsert = "INSERT INTO auth_user (adminID, username, password) VALUES (?, ?, ?)";
            PreparedStatement authStmt = conn.prepareStatement(authInsert);
            authStmt.setInt(1, adminID);
            authStmt.setString(2, username);
            authStmt.setString(3, password); 
            authStmt.executeUpdate();

            conn.commit();

            LoggerUtil.logInfo("Registration successful for user: " + username);
            SessionManager.setSession(username, institute);
            LoggerUtil.logInfo("Session initialized for: " + username + " from " + institute);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/SMS/views/dashboard.fxml"));
            Parent dashboardRoot = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(dashboardRoot));
            stage.setTitle("Admin Dashboard");
            stage.show();
            LoggerUtil.logInfo("Navigated to Admin Dashboard after registration");

        } catch (SQLException e) {
            LoggerUtil.logSevere("Registration failed due to SQL error for user: " + username, e);
            statusLabel.setText("Registration failed: " + e.getMessage());
        } catch (IOException e) {
            LoggerUtil.logSevere("Dashboard loading failed after registration", e);
            statusLabel.setText("Failed to load dashboard.");
        }
    }

    @FXML
    private void goToLogin(ActionEvent event) {
        LoggerUtil.logInfo("Navigating to Login screen from Registration");
        try {
            Parent loginRoot = FXMLLoader.load(getClass().getResource("/SMS/views/login.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(loginRoot));
            stage.setTitle("Login");
            stage.show();
        } catch (IOException e) {
            LoggerUtil.logSevere("Failed to load login page", e);
            statusLabel.setText("Failed to load login page.");
        }
    }
}
