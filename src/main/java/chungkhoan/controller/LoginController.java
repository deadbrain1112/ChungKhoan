package chungkhoan.controller;

import chungkhoan.entity.User;
import chungkhoan.entity.Role;
import chungkhoan.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.Optional;

@Controller
public class LoginController {

	@Autowired
	private UserService userService;

	@GetMapping("/login")
	public String loginForm() {
		return "nhanvien/login";
	}

	@PostMapping("/login")
	public ModelAndView login(@RequestParam("username") String username,
							  @RequestParam("password") String password,
							  HttpSession session) {
		ModelAndView mav = new ModelAndView();
		Optional<User> userOptional = userService.authenticateUser(username, password);

		if (userOptional.isPresent()) {
			User user = userOptional.get();
			session.setAttribute("username", username);
			session.setAttribute("role", user.getRole());

			if (user.getRole() == Role.ROLE_INVESTOR && user.getNdt() != null) {
				session.setAttribute("maNDT", user.getNdt().getMaNDT());
				mav.setViewName("redirect:/ndt/success");
			} else if (user.getRole() == Role.ROLE_EMPLOYEE && user.getNhanVien() != null) {
				session.setAttribute("maNV", user.getNhanVien().getMaNV());
				mav.setViewName("redirect:/nhanvien/success");
			} else {
				mav.setViewName("redirect:/login?error=invalid_role");
			}
		} else {
			mav.setViewName("redirect:/login?error=invalid_credentials");
		}
		return mav;
	}

	@GetMapping("/logout")
	public String logout(HttpSession session) {
		session.invalidate();
		return "redirect:/login";
	}
}
