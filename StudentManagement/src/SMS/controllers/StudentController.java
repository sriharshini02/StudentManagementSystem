package SMS.controllers;

import SMS.db.DatabaseConnector;
import SMS.utils.LoggerUtil;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.*;
import java.time.LocalDate;
import SMS.models.Student;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Alert;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;

public class StudentController {

    @FXML
    private TextField rollField, nameField, ageField, gradeField, contactField, instituteField;
    @FXML
    private DatePicker dobPicker;
    @FXML
    private TextArea outputArea;
    @FXML
    private TableView<Student> studentTable;

    @FXML
    private TableColumn<Student, String> rollColumn;
    @FXML
    private TableColumn<Student, String> nameColumn;
    @FXML
    private TableColumn<Student, Integer> ageColumn;
    @FXML
    private TableColumn<Student, String> gradeColumn;
    @FXML
    private TableColumn<Student, String> contactColumn;
    @FXML
    private TableColumn<Student, LocalDate> dobColumn;
    @FXML
    private TableColumn<Student, String> instituteColumn;

    @FXML
    public void initialize() {
        LoggerUtil.logInfo("Initializing StudentController table bindings.");
        rollColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getRoll()));
        nameColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getName()));
        ageColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getAge()));
        gradeColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getGrade()));
        contactColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getContact()));
        dobColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getDob()));
        instituteColumn.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(cellData.getValue().getInstitute()));
    }

    public void handleAddStudent() {
        LoggerUtil.logInfo("handleAddStudent() invoked.");
        try (Connection conn = DatabaseConnector.getConnection()) {
            String sql = "INSERT INTO student (roll, name, age, grade, contact, dob, institute) VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, rollField.getText());
            stmt.setString(2, nameField.getText());
            stmt.setInt(3, Integer.parseInt(ageField.getText()));
            stmt.setString(4, gradeField.getText());
            stmt.setString(5, contactField.getText());
            stmt.setDate(6, Date.valueOf(dobPicker.getValue()));
            stmt.setString(7, instituteField.getText());

            LoggerUtil.logInfo("Executing INSERT for roll=" + rollField.getText() + ", name=" + nameField.getText());

            stmt.executeUpdate();
            LoggerUtil.logInfo("Student added successfully: " + nameField.getText());
            showAlert("Success", "Student added successfully.");
        } catch (SQLIntegrityConstraintViolationException e) {
            LoggerUtil.logWarning("Duplicate student entry attempted for roll=" + rollField.getText() + " in institute=" + instituteField.getText());
            showAlert("Error", "Duplicate roll number for the institute.");
        } catch (Exception e) {
            LoggerUtil.logSevere("Error adding student", e);
            showAlert("Error", "Failed to add student.");
        }
    }

    public void handleUpdateStudent() {
        LoggerUtil.logInfo("handleUpdateStudent() invoked.");
        try (Connection conn = DatabaseConnector.getConnection()) {
            String sql = "UPDATE student SET name=?, age=?, grade=?, contact=?, dob=?, institute=? WHERE roll=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, nameField.getText());
            stmt.setInt(2, Integer.parseInt(ageField.getText()));
            stmt.setString(3, gradeField.getText());
            stmt.setString(4, contactField.getText());
            stmt.setDate(5, Date.valueOf(dobPicker.getValue()));
            stmt.setString(6, instituteField.getText());
            stmt.setString(7, rollField.getText());

            LoggerUtil.logInfo("Executing UPDATE for roll=" + rollField.getText());

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                LoggerUtil.logInfo("Student updated: " + rollField.getText());
                showAlert("Success", "Student updated successfully.");
            } else {
                LoggerUtil.logWarning("No student found with roll: " + rollField.getText());
                showAlert("Info", "Student not found.");
            }
        } catch (Exception e) {
            LoggerUtil.logSevere("Error updating student", e);
            showAlert("Error", "Failed to update student.");
        }
    }

    public void handleDeleteStudent() {
        LoggerUtil.logInfo("handleDeleteStudent() invoked.");
        try (Connection conn = DatabaseConnector.getConnection()) {
            String sql = "DELETE FROM student WHERE roll=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, rollField.getText());

            LoggerUtil.logInfo("Executing DELETE for roll=" + rollField.getText());

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                LoggerUtil.logInfo("Student deleted: " + rollField.getText());
                showAlert("Success", "Student deleted successfully.");
            } else {
                LoggerUtil.logWarning("No student found to delete with roll: " + rollField.getText());
                showAlert("Info", "Student not found.");
            }
        } catch (Exception e) {
            LoggerUtil.logSevere("Error deleting student", e);
            showAlert("Error", "Failed to delete student.");
        }
    }

    public void handleViewStudent() {
        LoggerUtil.logInfo("handleViewStudent() invoked.");
        ObservableList<Student> students = FXCollections.observableArrayList();

        try (Connection conn = DatabaseConnector.getConnection()) {
            String sql = "SELECT * FROM student";
            PreparedStatement stmt = conn.prepareStatement(sql);
            LoggerUtil.logInfo("Executing SELECT * FROM student");

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Student student = new Student(
                    rs.getInt("studID"),
                    rs.getString("roll"),
                    rs.getString("name"),
                    rs.getInt("age"),
                    rs.getString("grade"),
                    rs.getString("contact"),
                    rs.getDate("dob").toLocalDate(),
                    rs.getString("institute")
                );
                students.add(student);
            }

            studentTable.setItems(students);
            LoggerUtil.logInfo("Loaded " + students.size() + " students into table.");
        } catch (Exception e) {
            LoggerUtil.logSevere("Error viewing students", e);
            showAlert("Error", "Failed to retrieve student.");
        }
    }

    private void showAlert(String title, String msg) {
        LoggerUtil.logInfo("Showing alert: " + title + " - " + msg);
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
