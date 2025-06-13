package SMS.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.event.ActionEvent;
import SMS.db.DatabaseConnector;
import SMS.utils.LoggerUtil;
import SMS.utils.SessionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class DeleteStudentController {

    @FXML private TextField rollField;
    private String institute = SessionManager.getInstitute();

    @FXML
    private void handleDeleteStudent(ActionEvent event) {
        // Ensure the institute is up-to-date in case the session changed
        institute = SessionManager.getInstitute();
        LoggerUtil.logInfo("Delete Student operation initiated by admin of institute: " + institute);

        String rollText = rollField.getText().trim(); // Get text from the TextField

        if (rollText.isEmpty()) {
            LoggerUtil.logWarning("Delete operation aborted: No roll number provided.");
            showAlert("Warning", "Please enter a roll number to delete.");
            return;
        }

        int rollToDelete;
        try {
            rollToDelete = Integer.parseInt(rollText); // Convert String to int
            if (rollToDelete <= 0) {
                LoggerUtil.logWarning("Validation failed: Roll Number must be a positive integer. Entered: " + rollText);
                showAlert("Validation Error", "Roll Number must be a positive integer.");
                return;
            }
        } catch (NumberFormatException e) {
            LoggerUtil.logWarning("Validation failed: Roll Number is not a valid integer. Entered: " + rollText);
            showAlert("Validation Error", "Roll Number must be a valid integer.");
            return;
        }

        try (Connection conn = DatabaseConnector.getConnection()) {
            LoggerUtil.logInfo("Database connection established for delete operation.");

            String sql = "DELETE FROM student WHERE roll=? AND institute=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, rollToDelete); // Set roll as int
            stmt.setString(2, institute);

            LoggerUtil.logInfo("Executing delete query for roll: " + rollToDelete);
            int rows = stmt.executeUpdate();

            if (rows > 0) {
                LoggerUtil.logInfo("Student deleted successfully: Roll = " + rollToDelete + ", Institute = " + institute);
                showAlert("Success", "Student deleted successfully.");
                rollField.clear();
            } else {
                LoggerUtil.logWarning("Delete operation failed: No student found with roll = " + rollToDelete + " in institute = " + institute);
                showAlert("Info", "Student not found.");
            }

        } catch (Exception e) {
            LoggerUtil.logSevere("Exception during delete operation for roll: " + rollToDelete, e);
            showAlert("Error", "Failed to delete student.");
        }
    }

    private void showAlert(String title, String message) {
        LoggerUtil.logInfo("Showing alert - Title: " + title + ", Message: " + message);

        Alert alert;
        if (title.equalsIgnoreCase("Success")) {
            alert = new Alert(Alert.AlertType.INFORMATION);
        } else if (title.equalsIgnoreCase("Warning") || title.equalsIgnoreCase("Info")) {
            alert = new Alert(Alert.AlertType.WARNING);
        } else {
            alert = new Alert(Alert.AlertType.ERROR);
        }
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
