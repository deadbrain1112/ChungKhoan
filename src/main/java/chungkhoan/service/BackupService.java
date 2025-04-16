package chungkhoan.service;

import chungkhoan.util.DynamicJdbcUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class BackupService {

    @Autowired
    private HttpSession session;  // Inject HttpSession vào service

    // Lấy thông tin username từ session
    private String getUsernameFromSession() {
        // Lấy username từ session
        return (String) session.getAttribute("username");
    }

    // Lấy mật khẩu từ session
    private String getPasswordFromSession() {
        // Lấy mật khẩu từ session
        return (String) session.getAttribute("password");
    }




    // Tạo phương thức này để sử dụng JdbcTemplate động
    private JdbcTemplate getJdbcTemplate() {
        String username = getUsernameFromSession();
        String password = getPasswordFromSession();
        return DynamicJdbcUtil.create(username, password);
    }

    // Lấy danh sách tất cả các cơ sở dữ liệu
    public List<String> getAllDatabases() {
        JdbcTemplate jdbcTemplate = getJdbcTemplate();
        return jdbcTemplate.queryForList(
                "SELECT name FROM sys.databases WHERE database_id > 4", String.class);
    }

    public List<Map<String, Object>> getBackupHistory(String dbName) {
        JdbcTemplate jdbcTemplate = getJdbcTemplate();
        String sql = """
        SELECT 
            bs.position,
            bs.description,
            bs.backup_finish_date,
            bs.user_name AS [user]
        FROM msdb.dbo.backupset bs
        JOIN msdb.dbo.backupmediafamily bmf ON bs.media_set_id = bmf.media_set_id
        WHERE bs.database_name = ? AND bs.type = 'D'
        ORDER BY bs.backup_finish_date DESC
    """;

        List<Map<String, Object>> rawList = jdbcTemplate.queryForList(sql, dbName);

        // Format lại thời gian trong Java
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm:ss a");

        for (Map<String, Object> row : rawList) {
            Timestamp ts = (Timestamp) row.get("backup_finish_date");
            LocalDateTime ldt = ts.toLocalDateTime();
            row.put("backupTime", ldt.format(formatter));
            row.remove("backup_finish_date");
        }

        return rawList;
    }



    // Tạo backup device nếu chưa tồn tại
    public void createBackupDevice(String dbName) {
        JdbcTemplate jdbcTemplate = getJdbcTemplate();
        String logicalName = "DEVICE_" + dbName;
        String path = "C:\\Backup\\" + dbName + ".bak";

        String check = """
            IF NOT EXISTS (
                SELECT * FROM sys.backup_devices WHERE name = ?
            )
            EXEC sp_addumpdevice 'disk', ?, ?
        """;

        jdbcTemplate.update(check, logicalName, logicalName, path);
    }

    // Sao lưu cơ sở dữ liệu
    public void backupDatabase(String dbName, boolean deleteOld) {
        JdbcTemplate jdbcTemplate = getJdbcTemplate();
        String deviceName = "DEVICE_" + dbName;
        String command = deleteOld
                ? "BACKUP DATABASE [" + dbName + "] TO [" + deviceName + "] WITH INIT, DESCRIPTION = 'Sao lưu toàn bộ'"
                : "BACKUP DATABASE [" + dbName + "] TO [" + deviceName + "] WITH DESCRIPTION = 'Sao lưu toàn bộ'";
        jdbcTemplate.execute(command);
    }

    // Phục hồi cơ sở dữ liệu từ backup
    public void restoreDatabase(String dbName) {
        JdbcTemplate jdbcTemplate = getJdbcTemplate();
        String command = """
            ALTER DATABASE [%s] SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
            RESTORE DATABASE [%s] FROM [%s] WITH REPLACE;
            ALTER DATABASE [%s] SET MULTI_USER;
        """.formatted(dbName, dbName, "DEVICE_" + dbName, dbName);
        jdbcTemplate.execute(command);
    }

    // Phục hồi cơ sở dữ liệu đến một thời điểm nhất định
    public void restoreToTime(String dbName, LocalDate date, LocalTime time) {
        JdbcTemplate jdbcTemplate = getJdbcTemplate();
        String datetime = date + " " + time;
        String command = """
            ALTER DATABASE [%s] SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
            RESTORE DATABASE [%s] FROM [%s] WITH NORECOVERY;
            RESTORE LOG [%s] FROM [%s] WITH STOPAT = '%s', RECOVERY;
            ALTER DATABASE [%s] SET MULTI_USER;
        """.formatted(dbName, dbName, "DEVICE_" + dbName, dbName, "DEVICE_" + dbName, datetime, dbName);
        jdbcTemplate.execute(command);
    }
}
