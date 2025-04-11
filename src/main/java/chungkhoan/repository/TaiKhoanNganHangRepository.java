package chungkhoan.repository;

import chungkhoan.entity.NhaDauTu;
import chungkhoan.entity.TaiKhoanNganHang;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TaiKhoanNganHangRepository extends JpaRepository<TaiKhoanNganHang, String> {
    //TaiKhoanNganHang findByNhaDauTu(NhaDauTu nhaDauTu);
    List<TaiKhoanNganHang> findByNhaDauTu(NhaDauTu nhaDauTu);
}
