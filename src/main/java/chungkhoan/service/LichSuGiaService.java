package chungkhoan.service;


import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import chungkhoan.entity.CoPhieu;
import chungkhoan.entity.LichSuGia;
import chungkhoan.repository.CoPhieuRepository;
import chungkhoan.repository.LichSuGiaRepository;

@Service
public class LichSuGiaService {
	
	@Autowired
    private LichSuGiaRepository lichSuGiaRepository;
	
	@Autowired
	private CoPhieuRepository coPhieuRepository;
	
	// Lấy giá tham chiếu mới nhất của cổ phiếu
	public Float getGiaThamChieuMoiNhat(String maCP) {
        Float giaTC = lichSuGiaRepository.getGiaThamChieuMoiNhat(maCP);
        return giaTC != null ? giaTC : 0f; // Trả về 0 nếu không tìm thấy giá
    }	
	
	public LichSuGia layGiaMoiNhat(String maCP) {
        return lichSuGiaRepository.layGiaMoiNhat(maCP);
    }
	
	public void luuGiaMoi(String maCP, double gia, LocalDateTime thoiGian) {
	    CoPhieu cp = coPhieuRepository.findById(maCP)
	        .orElseThrow(() -> new RuntimeException("Không tìm thấy cổ phiếu " + maCP));

	    Timestamp timestamp = Timestamp.valueOf(thoiGian);

	    LichSuGia lichSuGia = LichSuGia.builder()
	            .maCP(maCP)
	            .ngay(timestamp)
	            .giaTC(gia)
	            .giaTran(Math.round(gia * 1.07 * 100.0) / 100.0)  // Làm tròn 2 chữ số
	            .giaSan(Math.round(gia * 0.93 * 100.0) / 100.0)
	            .coPhieu(cp)
	            .build();

	    lichSuGiaRepository.save(lichSuGia);
	}
}
