package SMS;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class MainApp extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            // Load the new role selection FXML first
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/SMS/views/role_selection.fxml"));
            Scene scene = new Scene(loader.load());
            primaryStage.setScene(scene);
            primaryStage.setTitle("Select Role"); // Set a title for the initial screen
            primaryStage.show();
        } catch (IOException e) {
            System.err.println("Failed to load role_selection.fxml: " + e.getMessage());
            e.printStackTrace();
            // Optionally, show an alert to the user
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Application Error");
            alert.setHeaderText("Failed to start application");
            alert.setContentText("The initial screen could not be loaded. Please contact support.");
            alert.showAndWait();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
