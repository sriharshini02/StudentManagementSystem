package SMS.db;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConnector {
    public static Connection getConnection() throws SQLException {
        return ConnectionPool.getDataSource().getConnection();
    }
}
