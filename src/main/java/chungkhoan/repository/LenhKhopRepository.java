package chungkhoan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import chungkhoan.entity.CoPhieu;
import chungkhoan.entity.LenhKhop;

import java.time.LocalDateTime;
import java.util.List;

public interface LenhKhopRepository extends JpaRepository<LenhKhop, Long> {

    // Lấy lệnh khớp mới nhất theo ngayGioKhop, thêm tiêu chí phụ để đảm bảo 1 bản ghi duy nhất
    @Query("SELECT lk FROM LenhKhop lk WHERE lk.lenhDat.coPhieu = :cp " +
           "AND lk.ngayGioKhop >= :startOfDay AND lk.ngayGioKhop < :endOfDay " +
           "ORDER BY lk.ngayGioKhop DESC, lk.maLK DESC")
    List<LenhKhop> findTopByLenhDat_CoPhieuOrderByNgayGioKhopDesc(
            @Param("cp") CoPhieu cp,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );
    
    @Query("SELECT lk FROM LenhKhop lk WHERE lk.lenhDat.coPhieu.maCP = :maCP ORDER BY lk.ngayGioKhop DESC")
    LenhKhop findLatestKhopLenh(@Param("maCP") String maCP);

    // Tính tổng khối lượng khớp trong ngày hiện tại
    @Query("SELECT SUM(lk.soLuongKhop) FROM LenhKhop lk WHERE lk.lenhDat.coPhieu = :cp " +
           "AND lk.ngayGioKhop >= :startOfDay AND lk.ngayGioKhop < :endOfDay")
    Long sumSoLuongKhopByCoPhieu(
            @Param("cp") CoPhieu cp,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );

    @Query(value = "SELECT SUM(SoLuongKhop) FROM LenhKhop WHERE MaGD = :maGD", nativeQuery = true)
    Integer tongSoLuongKhop(@Param("maGD") Long maGD);

    @Query(value = "SELECT * FROM LenhKhop WHERE MaGD = :maGD ORDER BY NgayGioKhop DESC", nativeQuery = true)
    List<LenhKhop> findAllByMaGDOrderByNgayGioKhopDesc(@Param("maGD") Long maGD);
    
	    //Lệnh khớp cuối cùng trong ngày của một mã CP
    	@Query(value = """
    		    SELECT TOP 1 kh.*
    		    FROM LenhKhop kh
    		    JOIN LenhDat ld ON kh.MaGD = ld.MaGD
    		    JOIN CoPhieu cp ON ld.MaCP = cp.MaCP
    		    WHERE cp.MaCP = :maCP
    		      AND CONVERT(date, kh.NgayGioKhop) = :ngay
    		    ORDER BY kh.NgayGioKhop DESC
    		    """, nativeQuery = true)
    		LenhKhop findLenhKhopCuoiTrongNgay(@Param("maCP") String maCP, @Param("ngay") String ngay);

    @Query("SELECT COALESCE(SUM(lk.soLuongKhop), 0) FROM LenhKhop lk WHERE lk.lenhDat.maGD = :maGD")
    int sumSoLuongKhopByLenhDatId(@Param("maGD") Long maGD);
}