import java.sql.*;
import java.util.Scanner;

public class SmartNotesBackend {

    // ✅ Database connection details
    static final String URL = "jdbc:mysql://localhost:3306/smartnotes";
    static final String USER = "root";          // Your MySQL username
    static final String PASS = "Rofi@1013";     // Your MySQL password

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        try {
            // ✅ Load MySQL JDBC Driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            // ✅ Connect to MySQL Database
            Connection conn = DriverManager.getConnection(URL, USER, PASS);
            System.out.println("\n✅ Connected to SmartNotes database!");

            while (true) {
                System.out.println("\n1️⃣ Signup");
                System.out.println("2️⃣ Login");
                System.out.println("3️⃣ Exit");
                System.out.print("👉 Choose option: ");

                int choice;
                try {
                    choice = Integer.parseInt(sc.nextLine());
                } catch (NumberFormatException e) {
                    System.out.println("⚠️ Please enter a valid number!");
                    continue;
                }

                switch (choice) {
                    case 1 -> signup(conn, sc);
                    case 2 -> {
                        if (login(conn, sc)) {
                            System.out.println("🎯 Opening Smart Notes App...");
                            try {
                                ProcessBuilder pb = new ProcessBuilder(
                                    "java",
                                    "-cp",
                                    ".;mysql-connector-j-9.5.0.jar",
                                    "SmartNotesApp"
                                );
                                pb.inheritIO();
                                pb.start();
                                return; // Stop backend after launching app
                            } catch (Exception e) {
                                System.out.println("❌ Failed to launch SmartNotesApp: " + e.getMessage());
                            }
                        }
                    }
                    case 3 -> {
                        System.out.println("👋 Exiting... Goodbye!");
                        conn.close();
                        return;
                    }
                    default -> System.out.println("⚠️ Invalid option. Try again!");
                }
            }

        } catch (ClassNotFoundException e) {
            System.out.println("❌ MySQL JDBC Driver not found! Add the connector JAR.");
        } catch (SQLException e) {
            System.out.println("❌ Database error: " + e.getMessage());
        }
    }

    // 🔹 Signup function
    private static void signup(Connection conn, Scanner sc) {
        try {
            System.out.print("Enter new username: ");
            String username = sc.nextLine();
            System.out.print("Enter new password: ");
            String password = sc.nextLine();

            String query = "INSERT INTO users (username, password) VALUES (?, ?)";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, username);
            ps.setString(2, password);
            ps.executeUpdate();

            System.out.println("✅ Signup successful!");

        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("⚠️ Username already exists! Try a different one.");
        } catch (SQLException e) {
            System.out.println("❌ Signup error: " + e.getMessage());
        }
    }

    // 🔹 Login function (returns true if successful)
    private static boolean login(Connection conn, Scanner sc) {
        try {
            System.out.print("Enter username: ");
            String username = sc.nextLine();
            System.out.print("Enter password: ");
            String password = sc.nextLine();

            String query = "SELECT * FROM users WHERE username=? AND password=?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                System.out.println("✅ Login successful! Welcome, " + username + "!");
                return true;
            } else {
                System.out.println("❌ Invalid username or password.");
            }

        } catch (SQLException e) {
            System.out.println("❌ Login error: " + e.getMessage());
        }
        return false;
    }
}
