package SMS.controllers;

import SMS.db.DatabaseConnector;
import SMS.utils.LoggerUtil;
import SMS.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.event.ActionEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

public class ManageFacultyController {

    @FXML private TextField nameField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField facultyIDSearchField; // For update/delete by ID, or for searching

    private String currentInstitute;

    @FXML
    public void initialize() {
        currentInstitute = SessionManager.getInstitute();
        if (currentInstitute == null) {
            showAlert("Session Error", "Institute information not found. Please log in as Admin.");
            LoggerUtil.logWarning("Institute null in ManageFacultyController.initialize.");
            return;
        }
        LoggerUtil.logInfo("ManageFacultyController initialized for institute: " + currentInstitute);
    }

    @FXML
    private void handleAddFaculty(ActionEvent event) {
        String name = nameField.getText().trim();
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (name.isEmpty() || username.isEmpty() || password.isEmpty()) {
            showAlert("Validation Error", "Please fill all fields to add a faculty member.");
            LoggerUtil.logWarning("Add Faculty failed: Missing fields.");
            return;
        }

        try (Connection conn = DatabaseConnector.getConnection()) {
            String sql = "INSERT INTO faculty (username, password, name, institute) VALUES (?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setString(2, password); // In a real app, hash this password!
            stmt.setString(3, name);
            stmt.setString(4, currentInstitute);

            stmt.executeUpdate();
            showAlert("Success", "Faculty member '" + name + "' added successfully.");
            LoggerUtil.logInfo("Faculty added: " + name + " (Username: " + username + ")");
            clearFields();

        } catch (SQLIntegrityConstraintViolationException e) {
            showAlert("Error", "Faculty with this username already exists or other unique constraint violation.");
            } catch (SQLException e) {
            showAlert("Error", "Database error occurred while adding faculty: " + e.getMessage());
            LoggerUtil.logSevere("Database error adding faculty: " + username, e);
        } catch (Exception e) {
            showAlert("Error", "An unexpected error occurred while adding faculty.");
            LoggerUtil.logSevere("Unexpected error adding faculty: " + username, e);
        }
    }

    @FXML
    private void handleUpdateFaculty(ActionEvent event) {
        // This will be implemented in a future iteration.
        // Needs a way to select a faculty (e.g., from a table, or search by ID/username)
        // Then populate fields, allow edits, and update DB.
        showAlert("Coming Soon", "Update Faculty functionality is not yet implemented.");
        LoggerUtil.logInfo("Update Faculty button clicked.");
    }

    @FXML
    private void handleDeleteFaculty(ActionEvent event) {
        // This will be implemented in a future iteration.
        // Needs a way to select a faculty (e.g., from a table, or search by ID/username)
        // And a confirmation step.
        showAlert("Coming Soon", "Delete Faculty functionality is not yet implemented.");
        LoggerUtil.logInfo("Delete Faculty button clicked.");
    }

    private void showAlert(String title, String message) {
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
        // facultyIDSearchField.clear(); // Clear this if it's used for input
    }
}
