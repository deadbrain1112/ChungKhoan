package chungkhoan.repository;

import chungkhoan.entity.LichSuGia;
import chungkhoan.entity.LichSuGiaKey;

import java.security.Timestamp;
import java.util.Optional;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

@Repository
public interface LichSuGiaRepository extends JpaRepository<LichSuGia, LichSuGiaKey> {
	Optional<LichSuGia> findById(LichSuGiaKey key);
}

