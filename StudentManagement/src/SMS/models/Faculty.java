package SMS.models;

import java.util.ArrayList;
import java.util.List;

public class Faculty {
    private int facultyID;
    private String username;
    private String name;
    private String institute;
    private List<String> assignedGrades; // List of grades assigned to this faculty

    /**
     * Constructor for the Faculty model.
     * @param facultyID The unique ID of the faculty member.
     * @param username The login username for the faculty member.
     * @param name The full name of the faculty member.
     * @param institute The institute the faculty member belongs to.
     * @param assignedGrades A list of grades assigned to this faculty member.
     */
    public Faculty(int facultyID, String username, String name, String institute, List<String> assignedGrades) {
        this.facultyID = facultyID;
        this.username = username;
        this.name = name;
        this.institute = institute;
        // Defensive copy to ensure the internal list cannot be modified externally
        this.assignedGrades = assignedGrades != null ? new ArrayList<>(assignedGrades) : new ArrayList<>();
    }

    // --- Getters for JavaFX TableView compatibility (PropertyValueFactory) ---

    public int getFacultyID() {
        return facultyID;
    }

    public String getUsername() {
        return username;
    }

    public String getName() {
        return name;
    }

    public String getInstitute() {
        return institute;
    }

    /**
     * Returns a copy of the list of assigned grades for this faculty.
     * This prevents external modification of the internal list.
     * @return A new ArrayList containing the assigned grades.
     */
    public List<String> getAssignedGrades() {
        return new ArrayList<>(assignedGrades);
    }

    // --- Optional: Setters (if you need to modify Faculty objects after creation) ---
    // For display purposes, getters are sufficient.

    public void setFacultyID(int facultyID) {
        this.facultyID = facultyID;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setInstitute(String institute) {
        this.institute = institute;
    }

    public void setAssignedGrades(List<String> assignedGrades) {
        this.assignedGrades = assignedGrades != null ? new ArrayList<>(assignedGrades) : new ArrayList<>();
    }

    // --- Optional: toString method for logging/debugging ---
    @Override
    public String toString() {
        return "Faculty{" +
               "facultyID=" + facultyID +
               ", username='" + username + '\'' +
               ", name='" + name + '\'' +
               ", institute='" + institute + '\'' +
               ", assignedGrades=" + assignedGrades +
               '}';
    }
}
