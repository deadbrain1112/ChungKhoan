package chungkhoan.repository;

import chungkhoan.entity.NhaDauTu;
import chungkhoan.entity.NhanVien;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.util.List;

public interface NDTRepository extends JpaRepository<NhaDauTu, String> {
    NhaDauTu findByMaNDT(String maNDT);
    List<NhaDauTu> findAll();
    
    @Query("SELECT n FROM NhaDauTu n WHERE n.maNDT = :username")
    NhaDauTu findByUsername(@Param("username") String username);

    @Query(value = """
    SELECT * FROM ndt nhadautu
    WHERE EXISTS (
        SELECT 1 FROM sys.sql_logins sl
        WHERE sl.name = nhadautu.mandt
    )
    """, nativeQuery = true)
    List<NhaDauTu> findAllNhaDauTuCoTaiKhoan();
    @Query(value = """
    SELECT * FROM ndt nhadautu
    WHERE NOT EXISTS (
        SELECT 1 FROM sys.sql_logins sl
        WHERE sl.name = nhadautu.maNDT
    )
    """, nativeQuery = true)
    List<NhaDauTu> findAllNhaDauTuChuaCoTaiKhoan();
}