package chungkhoan.service;


import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
        return giaTC != null ? giaTC : 0f;
    }	
	
	public LichSuGia layGiaMoiNhat(String maCP) {
        return lichSuGiaRepository.layGiaMoiNhat(maCP);
    }
	
//	public void luuGiaMoi(String maCP, double gia, LocalDateTime thoiGian) {
//	    CoPhieu cp = coPhieuRepository.findById(maCP)
//	        .orElseThrow(() -> new RuntimeException("Không tìm thấy cổ phiếu " + maCP));
//
//	    Timestamp timestamp = Timestamp.valueOf(thoiGian);
//
//	    LichSuGia lichSuGia = LichSuGia.builder()
//	            .maCP(maCP)
//	            .ngay(timestamp)
//	            .giaTC(gia)
//	            .giaTran(Math.round(gia * 1.07 * 100.0) / 100.0)  // Làm tròn 2 chữ số
//	            .giaSan(Math.round(gia * 0.93 * 100.0) / 100.0)
//	            .coPhieu(cp)
//	            .build();
//
//	    lichSuGiaRepository.save(lichSuGia);
//	}

	public Map<String, Double> getGiaThamChieu(String maCP) {
        LocalDateTime now = LocalDateTime.now();

        Optional<LichSuGia> opt = lichSuGiaRepository.findFirstByMaCPAndNgayLessThan(maCP, Timestamp.valueOf(now));
        Map<String, Double> giaMap = new HashMap<>();

        if (opt.isPresent()) {
            LichSuGia ls = opt.get();

            double giaTC = ls.getGiaTC();
            double giaTran = (ls.getGiaTran() == 0.0) ? giaTC * 1.07 : ls.getGiaTran();
            double giaSan = (ls.getGiaSan() == 0.0) ? giaTC * 0.93 : ls.getGiaSan();

            giaMap.put("tc", giaTC);
            giaMap.put("tran", giaTran);
            giaMap.put("san", giaSan);
        } else {
            giaMap.put("tc", 0.0);
            giaMap.put("tran", 0.0);
            giaMap.put("san", 0.0);
        }

        return giaMap;
    }
}
