package chungkhoan.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import chungkhoan.entity.CoPhieu;
import chungkhoan.entity.LenhKhop;

public interface LenhKhopRepository extends JpaRepository<LenhKhop, Long> {
    LenhKhop findTopByLenhDat_CoPhieuOrderByNgayGioKhopDesc(CoPhieu cp);

    @Query("SELECT SUM(lk.soLuongKhop) FROM LenhKhop lk WHERE lk.lenhDat.coPhieu = :cp")
    Long sumSoLuongKhopByCoPhieu(@Param("cp") CoPhieu cp);
}


