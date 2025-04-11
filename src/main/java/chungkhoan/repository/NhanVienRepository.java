package chungkhoan.repository;

import chungkhoan.entity.NhanVien;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NhanVienRepository extends JpaRepository<NhanVien, String> {
    List<NhanVien> findAll();
    NhanVien findByMaNV(String maNV);
}