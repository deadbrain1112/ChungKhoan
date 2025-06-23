package chungkhoan.controller;

import chungkhoan.repository.NDTRepository;
import chungkhoan.repository.NhanVienRepository;
import chungkhoan.service.TaiKhoanService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
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
    private final HttpSession session;

    @Autowired
    public RegisterController(NhanVienRepository nhanVienRepo,
                              NDTRepository nhaDauTuRepo,
                              TaiKhoanService taiKhoanService,
                              HttpSession session) {
        this.nhanVienRepository = nhanVienRepo;
        this.ndtRepository = nhaDauTuRepo;
        this.taiKhoanService = taiKhoanService;
        this.session = session;
    }

    @GetMapping("/register")
    public String showRegisterForm(@RequestParam(value = "action", defaultValue = "create") String action, Model model) {
        String currentLogin = (String) session.getAttribute("username");

        model.addAttribute("nhanViensWithoutAccount", nhanVienRepository.findAllNhanVienChuaCoTaiKhoan());
        model.addAttribute("nhaDauTusWithoutAccount", ndtRepository.findAllNhaDauTuChuaCoTaiKhoan());
        model.addAttribute("nhanViensWithAccount", nhanVienRepository.findAllNhanVienCoTaiKhoanTruNguoiDangNhap(currentLogin));
        model.addAttribute("nhaDauTusWithAccount", ndtRepository.findAllNhaDauTuCoTaiKhoan());
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

        // Cập nhật lại danh sách
        String currentLogin = (String) session.getAttribute("username");

        model.addAttribute("nhanViensWithoutAccount", nhanVienRepository.findAllNhanVienChuaCoTaiKhoan());
        model.addAttribute("nhaDauTusWithoutAccount", ndtRepository.findAllNhaDauTuChuaCoTaiKhoan());
        model.addAttribute("nhanViensWithAccount", nhanVienRepository.findAllNhanVienCoTaiKhoanTruNguoiDangNhap(currentLogin));
        model.addAttribute("nhaDauTusWithAccount", ndtRepository.findAllNhaDauTuCoTaiKhoan());
        model.addAttribute("action", action);

        return "nhanvien/register";
    }
    @PostMapping("/change-password")
    public String doiMatKhau(@RequestParam String loai,
                             @RequestParam String maLienKet,
                             @RequestParam String matKhau,
                             @RequestParam String xacNhan,
                             Model model) {
        try {
            if (!matKhau.equals(xacNhan)) {
                throw new IllegalArgumentException("Xác nhận mật khẩu không khớp!");
            }

            String username = (String) session.getAttribute("username");
            String password = (String) session.getAttribute("password");

            JdbcTemplate jdbcTemplate = chungkhoan.util.DynamicJdbcUtil.create(username, password);

            jdbcTemplate.update("EXEC master.dbo.sp_DoiMatKhau ?, ?", maLienKet, matKhau);

            model.addAttribute("success", "Đổi mật khẩu thành công!");
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi: " + e.getMessage());
        }

        model.addAttribute("nhanViens", nhanVienRepository.findAllNhanVienCoTaiKhoan());
        model.addAttribute("nhaDauTus", ndtRepository.findAllNhaDauTuCoTaiKhoan());
        return "nhanvien/change_password";
    }
}
