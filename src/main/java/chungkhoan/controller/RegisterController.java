package chungkhoan.controller;

import chungkhoan.service.TaiKhoanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import chungkhoan.repository.NDTRepository;
import chungkhoan.repository.NhanVienRepository;
import jakarta.persistence.EntityManager;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class RegisterController {
	private final NhanVienRepository nhanVienRepository;
    private final NDTRepository  ndtRepository;
    private final TaiKhoanService taiKhoanService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public RegisterController(NhanVienRepository nhanVienRepo, NDTRepository nhaDauTuRepo, EntityManager entityManager, TaiKhoanService taiKhoanService, TaiKhoanService taiKhoanService1) {
        this.nhanVienRepository = nhanVienRepo;
        this.ndtRepository = nhaDauTuRepo;
        this.taiKhoanService = taiKhoanService1;
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("nhanViens", nhanVienRepository.findAll());
        model.addAttribute("nhaDauTus", ndtRepository.findAll());
        return "nhanvien/register";
    }

    @PostMapping("/create")
    public String createAccount(@RequestParam("tenDangNhap") String tenDangNhap,
                                @RequestParam("matKhau") String matKhau,
                                @RequestParam("loai") String loai,
                                @RequestParam("maLienKet") String maLienKet,
                                Model model) {
        try {
            taiKhoanService.taoTaiKhoan(tenDangNhap, matKhau, loai, maLienKet);
            model.addAttribute("success", "Tạo tài khoản thành công!");
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi khi tạo tài khoản: " + e.getMessage());
        }

        model.addAttribute("nhanViens", nhanVienRepository.findAll());
        model.addAttribute("nhaDauTus", ndtRepository.findAll());
        return "nhanvien/register";
    }


}