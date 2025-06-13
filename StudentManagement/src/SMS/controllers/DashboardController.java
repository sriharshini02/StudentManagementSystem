package SMS.controllers;

import SMS.utils.LoggerUtil;
import SMS.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.scene.control.Alert;

import java.io.IOException;

public class DashboardController {

    @FXML
    private AnchorPane contentPane;

    @FXML
    public void initialize() {
        if (!SessionManager.isLoggedIn() || !"admin".equals(SessionManager.getUserRole())) {
            LoggerUtil.logWarning("Unauthorized access attempt to Admin Dashboard. Redirecting to role selection/login.");
            redirectToRoleSelection();
        } else {
            LoggerUtil.logInfo("Admin Dashboard loaded for user: " + SessionManager.getLoggedInUsername() + " (Role: " + SessionManager.getUserRole() + ") at Institute: " + SessionManager.getInstitute());
            }
    }

    private void redirectToRoleSelection() {
        try {
            SessionManager.clearSession();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/SMS/views/role_selection.fxml"));
            Parent roleSelectionPane = loader.load();

            Stage currentStage = (Stage) contentPane.getScene().getWindow();
            currentStage.setScene(new Scene(roleSelectionPane));
            currentStage.setTitle("Select Role");
            currentStage.show();

            LoggerUtil.logInfo("Redirected to role selection screen and closed dashboard.");
        } catch (IOException e) {
            LoggerUtil.logSevere("Failed to redirect to role selection page.", e);
            showAlert("Application Error", "Failed to load the role selection screen.");
        } catch (Exception e) {
            LoggerUtil.logSevere("Error during redirection to role selection.", e);
            showAlert("Application Error", "An unexpected error occurred during redirection.");
        }
    }

    @FXML
    public void loadAddStudent() {
        if (!"admin".equals(SessionManager.getUserRole())) {
            showAlert("Access Denied", "Only administrators can add new students.");
            LoggerUtil.logWarning("Unauthorized access attempt to Add Student by role: " + SessionManager.getUserRole());
            return;
        }
        try {
            AnchorPane pane = FXMLLoader.load(getClass().getResource("/SMS/views/add_student.fxml"));
            contentPane.getChildren().setAll(pane);
            LoggerUtil.logInfo("Loaded Add Student view.");
        } catch (IOException e) {
            LoggerUtil.logSevere("Failed to load Add Student view.", e);
            showAlert("Error", "Failed to load Add Student page.");
        }
    }

    @FXML
    public void loadUpdateStudent() {
        if (!"admin".equals(SessionManager.getUserRole())) {
            showAlert("Access Denied", "Only administrators can update student information.");
            LoggerUtil.logWarning("Unauthorized access attempt to Update Student by role: " + SessionManager.getUserRole());
            return;
        }
        try {
            AnchorPane pane = FXMLLoader.load(getClass().getResource("/SMS/views/update_student.fxml"));
            contentPane.getChildren().setAll(pane);
            LoggerUtil.logInfo("Loaded Update Student view.");
        } catch (IOException e) {
            LoggerUtil.logSevere("Failed to load Update Student view.", e);
            showAlert("Error", "Failed to load Update Student page.");
        }
    }

    @FXML
    public void loadDeleteStudent() {
        if (!"admin".equals(SessionManager.getUserRole())) {
            showAlert("Access Denied", "Only administrators can delete students.");
            LoggerUtil.logWarning("Unauthorized access attempt to Delete Student by role: " + SessionManager.getUserRole());
            return;
        }
        try {
            AnchorPane pane = FXMLLoader.load(getClass().getResource("/SMS/views/delete_student.fxml"));
            contentPane.getChildren().setAll(pane);
            LoggerUtil.logInfo("Loaded Delete Student view.");
        }  catch (IOException e) {
            LoggerUtil.logSevere("Failed to load Delete Student view.", e);
            showAlert("Error", "Failed to load Delete Student page.");
        }
    }

