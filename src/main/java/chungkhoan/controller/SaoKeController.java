package chungkhoan.controller;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import chungkhoan.entity.LenhDat;
import chungkhoan.entity.LenhKhop;
import chungkhoan.entity.NhaDauTu;
import chungkhoan.repository.LenhKhopRepository;
import chungkhoan.service.LenhDatService;
import jakarta.servlet.http.HttpSession;

@Controller
public class SaoKeController {

    @Autowired
    private LenhDatService lenhDatService;
    
    @Autowired
    private LenhKhopRepository lenhKhopRepository;

    @GetMapping("/nhadautu/sao-ke-gdck")
    public String hienThiLenhDatTheoNDT(@RequestParam(required = false) String trangThai,
                                        Model model, HttpSession session) {
        NhaDauTu nhaDauTu = (NhaDauTu) session.getAttribute("nhaDauTu");
        if (nhaDauTu == null || nhaDauTu.getMaNDT() == null || nhaDauTu.getMaNDT().isBlank()) {
            model.addAttribute("error", "Không xác định được tài khoản nhà đầu tư.");
            return "nhanvien/login";
        }

        String maNDT = nhaDauTu.getMaNDT();
        List<LenhDat> danhSach = (trangThai == null || trangThai.isBlank())
                ? lenhDatService.timTheoMaNhaDauTu(maNDT)
                : lenhDatService.timTheoMaNhaDauTuVaTrangThai(maNDT, trangThai);

        if (trangThai != null && !trangThai.isBlank()) {
            model.addAttribute("selectedTrangThai", trangThai);
        }

        Map<Long, String> giaFormattedMap = new HashMap<>();
        Map<Long, Integer> mapSoLuongKhop = new HashMap<>();
        Map<Long, String> mapGiaKhop = new HashMap<>();
        Map<Long, LocalDateTime> mapNgayKhop = new HashMap<>();

        for (LenhDat lenh : danhSach) {
            Long maGD = lenh.getMaGD();
            giaFormattedMap.put(maGD, formatGia(lenh.getGia()));

            // Tổng khớp
            Integer tongKhop = lenhKhopRepository.tongSoLuongKhop(maGD);
            mapSoLuongKhop.put(maGD, tongKhop != null ? tongKhop : 0);

            // Lấy khớp gần nhất
            List<LenhKhop> khops = lenhKhopRepository.findAllByMaGDOrderByNgayGioKhopDesc(maGD);
            if (khops != null && !khops.isEmpty()) {
                LenhKhop latest = khops.get(0);
                mapGiaKhop.put(maGD, formatGia(latest.getGiaKhop()));
                mapNgayKhop.put(maGD, latest.getNgayGioKhop());;
            } else {
                mapGiaKhop.put(maGD, "—");
                mapNgayKhop.put(maGD, null);
            }
        }

        model.addAttribute("lenhDatList", danhSach);
        model.addAttribute("giaFormattedMap", giaFormattedMap);
        model.addAttribute("mapSoLuongKhop", mapSoLuongKhop);
        model.addAttribute("mapGiaKhop", mapGiaKhop);
        model.addAttribute("mapNgayKhop", mapNgayKhop);
        model.addAttribute("nhaDauTu", nhaDauTu);
        model.addAttribute("selectedTrangThai", trangThai);
        return "ndt/sao_ke_gdck";
    }


