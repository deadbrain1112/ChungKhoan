package chungkhoan.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import chungkhoan.repository.LichSuGiaRepository;

@Service
public class LichSuGiaService {
	
	@Autowired
    private LichSuGiaRepository lichSuGiaRepository;
	
	// Lấy giá tham chiếu mới nhất của cổ phiếu
	public Float getGiaThamChieuMoiNhat(String maCP) {
        Float giaTC = lichSuGiaRepository.getGiaThamChieuMoiNhat(maCP);
        return giaTC != null ? giaTC : 0f; // Trả về 0 nếu không tìm thấy giá
    }
}
