package chungkhoan.repository;

import chungkhoan.entity.NhaDauTu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.util.List;

public interface NDTRepository extends JpaRepository<NhaDauTu, String> {
    NhaDauTu findByMaNDT(String maNDT);
    List<NhaDauTu> findAll();
    
    @Query("SELECT n FROM NhaDauTu n WHERE n.maNDT = :username")
    NhaDauTu findByUsername(@Param("username") String username);
}