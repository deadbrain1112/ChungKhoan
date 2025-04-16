package chungkhoan.util;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

public class DynamicJdbcUtil {
    public static JdbcTemplate create(String username, String password) {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        ds.setUrl("jdbc:sqlserver://localhost:1433;databaseName=QUANLYGIAODICHCHUNGKHOAN;encrypt=false;trustServerCertificate=true");
        ds.setUsername(username);
        ds.setPassword(password);
        return new JdbcTemplate(ds);
    }
}
