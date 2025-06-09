package chungkhoan.controller;

import chungkhoan.entity.*;
import chungkhoan.repository.*;
import chungkhoan.service.LichSuGiaService;
import chungkhoan.util.TradingTimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/stock-board")
public class StockBoardController {

    private final CoPhieuRepository coPhieuRepo;
    private final LenhDatRepository lenhDatRepo;
    private final LenhKhopRepository lenhKhopRepo;
    private final LichSuGiaService lichSuGiaService;
    private final TradingTimeUtil tradingTimeUtil;

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

        TradingTimeUtil.Phase phase = tradingTimeUtil.getCurrentPhase(now);
        System.out.println("[BANG GIA] Phiên hiện tại: " + phase);
        model.addAttribute("phase", phase.name());

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

            lenhKhopRepo.findTopByLenhDat_CoPhieuOrderByNgayGioKhopDesc(cp, startOfDay, endOfDay).stream()
                .filter(lk -> tradingTimeUtil.isTrongGioGiaoDich(lk.getNgayGioKhop()) || phase == TradingTimeUtil.Phase.NGHI)
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

            List<LenhDat> lenhMua = lenhDatRepo
                .findByCoPhieu_MaCPAndLoaiGDAndTrangThaiInOrderByGiaDescNgayGDAsc(maCP, "M", statuses).stream()
                .filter(ld -> (phase != TradingTimeUtil.Phase.NGHI || tradingTimeUtil.isTrongGioGiaoDich(ld.getNgayGD())) &&
                              !ld.getNgayGD().toLocalDate().isBefore(LocalDate.now()))
                .collect(Collectors.toList());
            benMuaMap.put(maCP, tongHopTheoGia(lenhMua, true));

            List<LenhDat> lenhBan = lenhDatRepo
                .findByCoPhieu_MaCPAndLoaiGDAndTrangThaiInOrderByGiaAscNgayGDAsc(maCP, "B", statuses).stream()
                .filter(ld -> (phase != TradingTimeUtil.Phase.NGHI || tradingTimeUtil.isTrongGioGiaoDich(ld.getNgayGD())) &&
                              !ld.getNgayGD().toLocalDate().isBefore(LocalDate.now()))
                .collect(Collectors.toList());
            benBanMap.put(maCP, tongHopTheoGia(lenhBan, false));

            Long tongKL = lenhKhopRepo.findTopByLenhDat_CoPhieuOrderByNgayGioKhopDesc(cp, startOfDay, endOfDay).stream()
                .filter(lk -> tradingTimeUtil.isTrongGioGiaoDich(lk.getNgayGioKhop()) || phase == TradingTimeUtil.Phase.NGHI)
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
                l -> (l.getLoaiLenh().equals("ATO") || l.getLoaiLenh().equals("ATC"))
                        ? l.getLoaiLenh()
                        : String.valueOf(l.getGia()),
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
            .sorted((a, b) -> {
                String giaA = a.get("gia").toString();
                String giaB = b.get("gia").toString();

                try {
                    Double gA = Double.parseDouble(giaA);
                    Double gB = Double.parseDouble(giaB);
                    return isMua ? Double.compare(gB, gA) : Double.compare(gA, gB);
                } catch (NumberFormatException e) {
                    return 0;
                }
            })
            .limit(3)
            .collect(Collectors.toList());
    }
}
