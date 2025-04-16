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
    @Query("SELECT l FROM LichSuGia l WHERE l.maCP = :maCP AND l.ngay < :ngay ORDER BY l.ngay DESC")
    Optional<LichSuGia> findFirstByMaCPAndNgayLessThanOrderByNgayDesc(
            @Param("maCP") String maCP,
            @Param("ngay") Timestamp ngay
    );
}