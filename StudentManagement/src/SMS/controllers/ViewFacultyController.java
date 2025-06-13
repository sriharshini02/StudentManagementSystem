package SMS.controllers;

import SMS.db.DatabaseConnector;
import SMS.utils.LoggerUtil;
import SMS.utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import SMS.models.Faculty; // Assuming you have this model now

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors; // For joining grades

public class ViewFacultyController {

    @FXML private TableView<Faculty> facultyTable;
    @FXML private TableColumn<Faculty, Integer> facultyIDColumn;
    @FXML private TableColumn<Faculty, String> nameColumn;
    @FXML private TableColumn<Faculty, String> usernameColumn;
    @FXML private TableColumn<Faculty, String> instituteColumn;
    @FXML private TableColumn<Faculty, String> assignedGradesColumn; // New column for assigned grades

    private String currentInstitute;

    @FXML
    public void initialize() {
        LoggerUtil.logInfo("Initializing ViewFacultyController table columns.");

        currentInstitute = SessionManager.getInstitute();
        if (currentInstitute == null) {
            showAlert("Session Error", "Institute information not found. Please log in again.");
            LoggerUtil.logWarning("Institute null in ViewFacultyController.initialize. Redirecting to login.");
            // Consider redirecting to login here if this view is critical on institute
            return;
        }

        // Set up cell value factories
        facultyIDColumn.setCellValueFactory(new PropertyValueFactory<>("facultyID"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        instituteColumn.setCellValueFactory(new PropertyValueFactory<>("institute"));
        // For the assignedGradesColumn, we need a custom cell value factory
        assignedGradesColumn.setCellValueFactory(cellData -> {
            List<String> grades = cellData.getValue().getAssignedGrades();
            return new javafx.beans.property.SimpleStringProperty(
                grades.isEmpty() ? "None" : grades.stream().collect(Collectors.joining(", "))
            );
        });


        handleRefreshFaculty(); // Load faculty data on initialization
    }

    @FXML
    private void handleRefreshFaculty() {
        LoggerUtil.logInfo("Fetching faculty list for institute: " + currentInstitute);
        ObservableList<Faculty> faculties = FXCollections.observableArrayList();

        if (currentInstitute == null) {
            // This check is already in initialize, but good to have for direct calls
            showAlert("Session Error", "Institute information not found. Please log in again.");
            LoggerUtil.logWarning("Institute null during handleRefreshFaculty.");
            return;
        }

        try (Connection conn = DatabaseConnector.getConnection()) {
            // Select all faculty members for the current institute
            String sql = "SELECT facultyID, username, name, institute FROM faculty WHERE institute = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, currentInstitute);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int facultyID = rs.getInt("facultyID");
                
                // Fetch assigned grades for the current faculty member
                List<String> assignedGrades = new ArrayList<>();
                String gradesSql = "SELECT grade FROM faculty_assigned_grades WHERE facultyID = ?";
                try (PreparedStatement gradesStmt = conn.prepareStatement(gradesSql)) {
                    gradesStmt.setInt(1, facultyID);
                    ResultSet gradesRs = gradesStmt.executeQuery();
                    while(gradesRs.next()) {
                        assignedGrades.add(gradesRs.getString("grade"));
                    }
                }
                
                faculties.add(new Faculty(
                    rs.getInt("facultyID"),
                    rs.getString("username"),
                    rs.getString("name"),
                    rs.getString("institute"),
                    assignedGrades // Pass assigned grades to the Faculty model
                ));
            }
            facultyTable.setItems(faculties);
            LoggerUtil.logInfo("Total faculties retrieved for institute " + currentInstitute + ": " + faculties.size());

        } catch (SQLException e) {
            LoggerUtil.logSevere("Database error retrieving faculty list", e);
            showAlert("Error", "Failed to retrieve faculty list: " + e.getMessage());
        } catch (Exception e) {
            LoggerUtil.logSevere("Error in ViewFacultyController.handleRefreshFaculty", e);
            showAlert("Error", "An unexpected error occurred while refreshing faculty list.");
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
