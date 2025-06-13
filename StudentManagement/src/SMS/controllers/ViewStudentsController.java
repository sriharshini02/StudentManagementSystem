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
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.Collections;
import java.util.HashSet; // For unique grades/rolls check
import java.util.Set;     // For unique grades/rolls check


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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

    // Instance variables to hold session data for repeated use
    private String currentUserRole;
    private String currentInstitute;
    private List<String> currentAssignedGrades;

    @FXML
    public void initialize() {
        LoggerUtil.logInfo("Initializing ViewStudentsController table columns.");

        // Get session details immediately on initialize
        currentUserRole = SessionManager.getUserRole();
        currentInstitute = SessionManager.getInstitute();
        currentAssignedGrades = SessionManager.getAssignedGrades();

        // Configure TableColumns
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

        // Populate gradeComboBox based on user role
        populateGradeComboBox();

        LoggerUtil.logInfo("Column setup completed. Fetching initial student list...");
        // Initial load of students (no search/grade filter applied yet)
        loadStudentsIntoTable("", "All"); // Load all students for admin, or all assigned for faculty
    }

    /**
     * Populates the gradeComboBox based on the current user's role.
     * Admins see all grades (1-12), Faculty see only their assigned grades.
     */
    private void populateGradeComboBox() {
        ObservableList<String> grades = FXCollections.observableArrayList();

        if ("admin".equals(currentUserRole)) {
            grades.add("All"); // Admin can view all grades
            for (int i = 1; i <= 12; i++) {
                grades.add(String.valueOf(i));
            }
            LoggerUtil.logInfo("Grade ComboBox populated for Admin with all grades.");
        } else if ("faculty".equals(currentUserRole) && currentAssignedGrades != null && !currentAssignedGrades.isEmpty()) {
            grades.add("All"); // Faculty can view all their assigned grades
            List<String> sortedGrades = new ArrayList<>(currentAssignedGrades);
            Collections.sort(sortedGrades, new Comparator<String>() {
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
            grades.addAll(sortedGrades);
            LoggerUtil.logInfo("Grade ComboBox populated for Faculty with assigned grades: " + currentAssignedGrades);
        } else {
            // This case handles a faculty user with no grades assigned.
            // They will see an empty combo box which defaults to "All" (which will then show no students).
            grades.add("All"); // Still add "All" to avoid empty combo box items
            LoggerUtil.logWarning("Could not determine user role or assigned grades for ComboBox population. Possibly no grades assigned to faculty.");
        }
        gradeComboBox.setItems(grades);
        gradeComboBox.setValue("All"); // Default selection
    }

    /**
     * Fetches students from the database based on the provided filters and current user's role.
     * This is the core data retrieval logic.
     * @param rollText The roll number text from the search field (can be empty).
     * @param selectedGrade The grade selected in the combo box (can be "All").
     * @return An ObservableList of Student objects matching the criteria.
     */
    private ObservableList<Student> getFilteredStudents(String rollText, String selectedGrade) throws SQLException, NumberFormatException {
        ObservableList<Student> students = FXCollections.observableArrayList();

        if (!SessionManager.isLoggedIn() || currentInstitute == null) {
            LoggerUtil.logWarning("Session error in getFilteredStudents: No active session found.");
            return FXCollections.emptyObservableList(); // Return empty list to prevent further errors
        }

        Integer roll = null; // Use Integer object for nullability
        if (!rollText.isEmpty()) {
            roll = Integer.parseInt(rollText); // Parse roll number
            if (roll <= 0) {
                throw new NumberFormatException("Roll Number must be a positive integer.");
            }
        }

        try (Connection conn = DatabaseConnector.getConnection()) {
            StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM student WHERE institute = ?");
            List<Object> params = new ArrayList<>();
            params.add(currentInstitute);

            // Apply grade filtering based on user role
            if ("faculty".equals(currentUserRole)) {
                if (currentAssignedGrades != null && !currentAssignedGrades.isEmpty()) {
                    if ("All".equals(selectedGrade)) {
                        String gradePlaceholders = String.join(",", Collections.nCopies(currentAssignedGrades.size(), "?"));
                        sqlBuilder.append(" AND grade IN (").append(gradePlaceholders).append(")");
                        params.addAll(currentAssignedGrades);
                    } else if (currentAssignedGrades.contains(selectedGrade)) {
                        sqlBuilder.append(" AND grade = ?");
                        params.add(selectedGrade);
                    } else {
                        // Faculty selected a grade not assigned to them, return empty list
                        LoggerUtil.logWarning("Faculty attempted to retrieve students for an unauthorized grade: " + selectedGrade);
                        showAlert("Access Denied", "You are not authorized to view students in grade: " + selectedGrade);
                        return FXCollections.emptyObservableList();
                    }
                } else {
                    // Faculty user has no grades assigned at all
                    LoggerUtil.logWarning("Faculty user has no grades assigned. Returning empty list.");
                    showAlert("Info", "You have no grades assigned. No students to display.");
                    return FXCollections.emptyObservableList();
                }
            } else { // Admin role
                if (selectedGrade != null && !selectedGrade.equals("All")) {
                    sqlBuilder.append(" AND grade = ?");
                    params.add(selectedGrade);
                }
            }

            // Apply roll number filter
            if (roll != null) {
                sqlBuilder.append(" AND roll = ?");
                params.add(roll);
            }

            PreparedStatement stmt = conn.prepareStatement(sqlBuilder.toString());
            for (int i = 0; i < params.size(); i++) {
                if (params.get(i) instanceof Integer) {
                    stmt.setInt(i + 1, (Integer) params.get(i));
                } else if (params.get(i) instanceof String) {
                    stmt.setString(i + 1, (String) params.get(i));
                } else {
                    stmt.setObject(i + 1, params.get(i)); // Fallback
                }
            }

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                students.add(mapResultSetToStudent(rs));
            }
            return students;

        }
    }

    /**
     * Loads the fetched student data into the table and updates the count label.
     * @param rollText The roll number text for filtering.
     * @param selectedGrade The grade for filtering.
     */
    private void loadStudentsIntoTable(String rollText, String selectedGrade) {
        try {
            ObservableList<Student> students = getFilteredStudents(rollText, selectedGrade);
            studentTable.setItems(students);
            updateStudentCount(students.size());
            LoggerUtil.logInfo("Student table updated with " + students.size() + " records.");
        } catch (NumberFormatException e) {
            LoggerUtil.logWarning("Validation failed for roll number: " + e.getMessage());
            showAlert("Validation Error", "Roll Number must be a valid positive integer.");
            studentTable.setItems(FXCollections.emptyObservableList());
            updateStudentCount(0);
        } catch (SQLException e) {
            LoggerUtil.logSevere("Database error loading students: " + e.getMessage(), e);
            showAlert("Error", "Failed to load students due to a database error.");
            studentTable.setItems(FXCollections.emptyObservableList());
            updateStudentCount(0);
        } catch (Exception e) {
            LoggerUtil.logSevere("An unexpected error occurred while loading students: " + e.getMessage(), e);
            showAlert("Error", "An unexpected error occurred while loading students.");
            studentTable.setItems(FXCollections.emptyObservableList());
            updateStudentCount(0);
        }
    }


    @FXML
    private void handleViewStudent(ActionEvent event) {
        LoggerUtil.logInfo("Refresh View Students operation triggered.");
        // When view students is explicitly called, reset search field and load all applicable students
        searchField.clear();
        gradeComboBox.setValue("All"); // Reset grade filter for a fresh view
        loadStudentsIntoTable("", "All");
    }

    @FXML
    private void handleSearch(ActionEvent event) {
        LoggerUtil.logInfo("Search operation triggered.");
        String rollText = searchField.getText().trim();
        String selectedGrade = gradeComboBox.getValue();
        loadStudentsIntoTable(rollText, selectedGrade);
    }

    private Student mapResultSetToStudent(ResultSet rs) throws SQLException {
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
            // FIX: Get students to export directly from the currently displayed table items
            ObservableList<Student> studentsToExport = studentTable.getItems();

            try (PrintWriter writer = new PrintWriter(file)) {
                // Write CSV header
                writer.println("Roll Number,Full Name,Age,Grade,Contact Info,Date of Birth,Institute");

                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");

                for (Student s : studentsToExport) {
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
                showAlert("Success", "Student list exported as CSV successfully. Total records: " + studentsToExport.size());
            } catch (IOException e) {
                LoggerUtil.logSevere("Error writing CSV file", e);
                showAlert("Error", "Failed to export CSV: " + e.getMessage());
            } catch (Exception e) { // Catch any other unexpected exceptions
                LoggerUtil.logSevere("An unexpected error occurred during CSV export", e);
                showAlert("Error", "An unexpected error occurred during CSV export.");
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
            // FIX: Get students to export directly from the currently displayed table items
            ObservableList<Student> studentsToExport = studentTable.getItems();

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

                // --- Generate report details based on the actual displayed data ---
                String reportInstitute = currentInstitute; // Always report the current user's institute

                // Analyze studentsToExport to determine displayed roll and grade filters for the header
                Set<String> uniqueGradesInExport = new HashSet<>();
                Set<Integer> uniqueRollsInExport = new HashSet<>();
                for (Student s : studentsToExport) {
                    uniqueGradesInExport.add(s.getGrade());
                    uniqueRollsInExport.add(s.getRoll());
                }

                String displayedRollSummary = uniqueRollsInExport.size() == 1 ?
                                              String.valueOf(uniqueRollsInExport.iterator().next()) : "All";
                String displayedGradeSummary;

                if ("faculty".equals(currentUserRole)) {
                    // For faculty, report on grades from displayed students, or assigned if 'All' shown
                    if (uniqueGradesInExport.size() == 1 && currentAssignedGrades.contains(uniqueGradesInExport.iterator().next())) {
                         displayedGradeSummary = uniqueGradesInExport.iterator().next() + " (Faculty View)";
                    } else if (!uniqueGradesInExport.isEmpty()) {
                        displayedGradeSummary = "Assigned Grades: " + String.join(", ", uniqueGradesInExport) + " (Faculty View)";
                    } else {
                        displayedGradeSummary = "No Grades Assigned / Displayed (Faculty View)";
                    }
                } else { // Admin view
                    if (uniqueGradesInExport.size() == 1) {
                        displayedGradeSummary = uniqueGradesInExport.iterator().next() + " (Admin View)";
                    } else {
                        displayedGradeSummary = "All Grades Included (Admin View)";
                    }
                }


                if (reportInstitute != null && !reportInstitute.isEmpty()) {
                    Paragraph instituteInfo = new Paragraph("Institute: " + reportInstitute, fontSubtitle);
                    instituteInfo.setAlignment(Element.ALIGN_CENTER);
                    instituteInfo.setSpacingAfter(5);
                    document.add(instituteInfo);
                }

                // Add grade info
                Paragraph gradeInfo = new Paragraph("Showing Grade: " + displayedGradeSummary, fontSubtitle);
                gradeInfo.setAlignment(Element.ALIGN_CENTER);
                gradeInfo.setSpacingAfter(5);
                document.add(gradeInfo);

                // Add roll info if applicable
                if (!"All".equals(displayedRollSummary)) {
                    Paragraph rollInfo = new Paragraph("Showing Roll Number: " + displayedRollSummary, fontSubtitle);
                    rollInfo.setAlignment(Element.ALIGN_CENTER);
                    rollInfo.setSpacingAfter(5);
                    document.add(rollInfo);
                }

                Paragraph countInfo = new Paragraph("Total Students Displayed (in report): " + studentsToExport.size(), fontSubtitle);
                countInfo.setAlignment(Element.ALIGN_CENTER);
                countInfo.setSpacingAfter(20);
                document.add(countInfo);
                // --- End detailed description ---


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

                for (Student s : studentsToExport) { // Iterate over the currently displayed list
                    table.addCell(new PdfPCell(new Phrase(String.valueOf(s.getRoll()), fontBody)));
                    table.addCell(new PdfPCell(new Phrase(s.getName(), fontBody)));
                    table.addCell(new PdfPCell(new Phrase(String.valueOf(s.getAge()), fontBody)));
                    table.addCell(new PdfPCell(new Phrase(s.getGrade(), fontBody)));
                    table.addCell(new PdfPCell(new Phrase(s.getContact(), fontBody)));
                    table.addCell(new PdfPCell(new Phrase(s.getDob() != null ? s.getDob().format(dtf) : "", fontBody)));
                    table.addCell(new PdfPCell(new Phrase(s.getInstitute(), fontBody)));
                }

                document.add(table);
                document.close();

                showAlert("Success", "Student list exported as PDF successfully. Total records: " + studentsToExport.size());
            } catch (IOException e) {
                LoggerUtil.logSevere("Error writing PDF file", e);
                showAlert("Error", "Failed to export PDF: " + e.getMessage());
            } catch (Exception e) {
                LoggerUtil.logSevere("An unexpected error occurred during PDF export", e);
                showAlert("Error", "An unexpected error occurred during PDF export.");
            }
        }
    }

}
