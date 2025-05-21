package chungkhoan.controller;

import chungkhoan.entity.*;
import chungkhoan.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/stock-board")
public class StockBoardController {

    private final CoPhieuRepository coPhieuRepo;
    private final LichSuGiaRepository lichSuGiaRepo;
    private final LenhDatRepository lenhDatRepo;
    private final LenhKhopRepository lenhKhopRepo;

    @GetMapping
    public String getBangGia(Model model) {
        List<CoPhieu> dsCP = coPhieuRepo.findAll();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        Map<String, Double> giaTCMap = new HashMap<>();
        Map<String, Double> giaTranMap = new HashMap<>();
        Map<String, Double> giaSanMap = new HashMap<>();
        Map<String, LenhKhop> lenhKhopMoiNhatMap = new HashMap<>();
        Map<String, Long> tongKLMoiMap = new HashMap<>();
        Map<String, List<Map<String, Object>>> benMuaMap = new HashMap<>();
        Map<String, List<Map<String, Object>>> benBanMap = new HashMap<>();

        for (CoPhieu cp : dsCP) {
            String maCP = cp.getMaCP();

            // Tham chiếu, trần, sàn
            lichSuGiaRepo.findFirstByMaCPAndNgayLessThan(maCP, Timestamp.valueOf(now)).ifPresentOrElse(lichSuGia -> {
                Double giaTC = lichSuGia.getGiaTC();
                Double giaTran = lichSuGia.getGiaTran();
                Double giaSan = lichSuGia.getGiaSan();

                giaTCMap.put(maCP, giaTC != null ? giaTC : 0.0);
                giaTranMap.put(maCP, giaTran != null ? giaTran : (giaTC != null ? giaTC * 1.07 : 0.0));
                giaSanMap.put(maCP, giaSan != null ? giaSan : (giaTC != null ? giaTC * 0.93 : 0.0));
            }, () -> {
                giaTCMap.put(maCP, 0.0);
                giaTranMap.put(maCP, 0.0);
                giaSanMap.put(maCP, 0.0);
            });

            List<String> statuses = Arrays.asList("Chờ", "Một phần");

            // Mua
            List<LenhDat> lenhMua = lenhDatRepo
                    .findByCoPhieu_MaCPAndLoaiGDAndTrangThaiInOrderByGiaDescNgayGDAsc(maCP, "M", statuses);
            benMuaMap.put(maCP, tongHopTheoGia(lenhMua, true));

            // Bán
            List<LenhDat> lenhBan = lenhDatRepo
                    .findByCoPhieu_MaCPAndLoaiGDAndTrangThaiInOrderByGiaAscNgayGDAsc(maCP, "B", statuses);
            benBanMap.put(maCP, tongHopTheoGia(lenhBan, false));

            // Lệnh khớp mới nhất
            lenhKhopRepo.findTopByLenhDat_CoPhieuOrderByNgayGioKhopDesc(cp, startOfDay, endOfDay)
                    .stream().findFirst()
                    .ifPresent(lk -> lenhKhopMoiNhatMap.put(maCP, lk));

            // Tổng khối lượng
            Long tongKL = lenhKhopRepo.sumSoLuongKhopByCoPhieu(cp, startOfDay, endOfDay);
            tongKLMoiMap.put(maCP, tongKL != null ? tongKL : 0L);
        }

        model.addAttribute("dsCP", dsCP);
        model.addAttribute("giaTCMap", giaTCMap);
        model.addAttribute("giaTranMap", giaTranMap);
        model.addAttribute("giaSanMap", giaSanMap);
        model.addAttribute("benMuaMap", benMuaMap);
        model.addAttribute("benBanMap", benBanMap);
        model.addAttribute("lenhKhopMap", lenhKhopMoiNhatMap);
        model.addAttribute("tongKhopMap", tongKLMoiMap);

        return "ndt/stock_board";
    }

    private List<Map<String, Object>> tongHopTheoGia(List<LenhDat> lenhList, boolean isMua) {
        return lenhList.stream()
            .collect(Collectors.groupingBy(
                LenhDat::getGia,
                LinkedHashMap::new,
                Collectors.summingInt(LenhDat::getSoLuong)
            ))
            .entrySet().stream()
            .map(e -> {
                Map<String, Object> m = new HashMap<>();
                m.put("gia", e.getKey());
                m.put("soLuong", e.getValue());
                return m;
            })
            .sorted((a, b) -> isMua
                ? Double.compare((Double) b.get("gia"), (Double) a.get("gia"))
                : Double.compare((Double) a.get("gia"), (Double) b.get("gia")))
            .limit(3)
            .collect(Collectors.toList());
    }
}
