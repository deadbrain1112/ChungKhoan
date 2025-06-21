package chungkhoan.controller;

import chungkhoan.entity.*;
import chungkhoan.repository.*;
import chungkhoan.service.KhopLenhService;
import chungkhoan.service.LichSuGiaService;
import chungkhoan.service.SoHuuService;
import chungkhoan.util.TradingTimeUtil;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/stock-board")
public class StockBoardController {

    private final CoPhieuRepository coPhieuRepo;
    private final SoHuuService soHuuService;
    private final LenhDatRepository lenhDatRepo;
    private final LenhKhopRepository lenhKhopRepo;
    private final LichSuGiaService lichSuGiaService;
    private final TradingTimeUtil tradingTimeUtil;
    private final KhopLenhService khopLenhService;
    
    @GetMapping
    public String getBangGia(@RequestParam(name = "filter", defaultValue = "tatca") String filter,
                             Model model, HttpSession session) {
        NhaDauTu nhaDauTu = (NhaDauTu) session.getAttribute("nhaDauTu");
        List<CoPhieu> dsCP;
        if (nhaDauTu != null) {
            model.addAttribute("nhaDauTu", nhaDauTu);
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        LocalDate homNay = now.toLocalDate();

        // Khởi tạo các map dữ liệu
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
        model.addAttribute("phase", phase.name());

        if ("sohuu".equals(filter) && nhaDauTu != null) {
            List<SoHuu> danhSachSoHuu = soHuuService.getSoHuuByNDT(nhaDauTu.getMaNDT());
            dsCP = danhSachSoHuu.stream()
                    .map(SoHuu::getCoPhieu)
                    .collect(Collectors.toList());
        } else {
            dsCP = coPhieuRepo.findAll();
        }

        for (CoPhieu cp : dsCP) {
            String maCP = cp.getMaCP();

            // Tham chiếu, trần, sàn
            Map<String, Double> giaMap = lichSuGiaService.getLichSuGia(maCP, phase);
            double giaTC = giaMap.get("tc");
            double giaTran = giaMap.get("tran");
            double giaSan = giaMap.get("san");

            giaTCMap.put(maCP, giaTC);
            giaTranMap.put(maCP, giaTran);
            giaSanMap.put(maCP, giaSan);

            // 🟩 Giao dịch NGHỈ: lấy từ snapshot service (không truy vấn DB)
            if (phase == TradingTimeUtil.Phase.NGHI) {
                List<Object[]> topMuaSnapshot = khopLenhService.getTopMuaSnapshot(maCP);
                List<Object[]> topBanSnapshot = khopLenhService.getTopBanSnapshot(maCP);

                benMuaMap.put(maCP, convertObjectListToMapList(topMuaSnapshot));
                benBanMap.put(maCP, convertObjectListToMapList(topBanSnapshot));

                // Lấy lệnh khớp cuối từ DB (đã snapshot)
                LenhKhop khopCuoi = lenhKhopRepo.findLenhKhopCuoiTrongNgay(maCP, homNay.toString());
                if (khopCuoi != null) {
                    lenhKhopMoiNhatMap.put(maCP, khopCuoi);
                    double giaKhop = khopCuoi.getGiaKhop();

                    String cls = "gia-tham-chieu";
                    if (giaKhop == giaTran) cls = "gia-tran";
                    else if (giaKhop == giaSan) cls = "gia-san";
                    else if (giaKhop > giaTC) cls = "gia-tang";
                    else if (giaKhop < giaTC) cls = "gia-giam";

                    colorMap.put(maCP, cls);
                    deltaMap.put(maCP, giaKhop - giaTC);
                }

                Long tongKL = lenhKhopRepo.sumSoLuongKhopByMaCPAndNgay(maCP, homNay.toString());
                tongKLMoiMap.put(maCP, tongKL != null ? tongKL : 0L);
            } else {
                // 🛑 Cũ: xử lý realtime
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

                // Giá và khối lượng realtime
                LenhKhop khopCuoi = lenhKhopRepo.findLenhKhopCuoiTrongNgay(maCP, homNay.toString());
                if (khopCuoi != null) {
                    lenhKhopMoiNhatMap.put(maCP, khopCuoi);
                    double giaKhop = khopCuoi.getGiaKhop();

                    String cls = "gia-tham-chieu";
                    if (giaKhop == giaTran) cls = "gia-tran";
                    else if (giaKhop == giaSan) cls = "gia-san";
                    else if (giaKhop > giaTC) cls = "gia-tang";
                    else if (giaKhop < giaTC) cls = "gia-giam";

                    colorMap.put(maCP, cls);
                    deltaMap.put(maCP, giaKhop - giaTC);
                }

                Long tongKL = lenhKhopRepo.findTopByLenhDat_CoPhieuOrderByNgayGioKhopDesc(cp, startOfDay, endOfDay).stream()
                        .mapToLong(LenhKhop::getSoLuongKhop)
                        .sum();
                tongKLMoiMap.put(maCP, tongKL);
            }
        }

        model.addAttribute("filter", filter);
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
    
    // 🟩 Chuyển từ List<Object[]> → List<Map<String, Object>>
    private List<Map<String, Object>> convertObjectListToMapList(List<Object[]> rawList) {
        return rawList.stream().map(obj -> {
            Map<String, Object> m = new HashMap<>();
            m.put("gia", obj[0]);
            m.put("soLuong", obj[1]);
            return m;
        }).collect(Collectors.toList());
    }
}