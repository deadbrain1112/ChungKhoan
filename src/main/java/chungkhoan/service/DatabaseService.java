package chungkhoan.service;

import chungkhoan.repository.DatabaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.sql.*;
import java.util.List;

@Service
public class DatabaseService {
    private static final String URL = "jdbc:sqlserver://localhost:1433;databaseName=QUANLYGIAODICHCHUNGKHOAN;encrypt=false;trustServerCertificate=true;charset=UTF-8";

    @Autowired
    private DatabaseRepository repository;

    public List<String> getDatabases() {
        return repository.getAllUserDatabases();
    }
    public boolean testConnection(String username, String password) {
        String connectionUrl = URL + ";user=" + username + ";password=" + password;
        try (Connection conn = DriverManager.getConnection(connectionUrl)) {
            return conn != null;
        } catch (SQLException e) {
            return false;
        }
    }

    public String getUserRole(String username, String password) {
        String connectionUrl = URL + ";user=" + username + ";password=" + password;
        String sql = "SELECT r.name AS role_name " +
                "FROM sys.database_role_members drm " +
                "JOIN sys.database_principals r ON drm.role_principal_id = r.principal_id " +
                "JOIN sys.database_principals u ON drm.member_principal_id = u.principal_id " +
                "WHERE u.name = ?";
        try (Connection conn = DriverManager.getConnection(connectionUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String role = rs.getString("role_name");
                    if ("nhanvien_role".equalsIgnoreCase(role)) {
                        return "nhanvien";
                    } else if ("nhadautu_role".equalsIgnoreCase(role)) {
                        return "nhadautu";
                    }
                }
            }
        } catch (SQLException e) {
            return "loi: " + e.getMessage();
        }

        return "khong_ro_role";
    }
}
