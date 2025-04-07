package chungkhoan.config;

import org.springframework.jdbc.datasource.DriverManagerDataSource;
import javax.sql.DataSource;

public class DatabaseConfig {

    public static DataSource getDataSource(String username, String password) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        dataSource.setUrl("jdbc:sqlserver://localhost:1433;databaseName=QUANLYGIAODICHCHUNGKHOAN;encrypt=false;trustServerCertificate=true");
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        return dataSource;
    }
}
