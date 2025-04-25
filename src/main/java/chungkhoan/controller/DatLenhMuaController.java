package chungkhoan.controller;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import chungkhoan.entity.CoPhieu;
import chungkhoan.entity.LichSuGia;
import chungkhoan.entity.NhaDauTu;
import chungkhoan.entity.TaiKhoanNganHang;
import chungkhoan.service.CoPhieuService;
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

    @GetMapping("/nhadautu/dat-lenh-mua")
    public String getView(@RequestParam(value = "maCP", required = false) String maCP, Model model, HttpSession session) {
        // Lấy thông tin nhà đầu tư từ session
        NhaDauTu nhaDauTu = (NhaDauTu) session.getAttribute("nhaDauTu");

        if (nhaDauTu == null) {
            return "nhanvien/login";
        }

        // Lấy danh sách tài khoản ngân hàng của nhà đầu tư
        List<TaiKhoanNganHang> danhSachTaiKhoan = taiKhoanNganHangService.getAllByNDT(nhaDauTu);
        TaiKhoanNganHang taiKhoan = danhSachTaiKhoan.isEmpty() ? null : danhSachTaiKhoan.get(0);

        // Định dạng số dư tiền
        String formattedSoTien = "0";
        if (taiKhoan != null && taiKhoan.getSoTien() != null) {
            DecimalFormat decimalFormat = new DecimalFormat("#,###");
            formattedSoTien = decimalFormat.format(taiKhoan.getSoTien());
        }

        model.addAttribute("danhSachTaiKhoan", danhSachTaiKhoan);
        model.addAttribute("taiKhoan", taiKhoan);
        model.addAttribute("formattedSoTien", formattedSoTien);

        if (maCP != null && !maCP.trim().isEmpty()) {
            // Gọi service để lấy giá cổ phiếu từ stored procedure
            LichSuGia lichSuGia = lichSuGiaService.layGiaMoiNhat(maCP.trim());

            if (lichSuGia != null) {
                model.addAttribute("coPhieu", lichSuGia);
            } else {
                model.addAttribute("khongTimThay", true);
            }
        }

        return "ndt/dat_lenh_mua";
    }
}
