package SMS.models;

import java.time.LocalDate;

public class Student {
    private int studID;
    private int roll; // Changed from String to int
    private String name;
    private int age;
    private String grade;
    private String contact;
    private LocalDate dob;
    private String institute;

    public Student(int studID, int roll, String name, int age, String grade, String contact, LocalDate dob, String institute) { // Constructor updated
        this.studID = studID;
        this.roll = roll; // No change needed here, as 'roll' is now an int
        this.name = name;
        this.age = age;
        this.grade = grade;
        this.contact = contact;
        this.dob = dob;
        this.institute = institute;
    }

    public int getStudID() {
        return studID;
    }

    public void setStudID(int studID) {
        this.studID = studID;
    }

    public int getRoll() { // Return type changed to int
        return roll;
    }

    public void setRoll(int roll) { // Parameter type changed to int
        this.roll = roll;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public LocalDate getDob() {
        return dob;
    }

    public void setDob(LocalDate dob) {
        this.dob = dob;
    }

    public String getInstitute() {
        return institute;
    }

    public void setInstitute(String institute) {
        this.institute = institute;
    }

    @Override
    public String toString() {
        // Updated toString to handle int 'roll' by concatenating directly
        return roll + " - " + name;
    }
}
