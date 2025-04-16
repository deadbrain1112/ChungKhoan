package chungkhoan.service;

import chungkhoan.repository.DatabaseRepository;
import chungkhoan.util.DynamicJdbcUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

@Service
public class DatabaseService {

    private static final String URL = "jdbc:sqlserver://localhost:1433;databaseName=QUANLYGIAODICHCHUNGKHOAN;encrypt=false;trustServerCertificate=true;charset=UTF-8";
    private static final String DRIVER_CLASS_NAME = "com.microsoft.sqlserver.jdbc.SQLServerDriver";

    @Autowired
    private DatabaseRepository repository;

    public List<String> getDatabases() {
        return repository.getAllUserDatabases();
    }

    public JdbcTemplate createJdbcTemplate(String username, String password) {
        // Nếu bạn dùng DynamicJdbcUtil thì giữ dòng sau:
        return DynamicJdbcUtil.create(username, password);

        // Hoặc nếu không dùng DynamicJdbcUtil thì dùng đoạn bên dưới:
        /*
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(URL);
        dataSource.setDriverClassName(DRIVER_CLASS_NAME);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        return new JdbcTemplate(dataSource);
        */
    }

    public boolean testConnection(String username, String password) {
        try (Connection conn = DriverManager.getConnection(URL, username, password)) {
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getUserRole(JdbcTemplate jdbcTemplate, String username) {
        String sql = """
        SELECT r.name AS role_name
        FROM sys.database_role_members drm
        JOIN sys.database_principals r ON drm.role_principal_id = r.principal_id
        JOIN sys.database_principals u ON drm.member_principal_id = u.principal_id
        WHERE u.name = ?
        """;

        try {
            return jdbcTemplate.query(sql, new Object[]{username}, rs -> {
                while (rs.next()) {
                    String role = rs.getString("role_name");
                    if ("nhanvien_role".equalsIgnoreCase(role)) return "nhanvien";
                    if ("nhadautu_role".equalsIgnoreCase(role)) return "nhadautu";
                }
                return "khong_ro_role";
            });
        } catch (Exception e) {
            return "loi: " + e.getMessage();
        }
    }
}
