package chungkhoan.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Service
public class BackupService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<String> getAllDatabases() {
        return jdbcTemplate.queryForList(
                "SELECT name FROM sys.databases WHERE database_id > 4", String.class);
    }

    public List<Map<String, Object>> getBackupHistory(String dbName) {
        String sql = """
            SELECT 
                ROW_NUMBER() OVER (ORDER BY bs.backup_finish_date DESC) AS position,
                bs.description,
                bs.backup_finish_date AS backupTime,
                bs.user_name AS [user]
            FROM msdb.dbo.backupset bs
            JOIN msdb.dbo.backupmediafamily bmf ON bs.media_set_id = bmf.media_set_id
            WHERE bs.database_name = ? AND bs.type = 'D'
        """;
        return jdbcTemplate.queryForList(sql, dbName);
    }

    public void createBackupDevice(String dbName) {
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

    public void backupDatabase(String dbName, boolean deleteOld) {
        String deviceName = "DEVICE_" + dbName;
        String command = deleteOld
                ? "BACKUP DATABASE [" + dbName + "] TO [" + deviceName + "] WITH INIT, DESCRIPTION = 'Sao lưu toàn bộ'"
                : "BACKUP DATABASE [" + dbName + "] TO [" + deviceName + "] WITH DESCRIPTION = 'Sao lưu toàn bộ'";
        jdbcTemplate.execute(command);
    }

    public void restoreDatabase(String dbName) {
        String command = """
            ALTER DATABASE [%s] SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
            RESTORE DATABASE [%s] FROM [%s] WITH REPLACE;
            ALTER DATABASE [%s] SET MULTI_USER;
        """.formatted(dbName, dbName, "DEVICE_" + dbName, dbName);
        jdbcTemplate.execute(command);
    }

    public void restoreToTime(String dbName, LocalDate date, LocalTime time) {
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
