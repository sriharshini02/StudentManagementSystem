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
        String name = nameField.getText().trim();
        String username = usernameField.getText().trim();
        String institute = instituteField.getText().trim();
        String password = passwordField.getText(); 
        LoggerUtil.logInfo("Registration attempt by username: " + username + " for institute: " + institute);

        if (name.isEmpty() || username.isEmpty() || institute.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please fill all fields.");
            LoggerUtil.logWarning("Registration failed: Missing fields.");
            return;
        }

        Connection conn = null; 
        try {
            conn = DatabaseConnector.getConnection();
            conn.setAutoCommit(false); // Start transaction

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
                throw new SQLException("Failed to retrieve admin ID after insertion.");
            }

            String authInsert = "INSERT INTO auth_user (adminID, username, password) VALUES (?, ?, ?)";
            PreparedStatement authStmt = conn.prepareStatement(authInsert);
            authStmt.setInt(1, adminID);
            authStmt.setString(2, username);
            authStmt.setString(3, password); 
            authStmt.executeUpdate();

            conn.commit(); // Commit transaction

            LoggerUtil.logInfo("Registration successful for Admin user: " + username);

            SessionManager.setAdminSession(username, institute);
            LoggerUtil.logInfo("Session initialized for new admin: " + username + " from " + institute);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/SMS/views/dashboard.fxml"));
            Parent dashboardRoot = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(dashboardRoot));
            stage.setTitle("Admin Dashboard"); 
            stage.show();
            LoggerUtil.logInfo("Navigated to Admin Dashboard after successful registration.");

        } catch (SQLIntegrityConstraintViolationException e) {
            // Specific catch for duplicate entry errors (username or institute UNIQUE constraint)
            if (conn != null) {
                try {
                    conn.rollback(); // Rollback on constraint violation
                    statusLabel.setText("Registration failed: Username or Institute already exists.");
                } catch (SQLException rollbackEx) {
                    LoggerUtil.logSevere("Error during transaction rollback for registration: " + username, rollbackEx);
                }
            }
        } catch (SQLException e) {
            // General SQL error during the transaction
            if (conn != null) {
                try {
                    conn.rollback(); // Rollback on other SQL errors
                    LoggerUtil.logSevere("Registration failed due to SQL error for user: " + username + ". Rolling back transaction.", e);
                    statusLabel.setText("Registration failed: Database error. " + e.getMessage());
                } catch (SQLException rollbackEx) {
                    LoggerUtil.logSevere("Error during transaction rollback for registration: " + username, rollbackEx);
                }
            }
        } catch (IOException e) {
            LoggerUtil.logSevere("Dashboard loading failed after registration for user: " + username, e);
            statusLabel.setText("Failed to load dashboard after registration.");
        } catch (Exception e) {
            LoggerUtil.logSevere("An unexpected error occurred during registration for user: " + username, e);
            statusLabel.setText("An unexpected error occurred: " + e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.close(); // Close the connection in finally block
                } catch (SQLException e) {
                    LoggerUtil.logSevere("Error closing database connection after registration: " + username, e);
                }
            }
        }
    }

    @FXML
    private void goToRoleSelection(ActionEvent event) {
        LoggerUtil.logInfo("Navigating back to role selection screen from Admin Registration.");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/SMS/views/role_selection.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Select Role");
            stage.show();
        } catch (IOException e) {
            LoggerUtil.logSevere("Failed to load role selection screen from Admin Registration.", e);
            // If role_selection.fxml can't be loaded, try to load login.fxml as a fallback
            try {
                Parent loginRoot = FXMLLoader.load(getClass().getResource("/SMS/views/login.fxml"));
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(new Scene(loginRoot));
                stage.setTitle("Admin Login");
                stage.show();
            } catch (IOException ex) {
                LoggerUtil.logSevere("Failed to load both role selection and login screens.", ex);
                showAlert("Navigation Error", "Could not load the previous screen.");
            }
        }
    }

    /**
     * Navigates to the Admin Login screen.
     * This method is re-added as per user request, linked from register.fxml.
     * @param event The ActionEvent that triggered this method.
     */
    @FXML
    private void goToLogin(ActionEvent event) {
        LoggerUtil.logInfo("Navigating to Admin Login screen from Registration.");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/SMS/views/login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Admin Login");
            stage.show();
        } catch (IOException e) {
            LoggerUtil.logSevere("Failed to load Admin Login screen from Registration.", e);
            showAlert("Navigation Error", "Could not load the Admin Login screen.");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR); 
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
