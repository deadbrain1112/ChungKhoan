package chungkhoan.controller;

import chungkhoan.entity.*;
import chungkhoan.repository.*;
import chungkhoan.service.LichSuGiaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    private final LichSuGiaService lichSuGiaService;

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
        Map<String, String> colorMap = new HashMap<>();
        Map<String, Double> deltaMap = new HashMap<>();

        for (CoPhieu cp : dsCP) {
            String maCP = cp.getMaCP();

            // Tham chiếu, trần, sàn
            Map<String, Double> giaMap = lichSuGiaService.getGiaThamChieu(maCP);
            double giaTC = giaMap.get("tc");
            double giaTran = giaMap.get("tran");
            double giaSan = giaMap.get("san");

            giaTCMap.put(maCP, giaTC);
            giaTranMap.put(maCP, giaTran);
            giaSanMap.put(maCP, giaSan);

            // Lệnh khớp mới nhất trong giờ
            lenhKhopRepo.findTopByLenhDat_CoPhieuOrderByNgayGioKhopDesc(cp, startOfDay, endOfDay).stream()
                .filter(lk -> isTrongGioGiaoDich(lk.getNgayGioKhop()))
                .findFirst()
                .ifPresent(lk -> {
                    lenhKhopMoiNhatMap.put(maCP, lk);
                    double giaKhop = lk.getGiaKhop();
                    double giaTCMoi = giaTCMap.get(maCP);

                    String cls = "gia-tham-chieu";
                    if (giaKhop == giaTranMap.get(maCP)) cls = "gia-tran";
                    else if (giaKhop == giaSanMap.get(maCP)) cls = "gia-san";
                    else if (giaKhop > giaTCMoi) cls = "gia-tang";
                    else if (giaKhop < giaTCMoi) cls = "gia-giam";

                    colorMap.put(maCP, cls);
                    deltaMap.put(maCP, giaKhop - giaTCMoi);
                });

            List<String> statuses = Arrays.asList("Chờ", "Một phần");

            // Mua trong giờ
            List<LenhDat> lenhMua = lenhDatRepo
            	    .findByCoPhieu_MaCPAndLoaiGDAndTrangThaiInOrderByGiaDescNgayGDAsc(maCP, "M", statuses).stream()
            	    .filter(ld -> isTrongGioGiaoDich(ld.getNgayGD())
            	           && !ld.getNgayGD().toLocalDate().isBefore(LocalDate.now()))
            	    .collect(Collectors.toList());
            benMuaMap.put(maCP, tongHopTheoGia(lenhMua, true));

            // Bán trong giờ
            List<LenhDat> lenhBan = lenhDatRepo
            	    .findByCoPhieu_MaCPAndLoaiGDAndTrangThaiInOrderByGiaAscNgayGDAsc(maCP, "B", statuses).stream()
            	    .filter(ld -> isTrongGioGiaoDich(ld.getNgayGD())
            	           && !ld.getNgayGD().toLocalDate().isBefore(LocalDate.now()))
            	    .collect(Collectors.toList());
            benBanMap.put(maCP, tongHopTheoGia(lenhBan, false));

            // Tổng KL khớp trong giờ
            Long tongKL = lenhKhopRepo.findTopByLenhDat_CoPhieuOrderByNgayGioKhopDesc(cp, startOfDay, endOfDay).stream()
                .filter(lk -> isTrongGioGiaoDich(lk.getNgayGioKhop()))
                .mapToLong(LenhKhop::getSoLuongKhop)
                .sum();
            tongKLMoiMap.put(maCP, tongKL);
        }

        model.addAttribute("colorMap", colorMap);
        model.addAttribute("deltaMap", deltaMap);
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
    
    private boolean isTrongGioGiaoDich(LocalDateTime thoiDiem) {
        if (thoiDiem == null) return false;

        DayOfWeek day = thoiDiem.getDayOfWeek();
        LocalTime time = thoiDiem.toLocalTime();

        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) return false;

        boolean sang = !time.isBefore(LocalTime.of(6, 0)) && time.isBefore(LocalTime.of(12, 59));
        boolean chieu = !time.isBefore(LocalTime.of(13, 0)) && time.isBefore(LocalTime.of(23, 59));

        return sang || chieu;
    }
}
