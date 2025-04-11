package chungkhoan.repository;

import chungkhoan.entity.NhaDauTu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.util.List;

public interface NDTRepository extends JpaRepository<NhaDauTu, String> {
    NhaDauTu findByMaNDT(String maNDT);
    List<NhaDauTu> findAll();

    @Modifying
    @Transactional
    @Query(value = "EXEC sp_ThemNhaDauTu :hoTen, :ngaySinh, :mkgd, :diaChi, :phone, :cmnd, :gioiTinh, :email", nativeQuery = true)
    void themNhaDauTu(
            @Param("hoTen") String hoTen,
            @Param("ngaySinh") Date ngaySinh,
            @Param("mkgd") String mkgd,
            @Param("diaChi") String diaChi,
            @Param("phone") String phone,
            @Param("cmnd") String cmnd,
            @Param("gioiTinh") String gioiTinh,
            @Param("email") String email
    );
}