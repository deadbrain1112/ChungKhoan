package chungkhoan.service;


import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import chungkhoan.entity.LichSuGia;
import chungkhoan.repository.LichSuGiaRepository;
import chungkhoan.util.TradingTimeUtil;

@Service
public class LichSuGiaService {
	
	@Autowired
    private LichSuGiaRepository lichSuGiaRepository;
	
	
	// Lấy giá tham chiếu mới nhất của cổ phiếu
	public Float getGiaThamChieuMoiNhat(String maCP) {
        Float giaTC = lichSuGiaRepository.getGiaThamChieuMoiNhat(maCP);
        return giaTC != null ? giaTC : 0f;
    }	
	
	public LichSuGia layGiaMoiNhat(String maCP) {
        return lichSuGiaRepository.layGiaMoiNhat(maCP);
    }

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
	
	public Map<String, Double> getLichSuGia(String maCP, TradingTimeUtil.Phase phase) {
		Timestamp homNay = Timestamp.valueOf(LocalDate.now().atStartOfDay());
        Optional<LichSuGia> opt;

        if (phase == TradingTimeUtil.Phase.ATO || phase == TradingTimeUtil.Phase.NGHI) {
            opt = lichSuGiaRepository.findFirstByMaCPAndNgayBeforeOrderByNgayDesc(maCP, homNay);
        } else {
            opt = lichSuGiaRepository.findByMaCPAndNgay(maCP, homNay);
            if (opt.isEmpty()) {
                opt = lichSuGiaRepository.findFirstByMaCPAndNgayBeforeOrderByNgayDesc(maCP, homNay);
            }
        }

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

	public void snapshotLichSuGia(String maCP, Timestamp ngay, double giaTC, boolean autoTinh) {
	    double giaTran = autoTinh ? giaTC * 1.07 : 0.0;
	    double giaSan = autoTinh ? giaTC * 0.93 : 0.0;

	    LichSuGia lichSu = LichSuGia.builder()
	        .maCP(maCP)
	        .ngay(ngay)
	        .giaTC(giaTC)
	        .giaTran(giaTran)
	        .giaSan(giaSan)
	        .build();

	    lichSuGiaRepository.save(lichSu);
	}
	
	public void snapshotGiaChoNgayHomSau(String maCP, double giaDongCua) {
	    Timestamp ngayHomSau = Timestamp.valueOf(LocalDate.now().plusDays(1).atStartOfDay());
	    snapshotLichSuGia(maCP, ngayHomSau, giaDongCua, false);
	}
}
