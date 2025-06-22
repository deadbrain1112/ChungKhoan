package chungkhoan.repository;

import chungkhoan.entity.CoPhieu;
import chungkhoan.entity.NhaDauTu;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.transaction.Transactional;

//@Repository
public interface CoPhieuRepository extends JpaRepository<CoPhieu, String> {

    @Modifying
    @Transactional
    @Query(value = "EXEC sp_ThemCoPhieu :maCP, :tenCty, :diaChi, :soLuongPH", nativeQuery = true)
    void themCoPhieu(
            @Param("maCP") String maCP,
            @Param("tenCty") String tenCty,
            @Param("diaChi") String diaChi,
            @Param("soLuongPH") int soLuongPH
    );

    List<CoPhieu> findByMaCPIn(List<String> maCPs);

    @Query("SELECT c.maCP FROM CoPhieu c")
    List<String> findAllMaCP();

    boolean existsByMaCP(String maCP);
}
