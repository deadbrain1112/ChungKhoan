package chungkhoan.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class TaiKhoanService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void taoTaiKhoan(String tenDangNhap, String matKhau, String loai, String maLienKet) {

        // Gọi stored procedure để tạo login và cấp quyền server (ở master)
        String sqlTaoLogin = "EXEC master.dbo.sp_CreateLoginAndGrantServerRole ?, ?, ?";

        // Gọi stored procedure để tạo user và cấp quyền database (ở QUANLYGIAODICHCHUNGKHOAN)
        String sqlTaoUser = "EXEC QUANLYGIAODICHCHUNGKHOAN.dbo.sp_CreateUserAndGrantDatabaseRoles ?, ?, ?, ?";

        try {
            jdbcTemplate.update(sqlTaoLogin, tenDangNhap, matKhau, loai);
            jdbcTemplate.update(sqlTaoUser, tenDangNhap, tenDangNhap, loai, matKhau);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
