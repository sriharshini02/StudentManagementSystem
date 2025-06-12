package SMS.controllers;

import SMS.db.DatabaseConnector;
import SMS.utils.LoggerUtil;
import SMS.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.event.ActionEvent;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.LocalDate;

public class UpdateStudentController {

    @FXML private TextField rollField;
    @FXML private TextField nameField;
    @FXML private TextField ageField;
    @FXML private TextField gradeField;
    @FXML private TextField contactField;
    @FXML private DatePicker dobPicker;

    private String institute = SessionManager.getInstitute();

    @FXML
    private void handleUpdateStudent(ActionEvent event) {
        LoggerUtil.logInfo("Update Student operation started by admin of institute: " + institute);

        String roll = rollField.getText().trim();
        String name = nameField.getText().trim();
        String ageStr = ageField.getText().trim();
        String grade = gradeField.getText().trim();
        String contact = contactField.getText().trim();
        LocalDate dob = dobPicker.getValue();

        LoggerUtil.logInfo("Received data - Roll: " + roll + ", Name: " + name + ", Age: " + ageStr + ", Grade: " + grade + ", Contact: " + contact + ", DOB: " + dob);

        if (roll.isEmpty() || name.isEmpty() || ageStr.isEmpty() || grade.isEmpty() || contact.isEmpty() || dob == null) {
            LoggerUtil.logWarning("Validation failed: Missing fields.");
            showAlert("Validation Error", "Please fill all fields.");
            return;
        }

        int age;
        try {
            age = Integer.parseInt(ageStr);
            if (age < 0) {
                LoggerUtil.logWarning("Validation failed: Age is negative.");
                showAlert("Validation Error", "Age must be a positive integer.");
                return;
            }
        } catch (NumberFormatException e) {
            LoggerUtil.logWarning("Validation failed: Age not a valid integer.");
            showAlert("Validation Error", "Age must be a valid integer.");
            return;
        }

        try (Connection conn = DatabaseConnector.getConnection()) {
            LoggerUtil.logInfo("Database connection established for update operation.");

            String sql = "UPDATE student SET name=?, age=?, grade=?, contact=?, dob=? WHERE roll=? AND institute=?";
            PreparedStatement stmt = conn.prepareStatement(sql);

            stmt.setString(1, name);
            stmt.setInt(2, age);
            stmt.setString(3, grade);
            stmt.setString(4, contact);
            stmt.setDate(5, Date.valueOf(dob));
            stmt.setString(6, roll);
            stmt.setString(7, institute);

            LoggerUtil.logInfo("Executing update for student roll: " + roll);
            int rowsUpdated = stmt.executeUpdate();

            if (rowsUpdated > 0) {
                LoggerUtil.logInfo("Student updated successfully: Roll = " + roll + ", Institute = " + institute);
                showAlert("Success", "Student updated successfully.");
                clearFields();
            } else {
                LoggerUtil.logWarning("Update failed: No matching student found for roll = " + roll + ", Institute = " + institute);
                showAlert("Info", "No student found with roll: " + roll);
            }

        } catch (Exception e) {
            LoggerUtil.logSevere("Exception occurred while updating student: Roll = " + roll, e);
            showAlert("Error", "Failed to update student.");
        }
    }

    private void showAlert(String title, String message) {
        LoggerUtil.logInfo("Displaying alert - Title: " + title + ", Message: " + message);
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void clearFields() {
        LoggerUtil.logInfo("Clearing input fields after update.");
        rollField.clear();
        nameField.clear();
        ageField.clear();
        gradeField.clear();
        contactField.clear();
        dobPicker.setValue(null);
    }
}
