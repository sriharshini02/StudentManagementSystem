package SMS.utils;

public class SessionManager {
    private static String adminUsername;
    private static String adminInstitute;

    public static void setSession(String username, String institute) {
        adminUsername = username;
        adminInstitute = institute;
        LoggerUtil.logInfo("Session started for admin: " + username + " from institute: " + institute);
    }

    public static String getUsername() {
        if (adminUsername == null) {
            LoggerUtil.logWarning("Attempted to get username but session is null.");
        }
        return adminUsername;
    }

    public static String getInstitute() {
        if (adminInstitute == null) {
            LoggerUtil.logWarning("Attempted to get institute but session is null.");
        }
        return adminInstitute;
    }

    public static void clearSession() {
        LoggerUtil.logInfo("Session cleared for admin: " + adminUsername);
        adminUsername = null;
        adminInstitute = null;
    }
}
