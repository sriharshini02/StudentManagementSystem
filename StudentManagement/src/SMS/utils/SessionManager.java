package SMS.utils;

import java.util.List;
import java.util.ArrayList; // Import ArrayList

public class SessionManager {
    private static String loggedInUsername;
    private static String institute;
    private static String userRole; // New field to store user role (e.g., "admin", "faculty")
    private static List<String> assignedGrades; // New field for faculty's assigned grades

    /**
     * Sets the session for an administrator.
     * @param username The username of the logged-in admin.
     * @param inst The institute the admin belongs to.
     */
    public static void setAdminSession(String username, String inst) {
        loggedInUsername = username;
        institute = inst;
        userRole = "admin"; // Set role for admin
        assignedGrades = null; // Admins don't have assigned grades in this context
        LoggerUtil.logInfo("Admin session set for: " + username + " at " + inst);
    }

    /**
     * Sets the session for a faculty member.
     * @param username The username of the logged-in faculty.
     * @param inst The institute the faculty belongs to.
     * @param grades A list of grades assigned to this faculty member.
     */
    public static void setFacultySession(String username, String inst, List<String> grades) {
        loggedInUsername = username;
        institute = inst;
        userRole = "faculty"; // Set role for faculty
        // Create a new ArrayList to store the grades to avoid direct modification of the passed list
        assignedGrades = new ArrayList<>(grades);
        LoggerUtil.logInfo("Faculty session set for: " + username + " at " + inst + " for grades: " + grades);
    }

    /**
     * Clears all session information.
     */
    public static void clearSession() {
        LoggerUtil.logInfo("Session cleared for user: " + loggedInUsername + " (Role: " + userRole + ")");
        loggedInUsername = null;
        institute = null;
        userRole = null;
        assignedGrades = null;
    }

    /**
     * Gets the username of the currently logged-in user.
     * @return The username, or null if no user is logged in.
     */
    public static String getLoggedInUsername() {
        if (loggedInUsername == null) {
            LoggerUtil.logWarning("Attempted to get username but no user is logged in.");
        }
        return loggedInUsername;
    }

    /**
     * Gets the institute of the currently logged-in user.
     * @return The institute name, or null if no user is logged in or institute is not set.
     */
    public static String getInstitute() {
        if (institute == null) {
            LoggerUtil.logWarning("Attempted to get institute but session is null or institute is not set.");
        }
        return institute;
    }

    /**
     * Checks if a user is currently logged in.
     * @return true if a user is logged in, false otherwise.
     */
    public static boolean isLoggedIn() {
        return loggedInUsername != null;
    }

    /**
     * Gets the role of the currently logged-in user.
     * @return The user's role (e.g., "admin", "faculty"), or null if no user is logged in.
     */
    public static String getUserRole() {
        if (userRole == null) {
            LoggerUtil.logWarning("Attempted to get user role but no user is logged in.");
        }
        return userRole;
    }

    /**
     * Gets the list of grades assigned to a faculty member.
     * This method should only be called if the userRole is "faculty".
     * @return A copy of the list of assigned grades, or null if not a faculty user or no grades assigned.
     */
    public static List<String> getAssignedGrades() {
        if (userRole == null || !userRole.equals("faculty")) {
            LoggerUtil.logWarning("Attempted to get assigned grades for a non-faculty user or no user logged in.");
            return null;
        }
        // Return a new ArrayList to prevent external modification of the internal list
        return assignedGrades != null ? new ArrayList<>(assignedGrades) : null;
    }
}
