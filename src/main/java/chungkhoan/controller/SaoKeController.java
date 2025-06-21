package chungkhoan.controller;

import java.math.BigDecimal;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import chungkhoan.entity.LenhDat;
import chungkhoan.entity.LenhKhop;
import chungkhoan.entity.LichSuGia;
import chungkhoan.entity.NhaDauTu;
import chungkhoan.entity.SoHuu;
import chungkhoan.repository.LenhKhopRepository;
import chungkhoan.util.TradingTimeUtil.Phase;
import chungkhoan.service.LenhDatService;
import chungkhoan.service.LichSuGiaService;
import chungkhoan.service.SaoKeService;
import chungkhoan.service.SoHuuService;
import chungkhoan.service.TaiKhoanNganHangService;
import chungkhoan.util.TradingTimeUtil;
import jakarta.servlet.http.HttpSession;

@Controller
public class SaoKeController {

    @Autowired
    private LenhDatService lenhDatService;
    
    @Autowired
    private LenhKhopRepository lenhKhopRepository;
    
    @Autowired
    private SaoKeService saoKeService;
    
    @Autowired
    private TaiKhoanNganHangService taiKhoanNganHangService;
    
    @Autowired
    private TradingTimeUtil tradingTimeUtil;
    
    @Autowired
    private LichSuGiaService lichSuGiaService;
    
    @Autowired
    private SoHuuService soHuuService;


    @GetMapping("/nhadautu/sao-ke-gdck")
    public String hienThiLenhDatTheoNDT(@RequestParam(required = false) String trangThai,
                                        Model model, HttpSession session) {
        NhaDauTu nhaDauTu = (NhaDauTu) session.getAttribute("nhaDauTu");
        if (nhaDauTu == null || nhaDauTu.getMaNDT() == null || nhaDauTu.getMaNDT().isBlank()) {
            model.addAttribute("error", "Không xác định được tài khoản nhà đầu tư.");
            return "nhanvien/login";
        }
        
        // Lấy lại lỗi từ session
        copyPopupSessionToModel(session, model);

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
        Map<Long, String> mapFormattedSoLuongDat = new HashMap<>();
        Map<Long, String> mapFormattedSoLuongKhop = new HashMap<>();

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
            
            mapFormattedSoLuongDat.put(maGD, formatSo(lenh.getSoLuong()));
            mapFormattedSoLuongKhop.put(maGD, formatSo(mapSoLuongKhop.get(maGD)));
        }

        model.addAttribute("lenhDatList", danhSach);
        model.addAttribute("giaFormattedMap", giaFormattedMap);
        model.addAttribute("mapSoLuongKhop", mapSoLuongKhop);
        model.addAttribute("mapGiaKhop", mapGiaKhop);
        model.addAttribute("mapNgayKhop", mapNgayKhop);
        model.addAttribute("mapFormattedSoLuongDat", mapFormattedSoLuongDat);
        model.addAttribute("mapFormattedSoLuongKhop", mapFormattedSoLuongKhop);
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
    
    private String formatSo(Number value) {
        return value != null ? new DecimalFormat("#,###").format(value) : "—";
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
            Map<Long, String> mapFormattedSoLuongDat = new HashMap<>();
            Map<Long, String> mapFormattedSoLuongKhop = new HashMap<>();

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
            
            for (LenhDat lenh : danhSach) {
                Long maGD = lenh.getMaGD();
                mapFormattedSoLuongDat.put(maGD, formatSo(lenh.getSoLuong()));
                Integer soLuongKhop = mapSoLuongKhop.get(maGD);
                mapFormattedSoLuongKhop.put(maGD, formatSo(soLuongKhop != null ? soLuongKhop : 0));
            }

            model.addAttribute("lenhDatList", danhSach);
            model.addAttribute("giaFormattedMap", giaFormattedMap);
            model.addAttribute("mapSoLuongKhop", mapSoLuongKhop);
            model.addAttribute("mapGiaKhop", mapGiaKhop);
            model.addAttribute("mapNgayKhop", mapNgayKhop);
            model.addAttribute("inputMaCP", maCP);
            model.addAttribute("inputTuNgay", tuNgay);
            model.addAttribute("inputDenNgay", denNgay);
            model.addAttribute("mapFormattedSoLuongDat", mapFormattedSoLuongDat);
            model.addAttribute("mapFormattedSoLuongKhop", mapFormattedSoLuongKhop);
            model.addAttribute("selectedTrangThai", trangThai);
        } catch (DateTimeParseException e) {
            model.addAttribute("error", "Định dạng ngày không hợp lệ.");
        }

