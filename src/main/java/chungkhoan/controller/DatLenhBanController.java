package chungkhoan.controller;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
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

    @GetMapping("/nhadautu/dat-lenh-ban")
    public String getView(@RequestParam(value = "maCP", required = false) String maCP,
                          Model model,
                          HttpSession session) {
        NhaDauTu nhaDauTu = (NhaDauTu) session.getAttribute("nhaDauTu");
        if (nhaDauTu == null) return "nhanvien/login";

        List<TaiKhoanNganHang> danhSachTaiKhoan = taiKhoanNganHangService.getAllByNDT(nhaDauTu);
        TaiKhoanNganHang taiKhoan = danhSachTaiKhoan.stream().findFirst().orElse(null);

        double soTien = (taiKhoan != null && taiKhoan.getSoTien() != null) ? taiKhoan.getSoTien().doubleValue() : 0;
        String formattedSoTien = formatGia(soTien);

        model.addAttribute("nhaDauTu", nhaDauTu);
        model.addAttribute("danhSachTaiKhoan", danhSachTaiKhoan);
        model.addAttribute("taiKhoan", taiKhoan);
        model.addAttribute("formattedSoTien", formattedSoTien);

        if (maCP != null && !maCP.isBlank()) {
            LichSuGia gia = lichSuGiaService.layGiaMoiNhat(maCP.trim());
            if (gia != null) {
                model.addAttribute("lichSuGia", gia);
                model.addAttribute("giaThamChieu", formatGia(gia.getGiaTC()));
                model.addAttribute("giaTran", formatGia(gia.getGiaTran()));
                model.addAttribute("giaSan", formatGia(gia.getGiaSan()));
            } else {
                model.addAttribute("khongTimThay", true);
            }
        }

        model.addAttribute("tatCaCoPhieu", coPhieuService.findByMaCPIn(soHuuService.getMaCPByNDT(nhaDauTu.getMaNDT())));

        return "ndt/dat_lenh_ban";
    }

    @PostMapping("/nhadautu/dat-lenh-ban")
    public String datLenhBan(@RequestParam String maCP,
                             @RequestParam String nganHang,
                             @RequestParam String loaiLenh,
                             @RequestParam Integer soLuong,
                             @RequestParam(required = false) Double gia,
                             @RequestParam String matKhau,
                             Model model,
                             HttpSession session) {

        NhaDauTu nhaDauTu = (NhaDauTu) session.getAttribute("nhaDauTu");
        if (nhaDauTu == null) return "nhanvien/login";

        List<TaiKhoanNganHang> danhSachTaiKhoan = taiKhoanNganHangService.getAllByNDT(nhaDauTu);
        TaiKhoanNganHang taiKhoan = danhSachTaiKhoan.stream().findFirst().orElse(null);

        model.addAttribute("nhaDauTu", nhaDauTu);
        model.addAttribute("danhSachTaiKhoan", danhSachTaiKhoan);
        model.addAttribute("taiKhoan", taiKhoan);

        double soTien = (taiKhoan != null && taiKhoan.getSoTien() != null) ? taiKhoan.getSoTien().doubleValue() : 0;
        model.addAttribute("formattedSoTien", formatGia(soTien));

        Optional<CoPhieu> coPhieuOpt = coPhieuService.findById(maCP);
        if (coPhieuOpt.isEmpty()) {
            model.addAttribute("error", "Không tìm thấy cổ phiếu");
            return "ndt/dat_lenh_ban";
        }

        if (soLuong == null || soLuong <= 0) {
            model.addAttribute("error", "Số lượng đặt lệnh phải lớn hơn 0!");
            return "ndt/dat_lenh_ban";
        }

        LichSuGia lichSuGia = lichSuGiaService.layGiaMoiNhat(maCP);
        if (lichSuGia == null) {
            model.addAttribute("error", "Không có dữ liệu giá sàn cho cổ phiếu này");
            return "ndt/dat_lenh_ban";
        }

        double giaDat = 0;

        if ("LO".equalsIgnoreCase(loaiLenh)) {
            if (gia == null || gia < lichSuGia.getGiaSan() || gia > lichSuGia.getGiaTran()) {
                model.addAttribute("error", "Giá bán không được thấp hơn giá sàn hoặc lớn hơn giá trần!");
                return "ndt/dat_lenh_ban";
            }
            giaDat = gia;
        } else {
            giaDat = 0; 
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
        model.addAttribute("success", "Đặt lệnh bán thành công, chờ khớp lệnh!");

        model.addAttribute("tatCaCoPhieu", coPhieuService.findByMaCPIn(soHuuService.getMaCPByNDT(nhaDauTu.getMaNDT())));

        return "redirect:/nhadautu/dat-lenh-ban";
    }

    private String formatGia(double value) {
        return new DecimalFormat("#,###").format(value) + " VND";
    }
}
