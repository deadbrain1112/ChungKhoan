package chungkhoan.repository;

import chungkhoan.entity.NhanVien;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NhanVienRepository extends JpaRepository<NhanVien, String> {
    boolean existsByCmnd(String cmnd);
    @Query("SELECT n FROM NhanVien n WHERE n.maNV = :username")
    NhanVien findByUsername(@Param("username") String username);
}