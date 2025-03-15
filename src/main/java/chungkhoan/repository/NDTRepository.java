package chungkhoan.repository;

import chungkhoan.entity.NDT;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NDTRepository extends JpaRepository<NDT, String> {
    NDT findByMaNDT(String maNDT);
    List<NDT> findAll();
}