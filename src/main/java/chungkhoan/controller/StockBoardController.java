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

        // Khởi tạo các map dữ liệu
        Map<String, Double> giaTCMap = new HashMap<>();
        Map<String, Double> giaTranMap = new HashMap<>();
        Map<String, Double> giaSanMap = new HashMap<>();
        Map<String, LenhKhop> lenhKhopMoiNhatMap = new HashMap<>();
        Map<String, Long> tongKLMoiMap = new HashMap<>();
        Map<String, List<Map<String, Object>>> benMuaMap = new HashMap<>();
        Map<String, List<Map<String, Object>>> benBanMap = new HashMap<>();

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

            // Giá tham chiếu, trần, sàn 
            Map<String, Double> giaMap = lichSuGiaService.getLichSuGia(maCP, phase);
            double giaTC = giaMap.get("tc");
            double giaTran = giaMap.get("tran");
            double giaSan = giaMap.get("san");

            giaTCMap.put(maCP, giaTC);
            giaTranMap.put(maCP, giaTran);
            giaSanMap.put(maCP, giaSan);

            // Lệnh đặt: theo phase
            List<Map<String, Object>> benMua;
            List<Map<String, Object>> benBan;

            if (phase == TradingTimeUtil.Phase.NGHI) {
                // Dùng snapshot
                List<Object[]> muaSnap = khopLenhService.getTopMuaSnapshot(maCP);
                List<Object[]> banSnap = khopLenhService.getTopBanSnapshot(maCP);
                benMua = convertSnapshotToMap(muaSnap, true);
                benBan = convertSnapshotToMap(banSnap, false);
            } else {
                List<String> trangThai = List.of("Chờ", "Một phần");
                List<String> loaiLenhCanLay = phase.getLenhHienThi(); // ATO: ATO+LO, LO: LO, ATC: ATC+LO

                List<LenhDat> lenhMua = lenhDatRepo
                    .findByMaCPLoaiGDTrangThaiLoaiLenh(maCP, "M", trangThai, loaiLenhCanLay);
                benMua = tongHopTheoGia(lenhMua, true);

                List<LenhDat> lenhBan = lenhDatRepo
                    .findByMaCPLoaiGDTrangThaiLoaiLenh(maCP, "B", trangThai, loaiLenhCanLay);
                benBan = tongHopTheoGia(lenhBan, false);
            }

            benMuaMap.put(maCP, benMua);
            benBanMap.put(maCP, benBan);

            // Lệnh khớp cuối 
            LocalDate ngayHienThi = (phase == TradingTimeUtil.Phase.NGHI)
                ? LocalDate.now().minusDays(1)
                : LocalDate.now();
            String ngayStr = ngayHienThi.toString();

            LenhKhop khopCuoi = (phase == TradingTimeUtil.Phase.NGHI)
                ? lenhKhopRepo.findLenhKhopCuoiTrongNgay(maCP, ngayStr)
                : lenhKhopRepo.findLatestKhopLenh(maCP).stream().findFirst().orElse(null);

            if (khopCuoi != null) {
                lenhKhopMoiNhatMap.put(maCP, khopCuoi);
            }

            // Tổng khối lượng khớp
            Long tongKL = lenhKhopRepo.sumSoLuongKhopByCoPhieu(cp, startOfDay, endOfDay);
            tongKLMoiMap.put(maCP, tongKL != null ? tongKL : 0L);
        }


        model.addAttribute("filter", filter);
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
                try {
                    m.put("gia", Double.parseDouble(e.getKey())); 
                } catch (NumberFormatException ex) {
                    m.put("gia", e.getKey()); 
                }
                m.put("soLuong", e.getValue());
                return m;
            })
            .sorted((a, b) -> {
                Object valA = a.get("gia");
                Object valB = b.get("gia");

                double gA = (valA instanceof Number) ? ((Number) valA).doubleValue() : -1;
                double gB = (valB instanceof Number) ? ((Number) valB).doubleValue() : -1;

                boolean isZeroA = (gA == 0.0);
                boolean isZeroB = (gB == 0.0);

                if (isZeroA && !isZeroB) return -1; // Ưu tiên giá = 0
                if (!isZeroA && isZeroB) return 1;

                if (valA instanceof String && !(valB instanceof String)) return -1; // Ưu tiên ATO/ATC
                if (!(valA instanceof String) && valB instanceof String) return 1;

                if (valA instanceof String && valB instanceof String) return 0;

                return isMua ? Double.compare(gB, gA) : Double.compare(gA, gB);
            })
            .limit(3)
            .collect(Collectors.toList());
    }
    
    private List<Map<String, Object>> convertSnapshotToMap(List<Object[]> list, boolean isMua) {
        return list.stream()
            .map(obj -> {
                Map<String, Object> m = new HashMap<>();
                m.put("gia", Double.parseDouble(obj[0].toString()));
                m.put("soLuong", Integer.parseInt(obj[1].toString()));
                return m;
            })
            .sorted((a, b) -> {
                Double giaA = (Double) a.get("gia");
                Double giaB = (Double) b.get("gia");
                return isMua ? Double.compare(giaB, giaA) : Double.compare(giaA, giaB);
            })
            .limit(3)
            .collect(Collectors.toList());
    }
}
