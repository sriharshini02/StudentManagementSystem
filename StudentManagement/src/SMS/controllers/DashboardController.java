package SMS.controllers;

import SMS.utils.LoggerUtil;
import SMS.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.io.IOException;

public class DashboardController {

    @FXML
    private AnchorPane contentPane;

    @FXML
    public void initialize() {
        if (SessionManager.getUsername() == null || SessionManager.getInstitute() == null) {
            LoggerUtil.logWarning("No active session. Redirecting to login.");
            redirectToLogin();
        } else {
            LoggerUtil.logInfo("Dashboard loaded for user: " + SessionManager.getUsername());
        }
    }

    private void redirectToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/SMS/views/login.fxml"));
            Parent loginPane = loader.load(); 

            Scene scene = new Scene(loginPane);
            Stage stage = new Stage();
            stage.setScene(scene);
            stage.setTitle("Login");
            stage.show();

            Stage currentStage = (Stage) contentPane.getScene().getWindow();
            currentStage.close();

            LoggerUtil.logInfo("Redirected to login page and closed dashboard.");
        } catch (IOException e) {
            LoggerUtil.logSevere("Failed to redirect to login page.", e);
        }
    }

    public void loadAddStudent() {
        try {
            AnchorPane pane = FXMLLoader.load(getClass().getResource("/SMS/views/add_student.fxml"));
            contentPane.getChildren().setAll(pane);
            LoggerUtil.logInfo("Loaded Add Student view.");
        } catch (IOException e) {
            LoggerUtil.logSevere("Failed to load Add Student view.", e);
        }
    }

    public void loadUpdateStudent() {
        try {
            AnchorPane pane = FXMLLoader.load(getClass().getResource("/SMS/views/update_student.fxml"));
            contentPane.getChildren().setAll(pane);
            LoggerUtil.logInfo("Loaded Update Student view.");
        } catch (IOException e) {
            LoggerUtil.logSevere("Failed to load Update Student view.", e);
        }
    }

    public void loadDeleteStudent() {
        try {
            AnchorPane pane = FXMLLoader.load(getClass().getResource("/SMS/views/delete_student.fxml"));
            contentPane.getChildren().setAll(pane);
            LoggerUtil.logInfo("Loaded Delete Student view.");
        } catch (IOException e) {
            LoggerUtil.logSevere("Failed to load Delete Student view.", e);
        }
    }

    public void loadViewStudents() {
        try {
            AnchorPane pane = FXMLLoader.load(getClass().getResource("/SMS/views/view_students.fxml"));
            contentPane.getChildren().setAll(pane);
            LoggerUtil.logInfo("Loaded View Students view.");
        } catch (IOException e) {
            LoggerUtil.logSevere("Failed to load View Students view.", e);
        }
    }
    
    @FXML
    private void handleLogout() {
        SessionManager.clearSession();
        LoggerUtil.logInfo("Admin logged out, returned to login page.");
        redirectToLogin();        
    }
}
