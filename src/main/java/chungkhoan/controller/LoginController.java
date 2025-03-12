package chungkhoan.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.Optional; 

@Controller
public class LoginController {
	
	@GetMapping("/login")
	public String loginForm() {
		return "admin/login";
	}
	
//	// Đăng nhập trang Admin
//	@PostMapping("/admin-login")
//	public ModelAndView loginAdminPage(@RequestParam("username") String username,
//									   @RequestParam("password") String password,
//									   HttpSession session) {
//			ModelAndView mav = new ModelAndView();
//			Optional<User> userOptionnal = userService.authenticateUser(username, password);
//			
//			if (userOptionnal.isPresent()) {
//				User user = userOptionnal.get();
//				
//				session.setAttribute("user", username);
//				session.setAttribute("role", user.getRole());
//				
//				mav.setViewName("admin");
//			}
//			return mav;
//	}
//    
//    // Đăng xuất tài khoản admin, xóa session
//    @GetMapping("logout")
//    public String logoutAdmin(HttpSession session) {
//    	session.invalidate();
//    	return "redirect:/admin-login";
//    }
//
//    // Quên mật khẩu
//	@GetMapping("/login/forget-password")
//	public String forgetPasswordForm() {
//		return "forgetPassword";
//	}
}