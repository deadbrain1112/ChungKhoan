package chungkhoan.controller;

import chungkhoan.entity.*;
import chungkhoan.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = now.toLocalDate().plusDays(1).atStartOfDay();

        Map<String, Float> giaTCMap = new HashMap<>();
        Map<String, Float> giaTranMap = new HashMap<>();
        Map<String, Float> giaSanMap = new HashMap<>();
        Map<String, LenhKhop> lenhKhopMoiNhatMap = new HashMap<>();
        Map<String, Long> tongKLMoiMap = new HashMap<>();
        Map<String, List<Map<String, Object>>> benMuaMap = new HashMap<>();
        Map<String, List<Map<String, Object>>> benBanMap = new HashMap<>();

        for (CoPhieu cp : dsCP) {
            String maCP = cp.getMaCP();

            // Lấy giá TC, trần, sàn từ lịch sử giá (lấy phiên gần nhất trước ngày hiện tại)
            Optional<LichSuGia> lichSuGiaOpt = lichSuGiaRepo.findFirstByMaCPAndNgayLessThanOrderByNgayDesc(maCP, Timestamp.valueOf(now));

            if (lichSuGiaOpt.isPresent()) {
                LichSuGia gia = lichSuGiaOpt.get();
                Float giaTC = gia.getGiaTC();
                Float giaTran = gia.getGiaTran();
                Float giaSan = gia.getGiaSan();

                // Đảm bảo luôn có giá tham chiếu
                giaTCMap.put(maCP, giaTC != null ? giaTC : 0f);

                // Nếu không có giá trần/sàn, tự tính (giả sử ±7% theo quy định HOSE)
                if (giaTC != null) {
                    giaTranMap.put(maCP, giaTran != null ? giaTran : giaTC * 1.07f);
                    giaSanMap.put(maCP, giaSan != null ? giaSan : giaTC * 0.93f);
                } 
            } else {
                // Nếu không có dữ liệu lịch sử giá, gán giá mặc định
                giaTCMap.put(maCP, 0f);
                giaTranMap.put(maCP, 0f);
                giaSanMap.put(maCP, 0f);
            }

            // Lấy danh sách lệnh đặt mua (chờ khớp, trong ngày hiện tại)
            List<LenhDat> lenhMua = lenhDatRepo.findByCoPhieuAndLoaiGDOrderByGiaDesc(
                    cp, "M", startOfDay, endOfDay
            );

            // Nhóm lệnh mua theo giá và cộng dồn số lượng
            Map<Double, Integer> muaTheoGia = new HashMap<>();
            for (LenhDat lenh : lenhMua) {
                muaTheoGia.merge(lenh.getGia(), lenh.getSoLuong(), Integer::sum);
            }

            // Chuyển dữ liệu thành danh sách các Map để truyền lên giao diện
            List<Map<String, Object>> muaCongDon = muaTheoGia.entrySet().stream()
                    .map(entry -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("gia", entry.getKey());
                        map.put("soLuong", entry.getValue());
                        return map;
                    })
                    .sorted((a, b) -> ((Double) b.get("gia")).compareTo((Double) a.get("gia"))) // Sắp xếp giá giảm dần
                    .limit(3) // Lấy 3 mức giá cao nhất
                    .collect(Collectors.toList());
            benMuaMap.put(maCP, muaCongDon);

            // Lấy danh sách lệnh đặt bán (chờ khớp, trong ngày hiện tại)
            List<LenhDat> lenhBan = lenhDatRepo.findByCoPhieuAndLoaiGDOrderByGiaAsc(
                    cp, "B", startOfDay, endOfDay
            );

            // Nhóm lệnh bán theo giá và cộng dồn số lượng
            Map<Double, Integer> banTheoGia = new HashMap<>();
            for (LenhDat lenh : lenhBan) {
                banTheoGia.merge(lenh.getGia(), lenh.getSoLuong(), Integer::sum);
            }

            // Chuyển dữ liệu thành danh sách các Map để truyền lên giao diện
            List<Map<String, Object>> banCongDon = banTheoGia.entrySet().stream()
                    .map(entry -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("gia", entry.getKey());
                        map.put("soLuong", entry.getValue());
                        return map;
                    })
                    .sorted(Comparator.comparing(m -> (Double) m.get("gia"))) // Sắp xếp giá tăng dần
                    .limit(3) // Lấy 3 mức giá thấp nhất
                    .collect(Collectors.toList());
            benBanMap.put(maCP, banCongDon);

            // Lấy lệnh khớp mới nhất theo ngayGioKhop, chỉ lấy trong ngày hiện tại
            LenhKhop lenhKhop = lenhKhopRepo.findTopByLenhDat_CoPhieuOrderByNgayGioKhopDesc(cp, startOfDay, endOfDay);
            lenhKhopMoiNhatMap.put(maCP, lenhKhop);

            // Tính tổng khối lượng khớp trong ngày hiện tại
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
}