package SMS.controllers;

import SMS.db.DatabaseConnector;
import SMS.utils.LoggerUtil;
import SMS.utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.event.ActionEvent;

import SMS.models.Faculty; // Assuming you have this model

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors; // Import Collectors


public class AssignGradesToFacultyController {

    @FXML private ComboBox<Faculty> facultyComboBox; // ComboBox to select faculty
    @FXML private ListView<String> assignedGradesListView; // List of grades already assigned
    @FXML private ListView<String> availableGradesListView; // List of grades available to assign
    @FXML private Label statusLabel;

    private String currentInstitute;
    private ObservableList<String> allPossibleGrades; // All grades (1-12)

    @FXML
    public void initialize() {
        currentInstitute = SessionManager.getInstitute();
        if (currentInstitute == null) {
            showAlert("Session Error", "Institute information not found. Please log in as Admin.");
            LoggerUtil.logWarning("Institute null in AssignGradesToFacultyController.initialize.");
            return;
        }
        LoggerUtil.logInfo("AssignGradesToFacultyController initialized for institute: " + currentInstitute);
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


        // Listen for faculty selection changes
        facultyComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadGradesForSelectedFaculty(newVal);
            } else {
                assignedGradesListView.setItems(FXCollections.emptyObservableList());
                availableGradesListView.setItems(allPossibleGrades); // Reset available grades
                statusLabel.setText("");
            }
        });

        loadFacultyMembers(); // Load faculty into the ComboBox
    }

    /**
     * Loads faculty members belonging to the current institute into the facultyComboBox.
     */
    private void loadFacultyMembers() {
        ObservableList<Faculty> facultyMembers = FXCollections.observableArrayList();
        try (Connection conn = DatabaseConnector.getConnection()) {
            String sql = "SELECT facultyID, username, name, institute FROM faculty WHERE institute = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, currentInstitute);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                // For simplicity, we're not loading assigned grades here for the ComboBox items.
                // Grades will be loaded specifically when a faculty is selected.
                facultyMembers.add(new Faculty(
                    rs.getInt("facultyID"),
                    rs.getString("username"),
                    rs.getString("name"),
                    rs.getString("institute"),
                    new ArrayList<>() // Empty list for initial ComboBox population
                ));
            }
            facultyComboBox.setItems(facultyMembers);
            // Set a custom StringConverter for the ComboBox to display faculty name/username
            facultyComboBox.setConverter(new javafx.util.StringConverter<Faculty>() {
                @Override
                public String toString(Faculty faculty) {
                    return faculty != null ? faculty.getName() + " (" + faculty.getUsername() + ")" : "";
                }

                @Override
                public Faculty fromString(String string) {
                    return null; // Not needed for this use case
                }
            });
            LoggerUtil.logInfo("Loaded " + facultyMembers.size() + " faculty members into ComboBox.");

        } catch (SQLException e) {
            LoggerUtil.logSevere("Database error loading faculty members for assignment", e);
            showAlert("Error", "Failed to load faculty members.");
        }
    }

    /**
     * Loads the grades assigned to the selected faculty member and updates the ListViews.
     * @param selectedFaculty The Faculty object whose grades are to be loaded.
     */
    private void loadGradesForSelectedFaculty(Faculty selectedFaculty) {
        ObservableList<String> assigned = FXCollections.observableArrayList();
        Set<String> assignedSet = new HashSet<>(); // Use a set for efficient lookup

        try (Connection conn = DatabaseConnector.getConnection()) {
            String sql = "SELECT grade FROM faculty_assigned_grades WHERE facultyID = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, selectedFaculty.getFacultyID());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String grade = rs.getString("grade");
                assigned.add(grade);
                assignedSet.add(grade);
            }
            Collections.sort(assigned, new Comparator<String>() {
                @Override
                public int compare(String s1, String s2) {
                    try {
                        return Integer.compare(Integer.parseInt(s1), Integer.parseInt(s2));
                    } catch (NumberFormatException e) {
                        return s1.compareTo(s2);
                    }
                }
            });
            assignedGradesListView.setItems(assigned);

            // Update available grades: filter out already assigned grades
            ObservableList<String> available = FXCollections.observableArrayList(
                allPossibleGrades.stream()
                                 .filter(grade -> !assignedSet.contains(grade))
                                 .collect(Collectors.toList())
            );
            availableGradesListView.setItems(available);
            LoggerUtil.logInfo("Loaded grades for faculty: " + selectedFaculty.getUsername() + ", Assigned: " + assigned.size() + ", Available: " + available.size());

        } catch (SQLException e) {
            LoggerUtil.logSevere("Database error loading assigned grades for faculty: " + selectedFaculty.getUsername(), e);
            showAlert("Error", "Failed to load assigned grades.");
        }
    }

    @FXML
    private void handleAssignGrade(ActionEvent event) {
        Faculty selectedFaculty = facultyComboBox.getSelectionModel().getSelectedItem();
        String selectedGrade = availableGradesListView.getSelectionModel().getSelectedItem();

        statusLabel.setText(""); // Clear previous status

        if (selectedFaculty == null) {
            statusLabel.setText("Please select a faculty member.");
            return;
        }
        if (selectedGrade == null) {
            statusLabel.setText("Please select a grade to assign.");
            return;
        }

        try (Connection conn = DatabaseConnector.getConnection()) {
            String sql = "INSERT INTO faculty_assigned_grades (facultyID, grade) VALUES (?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, selectedFaculty.getFacultyID());
            stmt.setString(2, selectedGrade);
            stmt.executeUpdate();

            statusLabel.setText("Grade " + selectedGrade + " assigned to " + selectedFaculty.getName() + ".");
            LoggerUtil.logInfo("Grade " + selectedGrade + " assigned to facultyID: " + selectedFaculty.getFacultyID());
            loadGradesForSelectedFaculty(selectedFaculty); // Refresh lists

        }  catch (SQLException e) {
            statusLabel.setText("Database error assigning grade: " + e.getMessage());
            LoggerUtil.logSevere("Database error assigning grade: " + selectedGrade + " to facultyID: " + selectedFaculty.getFacultyID(), e);
        } catch (Exception e) {
            statusLabel.setText("An unexpected error occurred during grade assignment.");
            LoggerUtil.logSevere("Unexpected error assigning grade: " + selectedGrade + " to facultyID: " + selectedFaculty.getFacultyID(), e);
        }
    }

    @FXML
    private void handleUnassignGrade(ActionEvent event) {
        Faculty selectedFaculty = facultyComboBox.getSelectionModel().getSelectedItem();
        String selectedGrade = assignedGradesListView.getSelectionModel().getSelectedItem();

        statusLabel.setText(""); // Clear previous status

        if (selectedFaculty == null) {
            statusLabel.setText("Please select a faculty member.");
            return;
        }
        if (selectedGrade == null) {
            statusLabel.setText("Please select a grade to unassign.");
            return;
        }

        try (Connection conn = DatabaseConnector.getConnection()) {
            String sql = "DELETE FROM faculty_assigned_grades WHERE facultyID = ? AND grade = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, selectedFaculty.getFacultyID());
            stmt.setString(2, selectedGrade);
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                statusLabel.setText("Grade " + selectedGrade + " unassigned from " + selectedFaculty.getName() + ".");
                LoggerUtil.logInfo("Grade " + selectedGrade + " unassigned from facultyID: " + selectedFaculty.getFacultyID());
                loadGradesForSelectedFaculty(selectedFaculty); // Refresh lists
            } else {
                statusLabel.setText("Failed to unassign grade (not found).");
                LoggerUtil.logWarning("Attempted to unassign non-existent grade: " + selectedGrade + " from facultyID: " + selectedFaculty.getFacultyID());
            }

        } catch (SQLException e) {
            statusLabel.setText("Database error unassigning grade: " + e.getMessage());
            LoggerUtil.logSevere("Database error unassigning grade: " + selectedGrade + " from facultyID: " + selectedFaculty.getFacultyID(), e);
        } catch (Exception e) {
            statusLabel.setText("An unexpected error occurred during grade unassignment.");
            LoggerUtil.logSevere("Unexpected error unassigning grade: " + selectedGrade + " from facultyID: " + selectedFaculty.getFacultyID(), e);
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
