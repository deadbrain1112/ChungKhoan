package chungkhoan.repository;

import chungkhoan.entity.NhanVien;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NhanVienRepository extends JpaRepository<NhanVien, String> {
    List<NhanVien> findAll();
    NhanVien findByMaNV(String maNV);
}