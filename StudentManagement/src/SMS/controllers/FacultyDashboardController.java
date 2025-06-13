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

public class FacultyDashboardController {

    @FXML
    private AnchorPane contentPane;

    @FXML
    public void initialize() {
        if (!SessionManager.isLoggedIn() || !"faculty".equals(SessionManager.getUserRole())) {
            LoggerUtil.logWarning("No active faculty session. Redirecting to role selection/login.");
            redirectToRoleSelection();
        } else {
            LoggerUtil.logInfo("Faculty Dashboard loaded for user: " + SessionManager.getLoggedInUsername() + " (Institute: " + SessionManager.getInstitute() + ", Grades: " + SessionManager.getAssignedGrades() + ")");
            loadViewStudents();
        }
    }

    
    @FXML
    public void loadViewStudents() {
        try {
            AnchorPane pane = FXMLLoader.load(getClass().getResource("/SMS/views/view_students.fxml"));
            contentPane.getChildren().setAll(pane);
            LoggerUtil.logInfo("Loaded View Students view for Faculty Dashboard.");
        } catch (IOException e) {
            LoggerUtil.logSevere("Failed to load View Students view for Faculty Dashboard.", e);
            showAlert("Error", "Failed to load View Students page.");
        }
    }

    /**
     * Handles the logout action for faculty, clearing the session and redirecting to the role selection screen.
     */
    @FXML
    private void handleLogout() {
        redirectToRoleSelection(); 
        LoggerUtil.logInfo("Faculty logged out, returned to role selection page.");
    }

    /**
     * Redirects the user to the initial role selection screen and clears the session.
     */
    private void redirectToRoleSelection() {
        try {
            SessionManager.clearSession(); 
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/SMS/views/role_selection.fxml"));
            Parent roleSelectionPane = loader.load();

            Stage currentStage = (Stage) contentPane.getScene().getWindow();
            currentStage.setScene(new Scene(roleSelectionPane));
            currentStage.setTitle("Select Role");
            currentStage.show();

            LoggerUtil.logInfo("Redirected to role selection screen and closed faculty dashboard.");
        } catch (IOException e) {
            LoggerUtil.logSevere("Failed to redirect to role selection page from Faculty Dashboard.", e);
            showAlert("Application Error", "Failed to load the role selection screen.");
        } catch (Exception e) {
            LoggerUtil.logSevere("Error during redirection to role selection from Faculty Dashboard.", e);
            showAlert("Application Error", "An unexpected error occurred during redirection.");
        }
    }

    /**
     * Shows a simple information/error alert to the user.
     * @param title The title of the alert.
     * @param message The message content of the alert.
     */
    private void showAlert(String title, String message) {
        LoggerUtil.logInfo("Showing alert: " + title + " - " + message);
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
