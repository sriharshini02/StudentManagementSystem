package SMS.controllers;

import SMS.db.DatabaseConnector;
import SMS.utils.LoggerUtil;
import SMS.utils.SessionManager;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Node;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javafx.event.ActionEvent;

public class FacultyLoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();

        LoggerUtil.logInfo("Faculty Login attempt by username: " + username);

        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Validation Error", "Please enter both username and password.");
            LoggerUtil.logWarning("Faculty login failed: Empty fields.");
            return;
        }

        try (Connection conn = DatabaseConnector.getConnection()) {
            // Query the faculty table to validate credentials
            String facultySql = "SELECT facultyID, username, institute FROM faculty WHERE username = ? AND password = ?";
            try (PreparedStatement facultyStmt = conn.prepareStatement(facultySql)) {
                facultyStmt.setString(1, username);
                facultyStmt.setString(2, password);
                ResultSet rs = facultyStmt.executeQuery();

                if (rs.next()) {
                    int facultyID = rs.getInt("facultyID");
                    String facultyUsername = rs.getString("username");
                    String institute = rs.getString("institute");

                    // Retrieve assigned grades for this faculty
                    List<String> assignedGrades = new ArrayList<>();
                    String gradesSql = "SELECT grade FROM faculty_assigned_grades WHERE facultyID = ?";
                    try (PreparedStatement gradesStmt = conn.prepareStatement(gradesSql)) {
                        gradesStmt.setInt(1, facultyID);
                        ResultSet gradesRs = gradesStmt.executeQuery();
                        while (gradesRs.next()) {
                            assignedGrades.add(gradesRs.getString("grade"));
                        }
                    }

                    // Set the faculty session using the updated SessionManager
                    SessionManager.setFacultySession(facultyUsername, institute, assignedGrades);

                    LoggerUtil.logInfo("Faculty Login successful for user: " + facultyUsername + " from institute: " + institute + " for grades: " + assignedGrades);

                    // FIX: Load the faculty_dashboard.fxml instead of dashboard.fxml
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/SMS/views/faculty_dashboard.fxml"));
                    Parent root = loader.load();

                    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                    stage.setScene(new Scene(root));
                    stage.setTitle("Faculty Dashboard"); // Specific title for faculty
                    stage.show();

                } else {
                    LoggerUtil.logWarning("Faculty Login failed: Invalid credentials for username: " + username);
                    showAlert("Login Failed", "Invalid username or password.");
                }
            }
        } catch (SQLException e) {
            LoggerUtil.logSevere("Database error during faculty login for user: " + username, e);
            showAlert("Error", "Database error occurred during login. Please try again later.");
        } catch (IOException e) {
            // Updated log message to reflect faculty_dashboard.fxml
            LoggerUtil.logSevere("Failed to load faculty_dashboard.fxml after faculty login for user: " + username, e);
            showAlert("Navigation Error", "Could not load the faculty dashboard screen.");
        } catch (Exception e) {
            LoggerUtil.logSevere("An unexpected error occurred during faculty login for user: " + username, e);
            showAlert("Error", "An unexpected error occurred.");
        }
    }

    /**
     * Displays an alert dialog to the user.
     * @param title The title of the alert.
     * @param message The message content of the alert.
     */
    private void showAlert(String title, String message) {
        LoggerUtil.logInfo("Showing alert: " + title + " - " + message);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Navigates back to the initial role selection screen.
     * @param event The ActionEvent that triggered this method.
     */
    @FXML
    private void goToRoleSelection(ActionEvent event) {
        LoggerUtil.logInfo("Navigating back to role selection screen from Faculty Login.");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/SMS/views/role_selection.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Select Role");
            stage.show();
        } catch (IOException e) {
            LoggerUtil.logSevere("Failed to load role selection screen from Faculty Login.", e);
            showAlert("Navigation Error", "Could not load the role selection screen.");
        }
    }
}
