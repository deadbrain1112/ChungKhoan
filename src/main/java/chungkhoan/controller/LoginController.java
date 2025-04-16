package chungkhoan.controller;

import chungkhoan.service.NDTService;
import chungkhoan.service.DatabaseService;
import chungkhoan.service.NhanVienService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
@Controller
public class LoginController {

	@Autowired
	private NDTService ndtService;

	@Autowired
	private DatabaseService databaseService;

	@Autowired
	private NhanVienService nhanVienService;

	@GetMapping("/login")
	public String loginForm(@RequestParam(value = "error", required = false) String error, Model model) {
		if (error != null) {
			switch (error) {
				case "invalid_credentials" -> model.addAttribute("error", "Sai tài khoản hoặc mật khẩu!");
				case "no_nv_found" -> model.addAttribute("error", "Không tìm thấy nhân viên!");
				case "no_ndt_found" -> model.addAttribute("error", "Không tìm thấy nhà đầu tư!");
				case "no_role" -> model.addAttribute("error", "Không xác định được vai trò!");
				default -> model.addAttribute("error", "Lỗi không xác định!");
			}
		}
		return "nhanvien/login";
	}
	@PostMapping("/login")
	public String login(@RequestParam("username") String username,
						@RequestParam("password") String password,
						HttpSession session) {

		// Kiểm tra thông tin tài khoản với DB
		boolean authenticated = databaseService.testConnection(username, password);
		if (!authenticated) {
			return "redirect:/login?error=invalid_credentials";
		}

		// Tạo JdbcTemplate động từ thông tin đăng nhập
		JdbcTemplate jdbcTemplate = databaseService.createJdbcTemplate(username, password);

		// Cấu hình các session attribute
		session.setAttribute("username", username);
		session.setAttribute("password", password);

		// Lấy vai trò người dùng
		String role = databaseService.getUserRole(jdbcTemplate, username);
		System.out.println("Role được nhận: " + role);

		// Dùng username làm mã liên kết
		String maLienKet = username;
		session.setAttribute("maLienKet", maLienKet);

		// Kiểm tra vai trò và điều hướng
		switch (role) {
			case "nhanvien" -> {
				var nhanVien = nhanVienService.getNhanVienByUsername(maLienKet);
				if (nhanVien == null) return "redirect:/login?error=no_nv_found";
				session.setAttribute("nhanVien", nhanVien);
				return "redirect:/nhanvien/layout";
			}
			case "nhadautu" -> {
				var nhaDauTu = ndtService.getNhaDauTuByUsername(maLienKet);
				if (nhaDauTu == null) return "redirect:/login?error=no_ndt_found";
				session.setAttribute("nhaDauTu", nhaDauTu);
				return "redirect:/nhadautu/home";
			}
			case "khong_ro_role" -> {
				session.invalidate();
				return "redirect:/login?error=no_role";
			}
			default -> {
				session.invalidate();
				return "redirect:/login?error=unknown";
			}
		}
	}


	@GetMapping("/logout")
	public String logout(HttpSession session) {
		session.invalidate();
		return "redirect:/login";
	}
}

