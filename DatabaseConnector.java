import java.sql.Connection;
import java.sql.DriverManager;

public class DatabaseConnector {
    public static Connection getConnection() {
        try {
            return DriverManager.getConnection("jdbc:mysql://localhost:3306/TravelDB", "root", "Hema2510");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
