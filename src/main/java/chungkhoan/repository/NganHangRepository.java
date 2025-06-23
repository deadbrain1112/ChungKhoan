package chungkhoan.repository;

import chungkhoan.entity.NganHang;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NganHangRepository extends JpaRepository<NganHang, String> {

    boolean existsByMaNH(String maNH);
    
    Optional<NganHang> findByMaNH(String maNH);
    
}