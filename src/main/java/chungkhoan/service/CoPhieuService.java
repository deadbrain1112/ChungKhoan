package chungkhoan.service;

import chungkhoan.entity.CoPhieu;
import chungkhoan.repository.CoPhieuRepository;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

@Service
public class CoPhieuService {

    @Autowired
    private CoPhieuRepository coPhieuRepository;

    public Page<CoPhieu> getPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("maCP").ascending());
        return coPhieuRepository.findAll(pageable);
    }

    // Ghi hoặc cập nhật
    public CoPhieu save(CoPhieu stock) {
        return coPhieuRepository.save(stock);
    }

    // Thêm mới sử dụng stored procedure
    public void themCoPhieuBangSP(CoPhieu stock) {
        coPhieuRepository.themCoPhieu(
                stock.getMaCP(),
                stock.getTenCty(),
                stock.getDiaChi(),
                stock.getSoLuongPH()
        );
    }

    public Optional<CoPhieu> findById(String maCP) {
        return coPhieuRepository.findById(maCP);
    }

    public void deleteById(String maCP) {
        coPhieuRepository.deleteById(maCP);
    }
}
