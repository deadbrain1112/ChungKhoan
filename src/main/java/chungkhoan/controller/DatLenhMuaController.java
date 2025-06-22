package chungkhoan.controller;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import chungkhoan.entity.CoPhieu;
import chungkhoan.entity.LenhDat;
import chungkhoan.entity.LichSuGia;
import chungkhoan.entity.NhaDauTu;
import chungkhoan.entity.TaiKhoanNganHang;
import chungkhoan.service.CoPhieuService;
import chungkhoan.service.LenhDatService;
import chungkhoan.service.LichSuGiaService;
import chungkhoan.service.TaiKhoanNganHangService;
import jakarta.servlet.http.HttpSession;

@Controller
public class DatLenhMuaController {

    @Autowired
    private TaiKhoanNganHangService taiKhoanNganHangService;

    @Autowired
    private CoPhieuService coPhieuService;

    @Autowired
    private LichSuGiaService lichSuGiaService;

    @Autowired
    private LenhDatService lenhDatService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @GetMapping("/nhadautu/dat-lenh-mua")
    public String getView(@RequestParam(value = "maCP", required = false) String maCP,
                          @RequestParam(value = "nganHang", required = false) String nganHang,
                          Model model,
                          HttpSession session) {
        NhaDauTu nhaDauTu = (NhaDauTu) session.getAttribute("nhaDauTu");
        if (nhaDauTu == null) return "nhanvien/login";

        List<TaiKhoanNganHang> danhSachTaiKhoan = taiKhoanNganHangService.getAllByNDT(nhaDauTu);
        TaiKhoanNganHang taiKhoan = null;

        if (nganHang != null && !nganHang.isBlank()) {
            for (TaiKhoanNganHang tk : danhSachTaiKhoan) {
                if (tk.getNganHang().getMaNH().equals(nganHang)) {
                    taiKhoan = tk;
                    break;
                }
            }
        }

        if (taiKhoan == null && !danhSachTaiKhoan.isEmpty()) {
            taiKhoan = danhSachTaiKhoan.get(0);
        }

        double soTien = (taiKhoan != null && taiKhoan.getSoTien() != null) ? taiKhoan.getSoTien().doubleValue() : 0;
        String formattedSoTien = new DecimalFormat("#,###").format(soTien) + " VND";

        model.addAttribute("nhaDauTu", nhaDauTu);
        model.addAttribute("danhSachTaiKhoan", danhSachTaiKhoan);
        model.addAttribute("taiKhoan", taiKhoan);
        model.addAttribute("formattedSoTien", formattedSoTien);

        if (maCP != null && !maCP.isBlank()) {
            Map<String, Double> giaMap = lichSuGiaService.getGiaThamChieu(maCP.trim());
            model.addAttribute("giaThamChieu", formatGia(giaMap.get("tc")));
            model.addAttribute("giaTran", formatGia(giaMap.get("tran")));
            model.addAttribute("giaSan", formatGia(giaMap.get("san")));
        }

        model.addAttribute("tatCaCoPhieu", coPhieuService.getAllCoPhieu());
        return "ndt/dat_lenh_mua";
    }

