package chungkhoan.util;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class UserJdbcSession {
    private final ThreadLocal<JdbcTemplate> jdbcTemplate = new ThreadLocal<>();

    public void set(JdbcTemplate template) {
        jdbcTemplate.set(template);
    }

    public JdbcTemplate get() {
        return jdbcTemplate.get();
    }

    public void clear() {
        jdbcTemplate.remove();
    }
}
