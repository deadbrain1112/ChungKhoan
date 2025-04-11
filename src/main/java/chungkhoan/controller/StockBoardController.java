package chungkhoan.controller;

import chungkhoan.entity.*;
import chungkhoan.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/stock-board")
public class StockBoardController {

    @Autowired private CoPhieuRepository coPhieuRepo;
    @Autowired private LichSuGiaRepository lichSuGiaRepo;
    @Autowired private LenhDatRepository lenhDatRepo;
    @Autowired private LenhKhopRepository lenhKhopRepo;

    @GetMapping
    public String getBangGia(Model model) {
        List<CoPhieu> dsCP = coPhieuRepo.findAll();
        LocalDate today = LocalDate.now();

        Map<String, Float> giaTCMap = new HashMap<>();
        Map<String, Float> giaTranMap = new HashMap<>();
        Map<String, Float> giaSanMap = new HashMap<>();
        Map<String, LenhKhop> lenhKhopMoiNhatMap = new HashMap<>();
        Map<String, Long> tongKLMoiMap = new HashMap<>();
        Map<String, List<LenhDat>> benMuaMap = new HashMap<>();
        Map<String, List<LenhDat>> benBanMap = new HashMap<>();

        for (CoPhieu cp : dsCP) {
            String maCP = cp.getMaCP();

            // Lấy giá TC / Trần / Sàn từ lịch sử giá
            LichSuGiaKey key = new LichSuGiaKey(maCP, Timestamp.valueOf(today.atStartOfDay()));
            lichSuGiaRepo.findById(key).ifPresent(gia -> {
                giaTCMap.put(maCP, gia.getGiaTC());
                giaTranMap.put(maCP, gia.getGiaTran());
                giaSanMap.put(maCP, gia.getGiaSan());
            });

            // Lấy 3 lệnh đặt mua giá cao nhất
            benMuaMap.put(maCP, lenhDatRepo.findTop3ByCoPhieuAndLoaiGDOrderByGiaDesc(
                    cp, "M"
            ));

            // Lấy 3 lệnh đặt bán giá thấp nhất
            benBanMap.put(maCP, lenhDatRepo.findTop3ByCoPhieuAndLoaiGDOrderByGiaAsc(
                    cp, "B"
            ));

            // Lấy lệnh khớp mới nhất
            lenhKhopMoiNhatMap.put(maCP, lenhKhopRepo.findTopByLenhDat_CoPhieuOrderByNgayGioKhopDesc(cp));

            // Tính tổng khối lượng khớp
            tongKLMoiMap.put(maCP, lenhKhopRepo.sumSoLuongKhopByCoPhieu(cp));
        }

        model.addAttribute("dsCP", dsCP);
        model.addAttribute("giaTCMap", giaTCMap);
        model.addAttribute("giaTranMap", giaTranMap);
        model.addAttribute("giaSanMap", giaSanMap);
        model.addAttribute("benMuaMap", benMuaMap);
        model.addAttribute("benBanMap", benBanMap);
        model.addAttribute("lenhKhopMap", lenhKhopMoiNhatMap);
        model.addAttribute("tongKhopMap", tongKLMoiMap);

        return "ndt/stock_board"; // → thymeleaf file
    }
}
