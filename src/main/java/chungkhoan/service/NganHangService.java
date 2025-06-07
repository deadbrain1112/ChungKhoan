package chungkhoan.service;

import chungkhoan.entity.NganHang;
import chungkhoan.repository.NganHangRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.sql.CallableStatement;
import java.sql.Connection;

@Service
public class NganHangService {

    @Autowired
    private NganHangRepository nganHangRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Transactional
    public void themNganHang(NganHang nganHang) {
        try {
            if (nganHang == null || nganHang.getMaNH() == null) {
                throw new IllegalArgumentException("NganHang hoặc MaNH không được null");
            }

            System.out.println("Attempting to add or check NganHang with MaNH: " + nganHang.getMaNH() +
                    ", TenNH: " + nganHang.getTenNH() +
                    ", DiaChi: " + nganHang.getDiaChi() +
                    ", Phone: " + nganHang.getPhone() +
                    ", Email: " + nganHang.getEmail());

            // Kiểm tra xem ngân hàng đã tồn tại chưa
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM nganhang WHERE MaNH = ?", Integer.class, nganHang.getMaNH());
            if (count == 0) {
                jdbcTemplate.execute(
                        (Connection conn) -> {
                            CallableStatement cs = conn.prepareCall("{call sp_ThemNganHang(?, ?, ?, ?, ?)}");
                            cs.setString(1, nganHang.getMaNH());
                            cs.setString(2, nganHang.getTenNH());
                            cs.setString(3, nganHang.getDiaChi());
                            cs.setString(4, nganHang.getPhone());
                            cs.setString(5, nganHang.getEmail());
                            boolean executed = cs.execute();
                            System.out.println("Stored procedure sp_ThemNganHang executed for MaNH " + nganHang.getMaNH() + ": " + executed);
                            return null;
                        }
                );
            } else {
                System.out.println("NganHang with MaNH " + nganHang.getMaNH() + " already exists, skipping.");
            }

            // Xác nhận dữ liệu sau khi lưu
            NganHang saved = nganHangRepository.findById(nganHang.getMaNH()).orElse(null);
            if (saved != null) {
                System.out.println("Confirmed NganHang in DB: MaNH=" + saved.getMaNH() + ", TenNH=" + saved.getTenNH());
            } else {
                System.out.println("Warning: NganHang with MaNH " + nganHang.getMaNH() + " not found after save.");
            }
        } catch (Exception e) {
            System.out.println("Exception during themNganHang: " + e.getMessage());
            throw new RuntimeException("Lỗi khi thêm ngân hàng: " + e.getMessage());
        }
    }

    public boolean existsByMaNH(String maNH) {
        return nganHangRepository.existsByMaNH(maNH);
    }
}