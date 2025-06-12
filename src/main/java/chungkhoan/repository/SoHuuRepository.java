package chungkhoan.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import chungkhoan.entity.SoHuu;
import chungkhoan.entity.SoHuuKey;

public interface SoHuuRepository extends JpaRepository<SoHuu, SoHuuKey> {

    // Lấy danh sách sở hữu của một nhà đầu tư
    List<SoHuu> findByMaNDT(@Param("maNDT") String maNDT);

    // Lấy danh sách mã cổ phiếu mà nhà đầu tư sở hữu
    @Query("SELECT DISTINCT s.maCP FROM SoHuu s WHERE s.maNDT = :maNDT")
    List<String> findMaCPByMaNDT(@Param("maNDT") String maNDT);
    
    Optional<SoHuu> findByMaNDTAndMaCP(String maNDT, String maCP);
    
    void deleteByNhaDauTu_MaNDTAndCoPhieu_MaCP(String maNDT, String maCP);
}