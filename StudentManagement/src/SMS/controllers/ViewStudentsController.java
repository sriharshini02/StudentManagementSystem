package SMS.controllers;
import javafx.stage.FileChooser;
import java.io.*;
import java.time.format.DateTimeFormatter;
import javafx.collections.ObservableList;

import com.itextpdf.text.*;
import com.itextpdf.text.Font;
import com.itextpdf.text.pdf.*;
import javafx.scene.control.TextField;

import SMS.db.DatabaseConnector;
import SMS.models.Student;
import SMS.utils.LoggerUtil;
import SMS.utils.SessionManager;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.event.ActionEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ViewStudentsController {

    @FXML private TableView<Student> studentTable;
    @FXML private TableColumn<Student, String> rollColumn;
    @FXML private TableColumn<Student, String> nameColumn;
    @FXML private TableColumn<Student, Integer> ageColumn;
    @FXML private TableColumn<Student, String> gradeColumn;
    @FXML private TableColumn<Student, String> contactColumn;
    @FXML private TableColumn<Student, String> dobColumn;
    @FXML private TableColumn<Student, String> instituteColumn;

    @FXML private TextField searchField;        
    @FXML private ComboBox<String> gradeComboBox; 
    @FXML
    public void initialize() {
        LoggerUtil.logInfo("Initializing ViewStudentsController table columns.");

        rollColumn.setCellValueFactory(new PropertyValueFactory<>("roll"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        ageColumn.setCellValueFactory(new PropertyValueFactory<>("age"));
        gradeColumn.setCellValueFactory(new PropertyValueFactory<>("grade"));
        contactColumn.setCellValueFactory(new PropertyValueFactory<>("contact"));

        dobColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getDob() != null) {
                return new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDob().toString());
            } else {
                return new javafx.beans.property.SimpleStringProperty("");
            }
        });

        instituteColumn.setCellValueFactory(new PropertyValueFactory<>("institute"));

        ObservableList<String> grades = FXCollections.observableArrayList("All");
        for (int i = 1; i <= 12; i++) {
            grades.add(String.valueOf(i));
        }
        gradeComboBox.setItems(grades);
        gradeComboBox.setValue("All"); 

        LoggerUtil.logInfo("Column setup completed. Fetching student list...");
        handleViewStudent(null); 
    }

    @FXML
    private void handleViewStudent(ActionEvent event) {
        LoggerUtil.logInfo("View student operation started.");

        ObservableList<Student> students = FXCollections.observableArrayList();
        String currentInstitute = SessionManager.getInstitute();

        if (currentInstitute == null) {
            LoggerUtil.logWarning("Session error: No admin session found.");
            showAlert("Session Error", "No admin session found. Please log in again.");
            return;
        }

        try (Connection conn = DatabaseConnector.getConnection()) {
            String sql = "SELECT * FROM student WHERE institute = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, currentInstitute);

            ResultSet rs = stmt.executeQuery();
            int count = 0;
            while (rs.next()) {
                students.add(mapResultSetToStudent(rs));
                count++;
            }

            LoggerUtil.logInfo("Total students retrieved: " + count);
            studentTable.setItems(students);

        } catch (Exception e) {
            LoggerUtil.logSevere("Error retrieving student list from database", e);
            showAlert("Error", "Failed to retrieve students.");
        }
    }

    @FXML
    private void handleSearch(ActionEvent event) {
        String roll = searchField.getText().trim();
        String grade = gradeComboBox.getValue();
        String currentInstitute = SessionManager.getInstitute();

        if (currentInstitute == null) {
            LoggerUtil.logWarning("Session error: No admin session found.");
            showAlert("Session Error", "No admin session found. Please log in again.");
            return;
        }

        ObservableList<Student> students = FXCollections.observableArrayList();

        try (Connection conn = DatabaseConnector.getConnection()) {
            StringBuilder sql = new StringBuilder("SELECT * FROM student WHERE institute = ?");
            if (!roll.isEmpty()) sql.append(" AND roll = ?");
            if (grade != null && !grade.equals("All")) sql.append(" AND grade = ?");

            PreparedStatement stmt = conn.prepareStatement(sql.toString());

            int index = 1;
            stmt.setString(index++, currentInstitute);
            if (!roll.isEmpty()) stmt.setString(index++, roll);
            if (grade != null && !grade.equals("All")) stmt.setString(index++, grade);

            ResultSet rs = stmt.executeQuery();

            boolean found = false;
            while (rs.next()) {
                students.add(mapResultSetToStudent(rs));
                found = true;
            }

            if (!found) {
                LoggerUtil.logInfo("No students found for given filters.");
                showAlert("No Results", "No students found for the given filters.");
            }

            studentTable.setItems(students);

        } catch (Exception e) {
            LoggerUtil.logSevere("Error filtering students", e);
            showAlert("Error", "Failed to search students.");
        }
    }

    private Student mapResultSetToStudent(ResultSet rs) throws Exception {
        return new Student(
                rs.getInt("studID"),
                rs.getString("roll"),
                rs.getString("name"),
                rs.getInt("age"),
                rs.getString("grade"),
                rs.getString("contact"),
                rs.getDate("dob") != null ? rs.getDate("dob").toLocalDate() : null,
                rs.getString("institute")
        );
    }

    private void showAlert(String title, String message) {
        LoggerUtil.logInfo("Showing alert - Title: " + title + ", Message: " + message);
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    @FXML
    private void handleExportCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Students as CSV");
        fileChooser.setInitialFileName("students.csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fileChooser.showSaveDialog(studentTable.getScene().getWindow());

        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file)) {
                // Write CSV header
                writer.println("Roll Number,Full Name,Age,Grade,Contact Info,Date of Birth,Institute");

                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                ObservableList<Student> students = studentTable.getItems();

                for (Student s : students) {
                    String dob = s.getDob() != null ? s.getDob().format(dtf) : "";
                    writer.printf("%s,%s,%d,%s,%s,%s,%s%n",
                        s.getRoll(),
                        s.getName(),
                        s.getAge(),
                        s.getGrade(),
                        s.getContact(),
                        dob,
                        s.getInstitute()
                    );
                }
                showAlert("Success", "Student list exported as CSV successfully.");
            } catch (IOException e) {
                LoggerUtil.logSevere("Error exporting CSV", e);
                showAlert("Error", "Failed to export CSV.");
            }
        }
    }
    @FXML
    private void handleExportPDF() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Students as PDF");
        fileChooser.setInitialFileName("students.pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = fileChooser.showSaveDialog(studentTable.getScene().getWindow());

        if (file != null) {
            Document document = new Document();
            try {
                PdfWriter.getInstance(document, new FileOutputStream(file));
                document.open();

                Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
                Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
                Font fontBody = FontFactory.getFont(FontFactory.HELVETICA, 10);

                Paragraph title = new Paragraph("Student List", fontTitle);
                title.setAlignment(Element.ALIGN_CENTER);
                title.setSpacingAfter(20);
                document.add(title);

                PdfPTable table = new PdfPTable(7);
                table.setWidthPercentage(100);
                table.setWidths(new float[]{2f, 3f, 1f, 1.5f, 3f, 2f, 3f});

                // Add table headers
                String[] headers = {"Roll Number", "Full Name", "Age", "Grade", "Contact Info", "Date of Birth", "Institute"};
                for (String h : headers) {
                    PdfPCell cell = new PdfPCell(new Phrase(h, fontHeader));
                    cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    table.addCell(cell);
                }

                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                ObservableList<Student> students = studentTable.getItems();

                for (Student s : students) {
                    table.addCell(new PdfPCell(new Phrase(s.getRoll(), fontBody)));
                    table.addCell(new PdfPCell(new Phrase(s.getName(), fontBody)));
                    table.addCell(new PdfPCell(new Phrase(String.valueOf(s.getAge()), fontBody)));
                    table.addCell(new PdfPCell(new Phrase(s.getGrade(), fontBody)));
                    table.addCell(new PdfPCell(new Phrase(s.getContact(), fontBody)));
                    table.addCell(new PdfPCell(new Phrase(s.getDob() != null ? s.getDob().format(dtf) : "", fontBody)));
                    table.addCell(new PdfPCell(new Phrase(s.getInstitute(), fontBody)));
                }

                document.add(table);
                document.close();

                showAlert("Success", "Student list exported as PDF successfully.");
            } catch (Exception e) {
                LoggerUtil.logSevere("Error exporting PDF", e);
                showAlert("Error", "Failed to export PDF.");
            }
        }
    }

}
