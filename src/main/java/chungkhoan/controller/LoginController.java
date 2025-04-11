package chungkhoan.controller;

import chungkhoan.service.NDTService;
import chungkhoan.service.DatabaseService;
import chungkhoan.service.NhanVienService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
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
			model.addAttribute("error", "Sai tài khoản hoặc mật khẩu!");
		}
		return "nhanvien/login";
	}

	@PostMapping("/login")
	public String login(@RequestParam("username") String username,
						@RequestParam("password") String password,
						HttpSession session) {

		boolean authenticated = databaseService.testConnection(username, password);
		if (!authenticated) {
			return "redirect:/login?error=invalid_credentials";
		}

		session.setAttribute("username", username);
		session.setAttribute("password", password);

		String role = databaseService.getUserRole(username, password);
		System.out.println("Role được nhận: " + role);

		// Dùng username làm mã liên kết
		String maLienKet = username;
		session.setAttribute("maLienKet", maLienKet);

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
