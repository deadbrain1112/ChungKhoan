package chungkhoan.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class DatabaseRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<String> getAllUserDatabases() {
        String sql = "SELECT name FROM sys.databases WHERE name NOT IN ('master', 'tempdb', 'model', 'msdb') ORDER BY name";
        return jdbcTemplate.queryForList(sql, String.class);
    }
}
