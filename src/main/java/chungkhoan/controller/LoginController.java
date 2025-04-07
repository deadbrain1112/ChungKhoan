package chungkhoan.controller;

import chungkhoan.service.UserService;
import chungkhoan.service.DatabaseService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class LoginController {

	@Autowired
	private UserService userService;

	@Autowired
	private DatabaseService databaseService;

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

		// Bước 1: kiểm tra kết nối thành công
		boolean authenticated = databaseService.testConnection(username, password);
		if (!authenticated) {
			return "redirect:/login?error=invalid_credentials";
		}

		// Bước 2: lưu user vào session
		session.setAttribute("username", username);
		session.setAttribute("password", password);

		// Bước 3: xác định role
		String role = databaseService.getUserRole(username, password);

		switch (role) {
			case "nhanvien":
				return "redirect:/nhanvien/home"; // giao diện nhân viên
			case "nhadautu":
				return "redirect:/nhadautu/home"; // giao diện nhà đầu tư
			case "khong_ro_role":
				session.invalidate();
				return "redirect:/login?error=no_role";
			default:
				session.invalidate();
				return "redirect:/login?error=unknown";
		}
	}

	@GetMapping("/logout")
	public String logout(HttpSession session) {
		session.invalidate();
		return "redirect:/login";
	}
}
