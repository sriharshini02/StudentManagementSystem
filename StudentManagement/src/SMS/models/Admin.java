package SMS.models;

public class Admin {
    private int adminID;
    private String name;
    private String username;
    private String institute;

    public Admin(int adminID, String name, String username, String institute) {
        this.adminID = adminID;
        this.name = name;
        this.username = username;
        this.institute = institute;
    }

    public int getAdminID() {
        return adminID;
    }

    public void setAdminID(int adminID) {
        this.adminID = adminID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getInstitute() {
        return institute;
    }

    public void setInstitute(String institute) {
        this.institute = institute;
    }

    @Override
    public String toString() {
        return name + " (" + username + ")";
    }
}