    @PostMapping("/nhadautu/dat-lenh-mua")
    public String datLenhMua(@RequestParam(required = false) String maCP,
                             @RequestParam String nganHang,
                             @RequestParam String loaiLenh,
                             @RequestParam(required = false) Integer soLuong,
                             @RequestParam(required = false) Double gia,
                             @RequestParam(required = false) String matKhau,
                             Model model,
                             HttpSession session) {

        NhaDauTu nhaDauTu = (NhaDauTu) session.getAttribute("nhaDauTu");
        if (nhaDauTu == null) return "nhanvien/login";

        if (maCP == null || maCP.isBlank()) {
            model.addAttribute("error", "Mã cổ phiếu không được để trống!");
            return prepareView(model, nhaDauTu, nganHang);
        }

        if (soLuong != null && soLuong % 100 != 0) {
            model.addAttribute("error", "Số lượng phải là bội số của 100!");
            return prepareView(model, nhaDauTu, nganHang);
        }

        if (gia != null && gia % 100 != 0) {
            model.addAttribute("error", "Giá phải là bội số của 100!");
            return prepareView(model, nhaDauTu, nganHang);
        }

        List<TaiKhoanNganHang> danhSachTaiKhoan = taiKhoanNganHangService.getAllByNDT(nhaDauTu);
        TaiKhoanNganHang taiKhoan = danhSachTaiKhoan.stream()
            .filter(tk -> tk.getNganHang().getMaNH().equals(nganHang))
            .findFirst().orElse(danhSachTaiKhoan.isEmpty() ? null : danhSachTaiKhoan.get(0));

        model.addAttribute("nhaDauTu", nhaDauTu);
        model.addAttribute("danhSachTaiKhoan", danhSachTaiKhoan);
        model.addAttribute("taiKhoan", taiKhoan);
        model.addAttribute("formattedSoTien", new DecimalFormat("#,###").format(
            taiKhoan != null && taiKhoan.getSoTien() != null ? taiKhoan.getSoTien() : BigDecimal.ZERO) + " VND");

        Optional<CoPhieu> coPhieuOpt = coPhieuService.findById(maCP);
        if (coPhieuOpt.isEmpty()) {
            model.addAttribute("error", "Không tìm thấy cổ phiếu");
            return "ndt/dat_lenh_mua";
        }

        if (soLuong == null || soLuong <= 0) {
            model.addAttribute("error", "Số lượng đặt lệnh phải lớn hơn 0!");
            return "ndt/dat_lenh_mua";
        }

        if (matKhau == null || !matKhau.equals(nhaDauTu.getMkGiaoDich())) {
            model.addAttribute("error", "Mật khẩu giao dịch không đúng!");
            return "ndt/dat_lenh_mua";
        }

        Map<String, Double> giaMap = lichSuGiaService.getGiaThamChieu(maCP);
        double giaTran = giaMap.get("tran");
        double giaSan = giaMap.get("san");
        double giaDat = 0;
        double soTien = taiKhoan != null && taiKhoan.getSoTien() != null ? taiKhoan.getSoTien().doubleValue() : 0;

        if ("LO".equalsIgnoreCase(loaiLenh)) {
            if (gia == null) {
                model.addAttribute("error", "Thiếu dữ liệu giá mua!");
                return "ndt/dat_lenh_mua";
            }

            if (gia < giaSan || gia > giaTran) {
                model.addAttribute("error", "Giá mua không được thấp hơn giá sàn hoặc lớn hơn giá trần!");
                return "ndt/dat_lenh_mua";
            }

            giaDat = gia;
            if (soTien < giaDat * soLuong) {
                model.addAttribute("error", "Số dư không đủ để đặt lệnh mua!");
                return "ndt/dat_lenh_mua";
            }

        } else if ("ATO".equalsIgnoreCase(loaiLenh) || "ATC".equalsIgnoreCase(loaiLenh)) {
            giaDat = 0;
            if (soTien < giaTran * soLuong) {
                model.addAttribute("error", "Số dư không đủ để đặt lệnh " + loaiLenh + " theo giá trần!");
                return "ndt/dat_lenh_mua";
            }

        } else {
            model.addAttribute("error", "Loại lệnh không hợp lệ!");
            return "ndt/dat_lenh_mua";
        }

        LenhDat lenh = LenhDat.builder()
            .coPhieu(coPhieuOpt.get())
            .taiKhoanNganHang(taiKhoan)
            .loaiGD("M")
            .loaiLenh(loaiLenh)
            .soLuong(soLuong)
            .gia(giaDat)
            .trangThai("Chờ")
            .ngayGD(LocalDateTime.now())
            .build();

        lenhDatService.save(lenh);

        messagingTemplate.convertAndSend("/topic/stock-board", createOrderMessage(maCP, giaDat, soLuong, "M"));

        model.addAttribute("success", "Đặt lệnh mua thành công!");
        model.addAttribute("tatCaCoPhieu", coPhieuService.getAllCoPhieu());

        return "redirect:/nhadautu/dat-lenh-mua";
    }

    private Map<String, Object> createOrderMessage(String maCPInput, double giaInput, int soLuongInput, String loaiGDInput) {
        return Map.of(
                "maCP", maCPInput,
                "gia", giaInput,
                "soLuong", soLuongInput,
                "loaiGD", loaiGDInput,
                "type", "order"
        );
    }


    private String formatGia(Double gia) {
        return gia != null ? new DecimalFormat("#,###").format(gia) + " VND" : "Chưa cập nhật";
    }

    private String prepareView(Model model, NhaDauTu nhaDauTu, String nganHang) {
        List<TaiKhoanNganHang> danhSach = taiKhoanNganHangService.getAllByNDT(nhaDauTu);
        model.addAttribute("danhSachTaiKhoan", danhSach);
        model.addAttribute("taiKhoan", danhSach.stream()
            .filter(tk -> tk.getNganHang().getMaNH().equals(nganHang))
            .findFirst().orElse(null));
        return "ndt/dat_lenh_mua";
    }

    @GetMapping("/nhadautu/so-du")
    @ResponseBody
    public String laySoDuTheoMaNH(@RequestParam("nganHang") String maNH, HttpSession session) {
        NhaDauTu nhaDauTu = (NhaDauTu) session.getAttribute("nhaDauTu");
        if (nhaDauTu == null) return "0";

        return taiKhoanNganHangService.getAllByNDT(nhaDauTu).stream()
            .filter(tk -> tk.getNganHang().getMaNH().equals(maNH))
            .map(tk -> new DecimalFormat("#,###").format(tk.getSoTien()))
            .findFirst().orElse("0");
    }

    @GetMapping("/nhadautu/gia-co-phieu")
    @ResponseBody
    public ResponseEntity<?> layGiaCoPhieu(@RequestParam("maCP") String maCP) {
        String maCPTrimmed = maCP.trim();

        // Chỉ cho phép mã toàn CHỮ HOA
        if (!maCPTrimmed.matches("^[A-Z]+$")) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Mã cổ phiếu không hợp lệ!");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        Optional<CoPhieu> cp = coPhieuService.findById(maCPTrimmed);

        // Kiểm tra tồn tại chính xác mã
        if (cp.isEmpty() || !cp.get().getMaCP().equals(maCPTrimmed)) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Không tìm thấy cổ phiếu!");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        Map<String, Double> giaMap = lichSuGiaService.getGiaThamChieu(maCPTrimmed);
        DecimalFormat df = new DecimalFormat("#,###");

        Map<String, String> result = new HashMap<>();
        result.put("maCP", maCPTrimmed);
        result.put("giaTC", df.format(giaMap.getOrDefault("tc", 0.0)) + " VND");
        result.put("giaTran", df.format(giaMap.getOrDefault("tran", 0.0)) + " VND");
        result.put("giaSan", df.format(giaMap.getOrDefault("san", 0.0)) + " VND");

        return ResponseEntity.ok(result);
    }
}
