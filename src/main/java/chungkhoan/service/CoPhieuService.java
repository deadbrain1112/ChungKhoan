package chungkhoan.service;

import chungkhoan.entity.CoPhieu;
import chungkhoan.entity.LenhKhop;
import chungkhoan.entity.LichSuGia;
import chungkhoan.entity.NhaDauTu;
import chungkhoan.repository.CoPhieuRepository;
import chungkhoan.repository.LenhKhopRepository;
import chungkhoan.repository.LichSuGiaRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

@Service
public class CoPhieuService {

    @Autowired
    private CoPhieuRepository coPhieuRepository;
    
    @Autowired
    private LichSuGiaRepository lichSuGiaRepo;
    
    @Autowired
    private LenhKhopRepository lenhKhopRepo;

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
    
    // Danh sách cổ phiếu của nhà đầu tư nhất định
//    public List<CoPhieu> getCoPhieuByNDT(NhaDauTu nhaDauTu) {
//        return coPhieuRepository.findByNhaDauTu(nhaDauTu);
//    }
    
    public List<CoPhieu> getAllCoPhieu() {
    	return coPhieuRepository.findAll();
    }
}
