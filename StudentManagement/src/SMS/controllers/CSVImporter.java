package SMS.controllers;
/*import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.text.SimpleDateFormat;
*/
/*
public class CSVImporter {

    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/student_managementdb?useSSL=false";
    private static final String DB_USER = "harshini";
    private static final String DB_PASSWORD = "harshini";

    public static void main(String[] args) {
        String csvFilePath = "C:/Users/DELL/Downloads/MOCK_DATA.csv";
        String institute = "anits2";

        try (
            Connection conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD);
            BufferedReader reader = new BufferedReader(new FileReader(csvFilePath))
        ) {
            String line = reader.readLine(); 

            String sql = "INSERT INTO student (roll, name, age, dob, grade, contact, institute) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?)";

            PreparedStatement stmt = conn.prepareStatement(sql);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            int count = 0;
            while ((line = reader.readLine()) != null) {
                String[] data = line.split(",", -1); 

                stmt.setString(1, data[0]); // roll
                stmt.setString(2, data[1]); // name
                stmt.setInt(3, Integer.parseInt(data[2])); // age
                java.util.Date utilDate = sdf.parse(data[3]);
                stmt.setDate(4, new java.sql.Date(utilDate.getTime()));
                stmt.setString(5, data[4]); // grade
                stmt.setString(6, data[5]); // contact
                stmt.setString(7, institute); // fixed value

                stmt.executeUpdate();
                count++;
            }

            System.out.println("Inserted " + count + " rows successfully.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
*/