    @PostMapping("/nhadautu/huy-lenh")
    @ResponseBody
    public ResponseEntity<String> huyLenh(@RequestParam Long maGD) {
        Optional<LenhDat> lenhOpt = lenhDatService.findById(maGD);
        if (lenhOpt.isPresent()) {
            LenhDat lenh = lenhOpt.get();
            if ("Chờ".equalsIgnoreCase(lenh.getTrangThai())) {
                lenh.setTrangThai("Hủy");
                lenhDatService.save(lenh);
                return ResponseEntity.ok("Đã hủy");
            }
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Không thể hủy");
    }

    private String formatGia(Double gia) {
        return gia != null ? new DecimalFormat("#,###").format(gia) + " VND" : "Chưa cập nhật";
    }
    
    // Sao kê giao dịch theo mã CP
    @PostMapping("/nhadautu/lich-su-lenh")
    public String lichSuLenhTheoMaCP(@RequestParam String maCP,
                                     @RequestParam String tuNgay,
                                     @RequestParam String denNgay,
                                     @RequestParam(required = false) String trangThai,
                                     Model model, HttpSession session) {
        NhaDauTu nhaDauTu = (NhaDauTu) session.getAttribute("nhaDauTu");
        if (nhaDauTu == null || nhaDauTu.getMaNDT().isBlank()) {
            model.addAttribute("error", "Không xác định được tài khoản nhà đầu tư.");
            return "nhanvien/login";
        }

        // Kiểm tra rỗng
        if (maCP == null || maCP.isBlank()) {
            model.addAttribute("error", "Vui lòng nhập mã cổ phiếu.");
            return "ndt/sao_ke_gdck";
        }

        try {
        	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDateTime startDate = LocalDate.parse(tuNgay, formatter).atStartOfDay();
            LocalDateTime endDate = LocalDate.parse(denNgay, formatter).atTime(23, 59, 59);
            System.out.println("Từ ngày: " + tuNgay);
            System.out.println("Đến ngày: " + denNgay);

            if (startDate.isAfter(endDate)) {
                model.addAttribute("error", "Ngày bắt đầu không được sau ngày kết thúc.");
                return "ndt/sao_ke_gdck";
            }

            List<LenhDat> danhSach = lenhDatService
                    .timTheoMaNhaDauTuVaMaCPTrongKhoangNgayVaTrangThai(nhaDauTu.getMaNDT(), maCP.trim(), startDate, endDate, trangThai);

            if (danhSach.isEmpty()) {
                model.addAttribute("error", "Không tìm thấy giao dịch nào trong khoảng thời gian này.");
            }

            // Map hiển thị (như cũ)
            Map<Long, String> giaFormattedMap = new HashMap<>();
            Map<Long, Integer> mapSoLuongKhop = new HashMap<>();
            Map<Long, String> mapGiaKhop = new HashMap<>();
            Map<Long, LocalDateTime> mapNgayKhop = new HashMap<>();

            for (LenhDat lenh : danhSach) {
                Long maGD = lenh.getMaGD();
                giaFormattedMap.put(maGD, formatGia(lenh.getGia()));
                Integer tongKhop = lenhKhopRepository.tongSoLuongKhop(maGD);
                mapSoLuongKhop.put(maGD, tongKhop != null ? tongKhop : 0);
                List<LenhKhop> khops = lenhKhopRepository.findAllByMaGDOrderByNgayGioKhopDesc(maGD);
                if (!khops.isEmpty()) {
                    LenhKhop latest = khops.get(0);
                    mapGiaKhop.put(maGD, formatGia(latest.getGiaKhop()));
                    mapNgayKhop.put(maGD, latest.getNgayGioKhop());
                } else {
                    mapGiaKhop.put(maGD, "—");
                    mapNgayKhop.put(maGD, null);
                }
            }

            model.addAttribute("lenhDatList", danhSach);
            model.addAttribute("giaFormattedMap", giaFormattedMap);
            model.addAttribute("mapSoLuongKhop", mapSoLuongKhop);
            model.addAttribute("mapGiaKhop", mapGiaKhop);
            model.addAttribute("mapNgayKhop", mapNgayKhop);
            model.addAttribute("inputMaCP", maCP);
            model.addAttribute("inputTuNgay", tuNgay);
            model.addAttribute("inputDenNgay", denNgay);
            model.addAttribute("selectedTrangThai", trangThai);
        } catch (DateTimeParseException e) {
            model.addAttribute("error", "Định dạng ngày không hợp lệ.");
        }

        model.addAttribute("nhaDauTu", nhaDauTu);
        return "ndt/sao_ke_gdck";
    }

}
