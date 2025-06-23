package chungkhoan.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
    
    // Lấy các mã cổ phiếu có khả năng khớp lệnh LO
    @Query(value = "EXEC sp_FindMaCPCoTheKhop :loaiLenh, :trangThai1, :trangThai2", nativeQuery = true)
    List<String> findMaCPCoTheKhop(
        @Param("loaiLenh") String loaiLenh,
        @Param("trangThai1") String trangThai1,
        @Param("trangThai2") String trangThai2
    );

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

    // Sao kê
    List<LenhDat> findByTaiKhoanNganHang_NhaDauTu_MaNDT(String maNDT);
    List<LenhDat> findByTaiKhoanNganHang_NhaDauTu_MaNDTAndTrangThai(String maNDT, String trangThai);

    List<LenhDat> findByCoPhieu_MaCPAndTrangThaiIn(String maCP, List<String> trangThai);
    List<LenhDat> findByLoaiLenhInAndTrangThai(List<String> loaiLenh, String trangThai);
    @Query("SELECT DISTINCT tk.maTK FROM TaiKhoanNganHang tk WHERE tk.maTK NOT IN (SELECT l.taiKhoanNganHang.maTK FROM LenhDat l WHERE l.taiKhoanNganHang.maTK IS NOT NULL)")
    List<String> findDistinctMaTK();

    @Query(
            value = """
                SELECT * FROM lenhdat 
                WHERE LTRIM(RTRIM(MaCP)) = :maCP 
                AND trangthai IN (:trangThai)
            """,
            nativeQuery = true
    )
    List<LenhDat> findKhoppableNative(@Param("maCP") String maCP, @Param("trangThai") List<String> trangThai);
    
    @Query(value = "SELECT ld.* " +
            "FROM lenhdat ld " +
            "JOIN taikhoan_nganhang tknh ON ld.MaTK = tknh.MaTK " +
            "JOIN ndt nd ON tknh.MaNDT = nd.MaNDT " +
            "WHERE nd.MaNDT = :maNDT " +
            "AND ld.MaCP = :maCP " +
            "AND ld.NgayGD BETWEEN :startDate AND :endDate " +
            "AND (:trangThai IS NULL OR :trangThai = '' OR ld.TrangThai = :trangThai) " +
            "ORDER BY ld.NgayGD DESC",
    nativeQuery = true)
	List<LenhDat> findByMaNDTAndMaCPAndNgayGDAndTrangThai(
	     @Param("maNDT") String maNDT,
	     @Param("maCP") String maCP,
	     @Param("startDate") LocalDateTime startDate,
	     @Param("endDate") LocalDateTime endDate,
	     @Param("trangThai") String trangThai);

    // Top 3 giá mua cuối phiên chưa khớp
    @Query(value = """
    	    SELECT TOP 3 ld.gia, SUM(ld.soluong) AS TongKL
    	    FROM lenhdat ld
    	    WHERE ld.loaigd = 'M'
    	      AND ld.trangthai = 'Chờ'
    	      AND ld.macp = :maCP
    	      AND CONVERT(date, ld.ngaygd) = :ngay
    	    GROUP BY ld.gia
    	    ORDER BY ld.gia DESC
    	    """, nativeQuery = true)
    List<Object[]> findTop3GiaMuaSnapshot(@Param("maCP") String maCP, @Param("ngay") String ngay);

	// Top 3 giá bán cuối phiên chưa khớp
    @Query(value = """
    		SELECT TOP 3 ld.gia, SUM(ld.soluong) AS TongKL
    		FROM lenhdat ld
    		WHERE ld.loaigd = 'B'
    		AND ld.trangthai = 'Chờ'
    		AND ld.macp = :maCP
    		AND CONVERT(date, ld.ngaygd) = :ngay
    		GROUP BY ld.gia
    		ORDER BY ld.gia ASC
    	""", nativeQuery = true)
    List<Object[]> findTop3GiaBanSnapshot(@Param("maCP") String maCP, @Param("ngay") String ngay);

    @Query(value = """
    	    SELECT DISTINCT MaCP
    	    FROM LenhDat
    	    WHERE LoaiLenh IN ('ATO', 'ATC')
    	      AND TrangThai = N'Chờ'
    	    """, nativeQuery = true)
    List<String> findMaCPConLenhATX();
    
    @Query("""
    	    SELECT l FROM LenhDat l
			WHERE l.coPhieu.maCP = :maCP
			  AND l.loaiGD = :loaiGD
			  AND l.trangThai IN :trangThais
			  AND l.loaiLenh IN :loaiLenhs
			  AND (
			    l.loaiLenh NOT IN ('ATO', 'ATC')
			    OR (l.loaiLenh IN ('ATO', 'ATC') AND l.gia = 0)
			  )
			ORDER BY 
			  CASE WHEN :loaiGD = 'M' THEN l.gia END DESC,
			  CASE WHEN :loaiGD = 'B' THEN l.gia END ASC,
			  l.ngayGD ASC

    	""")
    	List<LenhDat> findByMaCPLoaiGDTrangThaiLoaiLenh(
    	    @Param("maCP") String maCP,
    	    @Param("loaiGD") String loaiGD,
    	    @Param("trangThais") List<String> trangThais,
    	    @Param("loaiLenhs") List<String> loaiLenhs
    	);

}