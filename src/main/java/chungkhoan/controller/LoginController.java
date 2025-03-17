package chungkhoan.controller;

import chungkhoan.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

	@Autowired
	private UserService userService;

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

		String role = userService.authenticateUser(username, password);
		if (role == null) {
			return "redirect:/login?error=invalid_credentials";
		}

		session.setAttribute("username", username);
		session.setAttribute("role", role);

		if ("ROLE_INVESTOR".equals(role)) {
			session.setAttribute("maNDT", userService.getMaNDT(username));
			return "redirect:/ndt/success";
		} else if ("ROLE_EMPLOYEE".equals(role)) {
			session.setAttribute("maNV", userService.getMaNV(username));
			return "redirect:/nhanvien/success";
		} else {
			return "redirect:/login?error=unauthorized";
		}

	}

	@GetMapping("/logout")
	public String logout(HttpSession session) {
		session.invalidate();
		return "redirect:/login";
	}

	@GetMapping("/register")
	public String registerNhanVienForm() {
		return "nhanvien/register";
	}

	@PostMapping("/register")
	public String registerNhanVien(@RequestParam("username") String username,
								   @RequestParam("password") String password,
								   @RequestParam("maNV") String maNV,
								   @RequestParam("tenNV") String tenNV,
								   @RequestParam("ngaySinh") String ngaySinh,
								   @RequestParam("email") String email,
								   @RequestParam("soDienThoai") String soDienThoai) {

		boolean success = userService.themNhanVien(username, password, maNV, tenNV, ngaySinh, email, soDienThoai);
		return success ? "redirect:/login" : "redirect:/register-nhanvien?error=failed";
	}

	@GetMapping("/register-ndt")
	public String registerNhaDauTuForm() {
		return "ndt/register";
	}

	@PostMapping("/register-ndt")
	public String registerNhaDauTu(@RequestParam("username") String username,
								   @RequestParam("password") String password,
								   @RequestParam("maNDT") String maNDT,
								   @RequestParam("tenNDT") String tenNDT,
								   @RequestParam("ngaySinh") String ngaySinh,
								   @RequestParam("email") String email,
								   @RequestParam("soDienThoai") String soDienThoai) {

		boolean success = userService.themNhaDauTu(username, password, maNDT, tenNDT, ngaySinh, email, soDienThoai);
		return success ? "redirect:/login" : "redirect:/register-ndt?error=failed";
	}
}
