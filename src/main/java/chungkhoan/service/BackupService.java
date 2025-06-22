package chungkhoan.service;

import chungkhoan.util.DynamicJdbcUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BackupService {

    @Autowired
    private HttpSession session;

    private String getUsernameFromSession() {
        return (String) session.getAttribute("username");
    }

    private String getPasswordFromSession() {
        return (String) session.getAttribute("password");
    }

    private JdbcTemplate getJdbcTemplate() {
        String username = getUsernameFromSession();
        String password = getPasswordFromSession();
        return DynamicJdbcUtil.create(username, password);
    }


    public List<Map<String, Object>> getBackupHistory(String database, String device) {
        JdbcTemplate jdbcTemplate = getJdbcTemplate();

        SimpleJdbcCall jdbcCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("sp_DanhSachBackUp")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("database", Types.NVARCHAR),
                        new SqlParameter("device", Types.NVARCHAR)
                );

        Map<String, Object> inParams = new HashMap<>();
        inParams.put("database", database);
        inParams.put("device", device);

        Map<String, Object> result = jdbcCall.execute(inParams);

        return (List<Map<String, Object>>) result.get("#result-set-1");
    }


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

    public void backupDatabase(String dbName, boolean deleteOld) {
        JdbcTemplate jdbcTemplate = getJdbcTemplate();
        String deviceName = "[DEVICE_" + dbName + "]";

        jdbcTemplate.update("EXEC sp_SaoLuuCSDL ?, ?, ?", dbName, deviceName, deleteOld ? 1 : 0);
    }

    public void restoreBackup(String path, int index) {
        JdbcTemplate jdbcTemplate = getJdbcTemplate();

        String sql = "use master;EXEC master.dbo.sp_PhucHoiCSDL ?, ?";
        jdbcTemplate.update(sql, path, index);
    }



}
