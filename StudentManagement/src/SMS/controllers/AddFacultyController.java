package SMS.controllers;

import SMS.db.DatabaseConnector;
import SMS.utils.LoggerUtil;
import SMS.utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.event.ActionEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement; 
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
public class AddFacultyController {

    @FXML private TextField nameField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private ListView<String> assignedGradesListView;
    @FXML private ListView<String> availableGradesListView;
    @FXML private Label statusLabel;

    private String currentInstitute;
    private ObservableList<String> allPossibleGrades; // All grades (1-12)

    @FXML
    public void initialize() {
        currentInstitute = SessionManager.getInstitute();
        if (currentInstitute == null) {
            showAlert("Session Error", "Institute information not found. Please log in as Admin.");
            LoggerUtil.logWarning("Institute null in AddFacultyController.initialize.");
            return;
        }
        LoggerUtil.logInfo("AddFacultyController initialized for institute: " + currentInstitute);
        statusLabel.setText(""); // Clear status label on init

        // Populate all possible grades (1-12)
        allPossibleGrades = FXCollections.observableArrayList();
        for (int i = 1; i <= 12; i++) {
            allPossibleGrades.add(String.valueOf(i));
        }
        Collections.sort(allPossibleGrades, new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                try {
                    return Integer.compare(Integer.parseInt(s1), Integer.parseInt(s2));
                } catch (NumberFormatException e) {
                    return s1.compareTo(s2);
                }
            }
        });
        availableGradesListView.setItems(allPossibleGrades);
        assignedGradesListView.setItems(FXCollections.observableArrayList()); // Initially empty
    }

    @FXML
    private void handleAssignGrade(ActionEvent event) {
        String selectedGrade = availableGradesListView.getSelectionModel().getSelectedItem();
        if (selectedGrade != null) {
            assignedGradesListView.getItems().add(selectedGrade);
            availableGradesListView.getItems().remove(selectedGrade);
            sortList(assignedGradesListView.getItems());
            statusLabel.setText("");
        } else {
            statusLabel.setText("Select a grade to assign.");
        }
    }

    @FXML
    private void handleUnassignGrade(ActionEvent event) {
        String selectedGrade = assignedGradesListView.getSelectionModel().getSelectedItem();
        if (selectedGrade != null) {
            availableGradesListView.getItems().add(selectedGrade);
            assignedGradesListView.getItems().remove(selectedGrade);
            sortList(availableGradesListView.getItems());
            statusLabel.setText("");
        } else {
            statusLabel.setText("Select a grade to unassign.");
        }
    }

    private void sortList(ObservableList<String> list) {
        Collections.sort(list, new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                try {
                    return Integer.compare(Integer.parseInt(s1), Integer.parseInt(s2));
                } catch (NumberFormatException e) {
                    return s1.compareTo(s2);
                }
            }
        });
    }

    @FXML
    private void handleAddFaculty(ActionEvent event) {
        String name = nameField.getText().trim();
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        List<String> assignedGrades = assignedGradesListView.getItems();

        statusLabel.setText(""); // Clear previous status message

        if (name.isEmpty() || username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please fill name, username, and password fields.");
            LoggerUtil.logWarning("Add Faculty failed: Missing core fields.");
            return;
        }

        Connection conn = null;
        try {
            conn = DatabaseConnector.getConnection();
            conn.setAutoCommit(false); // Start transaction

            // 1. Insert into faculty table
            String facultySql = "INSERT INTO faculty (username, password, name, institute) VALUES (?, ?, ?, ?)";
            PreparedStatement facultyStmt = conn.prepareStatement(facultySql, Statement.RETURN_GENERATED_KEYS);
            facultyStmt.setString(1, username);
            facultyStmt.setString(2, password); // HASH THIS PASSWORD IN PRODUCTION!
            facultyStmt.setString(3, name);
            facultyStmt.setString(4, currentInstitute);
            facultyStmt.executeUpdate();

            ResultSet rs = facultyStmt.getGeneratedKeys();
            int facultyID = -1;
            if (rs.next()) {
                facultyID = rs.getInt(1);
                LoggerUtil.logInfo("New faculty inserted with ID: " + facultyID);
            } else {
                throw new SQLException("Failed to retrieve faculty ID after insertion.");
            }

            // 2. Insert into faculty_assigned_grades table (if grades are assigned)
            if (!assignedGrades.isEmpty()) {
                String gradesSql = "INSERT INTO faculty_assigned_grades (facultyID, grade) VALUES (?, ?)";
                PreparedStatement gradesStmt = conn.prepareStatement(gradesSql);
                for (String grade : assignedGrades) {
                    gradesStmt.setInt(1, facultyID);
                    gradesStmt.setString(2, grade);
                    gradesStmt.addBatch(); // Add to batch for efficiency
                }
                gradesStmt.executeBatch(); // Execute all inserts
                LoggerUtil.logInfo("Assigned " + assignedGrades.size() + " grades to new faculty ID: " + facultyID);
            }

            conn.commit(); // Commit transaction

            statusLabel.setText("Faculty member '" + name + "' added and grades assigned successfully.");
            LoggerUtil.logInfo("Faculty added successfully: " + name + " (ID: " + facultyID + ", Username: " + username + ")");
            clearFields();

        } catch (SQLIntegrityConstraintViolationException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException rbEx) { LoggerUtil.logSevere("Rollback failed", rbEx); }
            }
            statusLabel.setText("Faculty with this username already exists. Please choose a different username.");
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException rbEx) { LoggerUtil.logSevere("Rollback failed", rbEx); }
            }
            statusLabel.setText("Database error: " + e.getMessage());
            LoggerUtil.logSevere("Database error adding faculty: " + username, e);
        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException rbEx) { LoggerUtil.logSevere("Rollback failed", rbEx); }
            }
            statusLabel.setText("An unexpected error occurred during faculty addition.");
            LoggerUtil.logSevere("Unexpected error adding faculty: " + username, e);
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException e) { LoggerUtil.logSevere("Error closing connection", e); }
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
        assignedGradesListView.getItems().clear();
        availableGradesListView.setItems(allPossibleGrades); // Reset available grades
    }
}
