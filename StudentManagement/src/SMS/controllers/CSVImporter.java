package SMS.controllers;
import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList; // Import ArrayList
import java.util.List;     // Import List

public class CSVImporter {

    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/student_managementdb?useSSL=false";
    private static final String DB_USER = "harshini";
    private static final String DB_PASSWORD = "harshini";

    public static void main(String[] args) {
        String csvFilePath = "C:/Users/DELL/Downloads/MOCK_DATA12.csv"; // Changed to MOCK_DATA1.csv
        String institute = "anits";

        // Define a list of possible date formats to try
        List<SimpleDateFormat> dateFormats = new ArrayList<>();
        dateFormats.add(new SimpleDateFormat("MM-dd-yyyy")); // Format: 10-08-2025
        dateFormats.add(new SimpleDateFormat("M/d/yyyy"));   // Format: 10/20/2014 or 8/23/2022
        // Make parsing strict, so it doesn't try to leniently parse incorrect formats
        for (SimpleDateFormat sdf : dateFormats) {
            sdf.setLenient(false);
        }

        try (
            Connection conn = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD);
            BufferedReader reader = new BufferedReader(new FileReader(csvFilePath))
        ) {
            System.out.println("Database connection established for CSV import.");

            String line = reader.readLine();
            if (line == null) {
                System.out.println("CSV file is empty or only contains a header.");
                return;
            }
            System.out.println("Skipped header: " + line);

            String sql = "INSERT INTO student (roll, name, age, dob, grade, contact, institute) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?)";

            PreparedStatement stmt = conn.prepareStatement(sql);
            int count = 0;
            int skippedCount = 0;

            while ((line = reader.readLine()) != null) {
                String[] data = line.split(",", -1);

                if (data.length < 6) { // roll, name, age, dob, grade, contact (at least 6 fields)
                    System.err.println("Skipping malformed row (too few columns): " + line);
                    skippedCount++;
                    continue;
                }

                try {
                    // --- Parse Roll Number ---
                    int roll = Integer.parseInt(data[0].trim());
                    if (roll <= 0) {
                        System.err.println("Skipping row: Invalid roll number (must be positive integer): " + data[0] + " in line: " + line);
                        skippedCount++;
                        continue;
                    }
                    stmt.setInt(1, roll);

                    // --- Parse Name ---
                    stmt.setString(2, data[1].trim());

                    // --- Parse Age ---
                    int age = Integer.parseInt(data[2].trim());
                    if (age <= 0) {
                        System.err.println("Skipping row: Invalid age (must be positive integer): " + data[2] + " in line: " + line);
                        skippedCount++;
                        continue;
                    }
                    stmt.setInt(3, age);

                    // --- Parse Date of Birth with multiple formats ---
                    java.util.Date utilDate = null;
                    boolean dateParsed = false;
                    String dobString = data[3].trim();

                    if (!dobString.isEmpty()) {
                        for (SimpleDateFormat sdf : dateFormats) {
                            try {
                                utilDate = sdf.parse(dobString);
                                dateParsed = true;
                                break; // Date parsed successfully, exit loop
                            } catch (ParseException ignored) {
                                // Try next format
                            }
                        }
                        if (dateParsed) {
                            stmt.setDate(4, new java.sql.Date(utilDate.getTime()));
                        } else {
                            System.err.println("Skipping row due to Date Parsing error (no matching format): " + dobString + " in line: " + line);
                            skippedCount++;
                            continue; // Skip this row if date cannot be parsed by any format
                        }
                    } else {
                        stmt.setNull(4, java.sql.Types.DATE); // Set DOB as NULL if empty
                    }

                    // --- Parse Grade ---
                    stmt.setString(5, data[4].trim());

                    // --- Parse Contact ---
                    stmt.setString(6, data[5].trim());

                    // --- Set Institute (Fixed value) ---
                    stmt.setString(7, institute);

                    stmt.executeUpdate(); // Execute the insert statement for the current row
                    count++; // Increment count for successfully inserted rows

                } catch (NumberFormatException e) {
                    System.err.println("Skipping row due to Number Format error (roll, age, or dob): " + line + " - " + e.getMessage());
                    skippedCount++;
                } catch (java.sql.SQLIntegrityConstraintViolationException e) {
                    System.err.println("Skipping row due to duplicate entry (roll, grade, institute constraint violation): " + line + " - " + e.getMessage());
                    skippedCount++;
                } catch (Exception e) { // Catch any other unexpected exceptions
                    System.err.println("Skipping row due to unexpected error: " + line + " - " + e.getMessage());
                    e.printStackTrace();
                    skippedCount++;
                }
            }

            System.out.println("--- CSV Import Summary ---");
            System.out.println("Successfully inserted " + count + " rows.");
            System.out.println("Skipped " + skippedCount + " rows due to errors or invalid data.");

        }  catch (Exception e) {
            System.err.println("An unexpected error occurred during the CSV import process: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
