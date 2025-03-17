package chungkhoan.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Types;

@Service
public class UserService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public String authenticateUser(String username, String password) {
        try (Connection conn = jdbcTemplate.getDataSource().getConnection()) {
            CallableStatement stmt = conn.prepareCall("{CALL sp_XacThucTaiKhoan(?, ?, ?)}");
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.registerOutParameter(3, Types.VARCHAR);

            stmt.execute();
            return stmt.getString(3);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean themNhanVien(String username, String password, String maNV, String tenNV, String ngaySinh, String email, String soDienThoai) {
        try (Connection conn = jdbcTemplate.getDataSource().getConnection()) {
            CallableStatement stmt = conn.prepareCall("{CALL sp_ThemNhanVien(?, ?, ?, ?, ?, ?, ?, ?)}");
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setString(3, maNV);
            stmt.setString(4, tenNV);
            stmt.setString(5, ngaySinh);
            stmt.setString(6, email);
            stmt.setString(7, soDienThoai);
            stmt.registerOutParameter(8, Types.INTEGER);

            stmt.execute();
            return stmt.getInt(8) == 1;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean themNhaDauTu(String username, String password, String maNDT, String tenNDT, String ngaySinh, String email, String soDienThoai) {
        try (Connection conn = jdbcTemplate.getDataSource().getConnection()) {
            CallableStatement stmt = conn.prepareCall("{CALL sp_ThemNhaDauTu(?, ?, ?, ?, ?, ?, ?, ?)}");
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setString(3, maNDT);
            stmt.setString(4, tenNDT);
            stmt.setString(5, ngaySinh);
            stmt.setString(6, email);
            stmt.setString(7, soDienThoai);
            stmt.registerOutParameter(8, Types.INTEGER);

            stmt.execute();
            return stmt.getInt(8) == 1;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getMaNDT(String username) {
        String sql = "SELECT MaNDT FROM NhaDauTu WHERE Username = ?";
        return jdbcTemplate.queryForObject(sql, new Object[]{username}, String.class);
    }

    public String getMaNV(String username) {
        String sql = "SELECT MaNV FROM NhanVien WHERE Username = ?";
        return jdbcTemplate.queryForObject(sql, new Object[]{username}, String.class);
    }
}
