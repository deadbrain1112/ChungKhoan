package chungkhoan.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import chungkhoan.entity.CoPhieu;
import chungkhoan.entity.LenhDat;
import jakarta.transaction.Transactional;

public interface LenhDatRepository extends JpaRepository<LenhDat, Long> {
	// MUA: Lấy lệnh mua đang chờ hoặc một phần khớp, theo giá giảm dần và ngày tăng dần
	List<LenhDat> findByCoPhieu_MaCPAndLoaiGDAndTrangThaiInOrderByGiaDescNgayGDAsc(
	    String maCP,
	    String loaiGD,
	    List<String> trangThai
	);

	// BÁN: Lấy lệnh bán đang chờ hoặc một phần khớp, theo giá tăng dần và ngày tăng dần
	List<LenhDat> findByCoPhieu_MaCPAndLoaiGDAndTrangThaiInOrderByGiaAscNgayGDAsc(
	    String maCP,
	    String loaiGD,
	    List<String> trangThai
	);
    
    @Query(value = "EXEC sp_TimLenhDatTheoNhaDauTu :maNDT", nativeQuery = true)
    List<LenhDat> timLenhDatTheoMaNDT(@Param("maNDT") String maNDT);
    
    // Khớp lệnhs
    List<LenhDat> findByCoPhieu_MaCPAndLoaiGDAndTrangThaiOrderByGiaDescNgayGDAsc(String maCP, String loaiGD, String trangThai);
    List<LenhDat> findByCoPhieu_MaCPAndLoaiGDAndTrangThaiOrderByGiaAscNgayGDAsc(String maCP, String loaiGD, String trangThai);
    @Query("SELECT DISTINCT ld.coPhieu.maCP FROM LenhDat ld WHERE LOWER(ld.trangThai) = LOWER(:status)")
    List<String> findAllMaCPDangChoKhop(@Param("status") String status);
    
    List<LenhDat> findByTrangThai(String trangThai);

    // Cập nhật trạng thái lệnh theo ID
    @Modifying
    @Transactional
    @Query("UPDATE LenhDat l SET l.trangThai = :newStatus WHERE l.maGD = :maGD")
    void capNhatTrangThaiLenh(@Param("maGD") Long maGD, @Param("newStatus") String newStatus);

    // Cập nhật trạng thái tất cả lệnh "Chờ" sang "Hủy" (khi hết giờ giao dịch)
    @Modifying
    @Transactional
    @Query("UPDATE LenhDat l SET l.trangThai = 'Hủy' WHERE l.trangThai = 'Chờ'")
    void huyTatCaLenhCho();
}