    @FXML
    public void loadViewStudents() {
        try {
            AnchorPane pane = FXMLLoader.load(getClass().getResource("/SMS/views/view_students.fxml"));
            contentPane.getChildren().setAll(pane);
            LoggerUtil.logInfo("Loaded View Students view.");
        } catch (IOException e) {
            LoggerUtil.logSevere("Failed to load View Students view.", e);
            showAlert("Error", "Failed to load View Students page.");
        }
    }

    @FXML
    public void loadAddFaculty() {
        if (!"admin".equals(SessionManager.getUserRole())) {
            showAlert("Access Denied", "Only administrators can add new faculty.");
            LoggerUtil.logWarning("Unauthorized access attempt to Add Faculty by role: " + SessionManager.getUserRole());
            return;
        }
        try {
            AnchorPane pane = FXMLLoader.load(getClass().getResource("/SMS/views/add_faculty.fxml"));
            contentPane.getChildren().setAll(pane);
            LoggerUtil.logInfo("Loaded Add Faculty view.");
        } catch (IOException e) {
            LoggerUtil.logSevere("Failed to load Add Faculty view.", e);
            showAlert("Error", "Failed to load Add Faculty page.");
        }
    }

    @FXML
    public void loadViewFaculty() {
        if (!"admin".equals(SessionManager.getUserRole())) {
            showAlert("Access Denied", "Only administrators can view faculty details.");
            LoggerUtil.logWarning("Unauthorized access attempt to View Faculty by role: " + SessionManager.getUserRole());
            return;
        }
        try {
            AnchorPane pane = FXMLLoader.load(getClass().getResource("/SMS/views/view_faculty.fxml"));
            contentPane.getChildren().setAll(pane);
            LoggerUtil.logInfo("Loaded View Faculty view.");
        } catch (IOException e) {
            LoggerUtil.logSevere("Failed to load View Faculty view.", e);
            showAlert("Error", "Failed to load View Faculty page.");
        }
    }

    @FXML
    public void loadManageFaculty() {
        if (!"admin".equals(SessionManager.getUserRole())) {
            showAlert("Access Denied", "Only administrators can manage faculty details (update/delete).");
            LoggerUtil.logWarning("Unauthorized access attempt to Manage Faculty by role: " + SessionManager.getUserRole());
            return;
        }
        try {
            AnchorPane pane = FXMLLoader.load(getClass().getResource("/SMS/views/manage_faculty.fxml"));
            contentPane.getChildren().setAll(pane);
            LoggerUtil.logInfo("Loaded Manage Faculty view.");
        } catch (IOException e) {
            LoggerUtil.logSevere("Failed to load Manage Faculty view.", e);
            showAlert("Error", "Failed to load Manage Faculty page.");
        }
    }

    /**
     * Re-added: Loads the "Assign Grades to Faculty" view into the content pane.
     * Accessible only by Admin.
     */
    @FXML
    public void loadAssignGradesToFaculty() {
        if (!"admin".equals(SessionManager.getUserRole())) {
            showAlert("Access Denied", "Only administrators can assign grades to faculty.");
            LoggerUtil.logWarning("Unauthorized access attempt to Assign Grades to Faculty by role: " + SessionManager.getUserRole());
            return;
        }
        try {
            AnchorPane pane = FXMLLoader.load(getClass().getResource("/SMS/views/assign_grades_to_faculty.fxml"));
            contentPane.getChildren().setAll(pane);
            LoggerUtil.logInfo("Loaded Assign Grades to Faculty view.");
        } catch (IOException e) {
            LoggerUtil.logSevere("Failed to load Assign Grades to Faculty view.", e);
            showAlert("Error", "Failed to load Assign Grades to Faculty page.");
        }
    }

    @FXML
    private void handleLogout() {
        redirectToRoleSelection();
        LoggerUtil.logInfo("User logged out, returned to role selection page.");
    }

    private void showAlert(String title, String message) {
        LoggerUtil.logInfo("Showing alert: " + title + " - " + message);
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
