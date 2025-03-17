package chungkhoan.repository;

import chungkhoan.entity.NhaDauTu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NDTRepository extends JpaRepository<NhaDauTu, String> {
    NhaDauTu findByMaNDT(String maNDT);
    List<NhaDauTu> findAll();
}