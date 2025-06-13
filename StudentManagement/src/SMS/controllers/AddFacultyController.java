package SMS.controllers;

import SMS.db.DatabaseConnector;
import SMS.utils.LoggerUtil;
import SMS.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Label; // Import Label for statusLabel
import javafx.event.ActionEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLException;

public class AddFacultyController {

    @FXML private TextField nameField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel; // To display messages to the user

    private String currentInstitute;

    @FXML
    public void initialize() {
        currentInstitute = SessionManager.getInstitute();
        if (currentInstitute == null) {
            showAlert("Session Error", "Institute information not found. Please log in as Admin.");
            LoggerUtil.logWarning("Institute null in AddFacultyController.initialize.");
            // Potentially redirect to login if this view is accessed without proper session
            return;
        }
        LoggerUtil.logInfo("AddFacultyController initialized for institute: " + currentInstitute);
        statusLabel.setText(""); // Clear status label on init
    }

    @FXML
    private void handleAddFaculty(ActionEvent event) {
        String name = nameField.getText().trim();
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        statusLabel.setText(""); // Clear previous status message

        if (name.isEmpty() || username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please fill all fields.");
            LoggerUtil.logWarning("Add Faculty failed: Missing fields.");
            return;
        }

        Connection conn = null;
        try {
            conn = DatabaseConnector.getConnection();
            String sql = "INSERT INTO faculty (username, password, name, institute) VALUES (?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setString(2, password); // In a real app, hash this password!
            stmt.setString(3, name);
            stmt.setString(4, currentInstitute);

            stmt.executeUpdate();
            statusLabel.setText("Faculty member '" + name + "' added successfully.");
            LoggerUtil.logInfo("Faculty added: " + name + " (Username: " + username + ")");
            clearFields();

        } catch (SQLIntegrityConstraintViolationException e) {
            statusLabel.setText("Faculty with this username already exists.");
            } catch (SQLException e) {
            statusLabel.setText("Database error: " + e.getMessage());
            LoggerUtil.logSevere("Database error adding faculty: " + username, e);
        } catch (Exception e) {
            statusLabel.setText("An unexpected error occurred.");
            LoggerUtil.logSevere("Unexpected error adding faculty: " + username, e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    LoggerUtil.logSevere("Error closing connection after adding faculty", e);
                }
            }
        }
    }

    private void showAlert(String title, String message) {
        LoggerUtil.logInfo("Showing alert - Title: " + title + ", Message: " + message);
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void clearFields() {
        nameField.clear();
        usernameField.clear();
        passwordField.clear();
    }
}
