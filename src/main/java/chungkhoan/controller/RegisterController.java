package chungkhoan.controller;

import chungkhoan.entity.NhanVien;
import chungkhoan.entity.NhaDauTu;
import chungkhoan.repository.NDTRepository;
import chungkhoan.repository.NhanVienRepository;
import chungkhoan.service.TaiKhoanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class RegisterController {
    private final NhanVienRepository nhanVienRepository;
    private final NDTRepository ndtRepository;
    private final TaiKhoanService taiKhoanService;

    @Autowired
    public RegisterController(NhanVienRepository nhanVienRepo, NDTRepository nhaDauTuRepo, TaiKhoanService taiKhoanService) {
        this.nhanVienRepository = nhanVienRepo;
        this.ndtRepository = nhaDauTuRepo;
        this.taiKhoanService = taiKhoanService;
    }

    @GetMapping("/register")
    public String showRegisterForm(@RequestParam(value = "action", defaultValue = "create") String action, Model model) {
        // Thêm danh sách vào model
        model.addAttribute("nhanViensWithoutAccount", nhanVienRepository.findAllNhanVienChuaCoTaiKhoan());
        model.addAttribute("nhaDauTusWithoutAccount", ndtRepository.findAllNhaDauTuChuaCoTaiKhoan());
        model.addAttribute("nhanViensWithAccount", nhanVienRepository.findAllNhanVienCoTaiKhoan());
        model.addAttribute("nhaDauTusWithAccount", ndtRepository.findAllNhaDauTuCoTaiKhoan());
        // Lưu action để Thymeleaf sử dụng
        model.addAttribute("action", action);
        return "nhanvien/register";
    }

    @PostMapping("/create")
    public String createAccount(@RequestParam("tenDangNhap") String tenDangNhap,
                                @RequestParam(value = "matKhau", required = false) String matKhau,
                                @RequestParam("loai") String loai,
                                @RequestParam("action") String action,
                                @RequestParam("maLienKet") String maLienKet,
                                Model model) {
        try {
            if ("create".equals(action)) {
                if (matKhau == null || matKhau.trim().isEmpty()) {
                    throw new IllegalArgumentException("Mật khẩu không được để trống khi tạo tài khoản!");
                }
                taiKhoanService.taoTaiKhoan(tenDangNhap, matKhau, loai, maLienKet);
                model.addAttribute("success", "Tạo tài khoản thành công!");
            } else if ("delete".equals(action)) {
                taiKhoanService.xoaTaiKhoan(tenDangNhap);
                model.addAttribute("success", "Xóa tài khoản thành công!");
            }
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi: " + e.getMessage());
        }

        // Cập nhật danh sách và action
        model.addAttribute("nhanViensWithoutAccount", nhanVienRepository.findAllNhanVienChuaCoTaiKhoan());
        model.addAttribute("nhaDauTusWithoutAccount", ndtRepository.findAllNhaDauTuChuaCoTaiKhoan());
        model.addAttribute("nhanViensWithAccount", nhanVienRepository.findAllNhanVienCoTaiKhoan());
        model.addAttribute("nhaDauTusWithAccount", ndtRepository.findAllNhaDauTuCoTaiKhoan());
        model.addAttribute("action", action);
        return "nhanvien/register";
    }
}