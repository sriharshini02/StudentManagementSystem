package SMS.controllers;

import SMS.db.DatabaseConnector;
import SMS.models.Faculty;
import SMS.utils.LoggerUtil;
import SMS.utils.SessionManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.event.ActionEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ManageFacultyController {

    @FXML private TableView<Faculty> facultyTable;
    @FXML private TableColumn<Faculty, Integer> facultyIDColumn;
    @FXML private TableColumn<Faculty, String> nameColumn;
    @FXML private TableColumn<Faculty, String> usernameColumn;
    @FXML private TableColumn<Faculty, String> instituteColumn;
    @FXML private TableColumn<Faculty, String> assignedGradesDisplayColumn; 
    @FXML private TextField facultyIDReadonlyField;
    @FXML private TextField nameField;
    @FXML private TextField usernameField;

    @FXML private Label statusLabel;

    private String currentInstitute;
    private Faculty selectedFaculty;

    @FXML
    public void initialize() {
        currentInstitute = SessionManager.getInstitute();
        if (currentInstitute == null) {
            showAlert("Session Error", "Institute information not found. Please log in as Admin.");
            LoggerUtil.logWarning("Institute null in ManageFacultyController.initialize.");
            return;
        }
        LoggerUtil.logInfo("ManageFacultyController initialized for institute: " + currentInstitute);
        statusLabel.setText(""); 
        facultyIDColumn.setCellValueFactory(new PropertyValueFactory<>("facultyID"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        instituteColumn.setCellValueFactory(new PropertyValueFactory<>("institute"));
        assignedGradesDisplayColumn.setCellValueFactory(cellData -> {
            List<String> grades = cellData.getValue().getAssignedGrades();
            return new javafx.beans.property.SimpleStringProperty(
                grades.isEmpty() ? "None" : grades.stream().collect(Collectors.joining(", "))
            );
        });
        facultyTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedFaculty = newVal; 
            if (newVal != null) {
                populateFields(newVal);
                 } else {
                clearFields();
            }
        });

        handleRefreshFaculty(); 
       }

    @FXML
    private void handleRefreshFaculty() {
        LoggerUtil.logInfo("Refreshing faculty list for institute: " + currentInstitute);
        ObservableList<Faculty> faculties = FXCollections.observableArrayList();

        try (Connection conn = DatabaseConnector.getConnection()) {
            String sql = "SELECT facultyID, username, name, institute FROM faculty WHERE institute = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, currentInstitute);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int facultyID = rs.getInt("facultyID");
                List<String> assignedGrades = new ArrayList<>();
                // Fetch assigned grades for the current faculty member
                String gradesSql = "SELECT grade FROM faculty_assigned_grades WHERE facultyID = ?";
                try (PreparedStatement gradesStmt = conn.prepareStatement(gradesSql)) {
                    gradesStmt.setInt(1, facultyID);
                    ResultSet gradesRs = gradesStmt.executeQuery();
                    while(gradesRs.next()) {
                        assignedGrades.add(gradesRs.getString("grade"));
                    }
                }
                faculties.add(new Faculty(
                    facultyID,
                    rs.getString("username"),
                    rs.getString("name"),
                    rs.getString("institute"),
                    assignedGrades
                ));
            }
            Platform.runLater(() -> {
                facultyTable.setItems(faculties);
                if (faculties.isEmpty()) {
                    statusLabel.setText("No faculty members found for your institute.");
                } else {
                    statusLabel.setText("Faculty list refreshed. " + faculties.size() + " members found.");
                }
            });
            LoggerUtil.logInfo("Total faculties loaded: " + faculties.size());

        } catch (SQLException e) {
            LoggerUtil.logSevere("Database error retrieving faculty list", e);
            showAlert("Error", "Failed to retrieve faculty list: " + e.getMessage());
            Platform.runLater(() -> statusLabel.setText("Error loading faculty: " + e.getMessage()));
        } catch (Exception e) {
            LoggerUtil.logSevere("An unexpected error occurred in ManageFacultyController.handleRefreshFaculty", e);
            showAlert("Error", "An unexpected error occurred while refreshing faculty list.");
            Platform.runLater(() -> statusLabel.setText("Unexpected error loading faculty."));
        }
    }

    private void populateFields(Faculty faculty) {
        facultyIDReadonlyField.setText(String.valueOf(faculty.getFacultyID()));
        nameField.setText(faculty.getName());
        usernameField.setText(faculty.getUsername());
        statusLabel.setText("");
    }

    private void clearFields() {
        facultyIDReadonlyField.clear();
        nameField.clear();
        usernameField.clear();
        statusLabel.setText("");
        selectedFaculty = null; // Clear selected faculty reference
    }

    @FXML
    private void handleUpdateFacultyDetails(ActionEvent event) {
        if (selectedFaculty == null) {
            statusLabel.setText("Please select a faculty member from the table to update.");
            return;
        }

        String newName = nameField.getText().trim();
        String newUsername = usernameField.getText().trim();

        if (newName.isEmpty() || newUsername.isEmpty()) {
            statusLabel.setText("Name and Username cannot be empty.");
            return;
        }

        // Check if username is changed and if it's unique
        if (!newUsername.equals(selectedFaculty.getUsername())) {
            try (Connection conn = DatabaseConnector.getConnection()) {
                String checkSql = "SELECT COUNT(*) FROM faculty WHERE username = ? AND facultyID != ?";
                PreparedStatement checkStmt = conn.prepareStatement(checkSql);
                checkStmt.setString(1, newUsername);
                checkStmt.setInt(2, selectedFaculty.getFacultyID());
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    statusLabel.setText("Username '" + newUsername + "' is already taken. Please choose another.");
                    return;
                }
            } catch (SQLException e) {
                LoggerUtil.logSevere("Database error checking username uniqueness", e);
                showAlert("Error", "Database error during username check.");
                statusLabel.setText("Database error during username check.");
                return;
            }
        }

        Connection conn = null; // Declare connection for finally block
        try {
            conn = DatabaseConnector.getConnection();
            String sql = "UPDATE faculty SET name = ?, username = ? WHERE facultyID = ? AND institute = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, newName);
            stmt.setString(2, newUsername);
            stmt.setInt(3, selectedFaculty.getFacultyID());
            stmt.setString(4, currentInstitute);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                statusLabel.setText("Faculty details updated successfully.");
                LoggerUtil.logInfo("Faculty (ID: " + selectedFaculty.getFacultyID() + ") updated details.");
                // Update the local Faculty object with new details
                selectedFaculty.setName(newName);
                selectedFaculty.setUsername(newUsername);
                handleRefreshFaculty(); // Refresh table to show updated details
            } else {
                statusLabel.setText("Failed to update faculty details. No matching record found.");
                LoggerUtil.logWarning("Update faculty details failed: No record found for ID: " + selectedFaculty.getFacultyID());
            }
        } catch (SQLException e) {
            LoggerUtil.logSevere("Database error updating faculty details for ID: " + selectedFaculty.getFacultyID(), e);
            showAlert("Error", "Database error updating faculty details: " + e.getMessage());
            statusLabel.setText("Database error updating details.");
        } catch (Exception e) {
            LoggerUtil.logSevere("An unexpected error occurred during faculty details update for ID: " + selectedFaculty.getFacultyID(), e);
            showAlert("Error", "An unexpected error occurred.");
            statusLabel.setText("An unexpected error occurred.");
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException e) { LoggerUtil.logSevere("Error closing connection", e); }
            }
        }
    }

    @FXML
    private void handleDeleteFaculty(ActionEvent event) {
        if (selectedFaculty == null) {
            statusLabel.setText("Please select a faculty member from the table to delete.");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Deletion");
        confirmAlert.setHeaderText("Delete Faculty Member");
        confirmAlert.setContentText("Are you sure you want to delete '" + selectedFaculty.getName() + "' (ID: " + selectedFaculty.getFacultyID() + ")? This action cannot be undone.");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Connection conn = null;
            try {
                conn = DatabaseConnector.getConnection();
                conn.setAutoCommit(false); // Start transaction

                // Delete from faculty_assigned_grades first
                String deleteGradesSql = "DELETE FROM faculty_assigned_grades WHERE facultyID = ?";
                PreparedStatement deleteGradesStmt = conn.prepareStatement(deleteGradesSql);
                deleteGradesStmt.setInt(1, selectedFaculty.getFacultyID());
                deleteGradesStmt.executeUpdate();
                LoggerUtil.logInfo("Deleted assigned grades for faculty ID: " + selectedFaculty.getFacultyID());

                // Delete from faculty table
                String deleteFacultySql = "DELETE FROM faculty WHERE facultyID = ? AND institute = ?";
                PreparedStatement deleteFacultyStmt = conn.prepareStatement(deleteFacultySql);
                deleteFacultyStmt.setInt(1, selectedFaculty.getFacultyID());
                deleteFacultyStmt.setString(2, currentInstitute);
                int rowsAffected = deleteFacultyStmt.executeUpdate();

                if (rowsAffected > 0) {
                    conn.commit(); // Commit transaction
                    statusLabel.setText("Faculty member '" + selectedFaculty.getName() + "' deleted successfully.");
                    LoggerUtil.logInfo("Faculty deleted: " + selectedFaculty.getName() + " (ID: " + selectedFaculty.getFacultyID() + ")");
                    handleRefreshFaculty(); // Refresh table
                    clearFields(); // Clear input fields
                } else {
                    conn.rollback(); // Rollback if no faculty was deleted (shouldn't happen if selected)
                    statusLabel.setText("Failed to delete faculty. Record not found or unauthorized.");
                    LoggerUtil.logWarning("Delete faculty failed: No record found for ID: " + selectedFaculty.getFacultyID() + " or unauthorized.");
                }

            } catch (SQLException e) {
                if (conn != null) {
                    try { conn.rollback(); } catch (SQLException rbEx) { LoggerUtil.logSevere("Rollback failed during delete", rbEx); }
                }
                LoggerUtil.logSevere("Database error deleting faculty ID: " + selectedFaculty.getFacultyID(), e);
                showAlert("Error", "Database error deleting faculty: " + e.getMessage());
                statusLabel.setText("Database error deleting faculty.");
            } catch (Exception e) {
                LoggerUtil.logSevere("An unexpected error occurred during faculty deletion for ID: " + selectedFaculty.getFacultyID(), e);
                showAlert("Error", "An unexpected error occurred during deletion.");
                statusLabel.setText("An unexpected error occurred during deletion.");
            } finally {
                if (conn != null) {
                    try { conn.close(); } catch (SQLException e) { LoggerUtil.logSevere("Error closing connection during delete", e); }
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
}
