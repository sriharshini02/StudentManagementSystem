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
import java.util.Comparator; 

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ViewStudentsController {

    @FXML private TableView<Student> studentTable;
    @FXML private TableColumn<Student, Integer> rollColumn;
    @FXML private TableColumn<Student, String> nameColumn;
    @FXML private TableColumn<Student, Integer> ageColumn;
    @FXML private TableColumn<Student, String> gradeColumn; 
    @FXML private TableColumn<Student, String> contactColumn;
    @FXML private TableColumn<Student, String> dobColumn;
    @FXML private TableColumn<Student, String> instituteColumn;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> gradeComboBox;
    @FXML private Label studentCountLabel;

    @FXML
    public void initialize() {
        LoggerUtil.logInfo("Initializing ViewStudentsController table columns.");

        rollColumn.setCellValueFactory(new PropertyValueFactory<>("roll"));

        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        ageColumn.setCellValueFactory(new PropertyValueFactory<>("age"));
        gradeColumn.setCellValueFactory(new PropertyValueFactory<>("grade"));
        gradeColumn.setComparator(new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                try {
                    int i1 = Integer.parseInt(s1);
                    int i2 = Integer.parseInt(s2);
                    return Integer.compare(i1, i2);
                } catch (NumberFormatException e) {
                    return s1.compareTo(s2);
                }
            }
        });


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
            while (rs.next()) {
                students.add(mapResultSetToStudent(rs));
            }

            LoggerUtil.logInfo("Total students retrieved: " + students.size());
            studentTable.setItems(students);
            updateStudentCount(students.size());

        } catch (Exception e) {
            LoggerUtil.logSevere("Error retrieving student list from database", e);
            showAlert("Error", "Failed to retrieve students.");
        }
    }

    @FXML
    private void handleSearch(ActionEvent event) {
        String rollText = searchField.getText().trim();
        String grade = gradeComboBox.getValue();
        String currentInstitute = SessionManager.getInstitute();

        if (currentInstitute == null) {
            LoggerUtil.logWarning("Session error: No admin session found.");
            showAlert("Session Error", "No admin session found. Please log in again.");
            return;
        }

        int roll = -1;
        if (!rollText.isEmpty()) {
            try {
                roll = Integer.parseInt(rollText);
                if (roll <= 0) {
                    LoggerUtil.logWarning("Validation failed: Roll Number must be a positive integer. Entered: " + rollText);
                    showAlert("Validation Error", "Roll Number must be a positive integer.");
                    return;
                }
            } catch (NumberFormatException e) {
                LoggerUtil.logWarning("Validation failed: Roll Number is not a valid integer. Entered: " + rollText);
                showAlert("Validation Error", "Roll Number must be a valid integer.");
                return;
            }
        }


        ObservableList<Student> students = FXCollections.observableArrayList();

        try (Connection conn = DatabaseConnector.getConnection()) {
            StringBuilder sql = new StringBuilder("SELECT * FROM student WHERE institute = ?");
            if (!rollText.isEmpty()) sql.append(" AND roll = ?");
            if (grade != null && !grade.equals("All")) sql.append(" AND grade = ?");

            PreparedStatement stmt = conn.prepareStatement(sql.toString());

            int index = 1;
            stmt.setString(index++, currentInstitute);
            if (!rollText.isEmpty()) stmt.setInt(index++, roll);
            if (grade != null && !grade.equals("All")) stmt.setString(index++, grade);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                students.add(mapResultSetToStudent(rs));
            }

            if (students.isEmpty()) {
                LoggerUtil.logInfo("No students found for given filters.");
                showAlert("No Results", "No students found for the given filters.");
            }

            studentTable.setItems(students);
            updateStudentCount(students.size());

        } catch (Exception e) {
            LoggerUtil.logSevere("Error filtering students", e);
            showAlert("Error", "Failed to search students.");
        }
    }

    private Student mapResultSetToStudent(ResultSet rs) throws Exception {
        return new Student(
                rs.getInt("studID"),
                rs.getInt("roll"),
                rs.getString("name"),
                rs.getInt("age"),
                rs.getString("grade"),
                rs.getString("contact"),
                rs.getDate("dob") != null ? rs.getDate("dob").toLocalDate() : null,
                rs.getString("institute")
        );
    }

    private void updateStudentCount(int count) {
        studentCountLabel.setText("Total Students Displayed: " + count);
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
                        String.valueOf(s.getRoll()),
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
                Font fontSubtitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
                Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
                Font fontBody = FontFactory.getFont(FontFactory.HELVETICA, 10);

                Paragraph title = new Paragraph("Student List Report", fontTitle);
                title.setAlignment(Element.ALIGN_CENTER);
                title.setSpacingAfter(10);
                document.add(title);

                String currentInstitute = SessionManager.getInstitute();
                ObservableList<Student> displayedStudents = studentTable.getItems();
                String selectedGrade = gradeComboBox.getValue();

                if (currentInstitute != null && !currentInstitute.isEmpty()) {
                    Paragraph instituteInfo = new Paragraph("Institute: " + currentInstitute, fontSubtitle);
                    instituteInfo.setAlignment(Element.ALIGN_CENTER);
                    instituteInfo.setSpacingAfter(5);
                    document.add(instituteInfo);
                }

                if (selectedGrade != null && !selectedGrade.equals("All")) {
                    Paragraph gradeInfo = new Paragraph("Filtered by Grade: " + selectedGrade, fontSubtitle);
                    gradeInfo.setAlignment(Element.ALIGN_CENTER);
                    gradeInfo.setSpacingAfter(5);
                    document.add(gradeInfo);
                } else {
                    Paragraph gradeInfo = new Paragraph("All Grades Included", fontSubtitle);
                    gradeInfo.setAlignment(Element.ALIGN_CENTER);
                    gradeInfo.setSpacingAfter(5);
                    document.add(gradeInfo);
                }

                Paragraph countInfo = new Paragraph("Total Students Displayed: " + displayedStudents.size(), fontSubtitle);
                countInfo.setAlignment(Element.ALIGN_CENTER);
                countInfo.setSpacingAfter(20);
                document.add(countInfo);


                PdfPTable table = new PdfPTable(7);
                table.setWidthPercentage(100);
                table.setWidths(new float[]{2f, 3f, 1f, 1.5f, 3f, 2f, 3f});

                String[] headers = {"Roll Number", "Full Name", "Age", "Grade", "Contact Info", "Date of Birth", "Institute"};
                for (String h : headers) {
                    PdfPCell cell = new PdfPCell(new Phrase(h, fontHeader));
                    cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    table.addCell(cell);
                }

                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");

                for (Student s : displayedStudents) {
                    table.addCell(new PdfPCell(new Phrase(String.valueOf(s.getRoll()), fontBody)));
                    table.addCell(new PdfPCell(new Phrase(s.getName(), fontBody)));
                    table.addCell(new PdfPCell(new Phrase(String.valueOf(s.getAge()), fontBody)));
                    table.addCell(new PdfPCell(new Phrase(s.getGrade(), fontBody))); // Grade might still be non-numeric
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
