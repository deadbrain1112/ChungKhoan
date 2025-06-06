package chungkhoan.controller;

import chungkhoan.repository.NDTRepository;
import chungkhoan.repository.NhanVienRepository;
import chungkhoan.service.TaiKhoanService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ChangePasswordController {

    private final NhanVienRepository nhanVienRepository;
    private final NDTRepository ndtRepository;
    private final TaiKhoanService taiKhoanService;

    public ChangePasswordController(NhanVienRepository nhanVienRepository,
                                    NDTRepository ndtRepository,
                                    TaiKhoanService taiKhoanService) {
        this.nhanVienRepository = nhanVienRepository;
        this.ndtRepository = ndtRepository;
        this.taiKhoanService = taiKhoanService;
    }

    @GetMapping("/change-password")
    public String hienThiFormDoiMatKhau(Model model) {
        model.addAttribute("nhanViens", nhanVienRepository.findAllNhanVienCoTaiKhoan());
        model.addAttribute("nhaDauTus", ndtRepository.findAllNhaDauTuCoTaiKhoan());
        return "nhanvien/change_password"; 
    }
}
