package chungkhoan.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import chungkhoan.entity.CoPhieu;
import chungkhoan.entity.LenhDat;

public interface LenhDatRepository extends JpaRepository<LenhDat, Long> {
    // Lấy tất cả lệnh mua, chỉ lấy lệnh chờ khớp và trong ngày hiện tại
    @Query("SELECT l FROM LenhDat l WHERE l.coPhieu = :cp AND l.loaiGD = :loaiGD " +
           "AND l.trangThai IN ('Chưa', 'Chờ') AND l.ngayGD >= :startOfDay AND l.ngayGD < :endOfDay " +
           "ORDER BY l.gia DESC")
    List<LenhDat> findByCoPhieuAndLoaiGDOrderByGiaDesc(
            @Param("cp") CoPhieu cp,
            @Param("loaiGD") String loaiGD,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );

    // Lấy tất cả lệnh bán, chỉ lấy lệnh chờ khớp và trong ngày hiện tại
    @Query("SELECT l FROM LenhDat l WHERE l.coPhieu = :cp AND l.loaiGD = :loaiGD " +
           "AND l.trangThai IN ('Chưa', 'Chờ') AND l.ngayGD >= :startOfDay AND l.ngayGD < :endOfDay " +
           "ORDER BY l.gia ASC")
    List<LenhDat> findByCoPhieuAndLoaiGDOrderByGiaAsc(
            @Param("cp") CoPhieu cp,
            @Param("loaiGD") String loaiGD,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );
}