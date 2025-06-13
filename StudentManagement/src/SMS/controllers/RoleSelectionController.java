package SMS.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import SMS.utils.LoggerUtil; 

import java.io.IOException;

public class RoleSelectionController {

    @FXML
    private void handleAdminLogin(ActionEvent event) {
        LoggerUtil.logInfo("Admin login selected. Navigating to admin login screen.");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/SMS/views/login.fxml"));
            Parent root = loader.load(); 

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Admin Login");
            stage.show();
        } catch (IOException e) {
            LoggerUtil.logSevere("Failed to load admin login screen.", e);
            showAlert("Navigation Error", "Could not load the Admin Login screen.");
        }
    }

    @FXML
    private void handleFacultyLogin(ActionEvent event) {
        LoggerUtil.logInfo("Faculty login selected. Navigating to faculty login screen.");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/SMS/views/faculty_login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Faculty Login");
            stage.show();
        } catch (IOException e) {
            LoggerUtil.logSevere("Failed to load faculty login screen.", e);
            showAlert("Navigation Error", "Could not load the Faculty Login screen.");
        }
    }

    private void showAlert(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
