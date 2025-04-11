package chungkhoan.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import chungkhoan.entity.CoPhieu;
import chungkhoan.entity.LenhDat;


public interface LenhDatRepository extends JpaRepository<LenhDat, Long> {
    List<LenhDat> findTop3ByCoPhieuAndLoaiGDOrderByGiaDesc(CoPhieu cp, String loaiGD); // Mua
    List<LenhDat> findTop3ByCoPhieuAndLoaiGDOrderByGiaAsc(CoPhieu cp, String loaiGD); // Bán
}


