package chungkhoan.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import chungkhoan.entity.TaiKhoanForm;
import chungkhoan.repository.NDTRepository;
import chungkhoan.repository.NhanVienRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;

@Controller
public class RegisterController {
	private final NhanVienRepository nhanVienRepository;
    private final NDTRepository  ndtRepository;
    private final EntityManager entityManager;
    
    public RegisterController(NhanVienRepository nhanVienRepo, NDTRepository nhaDauTuRepo, EntityManager entityManager) {
        this.nhanVienRepository = nhanVienRepo;
        this.ndtRepository = nhaDauTuRepo;
        this.entityManager = entityManager;
    }
    
	@GetMapping("/register")
	public String showRegisterForm(Model model) {
        model.addAttribute("user", new TaiKhoanForm());
        model.addAttribute("nhanViens", nhanVienRepository.findAll());
        model.addAttribute("nhaDauTus", ndtRepository.findAll());
        return "nhanvien/register";
    }
	
	@PostMapping("/create")
    public String createAccount(@ModelAttribute TaiKhoanForm user, Model model) {
        try {
            StoredProcedureQuery sp = entityManager
                .createStoredProcedureQuery("sp_TaoTaiKhoan")
                .registerStoredProcedureParameter("TenDangNhap", String.class, ParameterMode.IN)
                .registerStoredProcedureParameter("MatKhau", String.class, ParameterMode.IN)
                .registerStoredProcedureParameter("Loai", String.class, ParameterMode.IN)
                .setParameter("TenDangNhap", user.getUsername())
                .setParameter("MatKhau", user.getPassword())
                .setParameter("Loai", user.getRole().equals("ROLE_EMPLOYEE") ? "nhanvien" : "nhadautu");

            sp.execute();
            model.addAttribute("success", "Tạo tài khoản thành công!");
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi: " + e.getMessage());
        }

        model.addAttribute("nhanViens", nhanVienRepository.findAll());
        model.addAttribute("nhaDauTus", ndtRepository.findAll());
        return "nhanvien/register";
    }
}