        model.addAttribute("nhaDauTu", nhaDauTu);
        return "ndt/sao_ke_gdck";
    }
    
    @PostMapping("/nhadautu/sua-lenh")
    public String suaLenhGiaoDich(@RequestParam("maGD") Long maGD,
                                  @RequestParam("giaDat") String giaDatRaw,
                                  @RequestParam("soLuong") String soLuongRaw,
                                  HttpSession session) {

        NhaDauTu ndt = (NhaDauTu) session.getAttribute("nhaDauTu");
        if (ndt == null) {
            return "nhanvien/login";
        }

        Optional<LenhDat> opt = lenhDatService.findById(maGD);
        if (opt.isEmpty()) {
            return "redirect:/nhadautu/sao-ke-gdck";
        }

        LenhDat lenh = opt.get();
        if (!lenh.getTaiKhoanNganHang().getNhaDauTu().getMaNDT().equals(ndt.getMaNDT())) {
            return "redirect:/nhadautu/sao-ke-gdck";
        }

        double giaDat;
        int soLuong;
        try {
            giaDat = Double.parseDouble(giaDatRaw.replaceAll("[^\\d.]", ""));
            soLuong = Integer.parseInt(soLuongRaw.replaceAll("[^\\d]", ""));
        } catch (Exception e) {
            return errorBackToPopup("Giá hoặc số lượng không hợp lệ", lenh, giaDatRaw, soLuongRaw, session);
        }

        String loaiLenh = lenh.getLoaiLenh();
        String loaiGD = lenh.getLoaiGD();
        String maCP = lenh.getCoPhieu().getMaCP();

        boolean choPhepSua = tradingTimeUtil.getCurrentPhase(LocalDateTime.now()) == Phase.NGHI
                || "ATC".equals(loaiLenh) || "ATO".equals(loaiLenh);

        if (!choPhepSua) {
            return errorBackToPopup("Chỉ được sửa lệnh khi thị trường đang nghỉ", lenh, giaDatRaw, soLuongRaw, session);
        }

        if ("LO".equalsIgnoreCase(loaiLenh)) {
            Map<String, Double> giaBienDo = lichSuGiaService.getGiaThamChieu(maCP);
            double giaTran = giaBienDo.getOrDefault("tran", 0.0);
            double giaSan = giaBienDo.getOrDefault("san", 0.0);

            if (giaDat < giaSan || giaDat > giaTran) {
                return errorBackToPopup("Giá đặt phải nằm trong biên độ giao dịch", lenh, giaDatRaw, soLuongRaw, session);
            }
        }

        if ("M".equalsIgnoreCase(loaiGD)) {
            BigDecimal tongTien = BigDecimal.valueOf(giaDat).multiply(BigDecimal.valueOf(soLuong));
            BigDecimal soDu = lenh.getTaiKhoanNganHang().getSoTien();
            if (tongTien.compareTo(soDu) > 0) {
                return errorBackToPopup("Không đủ tiền để đặt lệnh", lenh, giaDatRaw, soLuongRaw, session);
            }
        } else {
            int soHuu = soHuuService.getSoLuong(ndt.getMaNDT(), maCP);
            if (soLuong > soHuu) {
                return errorBackToPopup("Không đủ cổ phiếu để bán", lenh, giaDatRaw, soLuongRaw, session);
            }
        }

        lenh.setSoLuong(soLuong);
        System.out.println("loaiLenh = '" + loaiLenh + "'");

        if (loaiLenh != null && loaiLenh.trim().equalsIgnoreCase("LO")) {
            lenh.setGia(giaDat);
            System.out.println("Sửa giá: " + giaDat);
        }
        
        lenh.setNgayGD(LocalDateTime.now());

        lenhDatService.save(lenh);
        return "redirect:/nhadautu/sao-ke-gdck";
    }

    // Đưa dữ liệu hiện tại vào session
    private String errorBackToPopup(String errorMsg, LenhDat lenh, String giaDat, String soLuong, HttpSession session) {
        session.setAttribute("popupError", errorMsg);
        session.setAttribute("openPopup", true);

        session.setAttribute("popupMaGD", lenh.getMaGD());
        session.setAttribute("popupGiaDat", formatDecimal(giaDat));
        session.setAttribute("popupSoLuong", formatDecimal(soLuong));
        session.setAttribute("popupMaCP", lenh.getCoPhieu().getMaCP());
        session.setAttribute("popupLoaiGD", lenh.getLoaiGD().equals("M") ? "Mua" : "Bán");
        session.setAttribute("popupLoaiLenh", lenh.getLoaiLenh());

        return "redirect:/nhadautu/sao-ke-gdck";
    }
    
    // Đưa session vào model
    private void copyPopupSessionToModel(HttpSession session, Model model) {
        Object popupError = session.getAttribute("popupError");
        if (popupError != null) {
            model.addAttribute("popupError", popupError);
            model.addAttribute("openPopup", session.getAttribute("openPopup"));
            model.addAttribute("popupMaGD", session.getAttribute("popupMaGD"));
            model.addAttribute("popupGiaDat", session.getAttribute("popupGiaDat"));
            model.addAttribute("popupSoLuong", session.getAttribute("popupSoLuong"));
            model.addAttribute("popupMaCP", session.getAttribute("popupMaCP"));
            model.addAttribute("popupLoaiGD", session.getAttribute("popupLoaiGD"));
            model.addAttribute("popupLoaiLenh", session.getAttribute("popupLoaiLenh"));

            // Loại khỏi session sau khi lấy
            session.removeAttribute("popupError");
            session.removeAttribute("openPopup");
            session.removeAttribute("popupMaGD");
            session.removeAttribute("popupGiaDat");
            session.removeAttribute("popupSoLuong");
            session.removeAttribute("popupMaCP");
            session.removeAttribute("popupLoaiGD");
            session.removeAttribute("popupLoaiLenh");
        }
    }

    // Format thủ công theo #,###
    private String formatDecimal(Object raw) {
        try {
            Number number = new BigDecimal(raw.toString().replaceAll("[^\\d]", ""));
            return new DecimalFormat("#,###").format(number);
        } catch (Exception e) {
            return "";
        }
    }
    
    // Sao kê tiền
    @GetMapping("/nhadautu/sao-ke-tien")
    public String saoKeTienTheoNgay(@RequestParam(required = false) String tuNgay,
                                    @RequestParam(required = false) String denNgay,
                                    @RequestParam(required = false) String maTK,
                                    Model model,
                                    HttpSession session) {
        NhaDauTu nhaDauTu = (NhaDauTu) session.getAttribute("nhaDauTu");
        if (nhaDauTu == null || nhaDauTu.getMaNDT().isBlank()) {
            model.addAttribute("error", "Không xác định được tài khoản nhà đầu tư.");
            return "nhanvien/login";
        }

        LocalDateTime fromDate = null;
        LocalDateTime toDate = null;

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            if (tuNgay != null && !tuNgay.isBlank()) {
                fromDate = LocalDate.parse(tuNgay, formatter).atStartOfDay();
            }

            if (denNgay != null && !denNgay.isBlank()) {
                toDate = LocalDate.parse(denNgay, formatter).atTime(23, 59, 59);
            }

            if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
                model.addAttribute("error", "Ngày bắt đầu không được sau ngày kết thúc.");
                return "ndt/sao_ke_tien";
            }

            // Gọi SP: nếu fromDate hoặc toDate = null thì SP xử lý toàn bộ
            List<Object[]> transactionList = saoKeService.getSaoKeAllTaiKhoan(
                    nhaDauTu.getMaNDT(),
                    null, // lọc tất cả mã CP
                    fromDate,
                    toDate
            );
            
            if (transactionList.isEmpty()) {
            	model.addAttribute("error", "Không tìm thấy giao dịch nào trong khoảng thời gian này.");
            }
            
            List<Map<String, String>> danhSachTaiKhoan = taiKhoanNganHangService.getMaTKVaTenNH(nhaDauTu.getMaNDT());
            model.addAttribute("danhSachTaiKhoan", danhSachTaiKhoan);
            model.addAttribute("inputMaTK", maTK);

            model.addAttribute("transactionList", transactionList);
            model.addAttribute("inputTuNgay", tuNgay);
            model.addAttribute("inputDenNgay", denNgay);
        } catch (DateTimeParseException e) {
            model.addAttribute("error", "Định dạng ngày không hợp lệ.");
        }

        model.addAttribute("nhaDauTu", nhaDauTu);
        return "ndt/sao_ke_tien";
    }
}
