package SMS.controllers;

import SMS.db.DatabaseConnector;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.event.ActionEvent;
import SMS.utils.LoggerUtil;
import SMS.utils.SessionManager;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLIntegrityConstraintViolationException;

public class AddStudentController {

    private String adminInstitute = SessionManager.getInstitute();

    @FXML private TextField rollField;
    @FXML private TextField nameField;
    @FXML private TextField ageField;
    @FXML private TextField gradeField;
    @FXML private TextField contactField;
    @FXML private DatePicker dobPicker;

    @FXML
    private void handleAddStudent(ActionEvent event) {
        LoggerUtil.logInfo("Add Student process initiated by admin from institute: " + adminInstitute);

        String roll = rollField.getText().trim();
        String name = nameField.getText().trim();
        String ageStr = ageField.getText().trim();
        String grade = gradeField.getText().trim();
        String contact = contactField.getText().trim();
        Date dob = (dobPicker.getValue() != null) ? Date.valueOf(dobPicker.getValue()) : null;

        if (roll.isEmpty() || name.isEmpty() || ageStr.isEmpty() || grade.isEmpty() || contact.isEmpty() || dob == null) {
            LoggerUtil.logWarning("Validation failed: One or more fields are empty.");
            showAlert("Validation Error", "Please fill all fields.");
            return;
        }

        int age;
        try {
            age = Integer.parseInt(ageStr);
            if (age <= 0) {
                LoggerUtil.logWarning("Validation failed: Age must be positive. Entered: " + ageStr);
                showAlert("Validation Error", "Age must be a positive integer.");
                return;
            }
        } catch (NumberFormatException e) {
            LoggerUtil.logWarning("Validation failed: Age is not a number. Entered: " + ageStr);
            showAlert("Validation Error", "Age must be a valid integer.");
            return;
        }

        try (Connection conn = DatabaseConnector.getConnection()) {
            LoggerUtil.logInfo("Database connection established successfully.");

            String sql = "INSERT INTO student (roll, name, age, grade, contact, dob, institute) VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);

            stmt.setString(1, roll);
            stmt.setString(2, name);
            stmt.setInt(3, age);
            stmt.setString(4, grade);
            stmt.setString(5, contact);
            stmt.setDate(6, dob);
            stmt.setString(7, adminInstitute);

            stmt.executeUpdate();

            LoggerUtil.logInfo("Student added successfully: Roll = " + roll + ", Name = " + name + ", Institute = " + adminInstitute);
            showAlert("Success", "Student added successfully.");
            clearFields();

        } catch (SQLIntegrityConstraintViolationException e) {
            LoggerUtil.logWarning("Duplicate student entry detected for roll: " + roll + ", institute: " + adminInstitute);
            showAlert("Error", "Duplicate roll number for the institute.");
        } catch (Exception e) {
            LoggerUtil.logSevere("Exception occurred while adding student to the database", e);
            showAlert("Error", "Failed to add student.");
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
        LoggerUtil.logInfo("Clearing input fields after student addition.");
        rollField.clear();
        nameField.clear();
        ageField.clear();
        gradeField.clear();
        contactField.clear();
        dobPicker.setValue(null);
    }
}
