package chungkhoan.repository;

import chungkhoan.entity.NhaDauTu;
import chungkhoan.entity.TaiKhoanNganHang;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaiKhoanNganHangRepository extends JpaRepository<TaiKhoanNganHang, String> {

    // Tìm danh sách tài khoản ngân hàng theo đối tượng NhaDauTu
    List<TaiKhoanNganHang> findByNhaDauTu(NhaDauTu nhaDauTu);

    // Tìm danh sách tài khoản ngân hàng theo maNDT
    @Query("SELECT t FROM TaiKhoanNganHang t WHERE t.nhaDauTu.maNDT = :maNDT")
    List<TaiKhoanNganHang> findByNhaDauTuMaNDT(@Param("maNDT") String maNDT);

    // Xóa tất cả tài khoản ngân hàng theo maNDT
    @Modifying
    @Query("DELETE FROM TaiKhoanNganHang t WHERE t.nhaDauTu.maNDT = :maNDT")
    void deleteByNhaDauTuMaNDT(@Param("maNDT") String maNDT);
    
    @Query("SELECT DISTINCT tk FROM TaiKhoanNganHang tk " +
            "WHERE EXISTS (" +
            "    SELECT 1 FROM LenhDat ld " +
            "    WHERE ld.taiKhoanNganHang.maTK = tk.maTK" +
            ")")
     List<TaiKhoanNganHang> findTaiKhoanNganHangInLenhDat();
}