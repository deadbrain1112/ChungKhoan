package chungkhoan.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class TaiKhoanService {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void taoTaiKhoan(String tenDangNhap, String matKhau, String loai) {
        String sql = "EXEC sp_TaoTaiKhoan ?, ?, ?";
        jdbcTemplate.update(sql, tenDangNhap, matKhau, loai);
    }
}
