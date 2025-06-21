package chungkhoan.repository;

import chungkhoan.entity.LichSuGia;
import chungkhoan.entity.LichSuGiaKey;

import java.sql.Timestamp;
import java.util.Optional;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LichSuGiaRepository extends JpaRepository<LichSuGia, LichSuGiaKey> {
    Optional<LichSuGia> findById(LichSuGiaKey key);

    @Query("SELECT l.giaTC FROM LichSuGia l WHERE l.maCP = :maCP ORDER BY l.ngay DESC LIMIT 1")
    Float getGiaThamChieuMoiNhat(@Param("maCP") String maCP);

    // Lấy bản ghi mới nhất trước ngày hiện tại, đảm bảo chỉ trả về 1 bản ghi
    @Query(value = "SELECT TOP 1 * FROM LichSuGia WHERE maCP = :maCP AND ngay < :ngay ORDER BY ngay DESC", nativeQuery = true)
    Optional<LichSuGia> findFirstByMaCPAndNgayLessThan(
            @Param("maCP") String maCP,
            @Param("ngay") Timestamp ngay
    );

    @Query(nativeQuery = true, value = "EXEC sp_LayGiaMoiNhat :maCP")
    LichSuGia layGiaMoiNhat(@Param("maCP") String maCP);
    
    Optional<LichSuGia> findByMaCPAndNgay(String maCP, Timestamp ngay);
    Optional<LichSuGia> findFirstByMaCPAndNgayBeforeOrderByNgayDesc(String maCP, Timestamp ngay);

}