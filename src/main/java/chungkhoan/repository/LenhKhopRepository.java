package chungkhoan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import chungkhoan.entity.CoPhieu;
import chungkhoan.entity.LenhKhop;

import java.time.LocalDateTime;

public interface LenhKhopRepository extends JpaRepository<LenhKhop, Long> {

    // Lấy lệnh khớp mới nhất theo ngayGioKhop, thêm tiêu chí phụ để đảm bảo 1 bản ghi duy nhất
    @Query("SELECT lk FROM LenhKhop lk WHERE lk.lenhDat.coPhieu = :cp " +
           "AND lk.ngayGioKhop >= :startOfDay AND lk.ngayGioKhop < :endOfDay " +
           "ORDER BY lk.ngayGioKhop DESC, lk.maLK DESC")
    LenhKhop findTopByLenhDat_CoPhieuOrderByNgayGioKhopDesc(
            @Param("cp") CoPhieu cp,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );

    // Tính tổng khối lượng khớp trong ngày hiện tại
    @Query("SELECT SUM(lk.soLuongKhop) FROM LenhKhop lk WHERE lk.lenhDat.coPhieu = :cp " +
           "AND lk.ngayGioKhop >= :startOfDay AND lk.ngayGioKhop < :endOfDay")
    Long sumSoLuongKhopByCoPhieu(
            @Param("cp") CoPhieu cp,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );
}