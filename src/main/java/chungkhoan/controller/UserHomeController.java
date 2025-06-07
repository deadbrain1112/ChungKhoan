package chungkhoan.controller;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import chungkhoan.entity.CoPhieu;
import chungkhoan.entity.NhaDauTu;
import chungkhoan.entity.SoHuu;
import chungkhoan.entity.TaiKhoanNganHang;
import chungkhoan.service.CoPhieuService;
import chungkhoan.service.LichSuGiaService;
import chungkhoan.service.NDTService;
import chungkhoan.service.SoHuuService;
import chungkhoan.service.TaiKhoanNganHangService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UserHomeController {
    @Autowired
    private TaiKhoanNganHangService taiKhoanNganHangService;

    @Autowired
    private CoPhieuService coPhieuService;

    @Autowired
    private LichSuGiaService lichSuGiaService;

    @Autowired
    private SoHuuService soHuuService;

    @Autowired
    private NDTService ndtService;

    @GetMapping("/nhadautu/home")
    public String home(@RequestParam(value = "maTK", required = false) String maTK, 
                       Model model, HttpSession session) {
        NhaDauTu nhaDauTu = (NhaDauTu) session.getAttribute("nhaDauTu");
        if (nhaDauTu == null) return "nhanvien/login";

        List<TaiKhoanNganHang> danhSachTaiKhoan = taiKhoanNganHangService.getAllByNDT(nhaDauTu);
        
        // Nếu không truyền maTK thì lấy tài khoản đầu tiên
        TaiKhoanNganHang taiKhoan = danhSachTaiKhoan.stream()
            .filter(tk -> maTK == null || tk.getMaTK().equals(maTK))
            .findFirst()
            .orElse(null);

        BigDecimal soTien = taiKhoan != null && taiKhoan.getSoTien() != null ? taiKhoan.getSoTien() : BigDecimal.ZERO;
        DecimalFormat decimalFormat = new DecimalFormat("#,###");
        String formattedSoTien = decimalFormat.format(soTien);

        List<SoHuu> danhSachSoHuu = soHuuService.getSoHuuByNDT(nhaDauTu.getMaNDT());
        Map<String, Integer> soLuongMap = new HashMap<>();
        for (SoHuu sh : danhSachSoHuu) {
            soLuongMap.put(sh.getMaCP(), sh.getSoLuong());
        }

        Map<String, String> giaThiTruongMap = new HashMap<>();
        Map<String, String> tongGiaTriMap = new HashMap<>();

        List<String> danhSachMaCP = new ArrayList<>(soLuongMap.keySet());
        List<CoPhieu> danhSachCP = coPhieuService.findByMaCPIn(danhSachMaCP);

        for (CoPhieu cp : danhSachCP) {
            Float giaTC = lichSuGiaService.getGiaThamChieuMoiNhat(cp.getMaCP());
            String giaText = giaTC != null ? decimalFormat.format(giaTC) : "Chưa cập nhật";
            giaThiTruongMap.put(cp.getMaCP(), giaText);

            BigDecimal tongGia = BigDecimal.valueOf(soLuongMap.get(cp.getMaCP()))
                    .multiply(BigDecimal.valueOf(giaTC != null ? giaTC : 0));
            tongGiaTriMap.put(cp.getMaCP(), decimalFormat.format(tongGia));
        }

        if (danhSachSoHuu.isEmpty()) {
            model.addAttribute("nullCP", "Không có dữ liệu cổ phiếu!");
        }

        model.addAttribute("nhaDauTu", nhaDauTu);
        model.addAttribute("taiKhoan", taiKhoan);
        model.addAttribute("danhSachTaiKhoan", danhSachTaiKhoan);
        model.addAttribute("formattedSoTien", formattedSoTien);
        model.addAttribute("danhSachCoPhieu", danhSachSoHuu);
        model.addAttribute("giaThiTruongMap", giaThiTruongMap);
        model.addAttribute("tongGiaTriMap", tongGiaTriMap);

        return "ndt/home";
    }

    @PostMapping("/nhadautu/doi-mat-khau-gd")
    public String doiMatKhau(@RequestParam String newPassword, HttpSession session, RedirectAttributes redirectAttrs) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            redirectAttrs.addFlashAttribute("error", "Bạn chưa đăng nhập");
            return "redirect:/login";
        }

        boolean thanhCong = ndtService.doiMatKhauGiaoDich(username, newPassword);
        if (thanhCong) {
            redirectAttrs.addFlashAttribute("message", "Đổi mật khẩu thành công");
        } else {
            redirectAttrs.addFlashAttribute("error", "Lỗi khi đổi mật khẩu");
        }

        return "redirect:/nhadautu/home";
    }

}
