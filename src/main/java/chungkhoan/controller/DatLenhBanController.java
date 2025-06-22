package chungkhoan.controller;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import chungkhoan.entity.CoPhieu;
import chungkhoan.entity.LenhDat;
import chungkhoan.entity.LichSuGia;
import chungkhoan.entity.NhaDauTu;
import chungkhoan.entity.TaiKhoanNganHang;
import chungkhoan.service.CoPhieuService;
import chungkhoan.service.LenhDatService;
import chungkhoan.service.LichSuGiaService;
import chungkhoan.service.SoHuuService;
import chungkhoan.service.TaiKhoanNganHangService;
import jakarta.servlet.http.HttpSession;

@Controller
public class DatLenhBanController {

    @Autowired
    private TaiKhoanNganHangService taiKhoanNganHangService;

    @Autowired
    private CoPhieuService coPhieuService;

    @Autowired
    private LichSuGiaService lichSuGiaService;

    @Autowired
    private LenhDatService lenhDatService;

    @Autowired
    private SoHuuService soHuuService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @GetMapping("/nhadautu/dat-lenh-ban")
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
        String formattedSoTien = formatGia(soTien);

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

        model.addAttribute("tatCaCoPhieu", coPhieuService.findByMaCPIn(soHuuService.getMaCPByNDT(nhaDauTu.getMaNDT())));

        return "ndt/dat_lenh_ban";
    }

    @PostMapping("/nhadautu/dat-lenh-ban")
    public String datLenhBan(@RequestParam(required = false) String maCP,
                             @RequestParam String nganHang,
                             @RequestParam String loaiLenh,
                             @RequestParam(required = false) Integer soLuong,
                             @RequestParam(required = false) Double gia,
                             @RequestParam(required = false) String matKhau,
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
        String formattedSoTien = formatGia(soTien);

        model.addAttribute("nhaDauTu", nhaDauTu);
        model.addAttribute("danhSachTaiKhoan", danhSachTaiKhoan);
        model.addAttribute("taiKhoan", taiKhoan);
        model.addAttribute("formattedSoTien", formattedSoTien);

        if (maCP == null || maCP.isBlank()) {
            model.addAttribute("error", "Mã cổ phiếu không được để trống!");
            return "ndt/dat_lenh_ban";
        }

        if (soLuong != null && soLuong % 100 != 0) {
            model.addAttribute("error", "Số lượng phải là bội số của 100!");
            return "ndt/dat_lenh_ban";
        }

        if (gia != null && gia % 100 != 0) {
            model.addAttribute("error", "Giá phải là bội số của 100!");
            return "ndt/dat_lenh_ban";
        }

        Optional<CoPhieu> coPhieuOpt = coPhieuService.findById(maCP);
        if (coPhieuOpt.isEmpty()) {
            model.addAttribute("error", "Không tìm thấy cổ phiếu");
            return "ndt/dat_lenh_ban";
        }

        if (soLuong == null || soLuong <= 0) {
            model.addAttribute("error", "Số lượng đặt lệnh phải lớn hơn 0!");
            return "ndt/dat_lenh_ban";
        }

        if (matKhau == null || !matKhau.equals(nhaDauTu.getMkGiaoDich())) {
            model.addAttribute("error", "Mật khẩu giao dịch không đúng!");
            return "ndt/dat_lenh_ban";
        }

        Map<String, Double> giaMap = lichSuGiaService.getGiaThamChieu(maCP);
        Double giaTran = giaMap.get("tran");
        Double giaSan = giaMap.get("san");

        if (giaTran == null || giaSan == null || giaTran == 0.0 || giaSan == 0.0) {
            model.addAttribute("error", "Không có dữ liệu giá sàn hoặc giá trần cho cổ phiếu này!");
            return "ndt/dat_lenh_ban";
        }

        double giaDat = 0;

        if ("LO".equalsIgnoreCase(loaiLenh)) {
            if (gia == null) {
                model.addAttribute("error", "Thiếu dữ liệu giá bán!");
                return "ndt/dat_lenh_ban";
            }

            if (gia < giaSan || gia > giaTran) {
                model.addAttribute("error", "Giá bán không được thấp hơn giá sàn hoặc lớn hơn giá trần!");
                return "ndt/dat_lenh_ban";
            }
            giaDat = gia;
        } else if ("ATO".equalsIgnoreCase(loaiLenh) || "ATC".equalsIgnoreCase(loaiLenh)) {
            giaDat = 0.0;
        } else {
            model.addAttribute("error", "Loại lệnh không hợp lệ!");
            return "ndt/dat_lenh_ban";
        }

        int soLuongSoHuu = soHuuService.getSoLuong(nhaDauTu.getMaNDT(), maCP);
        if (soLuongSoHuu < soLuong) {
            model.addAttribute("error", "Số lượng cổ phiếu không đủ để bán!");
            return "ndt/dat_lenh_ban";
        }

        LenhDat lenh = LenhDat.builder()
                .coPhieu(coPhieuOpt.get())
                .taiKhoanNganHang(taiKhoan)
                .loaiGD("B")
                .loaiLenh(loaiLenh)
                .soLuong(soLuong)
                .gia(giaDat)
                .trangThai("Chờ")
                .ngayGD(LocalDateTime.now())
                .build();

        lenhDatService.save(lenh);

        messagingTemplate.convertAndSend("/topic/stock-board", createOrderMessage(maCP, giaDat, soLuong, "B"));

        model.addAttribute("success", "Đặt lệnh bán thành công, chờ khớp lệnh!");
        model.addAttribute("tatCaCoPhieu", coPhieuService.findByMaCPIn(soHuuService.getMaCPByNDT(nhaDauTu.getMaNDT())));

        return "redirect:/nhadautu/dat-lenh-ban";
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



    private String formatGia(double value) {
        return new DecimalFormat("#,###").format(value) + " VND";
    }
}