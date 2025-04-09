package chungkhoan.repository;

import chungkhoan.entity.NhaDauTu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NDTRepository extends JpaRepository<NhaDauTu, String> {
    NhaDauTu findByMaNDT(String maNDT);
    List<NhaDauTu> findAll();

    @Procedure(name = "sp_ThemNhaDauTu")
    void themNhaDauTu(
            @Param("MaNDT") String maNDT,
            @Param("HoTen") String hoTen,
            @Param("NgaySinh") java.sql.Date ngaySinh,
            @Param("MKGD") String mkgd,
            @Param("DiaChi") String diaChi,
            @Param("Phone") String phone,
            @Param("CMND") String cmnd,
            @Param("GioiTinh") String gioiTinh,
            @Param("Email") String email
    );
}